package myra.bot.voice.gateway.commands

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import myra.bot.voice.gateway.models.Operations

@Serializable
abstract class Command(
    @Contextual val operation: Operations,
    @Contextual val sequence: Int? = null
)