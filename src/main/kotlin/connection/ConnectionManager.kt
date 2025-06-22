package learn.ktor.connection

import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

object ConnectionManager {

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    private val mutex = Mutex()

    suspend fun register(user: String, session: WebSocketSession) {
        mutex.withLock {
            sessions[user]?.takeIf { it.isActive }?.close()
            sessions[user] = session
        }
        println("Registered $user")
    }

    suspend fun sendMessage(user: String, message: String) {
        sessions[user]?.takeIf { it.isActive }?.send(message)
            ?: throw RuntimeException("Session $user doesn't exist")
    }

    suspend fun unregister(user: String) {
        mutex.withLock {
            sessions.remove(user)?.close()
        }
    }

}