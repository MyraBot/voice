package myra.bot.voice.voice

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.decodeFromJsonElement
import myra.bot.voice.utils.json
import myra.bot.voice.voice.gateway.VoiceGateway
import myra.bot.voice.voice.gateway.models.ConnectionReadyPayload
import myra.bot.voice.voice.gateway.models.Operations
import myra.bot.voice.voice.udp.UdpSocket
import org.slf4j.LoggerFactory

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
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val gateway = VoiceGateway(endpoint, token, session, guildId)
    private var udp: UdpSocket? = null

    suspend fun openVoiceGatewayConnection() {
        gateway.connect()
        val connectionDetails = gateway.eventDispatcher
            .first { it.operation == Operations.READY.code }
            .let { it.details ?: throw IllegalStateException("Invalid voice ready payload") }
            .let { json.decodeFromJsonElement<ConnectionReadyPayload>(it) }
        udp = UdpSocket(gateway).apply { openSocketConnection(connectionDetails) }
        logger.debug("Successfully created voice connection for $guildId")
    }

}