package learn.ktor.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val message: String? = null)

@Serializable
data class ErrorResponse(val error: String)
