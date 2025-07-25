package learn.ktor.repositories

import learn.ktor.model.Message
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class MessageRepository {

    suspend fun saveMessage(sender: String, recipient: String, content: String): Message = newSuspendedTransaction {
        val timestamp = System.currentTimeMillis()
        val id = Messages.insert {
            it[Messages.sender] = sender
            it[Messages.recipient] = recipient
            it[Messages.content] = content
            it[Messages.timestamp] = timestamp
        } get Messages.id

        Message(id, sender, recipient, content, timestamp)
    }

    suspend fun getMessagesBetween(user1: String, user2: String): List<Message> = newSuspendedTransaction {
        Messages.selectAll().where {
            (Messages.sender eq user1 and (Messages.recipient eq user2)) or
                    (Messages.sender eq user2 and (Messages.recipient eq user1))
        }.orderBy(Messages.timestamp to SortOrder.ASC)
            .map {
                Message(
                    id = it[Messages.id],
                    sender = it[Messages.sender],
                    recipient = it[Messages.recipient],
                    content = it[Messages.content],
                    timestamp = it[Messages.timestamp],
                    delivered = it[Messages.delivered]
                )
            }
    }

    suspend fun getUndeliveredMessagesFor(username: String): List<Message> = newSuspendedTransaction {
        Messages.selectAll().where {
            (Messages.recipient eq username) and (Messages.delivered eq false)
        }.orderBy(Messages.timestamp to SortOrder.ASC)
            .map {
                Message(
                    id = it[Messages.id],
                    sender = it[Messages.sender],
                    recipient = it[Messages.recipient],
                    content = it[Messages.content],
                    timestamp = it[Messages.timestamp]
                )
            }
    }

    suspend fun markAsDelivered(id: Int): Unit = newSuspendedTransaction {
        Messages.update({ Messages.id eq id }) {
            it[delivered] = true
        }
    }

}