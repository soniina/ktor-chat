package learn.ktor.services

import io.ktor.websocket.*
import learn.ktor.connection.ConnectionManager
import learn.ktor.model.ChatEvent
import learn.ktor.repositories.MessageRepository

class ChatService(private val connectionManager: ConnectionManager, private val messageRepository: MessageRepository, private val commandHandler: CommandHandler) {

    private suspend fun notifyUser(user: String, event: ChatEvent) {
        connectionManager.sendTo(user, event)
    }

    private suspend fun sendMessage(sender: String, recipient: String, message: String): Boolean {
        return if (connectionManager.isOnline(recipient)) {
            messageRepository.saveMessage(sender, recipient, message)
            connectionManager.sendTo(recipient, ChatEvent.UserMessage(sender, message))
            true
        } else false
    }

    suspend fun handleConnection(user: String, session: DefaultWebSocketSession) {
        connectionManager.register(user, session)
        notifyUser(user, ChatEvent.SystemMessage("Welcome, $user! You are now connected."))
    }

    suspend fun handleMessage(user: String, message: String) {
        when {
            message.startsWith("/") -> handleCommand(user, message)
            message.startsWith("@") -> handleDirectMessage(user, message)
            else -> notifyUser(user, ChatEvent.ErrorMessage("Unrecognized input"))
        }
    }

    private suspend fun handleCommand(user: String, text: String) {
        val event = commandHandler.handle(user, text)
        notifyUser(user, event)

        if (event is ChatEvent.CloseConnection)
            connectionManager.getSession(user)?.close(CloseReason(CloseReason.Codes.NORMAL, "User left"))
    }

    private suspend fun handleDirectMessage(user: String, text: String) {
        val parts = text.split(" ", limit = 2)
        if (parts.size < 2) {
            notifyUser(user, ChatEvent.ErrorMessage("Invalid message format. Use '@username message'"))
            return
        }

        val recipient = parts[0].substring(1)
        val message = parts[1]
        if (message.isBlank()) {
            notifyUser(user, ChatEvent.ErrorMessage("Message must not be empty"))
            return
        }

        if (sendMessage(user, recipient, message)) {
            notifyUser(user, ChatEvent.CommandResult("sent", "to $recipient"))
        } else {
            notifyUser(user, ChatEvent.ErrorMessage("User $recipient offline"))
        }
    }

    suspend fun cleanUp(user: String) = connectionManager.unregister(user)

}