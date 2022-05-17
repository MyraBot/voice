package myra.bot.voice.voice.udp

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import myra.bot.voice.voice.gateway.VoiceGateway
import myra.bot.voice.voice.gateway.commands.ProtocolDetails
import myra.bot.voice.voice.gateway.commands.SelectProtocol
import myra.bot.voice.voice.gateway.models.ConnectionReadyPayload
import org.slf4j.LoggerFactory

class UdpSocket(val gateway: VoiceGateway) {
    private val logger = LoggerFactory.getLogger(UdpSocket::class.java)
    private val coroutines = CoroutineScope(Dispatchers.IO)

    private var connected = false

     suspend fun openSocketConnection(event: ConnectionReadyPayload) {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val voiceServer = InetSocketAddress(event.ip, event.port)
        val udpSocket = aSocket(selectorManager).udp().connect(remoteAddress = voiceServer)
        val ip = discoverIp(udpSocket, voiceServer, event)
        logger.debug("Ip discovery successful, connecting to ${ip.hostname}:${ip.port}")

        val selectProtocol = SelectProtocol("udp", ProtocolDetails(ip.hostname, ip.port, "xsalsa20_poly1305_lite"))
        gateway.send(selectProtocol)
        /*
        socket.send(VOpcode(
            operation = VOperations.SPEAKING,
            details = JsonPrimitive(5)
        ).toJson())
*/

        // Datagram(ByteReadPacket())
    }

    private suspend fun discoverIp(socket: ConnectedDatagramSocket, voiceServer: InetSocketAddress, event: ConnectionReadyPayload): InetSocketAddress {
        socket.send(packet(voiceServer) {
            writeInt(event.ssrc)
            writeFully(ByteArray(66))
        })

        return with(socket.incoming.receive().packet) {
            discard(4)
            val ip = String(readBytes(64)).trimEnd(0.toChar())
            val port = readUShort().toInt()
            InetSocketAddress(ip, port)
        }
    }

    private fun packet(address: SocketAddress, builder: BytePacketBuilder.() -> Unit): Datagram {
        return Datagram(buildPacket(block = builder), address)
    }

    private suspend fun ReceiveChannel<Datagram>.receive() {
        receiveAsFlow().first().packet
    }

}