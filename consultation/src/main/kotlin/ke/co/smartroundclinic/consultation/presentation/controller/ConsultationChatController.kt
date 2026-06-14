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
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.service.ConsultationChatService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSessionService
import ke.co.smartroundclinic.consultation.domain.usecase.call.EndCallUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.HandleMeetingEndedWebhookUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.call.JoinConsultationCallUseCase
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationMessageRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
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

fun Route.consultationChatController(
    chatService: ConsultationChatService,
    sessionService: ConsultationSessionService,
    joinCallUseCase: JoinConsultationCallUseCase,
    endCallUseCase: EndCallUseCase,
    meetingWebhookUseCase: HandleMeetingEndedWebhookUseCase,
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

        // GET /consultation/{id}/messages?page=1&size=50
        // Paginated message history (REST, for on-demand loading).
        route("/consultation/{id}/messages") {
            get {
                val id = call.parameters["id"]
                    ?: throw MissingParametersException("id path parameter is required")
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 50
                val result = chatService.getHistory(id, page, size)
                call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
            }
        }

        // POST /consultation/{id}/files
        // Multipart file upload. The saved FILE message is broadcast to any open
        // WebSocket clients via the MongoDB change stream — the response is the
        // single source of truth for the sender's optimistic message.
        post("/consultation/{id}/files") {
            val consultationId = call.parameters["id"]
                ?: throw MissingParametersException("id path parameter is required")
            val userId = call.getUserId() ?: return@post
            val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() ?: ""

            val session = when (val r = sessionService.repository.getById(consultationId)) {
                is Resource.Success -> r.data
                is Resource.Error -> null
            }
            if (session == null) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("message" to "Consultation not found"))
            }
            if (userId != session.doctorId && userId != session.patientId) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("message" to "Not a participant"))
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
                consultationId = consultationId,
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

        // POST /consultation/{id}/call/join
        // Returns this user's Cloudflare RealtimeKit join token. Creates the
        // meeting on first call and persists the id on the consultation.
        post("/consultation/{id}/call/join") {
            val consultationId = call.parameters["id"]
                ?: throw MissingParametersException("id path parameter is required")
            val userId = call.getUserId() ?: return@post
            val result = joinCallUseCase(consultationId, userId)
            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
        }

        // POST /consultation/{id}/call/end  (doctor only)
        // Ends the Cloudflare meeting and clears videoRoomId so the next call/join
        // provisions a fresh meeting room.
        post("/consultation/{id}/call/end") {
            val consultationId = call.parameters["id"]
                ?: throw MissingParametersException("id path parameter is required")
            val doctorId = call.getUserId() ?: return@post
            val result = endCallUseCase(consultationId, doctorId)
            call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
        }

        // WS /consultation/{id}/chat
        // Real-time text chat. Send TEXT frames only:
        // {"type":"TEXT","message":"Hello"}
        // Files go through POST /consultation/{id}/files (see above).
        webSocket("/consultation/{id}/chat") {
            val consultationId = call.parameters["id"] ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing consultation id"))
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

            // Validate participation
            val session = when (val result = sessionService.repository.getById(consultationId)) {
                is Resource.Success -> result.data
                is Resource.Error -> null
            }
            if (session == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Consultation not found"))
                return@webSocket
            }
            if (userId != session.doctorId && userId != session.patientId) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Not a participant"))
                return@webSocket
            }

            val isDoctor = userId == session.doctorId
            val recipientId = if (isDoctor) session.patientId else session.doctorId
            val recipientDestination = if (isDoctor) NotificationDestination.PATIENT else NotificationDestination.DOCTOR

            val senderName = chatService.getUserName(userId) ?: "Unknown"

            // Push recent history to the newly connected client
            chatService.getRecentHistory(consultationId).forEach { msg ->
                send(Frame.Text(json.encodeToString<ConsultationMessageRes>(msg.toModel().toRes())))
            }

            // Stream new messages via MongoDB change stream
            val watchJob = launch {
                try {
                    chatService.watchMessages(consultationId).collect { msg ->
                        val resolved = chatService.resolveEntityFiles(msg)
                        send(Frame.Text(json.encodeToString<ConsultationMessageRes>(resolved.toModel().toRes())))
                    }
                } catch (_: Exception) {
                    // change stream interrupted — client reconnects
                }
            }

            // Handle incoming TEXT frames from this client
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    try {
                        chatService.handleIncomingMessage(
                            consultationId = consultationId,
                            appointmentId = session.appointmentId,
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

            watchJob.cancel()
        }
    }
}
