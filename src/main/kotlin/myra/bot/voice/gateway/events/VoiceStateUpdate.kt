package myra.bot.voice.gateway.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
@Serializable
data class VoiceStateUpdate(
    @SerialName("guild_id") val guildId: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("session_id") val session: String,
    val deaf: Boolean,
    val mute: Boolean,
    @SerialName("self_deaf") val selfDeaf: Boolean,
    @SerialName("self_mute") val selfMute: Boolean
) : VoiceEvent {
    override val identifier: String = "GUILD_MEMBER_UPDATE"
}*/