package ke.co.smartroundclinic.consultation.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.domain.service.ConsultationChatService
import ke.co.smartroundclinic.consultation.domain.service.ConsultationSessionService
import ke.co.smartroundclinic.consultation.presentation.dto.response.ConsultationMessageRes
import ke.co.smartroundclinic.consultation.presentation.dto.response.toRes
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun Route.consultationChatController(
    chatService: ConsultationChatService,
    sessionService: ConsultationSessionService,
) {
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

        // WS /consultation/{id}/chat
        // Real-time bidirectional chat. Only the doctor and patient of the consultation may connect.
        // Send TEXT: {"type":"TEXT","message":"Hello"}
        // Send FILE: {"type":"FILE","fileName":"report.pdf","contentType":"application/pdf","data":"<base64>"}
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

            val senderName = chatService.getUserName(userId) ?: "Unknown"

            // Push recent history to the newly connected client
            chatService.getRecentHistory(consultationId).forEach { msg ->
                send(Frame.Text(json.encodeToString<ConsultationMessageRes>(msg.toModel().toRes())))
            }

            // Stream new messages via MongoDB change stream
            val watchJob = launch {
                try {
                    chatService.watchMessages(consultationId).collect { msg ->
                        send(Frame.Text(json.encodeToString<ConsultationMessageRes>(msg.toModel().toRes())))
                    }
                } catch (_: Exception) {
                    // change stream interrupted — client reconnects
                }
            }

            // Handle incoming frames from this client
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    try {
                        chatService.handleIncomingMessage(
                            consultationId = consultationId,
                            senderId = userId,
                            senderRole = role,
                            senderName = senderName,
                            rawJson = frame.readText(),
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
