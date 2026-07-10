package ke.co.smartroundclinic.consultation.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.utils.io.toByteArray
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.consultation.domain.service.ConsultationChatService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry
import ke.co.smartroundclinic.consultation.domain.usecase.call.EndThreadCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.HandleMeetingEndedWebhookUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.JoinThreadCallUseCase
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationMessageRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationPresenceEventRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

// Cloudflare RealtimeKit webhook payload (actual format observed from dashboard)
// Events: meeting.started | meeting.ended | meeting.participantJoined | meeting.participantLeft
@Serializable
private data class CloudflareWebhookPayload(
    val event: String = "",
    val reason: String? = null,
    val meeting: CloudflareWebhookMeeting? = null,
)

@Serializable
private data class CloudflareWebhookMeeting(
    val id: String,
    val title: String? = null,
    val status: String? = null,
)

/** (doctorId, patientId), resolved from the caller's own id/role plus the other party's id. */
private fun resolvePair(callerId: String, role: String, otherUserId: String): Pair<String, String> =
    if (role.equals("DOCTOR", ignoreCase = true)) callerId to otherUserId else otherUserId to callerId

fun Route.consultationChatController(
    chatService: ConsultationChatService,
    appointmentRepository: AppointmentRepository,
    joinCallUseCase: JoinThreadCallUseCase,
    endCallUseCase: EndThreadCallUseCase,
    meetingWebhookUseCase: HandleMeetingEndedWebhookUseCase,
    socketRegistry: ConsultationSocketRegistry,
) {
    // POST /webhooks/cloudflare/realtime-kit  (unauthenticated — called by Cloudflare)
    // Configure this URL in the Cloudflare RealtimeKit dashboard under Webhooks.
    post("/webhooks/cloudflare/realtime-kit") {
        val payload = runCatching { call.receive<CloudflareWebhookPayload>() }.getOrNull()
        val meetingId = payload?.meeting?.id

        if (meetingId != null) {
            when (payload.event) {
                // Meeting was explicitly ended (doctor pressed End) — clean up immediately
                "meeting.ended" -> meetingWebhookUseCase(meetingId, forceCleanup = true)
                // A participant left — clean up only if the meeting is now empty (INACTIVE)
                "meeting.participantLeft" -> meetingWebhookUseCase(meetingId, forceCleanup = false)
                // Other events (started, participantJoined) — nothing to do
            }
        }

        // Always 200 so Cloudflare doesn't keep retrying
        call.respond(HttpStatusCode.OK)
    }

    authenticate("auth-jwt") {

        // POST /chat/{otherUserId}/files
        // Multipart file upload. The saved FILE message is broadcast to any open
        // WebSocket clients via the MongoDB change stream — the response is the
        // single source of truth for the sender's optimistic message.
        post("/chat/{otherUserId}/files") {
            val otherUserId = call.parameters["otherUserId"]
                ?: throw MissingParametersException("otherUserId path parameter is required")
            val userId = call.getUserId() ?: return@post
            val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() ?: ""
            val (doctorId, patientId) = resolvePair(userId, role, otherUserId)

            if (!appointmentRepository.existsConfirmedOrCompletedBetween(doctorId, patientId)) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("message" to "No consultation relationship with this user"))
            }

            var fileBytes: ByteArray? = null
            var fileName = "file"
            var contentType = "application/octet-stream"
            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        fileName = part.originalFileName?.ifBlank { null } ?: fileName
                        contentType = part.contentType?.toString()?.ifBlank { null } ?: contentType
                        fileBytes = part.provider().toByteArray()
                    }
                    is PartData.FormItem -> {
                        // Accept optional overrides if the client sends them
                        when (part.name) {
                            "fileName" -> fileName = part.value.ifBlank { fileName }
                            "contentType" -> contentType = part.value.ifBlank { contentType }
                        }
                    }
                    else -> Unit
                }
                part.dispose()
            }

            val bytes = fileBytes
            if (bytes == null || bytes.isEmpty()) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "file part is required"))
            }

            val senderName = chatService.getUserName(userId) ?: "Unknown"
            val result = chatService.uploadFile(
                doctorId = doctorId,
                patientId = patientId,
                senderId = userId,
                senderRole = role,
                senderName = senderName,
                fileName = fileName,
                contentType = contentType,
                bytes = bytes,
            )
            val response = result.toDefaultResponse(
                successStatusCode = HttpStatusCode.Created.value,
                failedStatusCode = HttpStatusCode.InternalServerError.value,
            ) { it?.toModel()?.toRes() }
            call.respond(HttpStatusCode.fromValue(response.httpStatusCode), response)
        }

        // POST /chat/{otherUserId}/call/join
        // Returns this user's Cloudflare RealtimeKit join token. Creates the
        // meeting on first call and persists the id on the permanent thread.
        post("/chat/{otherUserId}/call/join") {
            val otherUserId = call.parameters["otherUserId"]
                ?: throw MissingParametersException("otherUserId path parameter is required")
            val userId = call.getUserId() ?: return@post
            val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() ?: ""
            val (doctorId, patientId) = resolvePair(userId, role, otherUserId)

            if (!appointmentRepository.existsConfirmedOrCompletedBetween(doctorId, patientId)) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("message" to "No consultation relationship with this user"))
            }

            val result = joinCallUseCase(doctorId, patientId, userId)
            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
        }

        // POST /chat/{otherUserId}/call/end  (doctor only)
        // Ends the Cloudflare meeting and clears videoRoomId so the next call/join
        // provisions a fresh meeting room.
        post("/chat/{otherUserId}/call/end") {
            val otherUserId = call.parameters["otherUserId"]
                ?: throw MissingParametersException("otherUserId path parameter is required")
            val doctorId = call.getUserId() ?: return@post
            val result = endCallUseCase(doctorId, otherUserId, doctorId)
            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
        }

        // WS /chat/{otherUserId}
        // Real-time text chat over the permanent (doctorId, patientId) thread. Send TEXT frames only:
        // {"type":"TEXT","message":"Hello"}
        // Files go through POST /chat/{otherUserId}/files (see above).
        webSocket("/chat/{otherUserId}") {
            val otherUserId = call.parameters["otherUserId"] ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing otherUserId"))
                return@webSocket
            }

            val principal = call.principal<JWTPrincipal>() ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }
            val userId = principal.payload.getClaim("userId")?.asString() ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }
            val role = principal.payload.getClaim("role")?.asString() ?: ""
            val (doctorId, patientId) = resolvePair(userId, role, otherUserId)

            if (!appointmentRepository.existsConfirmedOrCompletedBetween(doctorId, patientId)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No consultation relationship with this user"))
                return@webSocket
            }

            val isDoctor = userId == doctorId
            val recipientId = if (isDoctor) patientId else doctorId
            val recipientDestination = if (isDoctor) NotificationDestination.PATIENT else NotificationDestination.DOCTOR

            val senderName = chatService.getUserName(userId) ?: "Unknown"
            val threadKey = ConsultationSocketRegistry.threadKey(doctorId, patientId)

            socketRegistry.register(threadKey, userId, this)
            var watchJob: Job? = null
            var presenceJob: Job? = null
            try {
                // Stream new messages via MongoDB change stream. The initial history load is the
                // client's own REST fetch (GET /consultation/threads/{doctorId}/{patientId}/messages) —
                // no separate "recent history" push here, so there's nothing to race against it.
                watchJob = launch {
                    try {
                        chatService.watchMessagesForThread(doctorId, patientId).collect { msg ->
                            val resolved = chatService.resolveEntityFiles(msg)
                            send(Frame.Text(json.encodeToString<ConsultationMessageRes>(resolved.toModel().toRes())))
                        }
                    } catch (_: Exception) {
                        // change stream interrupted — client reconnects
                    }
                }

                // Poll the counterpart's global presence and relay changes over this socket — presence
                // itself is tracked by the separate /auth/user/presence heartbeat, this just surfaces it here.
                presenceJob = launch {
                    var lastKnownOnline: Boolean? = null
                    while (isActive) {
                        try {
                            val online = chatService.isOnline(recipientId)
                            if (online != lastKnownOnline) {
                                lastKnownOnline = online
                                val lastSeenAt = if (!online) chatService.getLastSeenAt(recipientId) else null
                                send(Frame.Text(json.encodeToString<ConsultationPresenceEventRes>(
                                    ConsultationPresenceEventRes(userId = recipientId, isOnline = online, lastSeenAt = lastSeenAt)
                                )))
                            }
                        } catch (_: Exception) {
                            // transient Redis/Mongo hiccup — try again next tick
                        }
                        delay(9_000L)
                    }
                }

                // Handle incoming TEXT/TYPING/READ frames from this client
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        try {
                            chatService.handleIncomingMessage(
                                doctorId = doctorId,
                                patientId = patientId,
                                senderId = userId,
                                senderRole = role,
                                senderName = senderName,
                                rawJson = frame.readText(),
                                recipientId = recipientId,
                                recipientDestination = recipientDestination,
                            )
                        } catch (_: Exception) {
                            send(Frame.Text("""{"error":"Failed to process message"}"""))
                        }
                    }
                }
            } finally {
                watchJob?.cancel()
                presenceJob?.cancel()
                socketRegistry.unregister(threadKey, userId, this)
            }
        }
    }
}
