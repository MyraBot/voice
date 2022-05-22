package myra.bot.voice.voice.gateway.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionDescriptionPayload(
    @SerialName("audio_codec") val audioCodec: String,
    @SerialName("video_codec") val videoCodec: String?,
    @SerialName("secret_key") val secretKey: List<UByte>,
    val mode: String,
    @SerialName("media_session_id") val mediaSession: String
)