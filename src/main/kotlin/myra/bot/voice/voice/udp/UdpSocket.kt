package myra.bot.voice.voice.udp

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.decodeFromJsonElement
import myra.bot.voice.utils.json
import myra.bot.voice.voice.gateway.VoiceGateway
import myra.bot.voice.voice.gateway.commands.ProtocolDetails
import myra.bot.voice.voice.gateway.commands.SelectProtocol
import myra.bot.voice.voice.gateway.models.ConnectionReadyPayload
import myra.bot.voice.voice.gateway.models.Operations
import myra.bot.voice.voice.gateway.models.SessionDescriptionPayload
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Udp socket which sends the audio data.
 *
 * @property gateway It's [VoiceGateway] client.
 * @property connectDetails Information to connect.
 */
class UdpSocket(
    val gateway: VoiceGateway,
    val connectDetails: ConnectionReadyPayload,
) {
    private val logger: Logger = LoggerFactory.getLogger(UdpSocket::class.java)
    private lateinit var socket: ConnectedDatagramSocket
    private val voiceServer: SocketAddress = InetSocketAddress(connectDetails.ip, connectDetails.port)
    lateinit var audioProvider: AudioProvider

    suspend fun openSocketConnection() {
        socket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().connect(remoteAddress = voiceServer)
        val ip = discoverIp()
        val selectProtocol = SelectProtocol("udp", ProtocolDetails(ip.hostname, ip.port, "xsalsa20_poly1305_suffix"))
        gateway.send(selectProtocol)

        val secretKey = gateway.eventDispatcher
            .first { it.operation == Operations.SESSION_DESCRIPTION.code }
            .let { it.details ?: throw IllegalStateException() }
            .let { json.decodeFromJsonElement<SessionDescriptionPayload>(it) }
            .secretKey
        audioProvider = AudioProvider(
            gateway = gateway,
            socket = this,
            secretKey = secretKey.toUByteArray().toByteArray()
        )
        audioProvider.start()
    }

    private suspend fun discoverIp(): InetSocketAddress {
        send {
            writeInt(connectDetails.ssrc) // Ssrc
            writeFully(ByteArray(66)) // Address and port
        }

        return with(socket.incoming.receive().packet) {
            discard(4)
            val ip = String(readBytes(64)).trimEnd(0.toChar())
            val port = readUShort().toInt()
            logger.debug("Ip discovery successful, connecting to ${ip}:${port}")
            InetSocketAddress(ip, port)
        }
    }

    suspend fun send(builder: BytePacketBuilder.() -> Unit) {
        val datagram = Datagram(buildPacket(block = builder), voiceServer)
        socket.send(datagram)
    }

}