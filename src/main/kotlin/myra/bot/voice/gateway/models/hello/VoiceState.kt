package myra.bot.voice.gateway.models.hello

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoiceState(
    @SerialName("guild_id") val guildId: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("self_mute") val selfMute: Boolean,
    @SerialName("self_deaf") val selfDeaf: Boolean
)
