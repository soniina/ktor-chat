package learn.ktor.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import learn.ktor.services.TokenService
import learn.ktor.services.ChatService

import kotlin.time.Duration.Companion.seconds

fun Application.configureWebSockets(tokenService: TokenService, chatService: ChatService) {

    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/chat") {
            val token = call.request.queryParameters["token"] ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing token"))
                return@webSocket
            }

            val user = tokenService.verifyToken(token) ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }

            try {
                chatService.handleConnection(user, this)

                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    chatService.handleMessage(user, frame.readText())
                }
            } finally {
                chatService.cleanUp(user)
            }
        }
    }
}

