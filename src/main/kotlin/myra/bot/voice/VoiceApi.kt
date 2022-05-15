package myra.bot.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import myra.bot.voice.voice.udp.VoiceConnection
import myra.bot.voice.gateway.GatewayClient
import myra.bot.voice.gateway.commands.VoiceStateUpdate
import myra.bot.voice.utils.asDeferred

object VoiceApi {
    lateinit var token: String
    private val scope = CoroutineScope(Dispatchers.Default)
    var gateway: GatewayClient? = null

    fun connectGateway() {
        gateway = GatewayClient(token)
        scope.launch { gateway?.connect() }
    }

    suspend fun await() = asDeferred {
        while (true) {
            val ready = gateway?.ready ?: throw IllegalStateException("Call #connectGateway first")
            if (ready) return@asDeferred
            delay(250)
        }
    }.await()

    suspend fun connect(voiceState: VoiceStateUpdate): VoiceConnection = gateway?.requestConnection(voiceState) ?: error("Connect to the gateway client first")
}