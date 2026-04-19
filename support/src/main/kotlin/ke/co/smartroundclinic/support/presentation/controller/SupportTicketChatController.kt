package ke.co.smartroundclinic.support.presentation.controller

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import ke.co.smartroundclinic.support.domain.service.SupportTicketChatService
import ke.co.smartroundclinic.support.presentation.dto.response.SupportTicketChatRes
import ke.co.smartroundclinic.support.presentation.dto.response.toRes
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun Route.supportTicketChatController(service: SupportTicketChatService) {
    authenticate("auth-jwt") {
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

           /* if (principal.payload.getClaim("role")?.asString() != "ADMIN") {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Admins only"))
                return@webSocket
            }*/

            val senderName = service.getUserName(userId) ?: "Unknown"

            // Push chat history to the newly connected client
            service.getHistory(ticketId).forEach { msg ->
                send(Frame.Text(json.encodeToString<SupportTicketChatRes>(msg.toModel().toRes())))
            }

            // Stream new messages from the database change stream.
            // Each replica independently watches the same collection — no shared in-memory state.
            val watchJob = launch {
                try {
                    service.watchMessages(ticketId).collect { msg ->
                        send(Frame.Text(json.encodeToString<SupportTicketChatRes>(msg.toModel().toRes())))
                    }
                } catch (_: Exception) {
                    // change stream interrupted — client will reconnect
                }
            }

            // Handle frames sent by this client
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
