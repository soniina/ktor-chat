package learn.ktor.services

import learn.ktor.repositories.UserRepository
import org.mindrot.jbcrypt.BCrypt

class UserService(private val userRepository: UserRepository) {

    suspend fun register(username: String, password: String): Boolean {
        if (userRepository.getByUsername(username) != null) return false

        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        userRepository.addUser(username, hashedPassword)
        return true
    }

    suspend fun authenticate(username: String, password: String): Boolean {
        val user = userRepository.getByUsername(username) ?: return false
        return BCrypt.checkpw(password, user.password)
    }
}
