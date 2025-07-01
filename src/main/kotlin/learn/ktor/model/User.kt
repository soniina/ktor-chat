package learn.ktor.model

data class User(val username: String, val passwordHash: String, val createdAt: Long = System.currentTimeMillis())
