package myra.bot.voice.gateway.commands

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import myra.bot.voice.gateway.models.Operations

@Serializable
abstract class GatewayCommand(
    @Contextual val operation: Operations,
    @Contextual val sequence: Int? = null
)