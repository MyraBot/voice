package myra.bot.voice.gateway.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class VoiceStateUpdateEvent(
    @SerialName("guild_id") val guildId: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("user_id") val userId: String,
    val member: JsonObject? = null,
    @SerialName("session_id") val sessionId: String,
    val deaf: Boolean,
    val mute: Boolean,
    @SerialName("self_mute") val selfMute: Boolean,
    @SerialName("self_deaf") val selfDeaf: Boolean,
    val suppress: Boolean,
    @SerialName("request_to_speak_timestamp") val requestToSpeakSince: String?,
) : GatewayEvent {
    override val identifier: String = "VOICE_STATE_UPDATE"
}
