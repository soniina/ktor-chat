package learn.ktor.repositories

import org.jetbrains.exposed.sql.Table

object Messages: Table() {
    val id = integer("id").autoIncrement()
    val sender = varchar("sender", 50)
    val recipient = varchar("recipient", 50)
    val content = text("content")
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(id)
}