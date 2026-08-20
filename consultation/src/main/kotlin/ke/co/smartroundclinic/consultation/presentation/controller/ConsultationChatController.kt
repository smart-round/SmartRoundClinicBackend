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
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.service.ConsultationChatService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSocketRegistry
import ke.co.smartroundclinic.consultation.domain.usecase.call.CancelCallInviteUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.DeclineCallInviteUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.EndThreadCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.HandleMeetingEndedWebhookUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.InviteToCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.JoinThreadCallUseCase
import ke.co.smartroundclinic.consultation.presentation.dto.request.CallActionReq
import ke.co.smartroundclinic.consultation.presentation.dto.request.InviteToCallReq
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

// encodeDefaults = true — otherwise the "type" discriminator field on PRESENCE events (and any other
// defaulted field) is omitted whenever it equals its default value, which is always in this case, so
// clients doing type-based peek dispatch see no "type" at all and can't tell events apart.
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

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
    inviteToCallUseCase: InviteToCallUseCase,
    declineCallInviteUseCase: DeclineCallInviteUseCase,
    cancelCallInviteUseCase: CancelCallInviteUseCase,
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
        // Step 1 of the direct-to-R2 upload: mint a pre-signed PUT URL.
        //
        // Preferred over the multipart route below, which proxies the whole file through this
        // process and holds it in heap before forwarding it to R2 — paying for the transfer
        // twice and putting large files at risk of exhausting the JVM. Here the bytes go
        // straight from the client to storage.
        post("/chat/{otherUserId}/files/presign") {
            val otherUserId = call.parameters["otherUserId"]
                ?: throw MissingParametersException("otherUserId path parameter is required")
            val userId = call.getUserId() ?: return@post
            val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() ?: ""
            val (doctorId, patientId) = resolvePair(userId, role, otherUserId)

            if (!appointmentRepository.existsConfirmedOrCompletedBetween(doctorId, patientId)) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("message" to "No consultation relationship with this user"))
            }

            val req = call.receive<PresignUploadReq>()
            if (req.sizeBytes <= 0) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "sizeBytes must be positive"))
            }
            if (req.sizeBytes > MAX_CHAT_FILE_BYTES) {
                return@post call.respond(
                    HttpStatusCode.PayloadTooLarge,
                    mapOf("message" to "Unable to send file as it is too large. Please try again"),
                )
            }

            when (val result = chatService.presignFileUpload(doctorId, patientId, req.fileName, req.contentType)) {
                is Resource.Success -> {
                    val data = result.data!!
                    call.respond(
                        HttpStatusCode.OK,
                        DefaultResponse(
                            httpStatusCode = HttpStatusCode.OK.value,
                            message = "Upload URL generated",
                            status = true,
                            data = PresignUploadRes(
                                messageId = data.messageId,
                                key = data.key,
                                uploadUrl = data.uploadUrl,
                                contentType = data.contentType,
                            ),
                        ),
                    )
                }
                is Resource.Error -> call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("message" to (result.message ?: "Failed to prepare upload")),
                )
            }
        }

        // Step 2: record the message once the client's PUT to R2 has succeeded.
        post("/chat/{otherUserId}/files/complete") {
            val otherUserId = call.parameters["otherUserId"]
                ?: throw MissingParametersException("otherUserId path parameter is required")
            val userId = call.getUserId() ?: return@post
            val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() ?: ""
            val (doctorId, patientId) = resolvePair(userId, role, otherUserId)

            if (!appointmentRepository.existsConfirmedOrCompletedBetween(doctorId, patientId)) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("message" to "No consultation relationship with this user"))
            }

            val req = call.receive<CompleteUploadReq>()
            val senderName = chatService.getUserName(userId) ?: "Unknown"
            val result = chatService.completeFileUpload(
                doctorId = doctorId,
                patientId = patientId,
                senderId = userId,
                senderRole = role,
                senderName = senderName,
                messageId = req.messageId,
                key = req.key,
                fileName = req.fileName,
                contentType = req.contentType,
                sizeBytes = req.sizeBytes,
            )
            val response = result.toDefaultResponse(
                successStatusCode = HttpStatusCode.Created.value,
                failedStatusCode = HttpStatusCode.BadRequest.value,
            ) { it?.toModel()?.toRes() }
            call.respond(HttpStatusCode.fromValue(response.httpStatusCode), response)
        }

        // Legacy multipart file upload — kept so older clients keep working. The saved FILE
        // message is broadcast to any open WebSocket clients via the MongoDB change stream.
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

            if (!appointmentRepository.hasJoinableConfirmedAppointment(doctorId, patientId)) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("message" to "No active appointment ready for a call right now"))
            }

            val req = call.receive<CallActionReq>()
            val result = joinCallUseCase(doctorId, patientId, userId, req.callId)
            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
        }

        // POST /chat/{otherUserId}/call/invite
        // Rings the other party (WhatsApp-style) without touching the RealtimeKit meeting —
        // if they accept, their client calls /call/join as normal, which signals CALL_ANSWERED
        // back to this caller so it can also join.
        post("/chat/{otherUserId}/call/invite") {
            val otherUserId = call.parameters["otherUserId"]
                ?: throw MissingParametersException("otherUserId path parameter is required")
            val userId = call.getUserId() ?: return@post
            val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() ?: ""
            val (doctorId, patientId) = resolvePair(userId, role, otherUserId)

            if (!appointmentRepository.hasJoinableConfirmedAppointment(doctorId, patientId)) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("message" to "No active appointment ready for a call right now"))
            }

            val req = runCatching { call.receive<InviteToCallReq>() }.getOrDefault(InviteToCallReq())
            val result = inviteToCallUseCase(doctorId, patientId, userId, req.isVideo)
            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
        }

        // POST /chat/{otherUserId}/call/decline  (callee only)
        post("/chat/{otherUserId}/call/decline") {
            val userId = call.getUserId() ?: return@post
            val req = call.receive<CallActionReq>()
            val result = declineCallInviteUseCase(req.callId, userId)
            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
        }

        // POST /chat/{otherUserId}/call/cancel  (caller only — hung up before the callee answered)
        post("/chat/{otherUserId}/call/cancel") {
            val userId = call.getUserId() ?: return@post
            val req = call.receive<CallActionReq>()
            val result = cancelCallInviteUseCase(req.callId, userId)
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

/**
 * Server-side ceiling for chat attachments, mirrored by the apps so they can reject early.
 * Decimal MB deliberately — file managers report sizes in decimal, so a binary-MiB limit lets
 * a file the user calls "26 MB" slip under a cap labelled "25 MB".
 */
private const val MAX_CHAT_FILE_BYTES = 300L * 1_000_000

@Serializable
private data class PresignUploadReq(
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
)

@Serializable
private data class PresignUploadRes(
    val messageId: String,
    val key: String,
    val uploadUrl: String,
    val contentType: String,
)

@Serializable
private data class CompleteUploadReq(
    val messageId: String,
    val key: String,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
)
