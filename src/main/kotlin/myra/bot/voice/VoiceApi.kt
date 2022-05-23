package myra.bot.voice

import kotlinx.coroutines.delay
import myra.bot.voice.gateway.GatewayClient
import myra.bot.voice.gateway.commands.VoiceStateUpdate
import myra.bot.voice.utils.asDeferred
import myra.bot.voice.voice.VoiceConnection

object VoiceApi {
    lateinit var token: String
    lateinit var id: String
    var gateway: GatewayClient? = null

    fun connectGateway() {
        gateway = GatewayClient(token)
        gateway?.connect() ?: throw Exception("Couldn't create a gateway client")
    }

    suspend fun awaitReady() = asDeferred {
        while (true) {
            val ready = gateway?.ready ?: throw IllegalStateException("Call #connectGateway first")
            if (ready) return@asDeferred
            delay(250)
        }
    }.await()

    suspend fun connect(voiceState: VoiceStateUpdate): VoiceConnection {
       return gateway?.requestConnection(voiceState) ?: throw IllegalStateException("Connect to the gateway client first")
    }
}