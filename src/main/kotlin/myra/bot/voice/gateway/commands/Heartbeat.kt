package myra.bot.voice.gateway.commands

import kotlinx.serialization.Serializable
import myra.bot.voice.connection.gateway.commands.VoiceCommand
import myra.bot.voice.gateway.models.Operations

@Serializable
data class Heartbeat(
    val sequence: Int
) : VoiceCommand(Operations.HEARTBEAT)
