package myra.bot.voice.voice.udp

import com.codahale.xsalsa20poly1305.SecretBox
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonPrimitive
import myra.bot.voice.gateway.models.Opcode
import myra.bot.voice.voice.gateway.VoiceGateway
import myra.bot.voice.voice.gateway.commands.ProtocolDetails
import myra.bot.voice.voice.gateway.commands.SelectProtocol
import myra.bot.voice.voice.gateway.models.ConnectionReadyPayload
import myra.bot.voice.voice.gateway.models.Operations
import myra.bot.voice.voice.transmission.ByteArrayCursor
import myra.bot.voice.voice.transmission.VoiceTransmissionsConfiguration
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
    private val logger = LoggerFactory.getLogger(UdpSocket::class.java)
    private lateinit var socket: ConnectedDatagramSocket
    private val transmissionConfig: VoiceTransmissionsConfiguration = VoiceTransmissionsConfiguration()
    private val voiceServer = InetSocketAddress(connectDetails.ip, connectDetails.port)

    private lateinit var encryption: SecretBox
    fun createEncryption(key: ByteArray) {
        encryption = SecretBox(key)
    }

    suspend fun openSocketConnection() {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        socket = aSocket(selectorManager).udp().connect(remoteAddress = voiceServer)
        val ip = discoverIp()
        logger.debug("Ip discovery successful, connecting to ${ip.hostname}:${ip.port}")
        val selectProtocol = SelectProtocol("udp", ProtocolDetails(ip.hostname, ip.port, "xsalsa20_poly1305_suffix"))
        gateway.send(selectProtocol)
        // Required to send audio
        gateway.send(Opcode(
            operation = Operations.SPEAKING.code,
            details = JsonPrimitive(5)
        ))
    }

    suspend fun sendAudio() {
        val rawBytes = this::class.java.classLoader.getResourceAsStream("heylog - im sorry.ogx")?.readBytes() ?: error("Couldn't find song")
        val bytes = ByteArrayCursor(rawBytes)

        var sentPackets: UShort = 0u
        while (bytes.cursor < bytes.size) {
            sendAudioPacket(sentPackets, bytes.retrieve(transmissionConfig.samplesPerPacket))
            sentPackets++
            delay(transmissionConfig.millisPerPacket.toLong())
        }
    }

    private suspend fun sendAudioPacket(sentPackets: UShort, bytes: ByteArray) {
        val nonce = generateNonce()
        send(voiceServer) {
            writeHeader(sentPackets) // Rtp header
            writeFully(encryptBytes(bytes, nonce)) // Opus encrypted audio
            writeFully(nonce) // Generated nonce
        }
    }

    private fun BytePacketBuilder.writeHeader(sentPackets: UShort) {
        writeByte(80.toByte()) // Version + Flags
        writeByte(78.toByte()) // Payload type
        writeShort(sentPackets.toShort()) // Sequence, the count on how many packets have been sent yet
        writeInt(sentPackets.toInt() * transmissionConfig.incrementPerPacket.toInt()) // Timestamp
        writeInt(connectDetails.ssrc) // SSRC
    }

    private fun generateNonce(): ByteArray = encryption.nonce()
    private fun encryptBytes(bytes: ByteArray, nonce: ByteArray) = encryption.seal(nonce, bytes)

    private suspend fun discoverIp(): InetSocketAddress {
        send(voiceServer) {
            writeInt(connectDetails.ssrc) // Ssrc
            writeFully(ByteArray(66)) // Address and port
        }

        return with(socket.incoming.receive().packet) {
            discard(4)
            val ip = String(readBytes(64)).trimEnd(0.toChar())
            val port = readUShort().toInt()
            InetSocketAddress(ip, port)
        }
    }

    private suspend fun send(address: SocketAddress, builder: BytePacketBuilder.() -> Unit) {
        val datagram = Datagram(buildPacket(block = builder), address)
        socket.send(datagram)
    }

}