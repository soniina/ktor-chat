package learn.ktor.routes

import application.chat.CommandHandler
import learn.ktor.connection.ConnectionManager
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import learn.ktor.application.chat.MessageService
import model.ChatEvent

import kotlin.time.Duration.Companion.seconds

val messageService = MessageService()
val commandHandler = CommandHandler()

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
                messageService.notifyUser(user, ChatEvent.SystemMessage("Welcome, $user! You are now connected."))

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()

                        if (text.startsWith("/")) {
                            val event = commandHandler.handle(user, text)
                            messageService.notifyUser(user, event)
                            if (event is ChatEvent.CloseConnection) close(CloseReason(CloseReason.Codes.NORMAL, "User left"))
                        } else if (text.startsWith("@")) {
                            val parts = text.split(" ", limit = 2)
                            if (parts.size == 2) {
                                val recipient = parts[0].substring(1)
                                val message = parts[1]

                                launch {
                                    if (messageService.sendMessage(user, recipient, message))
                                        messageService.notifyUser(user, ChatEvent.CommandResult("send", "Message sent to $recipient"))
                                    else messageService.notifyUser(user, ChatEvent.SystemMessage("User $recipient is not connected"))
                                }
                            } else {
                                messageService.notifyUser(user, ChatEvent.ErrorMessage("Invalid message format. Use '@username message'"))
                            }
                        } else {
                            messageService.notifyUser(user, ChatEvent.ErrorMessage("Unrecognized input"))
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