package myra.bot.voice.gateway.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoiceServerUpdateEvent(
    val token: String,
    @SerialName("guild_id") val guildId: String,
    val endpoint: String
) : VoiceEvent {
    override val identifier: String = "VOICE_SERVER_UPDATE"
}