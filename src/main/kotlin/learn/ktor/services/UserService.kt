package learn.ktor.services

import learn.ktor.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt

object UserService {

    private val userRepository = UserRepository()

    fun register(username: String, password: String): Boolean {
        if (userRepository.findByUsername(username) != null) return false

        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        userRepository.addUser(username, hashedPassword)
        return true
    }

    fun authenticate(username: String, password: String): Boolean {
        val user = userRepository.findByUsername(username) ?: return false
        return BCrypt.checkpw(password, user.password)
    }

//    @VisibleForTesting
//    fun clear() {
//    }
}
