package myra.bot.voice.voice.gateway

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.encodeToJsonElement
import myra.bot.voice.voice.gateway.commands.VoiceCommand
import myra.bot.voice.voice.gateway.events.ConnectionReadyEvent
import myra.bot.voice.voice.gateway.models.Operations
import myra.bot.voice.gateway.events.VoiceEvent
import myra.bot.voice.gateway.models.Opcode
import myra.bot.voice.utils.gateway.Gateway
import myra.bot.voice.utils.json
import org.slf4j.LoggerFactory

class VoiceGateway(val endpoint: String) : Gateway<VoiceEvent>(
    url = endpoint,
    events = mutableMapOf(
        "READY" to ConnectionReadyEvent.serializer()),
    logger = LoggerFactory.getLogger(VoiceGateway::class.java)
) {
    private var ready = false

    suspend fun connect() = client.webSocket("wss://$endpoint") {
        socket = this
        incoming.receiveAsFlow().collect { frame ->
            val data = frame as Frame.Text
            logger.debug("Voice Gateway <<< ${data.readText()}")

            val opcode: Opcode = json.decodeFromString(data.readText())
            val op = opcode.operation ?: return@collect
            opcode.sequence?.let { sequence = it }

            when (Operations.from(op)) {
                Operations.IDENTIFY            -> TODO()
                Operations.SELECT_PROTOCOL     -> TODO()
                Operations.READY               -> TODO()
                Operations.HEARTBEAT           -> send(Opcode(operation = Operations.HEARTBEAT.code, sequence = sequence))
                Operations.SESSION_DESCRIPTION -> TODO()
                Operations.SPEAKING            -> TODO()
                Operations.HEARTBEAT_ATTACK    -> logger.debug("Gateway <<< acknowledged Heartbeat!")
                Operations.RESUME              -> TODO()
                Operations.HELLO               -> TODO()
                Operations.RESUMED             -> TODO()
                Operations.CLIENT_DISCONNECT   -> TODO()
            }

        }
    }

    /**
     * Sends a command to the voice gateway.
     *
     * @param command The command to send.
     */
    private suspend fun send(command: VoiceCommand) = send(Opcode(
        operation = command.operation.code,
        details = json.encodeToJsonElement(command),
        sequence = sequence
    ))
}