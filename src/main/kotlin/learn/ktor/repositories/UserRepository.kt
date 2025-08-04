package learn.ktor.repositories

import learn.ktor.model.User
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class UserRepository {

    suspend fun addUser(username: String, password: String): User? = newSuspendedTransaction {
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

    suspend fun getUserByUsername(username: String): User? = newSuspendedTransaction {
        try {
            Users.selectAll().where { Users.username eq username }
                .map {
                    User(it[Users.id], it[Users.username], it[Users.password])
                }.singleOrNull()
        } catch (e: ExposedSQLException) {
            null
        }
    }

    suspend fun getIdByUsername(username: String): Int? = newSuspendedTransaction {
        try {
            Users.selectAll().where { Users.username eq username }
                .map {
                    it[Users.id]
                }.singleOrNull()
        } catch (e: ExposedSQLException) {
            null
        }
    }

    suspend fun getUsernameById(userId: Int): String? = newSuspendedTransaction {
        try {
            Users.selectAll().where { Users.id eq userId }
                .map {
                    it[Users.username]
                }.singleOrNull()
        } catch (e: ExposedSQLException) {
            null
        }
    }

    suspend fun deleteUser(username: String): Boolean = newSuspendedTransaction {
        Users.deleteWhere { Users.username eq username } > 0
    }
}