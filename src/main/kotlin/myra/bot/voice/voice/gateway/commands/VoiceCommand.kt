package myra.bot.voice.voice.gateway.commands

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import myra.bot.voice.voice.gateway.models.Operations

@Serializable
abstract class VoiceCommand(
    @Transient val operation: Operations = Operations.INVALID
)