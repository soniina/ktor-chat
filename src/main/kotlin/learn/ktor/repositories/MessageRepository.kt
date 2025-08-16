package learn.ktor.repositories

import learn.ktor.model.Message
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class MessageRepository {

    suspend fun saveMessage(senderId: Int, recipientId: Int, content: String): Message = newSuspendedTransaction {
        val timestamp = System.currentTimeMillis()
        val id = Messages.insert {
            it[sender] = senderId
            it[recipient] = recipientId
            it[Messages.content] = content
            it[Messages.timestamp] = timestamp
        } get Messages.id

        Message(id, senderId, recipientId, content, timestamp)
    }

    suspend fun getMessagesBetween(user1Id: Int, user2Id: Int): List<Message> = newSuspendedTransaction {
        Messages.selectAll().where {
            (Messages.sender eq user1Id and (Messages.recipient eq user2Id)) or
                    (Messages.sender eq user2Id and (Messages.recipient eq user1Id))
        }.orderBy(Messages.timestamp to SortOrder.ASC)
            .map {
                Message(
                    id = it[Messages.id],
                    senderId = it[Messages.sender],
                    recipientId = it[Messages.recipient],
                    content = it[Messages.content],
                    timestamp = it[Messages.timestamp],
                    delivered = it[Messages.delivered]
                )
            }
    }

    suspend fun getUndeliveredMessagesFor(userId: Int): List<Message> = newSuspendedTransaction {
        Messages.selectAll().where {
            (Messages.recipient eq userId) and (Messages.delivered eq false)
        }.orderBy(Messages.timestamp to SortOrder.ASC)
            .map {
                Message(
                    id = it[Messages.id],
                    senderId = it[Messages.sender],
                    recipientId = it[Messages.recipient],
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
