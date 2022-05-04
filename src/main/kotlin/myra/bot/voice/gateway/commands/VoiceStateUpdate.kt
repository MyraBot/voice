package myra.bot.voice.gateway.commands

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import myra.bot.voice.connection.gateway.commands.VoiceCommand
import myra.bot.voice.gateway.models.Operations

@Serializable
data class VoiceStateUpdate(
    @SerialName("guild_id") val guildId: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("self_mute") val selfMute: Boolean,
    @SerialName("self_deaf") val selfDeaf: Boolean
) : VoiceCommand(Operations.VOICE_STATE_UPDATE)
