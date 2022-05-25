package myra.bot.voice.voice.udp

import com.codahale.xsalsa20poly1305.SecretBox
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import myra.bot.voice.voice.gateway.VoiceGateway
import myra.bot.voice.voice.gateway.models.SpeakingPayload
import kotlin.time.Duration.Companion.milliseconds

class AudioProvider(
    private val gateway: VoiceGateway,
    private val socket: UdpSocket,
    private val config: AudioProvidingConfiguration = AudioProvidingConfiguration(),
    secretKey: ByteArray,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val queuedFrames = Channel<ByteArray?>()
    private var encryption: SecretBox = SecretBox(secretKey)
    private var sentPackets: Short = 0

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

    /**
     * Starts sending audio frames to discord.
     * The frames get pulled from the [queuedFrames].
     */
    fun start() {
        queuedFrames.consumeAsFlow()
            .map { data ->
                data?.let {
                    if (!speaking) startSpeaking() // First audio frame ➜ tell Discord that we want tos peak
                    AudioFrame.fromBytes(data)
                } ?: provideNullFrame()
            }
            .filterNotNull()
            .onEach { sendAudioPacket(it) }
            .onEach { sentPackets++ }
            .launchIn(scope)
    }

    private suspend fun startSpeaking() {
        silenceFrames = 5 // Reset silent frames
        speaking = true
        gateway.send(SpeakingPayload(5, 0, socket.connectDetails.ssrc))
    }

    private suspend fun stopSpeaking() {
        speaking = false
        gateway.send(SpeakingPayload(0, 0, socket.connectDetails.ssrc))
    }

    /**
     * Provides an [AudioFrame] from null.
     *
     * @return Returns [AudioFrame.Silence] or null.
     */
    private suspend fun provideNullFrame(): AudioFrame? {
        // We are sending silent frames and haven't completed sending all 5 of them
        return if (silenceFrames > 0) {
            silenceFrames--
            AudioFrame.Silence
        } else {
            // After we stopped speaking & after we sent ALL silent frames, we can tell Discord that we want to stop speaking
            if (speaking) stopSpeaking()
            null
        }
    }

    private suspend fun sendAudioPacket(frame: AudioFrame) {
        val nonce = generateNonce()
        socket.send {
            writeHeader() // Rtp header
            writeFully(encryptBytes(frame.bytes, nonce)) // Encrypted opus audio
            writeFully(nonce) // Generated nonce
        }
    }

    private fun BytePacketBuilder.writeHeader() {
        writeByte(0x80.toByte()) // Version + Flags
        writeByte(0x78.toByte()) // Payload type
        writeShort(sentPackets) // Sequence, the count on how many packets have been sent yet
        writeUInt(sentPackets.toUInt() * config.timestampPerPacket) // Timestamp
        writeInt(socket.connectDetails.ssrc) // SSRC
    }

    private fun generateNonce(): ByteArray = encryption.nonce()
    private fun encryptBytes(bytes: ByteArray, nonce: ByteArray) = encryption.seal(nonce, bytes)

}