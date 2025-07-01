package learn.ktor.testutil

import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext

class FakeWebSocketSession : WebSocketSession {
    val sent = mutableListOf<String>()

    override suspend fun send(frame: Frame) {
        if (frame is Frame.Text) {
            sent.add(frame.readText())
        }
    }

    override val outgoing: SendChannel<Frame> = Channel()
    override val incoming: ReceiveChannel<Frame> = Channel()
    override var masking: Boolean = false
    override val extensions: List<WebSocketExtension<*>> = emptyList()
    override var maxFrameSize: Long = Long.MAX_VALUE
    override val coroutineContext: CoroutineContext = Dispatchers.Default

    override suspend fun flush() {}
    override fun terminate() {}
}