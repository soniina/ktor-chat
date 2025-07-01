package learn.ktor.services

import learn.ktor.model.User
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.ConcurrentHashMap
import org.mindrot.jbcrypt.BCrypt

class UserService {
    private val users = ConcurrentHashMap<String, User>()

    fun register(username: String, password: String): Boolean {
        if (users.containsKey(username)) return false

        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        users[username] = User(username, hashedPassword)
        return true
    }

    fun authenticate(username: String, password: String): Boolean {
        if (!users.containsKey(username)) return false
        return BCrypt.checkpw(password, users[username]?.passwordHash)
    }

    @VisibleForTesting
    fun clear() {
        users.clear()
    }
}
