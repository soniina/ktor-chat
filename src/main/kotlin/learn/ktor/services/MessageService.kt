package learn.ktor.services

import learn.ktor.connection.ConnectionManager
import learn.ktor.model.ChatEvent

class MessageService {

    suspend fun sendMessage(sender: String, recipient: String, message: String): Boolean {
        return if (ConnectionManager.isOnline(recipient)) {
            ConnectionManager.sendTo(recipient, ChatEvent.UserMessage(sender, message))
            true
        } else {
            false
        }
    }

    suspend fun notifyUser(user: String, event: ChatEvent) {
        ConnectionManager.sendTo(user, event)
    }

}