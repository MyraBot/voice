package myra.bot.voice.voice.gateway.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Voice gateway operations.
 *
 * @property code Operation code.
 */
@Serializable(with = Operations.Serializer::class)
enum class Operations(val code: Int) {
    IDENTIFY(0),
    SELECT_PROTOCOL(1),
    READY(2),
    HEARTBEAT(3),
    SESSION_DESCRIPTION(4),
    SPEAKING(5),
    HEARTBEAT_ATTACK(6),
    RESUME(7),
    HELLO(8),
    RESUMED(9),
    CLIENT_DISCONNECT(10),
    INVALID(-1);

    internal object Serializer : KSerializer<Operations> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("voice_gateway_operations", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): Operations = decoder.decodeInt().let { int -> values().first { it.code == int } }
        override fun serialize(encoder: Encoder, value: Operations) = if (value == INVALID) error("Invalid op code") else encoder.encodeInt(value.code)
    }

    companion object {
        fun from(code: Int): Operations = values().first { it.code == code }
    }

}