package learn.ktor.routes

import learn.ktor.services.CommandHandler
import learn.ktor.config.JwtConfig
import com.auth0.jwt.JWT
import learn.ktor.connection.ConnectionManager
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import learn.ktor.services.MessageService
import learn.ktor.model.ChatEvent

import kotlin.time.Duration.Companion.seconds

val messageService = MessageService()
val commandHandler = CommandHandler()

fun Application.configureWebSockets() {
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

            val verifier = JWT
                .require(JwtConfig.algorithm)
                .withIssuer("ktor-chat")
                .withAudience("chat-users")
                .build()

            val principal = try {
                verifier.verify(token)
            } catch (e: Exception) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }

            val user = principal.getClaim("user").asString()
            if (user.isNullOrBlank()) {
                close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Invalid user claim"))
                return@webSocket
            }

            try {
                ConnectionManager.register(user, this)
                messageService.notifyUser(user, ChatEvent.SystemMessage("Welcome, $user! You are now connected."))

                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    handleFrame(user, frame.readText())
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

private suspend fun DefaultWebSocketServerSession.handleFrame(user: String, text: String) {
    when {
        text.startsWith("/") -> handleCommand(user, text)
        text.startsWith("@") -> handleDirectMessage(user, text)
        else -> sendErrorMessage(user)
    }
}

private suspend fun DefaultWebSocketServerSession.handleCommand(user: String, text: String) {
    val event = commandHandler.handle(user, text)
    messageService.notifyUser(user, event)

    if (event is ChatEvent.CloseConnection) {
        close(CloseReason(CloseReason.Codes.NORMAL, "User left"))
    }
}

private suspend fun DefaultWebSocketServerSession.handleDirectMessage(user: String, text: String) {
    val parts = text.split(" ", limit = 2)
    if (parts.size < 2) {
        messageService.notifyUser(user, ChatEvent.ErrorMessage("Invalid message format. Use '@username message'"))
        return
    }

    val recipient = parts[0].substring(1)
    val message = parts[1]

    launch {
        if (messageService.sendMessage(user, recipient, message)) {
            messageService.notifyUser(user, ChatEvent.CommandResult("sent", "To $recipient"))
        } else {
            messageService.notifyUser(user, ChatEvent.ErrorMessage("User $recipient offline"))
        }
    }
}

private suspend fun DefaultWebSocketServerSession.sendErrorMessage(user: String) {
    messageService.notifyUser(user, ChatEvent.ErrorMessage("Unrecognized input"))
}
