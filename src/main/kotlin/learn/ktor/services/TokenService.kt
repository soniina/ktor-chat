package learn.ktor.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import learn.ktor.config.JwtProperties
import java.time.Instant

class TokenService(private val properties: JwtProperties) {

    private val algorithm: Algorithm = Algorithm.HMAC256(properties.secret)

    fun generateToken(user: String): String {

        return JWT.create()
            .withIssuer(properties.issuer)
            .withAudience(properties.audience)
            .withClaim("user", user)
            .withExpiresAt(Instant.now().plus(properties.expiration))
            .sign(algorithm)
    }

    fun verifyToken(token: String): String? {
        return try {
            JWT.require(algorithm)
                .withIssuer(properties.issuer)
                .withAudience(properties.audience)
                .build()
                .verify(token)
                .getClaim("user")
                .asString()
                .takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}