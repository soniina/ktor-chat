package learn.ktor.config

import java.time.Duration


data class JwtProperties(
    val secret: String,
    val issuer: String = "ktor-chat",
    val audience: String = "chat-users",
    val realm: String = "ktor chat app",
    val expiration: Duration = Duration.ofHours(24)
)
