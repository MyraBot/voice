package myra.bot.voice.connection.gateway.commands

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import myra.bot.voice.gateway.models.Operations

@Serializable
abstract class VoiceCommand(
    @Contextual val operation: Operations
)