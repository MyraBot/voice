package myra.bot.voice.voice.gateway

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
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
    private var connectionData: ConnectionReadyPayload? = null

    val eventDispatcher = MutableSharedFlow<Opcode>()
    val heartbeatDispatcher = MutableSharedFlow<Opcode>()

    suspend fun connect() = scope.launch {
        client.webSocket("wss://$endpoint") {
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
                    Operations.READY               -> eventDispatcher.emit(opcode)
                    Operations.HEARTBEAT           -> TODO()
                    Operations.SESSION_DESCRIPTION -> eventDispatcher.emit(opcode)
                    Operations.SPEAKING            -> TODO()
                    Operations.HEARTBEAT_ATTACK    -> heartbeatDispatcher.emit(opcode)
                    Operations.RESUME              -> TODO()
                    Operations.HELLO               -> identify(opcode)
                    Operations.RESUMED             -> TODO()
                    Operations.CLIENT_DISCONNECT   -> TODO()
                    Operations.INVALID             -> throw Exception()
                }

            }
        }
    }

    private suspend fun identify(opcode: Opcode) {
        val heartbeatInterval = opcode.details?.let { json.decodeFromJsonElement<HelloPayload>(it) }?.heartbeatInterval ?: throw IllegalStateException("Invalid hello payload")
        startHeartbeat(heartbeatInterval)

        send(Identify(guildId, VoiceApi.id, session, token))
    }

    private fun startHeartbeat(interval: Long) = scope.launch {
        while (true) {
            delay(interval)
            val random = (0..1_007).random()
            send(Opcode(Operations.HEARTBEAT.code, JsonPrimitive(random)))
            heartbeatDispatcher.first().also {
                if (it.details?.jsonPrimitive?.int != random) throw IllegalStateException("Heartbeat doesn't matched sent")
                else logger.debug("<<< acknowledged Heartbeat!")
            }
        }
    }

    /**
     * Sends a command to the voice gateway.
     *
     * @param command The command to send.
     */
    suspend inline fun <reified T : VoiceCommand> send(command: T) = send(Opcode(
        operation = command.operation.code,
        details = json.encodeToJsonElement(command),
        sequence = sequence
    ))

}