package myra.bot.voice.gateway

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.encodeToJsonElement
import myra.bot.voice.voice.udp.VoiceConnection
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
import myra.bot.voice.utils.gateway.Gateway
import myra.bot.voice.utils.json
import org.slf4j.LoggerFactory

/**
 * Normal discord gateway connection manger. Used to open voice connections.
 *
 * @property token The bot token to connect with.
 */
class GatewayClient(private val token: String) : Gateway<GatewayEvent>(
    url = "wss://gateway.discord.gg/?v=9&encoding=json",
    events = mutableMapOf(
        "READY" to ReadyEvent.serializer(),
        "VOICE_SERVER_UPDATE" to VoiceServerUpdateEvent.serializer(),
        "VOICE_STATE_UPDATE" to VoiceStateUpdateEvent.serializer()),
    logger = LoggerFactory.getLogger(GatewayClient::class.java)
) {
    var ready = false

    suspend fun connect() = client.webSocket(url) {
        socket = this
        incoming.receiveAsFlow().collect { frame ->
            val data = frame as Frame.Text
            logger.debug("Gateway <<< ${data.readText()}")

            val opcode: Opcode = json.decodeFromString(data.readText())
            opcode.sequence?.let { sequence = it }

            val op = opcode.operation ?: return@collect
            when (Operations.from(op)) {
                Operations.DISPATCH           -> dispatchEvent(opcode)
                Operations.HEARTBEAT          -> send(Opcode(operation = Operations.HEARTBEAT.code, sequence = sequence))
                Operations.IDENTIFY           -> TODO()
                Operations.VOICE_STATE_UPDATE -> TODO()
                Operations.HELLO              -> identify(opcode)
                Operations.HEARTBEAT_ATTACK   -> logger.debug("Gateway <<< acknowledged Heartbeat!")
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
        // TODo
        //socket.startInterval()
        //HeartbeatManager.startInterval(logger, socket!!, opcode.details) { sequence }
        send(Identify(token))
        CoroutineScope(Dispatchers.Default).launch {
            eventDispatcher.filterIsInstance<ReadyEvent>().first()
            println("received ready")
            ready = true
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
        awaitAll(voiceStateUpdateAwait, voiceServerUpdateAwait)
        return VoiceConnection()
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