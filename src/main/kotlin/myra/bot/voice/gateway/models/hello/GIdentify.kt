package myra.bot.voice.gateway.models.hello

import kotlinx.serialization.Serializable
import myra.bot.voice.connection.gateway.commands.VoiceCommand
import myra.bot.voice.gateway.models.Operations

@Serializable
data class GIdentify(
    val token: String,
    val properties: ConnectionProperties = ConnectionProperties(),
    val intents: Int = 0,
) : VoiceCommand(Operations.IDENTIFY)