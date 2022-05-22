package myra.bot.voice.gateway

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.encodeToJsonElement
import myra.bot.voice.VoiceApi
import myra.bot.voice.gateway.commands.GatewayCommand
import myra.bot.voice.gateway.commands.Identify
import myra.bot.voice.gateway.commands.VoiceStateUpdate
import myra.bot.voice.gateway.events.GatewayEvent
import myra.bot.voice.gateway.events.ReadyEvent
import myra.bot.voice.gateway.events.VoiceServerUpdateEvent
import myra.bot.voice.gateway.events.VoiceStateUpdateEvent
import myra.bot.voice.gateway.models.Opcode
import myra.bot.voice.gateway.models.Operations
import myra.bot.voice.utils.asDeferred
import myra.bot.voice.utils.Gateway
import myra.bot.voice.utils.json
import myra.bot.voice.voice.VoiceConnection
import org.slf4j.LoggerFactory

/**
 * Normal discord gateway connection manger. Used to open voice connections.
 *
 * @property token The bot token to connect with.
 */
class GatewayClient(private val token: String) : Gateway(
    url = "wss://gateway.discord.gg/?v=9&encoding=json",
    logger = LoggerFactory.getLogger(GatewayClient::class.java)
) {
    var ready = false
    lateinit var sessionId: String

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

    suspend fun connect() = client.webSocket(url) {
        socket = this
        incoming.receiveAsFlow().collect { frame ->
            val data = frame as Frame.Text
            logger.debug("<<< ${data.readText()}")

            val opcode: Opcode = json.decodeFromString(data.readText())
            opcode.sequence?.let { sequence = it }

            val op = opcode.operation ?: return@collect
            when (Operations.from(op)) {
                Operations.DISPATCH           -> dispatchEvent(opcode)
                Operations.HEARTBEAT          -> send(Opcode(operation = Operations.HEARTBEAT.code, sequence = sequence))
                Operations.IDENTIFY           -> TODO()
                Operations.VOICE_STATE_UPDATE -> TODO()
                Operations.HELLO              -> identify(opcode)
                Operations.HEARTBEAT_ATTACK   -> logger.debug("<<< acknowledged Heartbeat!")
                else                          -> println("nothing")
            }

        }
    }

    /**
     * Finishes the handshake by sending the "identify" payload and starting the heartbeat.
     *
     * @param opcode The opcode from the hello operation.
     */
    private suspend fun identify(opcode: Opcode) {
        send(Identify(token))
        CoroutineScope(Dispatchers.Default).launch {
            eventDispatcher.filterIsInstance<ReadyEvent>().first().apply {
                sessionId = session
                VoiceApi.id = user.id
                ready = true
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