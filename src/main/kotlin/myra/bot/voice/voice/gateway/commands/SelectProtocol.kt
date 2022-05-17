package myra.bot.voice.voice.gateway.commands

import kotlinx.serialization.Serializable
import myra.bot.voice.voice.gateway.models.Operations

@Serializable
data class SelectProtocol(
    val protocol: String,
    val data: ProtocolDetails
) : VoiceCommand(Operations.SELECT_PROTOCOL)

@Serializable
data class ProtocolDetails(
    val address: String,
    val port: Int,
    val mode: String
)