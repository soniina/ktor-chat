package learn.ktor.connection

import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import learn.ktor.config.JsonFormat
import learn.ktor.model.ChatEvent
import java.util.concurrent.ConcurrentHashMap

interface OnlineUserProvider {
    fun getOnlineUsers(): List<String>
}

class ConnectionManager: OnlineUserProvider {

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val mutex = Mutex()

    suspend fun register(user: String, session: WebSocketSession) {
        mutex.withLock {
            sessions[user]?.takeIf { it.isActive }?.close()
            sessions[user] = session
        }
        println("Registered $user")
    }

    suspend fun sendTo(user: String, event: ChatEvent) {
        sessions[user]?.takeIf { it.isActive }?.send(JsonFormat.encodeToString(event))
    }

    fun isOnline(user: String): Boolean = sessions[user]?.isActive ?: false

    fun getSession(user: String) = sessions[user]

    override fun getOnlineUsers(): List<String> = sessions.filterValues { it.isActive }.keys.toList()

    suspend fun unregister(user: String) {
        mutex.withLock {
            sessions.remove(user)?.close()
        }
    }
}