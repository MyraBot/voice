package myra.bot.voice.connection.gateway.commands

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import myra.bot.voice.gateway.models.Operations

@Serializable
data class Identify(
    @SerialName("server_id") val guildId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("session_id") val session: String,
    val token: String
) : VoiceCommand(Operations.IDENTIFY)