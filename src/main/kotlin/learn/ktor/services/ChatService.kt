package learn.ktor.services

import io.ktor.websocket.*
import learn.ktor.connection.ConnectionManager
import learn.ktor.model.ChatEvent
import learn.ktor.model.Message
import learn.ktor.repositories.MessageRepository

class ChatService(private val connectionManager: ConnectionManager, private val messageRepository: MessageRepository, private val commandHandler: CommandHandler) {

    private suspend fun notifyUser(user: String, event: ChatEvent) {
        connectionManager.sendTo(user, event)
    }

    private suspend fun sendMessage(message: Message): Boolean {
        return if (connectionManager.isOnline(message.recipient)) {
            connectionManager.sendTo(message.recipient, ChatEvent.UserMessage(message.sender, message.content))
            true
        } else false
    }

    suspend fun handleConnection(user: String, session: DefaultWebSocketSession) {
        connectionManager.register(user, session)

        notifyUser(user, ChatEvent.SystemMessage("Welcome, $user! You are now connected."))

        val undeliveredMessages = messageRepository.getUndeliveredMessagesFor(user)
        undeliveredMessages.forEach {
            if (sendMessage(it)) messageRepository.markAsDelivered(it.id)
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

    private suspend fun handleDirectMessage(user: String, text: String) {
        val parts = text.split(" ", limit = 2)
        if (parts.size < 2) {
            notifyUser(user, ChatEvent.ErrorMessage("Invalid message format. Use '@username message'"))
            return
        }

        val recipient = parts[0].substring(1)
        val messageText = parts[1]
        if (messageText.isBlank()) {
            notifyUser(user, ChatEvent.ErrorMessage("Message must not be empty"))
            return
        }

        val message = try {
            messageRepository.saveMessage(user, recipient, messageText)
        } catch (e: Exception) {
            notifyUser(user, ChatEvent.ErrorMessage("Failed to save message. Please try again later."))
            return
        }

        notifyUser(user, if (sendMessage(message)) {
            ChatEvent.CommandResult("sent", "to $recipient")
        } else {
            ChatEvent.CommandResult("queued", "to $recipient (offline — will be delivered later)")
        })
    }

    suspend fun cleanUp(user: String) = connectionManager.unregister(user)

}