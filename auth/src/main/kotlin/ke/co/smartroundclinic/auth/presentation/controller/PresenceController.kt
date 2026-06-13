package ke.co.smartroundclinic.auth.presentation.controller

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import ke.co.smartroundclinic.auth.domain.usecase.TrackUserPresenceUseCase
import ke.co.smartroundclinic.auth.presentation.dto.request.PresencePingReq
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val json = Json { ignoreUnknownKeys = true }

fun Route.presenceController(trackPresence: TrackUserPresenceUseCase) {
    authenticate("auth-jwt") {
        // WS /auth/user/presence
        // Client sends {"isActive": true|false} every ~30 s.
        // Server writes presence:{userId} = "true"|"false" in Redis with a 35 s TTL.
        // On clean disconnect the key is removed immediately.
        webSocket("/auth/user/presence") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asString()
                ?: run {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                    return@webSocket
                }

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue

                    val ping = runCatching {
                        json.decodeFromString<PresencePingReq>(frame.readText())
                    }.getOrNull() ?: continue

                    trackPresence(userId, ping.isActive)

                    val ack = buildJsonObject {
                        put("userId", userId)
                        put("isActive", ping.isActive)
                    }
                    send(Frame.Text(json.encodeToString(ack)))
                }
            } finally {
                // Graceful or unexpected disconnect — clear presence immediately
                // so others don't see a stale "online" state for up to 35 s.
                trackPresence.clear(userId)
            }
        }
    }
}
