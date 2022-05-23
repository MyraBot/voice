package myra.bot.voice.voice.udp

import com.codahale.xsalsa20poly1305.SecretBox
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.core.writeInt
import io.ktor.utils.io.core.writeShort
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import myra.bot.voice.voice.gateway.VoiceGateway
import myra.bot.voice.voice.gateway.models.SpeakingPayload
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

class AudioProvider(
    private val gateway: VoiceGateway,
    private val socket: UdpSocket,
    private val config: AudioProvidingConfiguration = AudioProvidingConfiguration(),
    secretKey: ByteArray,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val queuedFrames = Channel<ByteArray?>()
    private var encryption: SecretBox = SecretBox(secretKey)
    private var sentPackets: Short = 0
    private var timestamp: UInt = 0u

    private var speaking = false
    private var silenceFrames = 5

    fun provide(bytes: () -> ByteArray?) {
        scope.launch {
            while (isActive) {
                queuedFrames.send(bytes.invoke())
                delay(20.milliseconds)
            }
        }
    }

    fun start() {
        queuedFrames.consumeAsFlow()
            .map { data ->
                if (data != null) {
                    // Required to send audio
                    if (!speaking) {
                        silenceFrames = 5
                        speaking = true
                        gateway.send(SpeakingPayload(5, 0, socket.connectDetails.ssrc))
                    }
                    AudioFrame.fromBytes(data)
                } else {
                    if (silenceFrames > 0) {
                        silenceFrames--
                        AudioFrame.Silence
                    } else {
                        // Required to send audio
                        if (speaking) {
                            speaking = false
                            gateway.send(SpeakingPayload(0, 0, socket.connectDetails.ssrc))
                        }
                        null
                    }
                }
            }
            .filterNotNull()
            .onEach { sendAudioPacket(it) }
            .onEach { sentPackets++ }
            .onEach { timestamp += 960u }
            .launchIn(scope)
    }

    private suspend fun sendAudioPacket(frame: AudioFrame) {
        val nonce = generateNonce()
        socket.send {
            writeHeader(sentPackets) // Rtp header
            writeFully(encryptBytes(frame.bytes, nonce)) // Encrypted opus audio
            writeFully(nonce) // Generated nonce
        }
    }

    private fun BytePacketBuilder.writeHeader(sentPackets: Short) {
        writeByte(0x80.toByte()) // Version + Flags
        writeByte(0x78.toByte()) // Payload type
        writeShort(sentPackets) // Sequence, the count on how many packets have been sent yet
        writeInt(timestamp.toInt()) // Timestamp
        writeInt(socket.connectDetails.ssrc) // SSRC
    }

    private fun generateNonce(): ByteArray = encryption.nonce()
    private fun encryptBytes(bytes: ByteArray, nonce: ByteArray) = encryption.seal(nonce, bytes)

}