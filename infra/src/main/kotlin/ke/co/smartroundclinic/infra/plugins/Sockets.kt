package ke.co.smartroundclinic.infra.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets() {
    install(WebSockets) {
        // 15s/15s was far too tight for mobile clients — a brief cellular handoff, weak signal, or
        // the OS deprioritizing background network I/O for a few seconds is enough to miss a pong,
        // and the server would force-close a connection that wasn't actually dead. Both apps' own
        // client-side keep-alive pings are already on a 25s interval, so pingPeriod/timeout need
        // enough room above that to tolerate real-world mobile network variance without spuriously
        // killing live connections (which is what was showing up as constant reconnect churn).
        pingPeriod = 30.seconds
        timeout = 60.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/ws") { // websocketSession
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    outgoing.send(Frame.Text("YOU SAID: $text"))
                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                }
            }
        }
    }
}
