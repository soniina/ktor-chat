package learn.ktor.services

import io.ktor.websocket.*
import learn.ktor.connection.ConnectionManager
import learn.ktor.model.ChatEvent
import learn.ktor.repositories.MessageRepository
import learn.ktor.repositories.UserRepository

class ChatService(private val connectionManager: ConnectionManager, private val messageRepository: MessageRepository,
                  private val commandHandler: CommandHandler, private val userRepository: UserRepository) {

    private suspend fun notifyUser(user: String, event: ChatEvent) {
        connectionManager.sendTo(user, event)
    }

    private suspend fun sendMessage(sender: String, recipient: String, messageContent: String): Boolean {
        return if (connectionManager.isOnline(recipient)) {
            connectionManager.sendTo(recipient, ChatEvent.UserMessage(sender, messageContent))
            true
        } else false
    }

    suspend fun handleConnection(username: String, session: DefaultWebSocketSession) {
        connectionManager.register(username, session)

        notifyUser(username, ChatEvent.SystemMessage("Welcome, $username! You are now connected."))

        val userId = userRepository.getIdByUsername(username) ?: return notifyUser(username, ChatEvent.ErrorMessage("Internal error: user not found"))

        val undeliveredMessages = messageRepository.getUndeliveredMessagesFor(userId)
        undeliveredMessages.forEach {
            if (sendMessage(userRepository.getUsernameById(it.senderId) ?: "unknown", username, it.content)) messageRepository.markAsDelivered(it.id)
        }
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

    private suspend fun handleDirectMessage(username: String, text: String) {
        val parts = text.split(" ", limit = 2)
        if (parts.size < 2) {
            notifyUser(username, ChatEvent.ErrorMessage("Invalid message format. Use '@username message'"))
            return
        }

        val recipient = parts[0].substring(1)

        val messageText = parts[1]
        if (messageText.isBlank()) {
            notifyUser(username, ChatEvent.ErrorMessage("Message must not be empty"))
            return
        }

        val senderId = userRepository.getIdByUsername(username) ?: return notifyUser(username, ChatEvent.ErrorMessage("Internal error: user not found"))
        val recipientId = userRepository.getIdByUsername(recipient)

        if (recipientId == null) {
            notifyUser(username, ChatEvent.ErrorMessage("Unknown user: $recipient"))
            return
        }

        val message = try {
            messageRepository.saveMessage(senderId, recipientId, messageText)
        } catch (e: Exception) {
            notifyUser(username, ChatEvent.ErrorMessage("Failed to save message. Please try again later."))
            return
        }

        notifyUser(username, if (sendMessage(username, recipient, message.content)) {
            ChatEvent.CommandResult("sent", "to $recipient")
        } else {
            ChatEvent.CommandResult("queued", "to $recipient (offline — will be delivered later)")
        })
    }

    suspend fun cleanUp(user: String) = connectionManager.unregister(user)

}