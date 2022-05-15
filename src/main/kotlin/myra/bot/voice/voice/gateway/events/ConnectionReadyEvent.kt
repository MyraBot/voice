package myra.bot.voice.voice.gateway.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionReadyEvent(
    val ssrc: Int,
    val ip: String,
    val port: String,
    val modes: List<String>,
    @SerialName("heartbeat_interval") val heartbeatInterval: Int
)