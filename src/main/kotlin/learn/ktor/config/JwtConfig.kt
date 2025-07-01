package learn.ktor.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.security.SecureRandom
import java.util.*


object JwtConfig {
    private const val SECRET_LENGTH = 64
    private const val ISSUER = "ktor-chat"
    private const val AUDIENCE = "chat-users"
    private const val REALM = "ktor chat app"

    private val secret: ByteArray = run {
        val random = SecureRandom()
        ByteArray(SECRET_LENGTH).apply {
            random.nextBytes(this)
        }
    }
    val algorithm: Algorithm = Algorithm.HMAC256(secret)

    fun generateToken(user: String): String =
        JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("user", user)
            .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))
            .sign(algorithm)

    fun configure(application: Application) {
        application.install(Authentication) {
            jwt("auth-jwt") {
                realm = REALM
                verifier(
                    JWT.require(algorithm)
                        .withIssuer(ISSUER)
                        .withAudience(AUDIENCE)
                        .build()
                )
                validate { credential ->
                    if (credential.payload.getClaim("user").asString() != "") {
                        JWTPrincipal(credential.payload)
                    } else null
                }
            }
        }
    }
}