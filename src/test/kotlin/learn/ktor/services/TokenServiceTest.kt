package learn.ktor.services

import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.SignatureVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import learn.ktor.config.JwtProperties
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class TokenServiceTest {

    private val properties = JwtProperties(
        secret = "test-secret",
        issuer = "test-issuer",
        audience = "test-audience",
        realm = "test-realm",
        expiration = 360000
    )

    private val tokenService = TokenService(properties)


    private val username = "aliсe"

    @Test
    fun `should generate and verify token`() {
        val token = tokenService.generateToken(username)
        val user = tokenService.verifyToken(token)

        assertNotNull(user)
        assertEquals(username, user)
    }

    @Test
    fun `should not verify invalid token`() {
        val invalidToken = "not.a.jwt.token"

        assertFailsWith<JWTDecodeException> {
            tokenService.verifyToken(invalidToken)
        }
    }

    @Test
    fun `should not verify token with different secret`() {
        val token = tokenService.generateToken(username)

        val otherProperties = properties.copy(secret = "other-secret")
        val otherService = TokenService(otherProperties)

        assertFailsWith<SignatureVerificationException> {
            otherService.verifyToken(token)
        }
    }

    @Test
    fun `should not verify empty token`() {
        assertFailsWith<JWTDecodeException> {
            tokenService.verifyToken("")
        }
    }

    @Test
    fun `should not verify expired token`() {
        val expiredProperties = properties.copy(expiration = -1000)
        val expiredTokenService = TokenService(expiredProperties)
        val token = expiredTokenService.generateToken(username)

        assertFailsWith<TokenExpiredException> {
            tokenService.verifyToken(token)
        }
    }
}
