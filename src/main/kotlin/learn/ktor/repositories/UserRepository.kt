package learn.ktor.repositories

import learn.ktor.model.User
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {

    fun addUser(username: String, password: String): User? = transaction {
        try {
            val id = Users.insert {
                it[Users.username] = username
                it[Users.password] = password
            } get Users.id

            User(id, username, password)
        } catch (e: ExposedSQLException) {
            null
        }
    }

    fun getByUsername(username: String): User? = transaction {
        Users.selectAll().where { Users.username eq username }
            .map {
                User(it[Users.id], it[Users.username], it[Users.password])
            }.singleOrNull()
    }

    fun deleteUser(username: String): Boolean = transaction {
        Users.deleteWhere { Users.username eq username } > 0
    }
}