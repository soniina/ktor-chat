package learn.ktor.repositories

import learn.ktor.model.Message
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class MessageRepository {

    suspend fun saveMessage(sender: String, recipient: String, content: String): Message? = newSuspendedTransaction {
        try {
            val timestamp = System.currentTimeMillis()
            val id = Messages.insert {
                it[Messages.sender] = sender
                it[Messages.recipient] = recipient
                it[Messages.content] = content
                it[Messages.timestamp] = timestamp
            } get Messages.id

            Message(id, sender, recipient, content, timestamp)
        } catch (e: ExposedSQLException) {
            null
        }
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
                    timestamp = it[Messages.timestamp]
                )
            }
    }

}