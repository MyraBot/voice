package myra.bot.voice.gateway

import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import myra.bot.voice.VoiceApi
import myra.bot.voice.gateway.commands.GatewayCommand
import myra.bot.voice.gateway.commands.Identify
import myra.bot.voice.gateway.commands.Resume
import myra.bot.voice.gateway.commands.VoiceStateUpdate
import myra.bot.voice.gateway.events.GatewayEvent
import myra.bot.voice.gateway.events.ReadyEvent
import myra.bot.voice.gateway.events.VoiceServerUpdateEvent
import myra.bot.voice.gateway.events.VoiceStateUpdateEvent
import myra.bot.voice.gateway.models.Opcode
import myra.bot.voice.gateway.models.Operations
import myra.bot.voice.utils.Gateway
import myra.bot.voice.utils.asDeferred
import myra.bot.voice.utils.json
import myra.bot.voice.voice.VoiceConnection
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

/**
 * Normal discord gateway connection manger. Used to open voice connections.
 *
 * @property token The bot token to connect with.
 */
class GatewayClient(private val token: String) : Gateway(
    url = "wss://gateway.discord.gg/?v=9&encoding=json",
    logger = LoggerFactory.getLogger(GatewayClient::class.java)
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var sequence: Int? = null
    private lateinit var sessionId: String
    var ready = false

    private val events = mutableMapOf(
        "READY" to ReadyEvent.serializer(),
        "VOICE_SERVER_UPDATE" to VoiceServerUpdateEvent.serializer(),
        "VOICE_STATE_UPDATE" to VoiceStateUpdateEvent.serializer())
    private val eventDispatcher = MutableSharedFlow<GatewayEvent>()

    /**
     * Resolves the fired event and emits it to the [eventDispatcher] flow.
     *
     * @param opcode The event as an opcode.
     */
    private suspend fun dispatchEvent(opcode: Opcode) {
        val serializer = events[opcode.event] ?: return
        val data = opcode.details ?: return
        val event = json.decodeFromJsonElement(serializer, data)
        eventDispatcher.emit(event)
    }

    fun connect() {
        scope.launch {
            while (true) {
                socket = client.webSocketSession(url)
                try {
                    socket.incoming.receiveAsFlow().collect { frame ->
                        val data = frame as Frame.Text
                        logger.debug("<<< ${data.readText()}")

                        val opcode: Opcode = json.decodeFromString(data.readText())
                        opcode.sequence?.let { sequence = it }

                        val op = opcode.operation ?: return@collect
                        when (Operations.from(op)) {
                            Operations.DISPATCH         -> dispatchEvent(opcode)
                            Operations.HEARTBEAT        -> send(Opcode(operation = Operations.HEARTBEAT.code, sequence = sequence))
                            Operations.HELLO            -> onConnect(opcode)
                            Operations.HEARTBEAT_ATTACK -> logger.debug("<<< acknowledged Heartbeat!")
                            else                        -> println("nothing")
                        }

                    }
                } catch (e: Exception) {
                    logger.error("Socket closed, here info lol")
                    e.printStackTrace()
                }
                ready = false
                val reason = withTimeoutOrNull(5.seconds) { socket.closeReason.await() }
                logger.info("Close reason $reason")
            }
        }
    }

    private suspend fun onConnect(opcode: Opcode) {
        if (sequence == null) identify() else resumeSession()
        val interval = opcode.details?.jsonObject?.get("heartbeat_interval")?.jsonPrimitive?.long ?: throw IllegalStateException("Invalid hello payload")
        startHeartbeat(interval)
    }

    private fun startHeartbeat(interval: Long) = scope.launch {
        while (true) {
            delay(interval)
            send(Opcode(operation = Operations.HEARTBEAT.code, sequence = sequence))
        }
    }

    /**
     * Resumes an interrupted gateway session.
     */
    private suspend fun resumeSession() = send(Resume(token, sessionId, sequence!!))

    /**
     * Finishes the handshake by sending the "identify" payload and starting the heartbeat.
     */
    private suspend fun identify() {
        send(Identify(token))
        awaitReady()
    }

    /**
     * Awaits the ready payload and fills missing data.
     */
    private fun awaitReady() {
        scope.launch {
            try {
                withTimeout(5.seconds) {
                    eventDispatcher.filterIsInstance<ReadyEvent>().first().apply {
                        sessionId = session
                        VoiceApi.id = user.id
                        ready = true
                    }
                }
            } catch (e: TimeoutCancellationException) {
                throw Exception("Didn't receive ready event in time")
            }
        }
    }

    /**
     * Requests a voice connection for the provided voice data.
     *
     * @param voiceState The voice state, containing the data for the connection.
     */
    suspend fun requestConnection(voiceState: VoiceStateUpdate): VoiceConnection {
        logger.debug("Requesting connection for guild ${voiceState.guildId}")
        send(voiceState)
        val voiceStateUpdateAwait = asDeferred { eventDispatcher.filterIsInstance<VoiceStateUpdateEvent>().first { it.guildId == voiceState.guildId } }
        val voiceServerUpdateAwait = asDeferred { eventDispatcher.filterIsInstance<VoiceServerUpdateEvent>().first { it.guildId == voiceState.guildId } }
        val (stateEvent, serverEvent) = awaitAll(voiceStateUpdateAwait, voiceServerUpdateAwait)
        logger.debug("Received all information ➜ opening voice gateway connection")
        return VoiceConnection(
            endpoint = (serverEvent as VoiceServerUpdateEvent).endpoint,
            session = (stateEvent as VoiceStateUpdateEvent).sessionId,
            token = serverEvent.token,
            guildId = voiceState.guildId
        )
    }

    /**
     * Sends a command to the voice gateway.
     *
     * @param command The command to send.
     */
    private suspend inline fun <reified T : GatewayCommand> send(command: T) = send(Opcode(
        operation = command.operation.code,
        details = json.encodeToJsonElement(command),
        sequence = sequence
    ))

}