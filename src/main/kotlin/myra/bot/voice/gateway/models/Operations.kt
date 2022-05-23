package myra.bot.voice.gateway.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Operations.Serializer::class)
enum class Operations(val code: Int) {
    DISPATCH(0),
    HEARTBEAT(1),
    IDENTIFY(2),
    PRESENCE_UPDATE(3),
    VOICE_STATE_UPDATE(4),
    RESUME(6),
    HELLO(10),
    HEARTBEAT_ATTACK(11);

    internal object Serializer : KSerializer<Operations> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("gateway_operations", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): Operations = decoder.decodeInt().let { int -> values().first { it.code == int } }
        override fun serialize(encoder: Encoder, value: Operations) = encoder.encodeInt(value.code)

    }

    companion object {
        fun from(code: Int): Operations = values().first { it.code == code }
    }

}