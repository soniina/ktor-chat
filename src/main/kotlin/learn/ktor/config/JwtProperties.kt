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
        secret = property("ktor.jwt.secret").getString(),
        issuer = property("ktor.jwt.issuer").getString(),
        audience = property("ktor.jwt.audience").getString(),
        realm = property("ktor.jwt.realm").getString(),
        expiration = property("ktor.jwt.expiration").getString().toLong()
    )
}