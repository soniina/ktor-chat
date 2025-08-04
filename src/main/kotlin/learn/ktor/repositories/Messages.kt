package learn.ktor.repositories

import org.jetbrains.exposed.sql.Table

object Messages: Table() {
    val id = integer("id").autoIncrement()
    val sender = reference("sender_id", Users.id)
    val recipient = reference("recipient_id", Users.id)
    val content = text("content")
    val timestamp = long("timestamp")
    val delivered = bool("delivered").default(false)

    override val primaryKey = PrimaryKey(id)
}