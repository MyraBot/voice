package myra.bot.voice.gateway.commands

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import myra.bot.voice.gateway.models.Operations

@Serializable
data class Identify(
    val token: String,
    val properties: ConnectionProperties = ConnectionProperties(),
    val intents: Int = 128, // GUILD_VOICE_STATES
) : GatewayCommand(Operations.IDENTIFY)

@Serializable
data class ConnectionProperties(
    @SerialName("\$os") val os: String = "linux",
    @SerialName("\$browser") val browser: String = "idkyet",
    @SerialName("\$device") val device: String = "idkyet"
)