package myra.bot.voice.gateway

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.encodeToJsonElement
import myra.bot.voice.connection.udp.VoiceConnection
import myra.bot.voice.connection.gateway.commands.VoiceCommand
import myra.bot.voice.gateway.commands.Heartbeat
import myra.bot.voice.gateway.commands.VoiceStateUpdate
import myra.bot.voice.gateway.events.ReadyEvent
import myra.bot.voice.gateway.events.VoiceEvent
import myra.bot.voice.gateway.events.VoiceServerUpdateEvent
import myra.bot.voice.gateway.models.Opcode
import myra.bot.voice.gateway.models.Operations
import myra.bot.voice.gateway.models.hello.GIdentify
import myra.bot.voice.utils.HeartbeatManager
import myra.bot.voice.utils.asDeferred
import myra.bot.voice.utils.json
import myra.bot.voice.utils.toJson
import org.slf4j.LoggerFactory

class GatewayClient(private val token: String) {
    private val url = "wss://gateway.discord.gg/?v=9&encoding=json"
    private val logger = LoggerFactory.getLogger(GatewayClient::class.java)
    private val client = HttpClient(CIO) {
        install(WebSockets)
        expectSuccess = false
    }

    private var ready = false
    private var sequence: Int = 0
    private var socket: DefaultClientWebSocketSession? = null

    private val eventDispatcher = MutableSharedFlow<VoiceEvent>()
    private val events = mutableMapOf(
        "READY" to ReadyEvent.serializer(),
        "VOICE_SERVER_UPDATE" to VoiceServerUpdateEvent.serializer(),
        "VOICE_SERVER_UPDATE" to VoiceStateUpdate.serializer()
    )

    suspend fun connect() = client.webSocket(url) {
        socket = this
        incoming.receiveAsFlow().collect { frame ->
            val data = frame as Frame.Text
            logger.debug("Gateway <<< ${data.readText()}")

            val opcode: Opcode = json.decodeFromString(data.readText())
            opcode.sequence?.let { sequence = it }

            when (opcode.operation) {
                Operations.DISPATCH           -> dispatchEvent(opcode)
                Operations.HEARTBEAT          -> send(Heartbeat(sequence))
                Operations.IDENTIFY           -> TODO()
                Operations.VOICE_STATE_UPDATE -> TODO()
                Operations.HELLO              -> ready(opcode)
                Operations.HEARTBEAT_ATTACK   -> logger.debug("Gateway <<< acknowledged Heartbeat!")
                else                          -> println("nothing")
            }

        }
    }

    /**
     * Resolves the fired event and emits it to the [eventDispatcher] flow.
     *
     * @param opcode The event as an opcode.
     */
    private suspend fun dispatchEvent(opcode: Opcode) {
        val serializer = events[opcode.event] ?: return
        val data = opcode.details ?: return
        val event = json.decodeFromJsonElement(serializer, data)
        eventDispatcher.emit(event as VoiceEvent)
    }

    /**
     * Finishes the handshake by sending the "identify" payload and starting the heartbeat.
     *
     * @param opcode The opcode from the hello operation.
     */
    private suspend fun ready(opcode: Opcode) {

        HeartbeatManager.startInterval(logger, socket!!, opcode.details) { sequence }
        send(GIdentify(token))
        ready = true
        CoroutineScope(Dispatchers.Default).launch {
             eventDispatcher.filterIsInstance<ReadyEvent>().first()
        }
    }

    /**
     * Requests a voice connection for the provided voice data.
     *
     * @param voiceState The voice state, containing the data for the connection.
     */
    suspend fun requestConnection(voiceState: VoiceStateUpdate): VoiceConnection {
        while (!ready) delay(1000) // Wait until main gateway is ready
        logger.debug("Requesting connection for guild ${voiceState.guildId}")

        send(voiceState)

        val voiceStateUpdateAwait = asDeferred {
            eventDispatcher.filterIsInstance<VoiceStateUpdate>()
                .first { it.guildId == voiceState.guildId }
        }
        val voiceServerUpdateAwait = asDeferred {
            eventDispatcher.filterIsInstance<VoiceServerUpdateEvent>()
                .first { it.guildId == voiceState.guildId }
        }
        awaitAll(voiceStateUpdateAwait, voiceServerUpdateAwait)
        val voiceServerUpdate = voiceServerUpdateAwait.await()
        return VoiceConnection()
    }

    /**
     * Sends a command to the voice gateway.
     *
     * @param command The command to send.
     */
    private suspend fun send(command: VoiceCommand) {
        val opcode = Opcode(
            operation = command.operation,
            details = json.encodeToJsonElement(command),
            sequence = command.sequence
        )
        socket?.send(opcode.toJson())
    }

}