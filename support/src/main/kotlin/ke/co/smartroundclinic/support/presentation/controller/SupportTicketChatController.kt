package ke.co.smartroundclinic.support.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.toByteArray
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.infra.plugins.MissingParametersException
import ke.co.smartroundclinic.infra.plugins.getUserId
import ke.co.smartroundclinic.infra.plugins.requireRole
import ke.co.smartroundclinic.support.domain.service.SupportTicketChatService
import ke.co.smartroundclinic.support.presentation.dto.response.SupportTicketChatRes
import ke.co.smartroundclinic.support.presentation.dto.response.toRes
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private const val ADMIN = "ADMIN"
private const val DOCTOR = "DOCTOR"
private const val PATIENT = "PATIENT"

fun Route.supportTicketChatController(service: SupportTicketChatService) {

    authenticate("auth-jwt") {
        // ── REST: file upload ─────────────────────────────────────────────────
        route("/support/tickets/{ticketId}/chat") {
            get {
                call.requireRole(ADMIN, DOCTOR, PATIENT) {
                    val ticketId = call.parameters["ticketId"]
                        ?: throw MissingParametersException("ticketId is required")
                    val result = service.getChatHistory(ticketId)
                    call.respond(HttpStatusCode.fromValue(result.httpStatusCode), result)
                }
            }

            post("files") {
                call.requireRole(ADMIN, DOCTOR, PATIENT) {
                    val ticketId = call.parameters["ticketId"]
                        ?: throw MissingParametersException("ticketId is required")
                    val userId = call.getUserId() ?: return@requireRole
                    val senderName = service.getUserName(userId) ?: "Unknown"

                    var response: DefaultResponse<SupportTicketChatRes?> =
                        DefaultResponse(400, false, "No file received")

                    call.receiveMultipart().forEachPart { part ->
                        if (part is PartData.FileItem) {
                            val fileName = part.originalFileName ?: "file"
                            val contentType = part.contentType?.toString() ?: "application/octet-stream"
                            val bytes = part.provider().toByteArray()
                            response = service.uploadFile(ticketId, userId, senderName, fileName, contentType, bytes)
                            part.dispose()
                        }
                    }

                    call.respond(HttpStatusCode.fromValue(response.httpStatusCode), response)
                }
            }
        }

        // ── WebSocket: text messages only ─────────────────────────────────────
        webSocket("/support/tickets/{ticketId}/chat") {
            val ticketId = call.parameters["ticketId"] ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing ticketId"))
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

            val senderName = service.getUserName(userId) ?: "Unknown"

            // Push history to newly connected client
            service.getHistory(ticketId).forEach { msg ->
                send(Frame.Text(json.encodeToString<SupportTicketChatRes>(msg.toModel().toRes())))
            }

            // Stream new messages via MongoDB change stream
            val watchJob = launch {
                try {
                    service.watchMessages(ticketId).collect { msg ->
                        send(Frame.Text(json.encodeToString<SupportTicketChatRes>(msg.toModel().toRes())))
                    }
                } catch (_: Exception) { }
            }

            // Handle incoming text frames (text only — files go through REST)
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    try {
                        service.handleIncomingMessage(ticketId, userId, senderName, frame.readText())
                    } catch (_: Exception) {
                        send(Frame.Text("""{"error":"Failed to process message"}"""))
                    }
                }
            }

            watchJob.cancel()
        }
    }
}
