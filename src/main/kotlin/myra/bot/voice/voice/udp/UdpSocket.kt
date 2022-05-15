package myra.bot.voice.voice.udp

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import org.slf4j.LoggerFactory
import myra.bot.voice.voice.gateway.events.ConnectionReadyEvent

sealed class UdpSocket(
    val endpoint: String,
    val token: String,
    val guildId: String
) {
    private val logger = LoggerFactory.getLogger(UdpSocket::class.java)
    private val coroutines = CoroutineScope(Dispatchers.IO)

    private var connected = false

    private val client = HttpClient(CIO) {
        install(WebSockets)
        expectSuccess = false
    }

    private suspend fun ready(event: ConnectionReadyEvent) {
        /*
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val voiceServer = InetSocketAddress(event.ip, event.port)
        val udpSocket = aSocket(selectorManager).udp().connect(remoteAddress = voiceServer)
        val ip = discoverIp(udpSocket, voiceServer, event)
        logger.info("Ip discovery successful, connecting to ${ip.hostname}:${ip.port}")

        val selectProtocol = SelectProtocol("udp", UdpDetails(ip.hostname, ip.port, "xsalsa20_poly1305_lite"))
        val json = VOpcode(
            operation = VOperations.SELECT_PROTOCOL,
            details = json.encodeToJsonElement(selectProtocol)
        ).toJson()
        println(json)
        socket.send(json)
        println("send protocol thing")

        socket.send(VOpcode(
            operation = VOperations.SPEAKING,
            details = JsonPrimitive(5)
        ).toJson())


        // Datagram(ByteReadPacket())


         */
    }

    private suspend fun discoverIp(socket: ConnectedDatagramSocket, voiceServer: InetSocketAddress, event: ConnectionReadyEvent): InetSocketAddress {
        socket.send(packet(voiceServer) {
            writeInt(event.ssrc)
            writeFully(ByteArray(66))
        })

        println("Sent ip discovery")

        return with(socket.incoming.receive().packet) {
            discard(4)
            val ip = io.ktor.utils.io.core.String(readBytes(64)).trimEnd(0.toChar())
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