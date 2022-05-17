package myra.bot.voice.voice.gateway.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HelloPayload(
    @SerialName("heartbeat_interval") val heartbeatInterval: Int
)