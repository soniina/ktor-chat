package learn.ktor.routes

import learn.ktor.connection.ConnectionManager
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlin.time.Duration.Companion.seconds


fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/chat") {
            val user = call.parameters["user"] ?: return@webSocket
            try {
                ConnectionManager.register(user, this)
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val message = frame.readText()
                        ConnectionManager.sendMessage(user, "YOU SAID: $message")

                        if (message.equals("bye", ignoreCase = true)) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        }
                    }
                }
            } catch (e: Exception) {
                println("WebSocket error: ${e.message}")
            } finally {
                ConnectionManager.unregister(user)
                println("Connection closed")
            }
        }
    }
}
