package myra.bot.voice.voice.udp

/**
 * Configuration for audio transmissions
 *
 * @property millisPerPacket The milliseconds worth of audio data sent with each packet.
 * @property sampleRate Samples per second, usually 48kHz.
 */
data class AudioProvidingConfiguration(
    val millisPerPacket: UInt = 20u,
    val sampleRate: UInt = 48_000u,
) {
    val secondsPerPacket: Float = millisPerPacket.toFloat() / 1000f
    val millisOfASecond: UInt = (1f / secondsPerPacket).toUInt()

    val samplesPerPacket: Int = (sampleRate.toFloat() * secondsPerPacket).toInt()
    val timestampPerPacket: UInt = sampleRate / millisOfASecond
}