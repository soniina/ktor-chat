package learn.ktor.config

import io.ktor.server.config.*


data class JwtProperties(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expiration: Long
)

fun ApplicationConfig.getJwtProperties(): JwtProperties {
    return JwtProperties(
        secret = property("jwt.secret").getString(),
        issuer = property("jwt.issuer").getString(),
        audience = property("jwt.audience").getString(),
        realm = property("jwt.realm").getString(),
        expiration = property("jwt.expiration").getString().toLong()
    )
}