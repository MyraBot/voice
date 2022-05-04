package myra.bot.voice.gateway.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import myra.bot.voice.gateway.models.User

@Serializable
data class ReadyEvent(
    val user: User,
    @SerialName("session_id") val session: String
) : VoiceEvent {
    override val identifier: String = "READY"
}
