package myra.bot.voice.gateway.commands

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import myra.bot.voice.gateway.models.Operations

@Serializable
data class Resume(
    val token: String,
    @SerialName("session_id") val sessionId: String,
    val seq: Int
) : GatewayCommand(Operations.RESUME)
