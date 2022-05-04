package myra.bot.voice.utils

import com.github.m5rian.gateway.models.Opcode
import com.github.m5rian.gateway.models.GOperations
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.slf4j.Logger

object HeartbeatManager {
    private val coroutines = CoroutineScope(Dispatchers.Default)

    fun startInterval(logger: Logger, socket: DefaultClientWebSocketSession, details: JsonElement?, sequence: () -> Int) {
        val interval = details?.jsonObject?.get("heartbeat_interval")?.jsonPrimitive?.longOrNull ?: throw IllegalStateException("Discord is missing the 'heartbeat_interval' data")
        coroutines.launch {
            while (true) {
                delay(interval)
                logger.debug("Sending heartbeat")
                val opcode = Opcode(GOperations.HEARTBEAT, JsonPrimitive(sequence.invoke()), null, null)
                socket.send(opcode.toJson())
            }
        }
    }

    fun acknowledge() {

    }

}