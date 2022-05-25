package myra.bot.voice.voice.gateway

import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import myra.bot.voice.VoiceApi
import myra.bot.voice.gateway.models.Opcode
import myra.bot.voice.utils.Gateway
import myra.bot.voice.utils.json
import myra.bot.voice.utils.jsonLight
import myra.bot.voice.voice.gateway.commands.Identify
import myra.bot.voice.voice.gateway.commands.Resume
import myra.bot.voice.voice.gateway.commands.VoiceCommand
import myra.bot.voice.voice.gateway.models.HelloPayload
import myra.bot.voice.voice.gateway.models.Operations
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

class VoiceGateway(
    private val endpoint: String,
    private val token: String,
    private val session: String,
    private val guildId: String
) : Gateway(endpoint, LoggerFactory.getLogger(VoiceGateway::class.java)) {
    private val scope = CoroutineScope(Dispatchers.Default)
    val eventDispatcher = MutableSharedFlow<Opcode>()
    private var lastTimestamp: Long = System.currentTimeMillis()
    private var firstConnection = true

    suspend fun connect() {
        scope.launch {
            while (true) {
                socket = client.webSocketSession("wss://$endpoint/?v=4")

                identify()
                try {
                    socket.incoming.receiveAsFlow().collect { handleIncome(it) }
                } catch (e: ClosedReceiveChannelException) {

                }
                val reason = withTimeoutOrNull(5.seconds) { socket.closeReason.await() }
                logger.warn("Socket closed with reason $reason - attempting reconnection")
            }
        }
    }

    private suspend fun handleIncome(frame: Frame) {
        val data = frame as Frame.Text
        logger.debug("<<< ${data.readText()}")

        val opcode: Opcode = json.decodeFromString(data.readText())
        val op = opcode.operation ?: return
        when (Operations.from(op)) {
            Operations.READY                 -> eventDispatcher.emit(opcode)
            Operations.SESSION_DESCRIPTION   -> eventDispatcher.emit(opcode)
            Operations.HEARTBEAT_ACKNOWLEDGE -> handleHeartbeat(opcode)
            Operations.HELLO                 -> startHeartbeat(opcode)
            //Operations.RESUMED               -> TODO()
            //Operations.CLIENT_DISCONNECT     -> TODO()
            Operations.INVALID               -> throw Exception()
        }
    }

    private suspend fun identify() {
        if (firstConnection) {
            send(Identify(guildId, VoiceApi.id, session, token))
            firstConnection = false
        } else send(Resume(guildId, session, token))
    }

    private fun startHeartbeat(hello: Opcode) = scope.launch {
        val interval = hello.details?.let { json.decodeFromJsonElement<HelloPayload>(it) }?.heartbeatInterval?.toLong() ?: throw IllegalStateException("Invalid hello payload")

        eventDispatcher.first { it.operation == Operations.READY.code }
        val timestamp: Long = System.currentTimeMillis()

        while (true) {
            lastTimestamp = System.currentTimeMillis() - timestamp
            send(Opcode(Operations.HEARTBEAT.code, JsonPrimitive(lastTimestamp)))
            delay(interval)
        }
    }

    private fun handleHeartbeat(opcode: Opcode) {
        if (opcode.details?.jsonPrimitive?.long != lastTimestamp) logger.warn("Received non matching heartbeat")
        else logger.debug("Acknowledged heartbeat")
    }

    /**
     * Sends a command to the voice gateway.
     *
     * @param command The command to send.
     */
    suspend inline fun <reified T : VoiceCommand> send(command: T) = send(Opcode(
        operation = command.operation.code,
        details = jsonLight.encodeToJsonElement(command)
    ))

}