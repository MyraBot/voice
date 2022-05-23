package myra.bot.voice.voice.gateway

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import myra.bot.voice.VoiceApi
import myra.bot.voice.gateway.models.Opcode
import myra.bot.voice.utils.Gateway
import myra.bot.voice.utils.json
import myra.bot.voice.utils.jsonLight
import myra.bot.voice.voice.gateway.commands.Identify
import myra.bot.voice.voice.gateway.commands.VoiceCommand
import myra.bot.voice.voice.gateway.models.HelloPayload
import myra.bot.voice.voice.gateway.models.Operations
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

class VoiceGateway(
    val endpoint: String,
    val token: String,
    val session: String,
    val guildId: String
) : Gateway(endpoint, LoggerFactory.getLogger(VoiceGateway::class.java)) {
    private val scope = CoroutineScope(Dispatchers.Default)
    var lastTimestamp: Long = System.currentTimeMillis()
    val eventDispatcher = MutableSharedFlow<Opcode>()

    suspend fun connect() = scope.launch {
        client.webSocket("wss://$endpoint") {
            socket = this
            try {
                incoming.receiveAsFlow().collect { frame ->
                    val data = frame as Frame.Text
                    logger.debug("<<< ${data.readText()}")

                    val opcode: Opcode = json.decodeFromString(data.readText())
                    val op = opcode.operation ?: return@collect

                    when (Operations.from(op)) {
                        //Operations.IDENTIFY              -> TODO()
                        //Operations.SELECT_PROTOCOL       -> TODO()
                        Operations.READY                 -> eventDispatcher.emit(opcode)
                        Operations.HEARTBEAT             -> sendHeartbeat()
                        Operations.SESSION_DESCRIPTION   -> eventDispatcher.emit(opcode)
                        //Operations.SPEAKING              -> Unit // Ignore if somebody speaks
                        Operations.HEARTBEAT_ACKNOWLEDGE -> handleHeartbeat(opcode)
                        //Operations.RESUME                -> TODO()
                        Operations.HELLO                 -> identify(opcode)
                        //Operations.RESUMED               -> TODO()
                        //Operations.CLIENT_DISCONNECT     -> TODO()
                        //Operations.SOMEBODY_CONNECT      -> Unit // Ignore if somebody joins
                        Operations.INVALID               -> throw Exception()
                    }

                }
            } catch (e: Exception) {
                logger.error("Socket closed, here info lol")
                e.printStackTrace()
            }
            val reason = withTimeoutOrNull(5.seconds) { closeReason.await() }
            logger.info("Close reason $reason")
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
            sendHeartbeat()
        }
    }

    private suspend fun sendHeartbeat() {
        lastTimestamp = System.currentTimeMillis()
        send(Opcode(Operations.HEARTBEAT.code, JsonPrimitive(lastTimestamp)))
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