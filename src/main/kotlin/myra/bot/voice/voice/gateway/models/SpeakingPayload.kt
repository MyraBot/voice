package myra.bot.voice.voice.gateway.models

import kotlinx.serialization.Serializable
import myra.bot.voice.voice.gateway.commands.VoiceCommand

@Serializable
data class SpeakingPayload(
    val speaking: Int,
    val delay: Int,
    val ssrc: Int
):VoiceCommand(Operations.SPEAKING)