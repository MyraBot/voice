package myra.bot.voice.gateway.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Opcode(
    @SerialName("op") val operation: Int?,
    @SerialName("d") val details: JsonElement? = null,
    @SerialName("s") val sequence: Int? = null,
    @SerialName("t") val event: String? = null,
)
