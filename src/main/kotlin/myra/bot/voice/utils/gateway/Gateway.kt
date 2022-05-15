package myra.bot.voice.utils.gateway

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.websocket.send
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.KSerializer
import myra.bot.voice.gateway.models.Opcode
import myra.bot.voice.utils.json
import myra.bot.voice.utils.toJson
import org.slf4j.Logger

abstract class Gateway<EVENT_TYPE>(
    val url: String,
    val events: MutableMap<String, KSerializer<out Any>>,
    val logger: Logger
) {
    val client = HttpClient(CIO) {
        install(WebSockets)
        expectSuccess = false
    }

    var sequence: Int = 0
    var socket: DefaultClientWebSocketSession? = null

    val eventDispatcher = MutableSharedFlow<EVENT_TYPE>()

    /**
     * Resolves the fired event and emits it to the [eventDispatcher] flow.
     *
     * @param opcode The event as an opcode.
     */
    internal suspend fun dispatchEvent(opcode: Opcode) {
        val serializer = events[opcode.event] ?: return
        val data = opcode.details ?: return
        val event = json.decodeFromJsonElement(serializer, data)
        eventDispatcher.emit(event as EVENT_TYPE)
    }

    internal suspend fun send(opcode: Opcode) = socket?.send(opcode.toJson())

}