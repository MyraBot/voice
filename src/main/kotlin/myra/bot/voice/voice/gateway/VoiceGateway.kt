package myra.bot.voice.voice.gateway

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import myra.bot.voice.VoiceApi
import myra.bot.voice.gateway.models.Opcode
import myra.bot.voice.utils.gateway.Gateway
import myra.bot.voice.utils.json
import myra.bot.voice.voice.gateway.commands.Identify
import myra.bot.voice.voice.gateway.commands.VoiceCommand
import myra.bot.voice.voice.gateway.models.ConnectionReadyPayload
import myra.bot.voice.voice.gateway.models.HelloPayload
import myra.bot.voice.voice.gateway.models.Operations
import org.slf4j.LoggerFactory

class VoiceGateway(
    val endpoint: String,
    val token: String,
    val session: String,
    val guildId: String
) : Gateway(endpoint, LoggerFactory.getLogger(VoiceGateway::class.java)) {
    private val scope = CoroutineScope(Dispatchers.Default)

    suspend fun connect() = client.webSocket("wss://$endpoint") {
        socket = this
        incoming.receiveAsFlow().collect { frame ->
            val data = frame as Frame.Text
            logger.debug("<<< ${data.readText()}")

            val opcode: Opcode = json.decodeFromString(data.readText())
            val op = opcode.operation ?: return@collect
            opcode.sequence?.let { sequence = it }

            when (Operations.from(op)) {
                Operations.IDENTIFY            -> TODO()
                Operations.SELECT_PROTOCOL     -> TODO()
                Operations.READY               -> ready(opcode)
                Operations.HEARTBEAT           -> send(Opcode(operation = Operations.HEARTBEAT.code, sequence = sequence))
                Operations.SESSION_DESCRIPTION -> TODO()
                Operations.SPEAKING            -> TODO()
                Operations.HEARTBEAT_ATTACK    -> logger.debug("<<< acknowledged Heartbeat!")
                Operations.RESUME              -> TODO()
                Operations.HELLO               -> identify(opcode)
                Operations.RESUMED             -> TODO()
                Operations.CLIENT_DISCONNECT   -> TODO()
            }

        }
    }

    private suspend fun identify(opcode: Opcode) {
        scope.launch {
            val heartbeatInterval = opcode.details?.let { json.decodeFromJsonElement<HelloPayload>(it) }?.heartbeatInterval ?: throw IllegalStateException("Invalid hello payload")

            while (true) {
                delay(heartbeatInterval.toLong())
                send(Opcode(Operations.HEARTBEAT.code, JsonPrimitive(heartbeatInterval)))
            }
        }

        send(Identify(guildId, VoiceApi.id, session, token))
    }

    private fun ready(opcode: Opcode) {
        val event = opcode.details?.let { json.decodeFromJsonElement<ConnectionReadyPayload>(it) } ?: throw IllegalStateException("Invalid voice ready payload")

    }

    /**
     * Sends a command to the voice gateway.
     *
     * @param command The command to send.
     */
    private suspend inline fun <reified T : VoiceCommand> send(command: T) = send(Opcode(
        operation = command.operation.code,
        details = json.encodeToJsonElement(command),
        sequence = sequence
    ))
}