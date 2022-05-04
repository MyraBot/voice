package myra.bot.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import myra.bot.voice.gateway.GatewayClient
import myra.bot.voice.gateway.models.hello.VoiceState
import myra.bot.voice.connection.udp.VoiceConnection

object VoiceApi {
    lateinit var token: String
    private val scope = CoroutineScope(Dispatchers.Default)
    var gateway: GatewayClient? = null

    fun connectGateway() {
        gateway = GatewayClient(token)
        scope.launch { gateway?.connect() }
    }

    suspend fun connect(voiceState: VoiceState): VoiceConnection = gateway?.requestConnection(voiceState) ?: error("Connect to the gateway client first")
}