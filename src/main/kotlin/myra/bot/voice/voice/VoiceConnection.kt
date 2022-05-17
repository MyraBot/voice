package myra.bot.voice.voice

import myra.bot.voice.voice.gateway.VoiceGateway
import myra.bot.voice.voice.udp.UdpSocket

/**
 * Voice connection
 *
 * @property endpoint
 * @property session
 * @property token Voice connection token
 * @property guildId
 */
class VoiceConnection(
    val endpoint: String,
    val session: String,
    val token: String,
    val guildId: String
) {

    private val gateway = VoiceGateway(endpoint, token, session, guildId)
    private var udp: UdpSocket? = null

    suspend fun openVoiceGatewayConnection() {
        gateway.connect()
    }

}