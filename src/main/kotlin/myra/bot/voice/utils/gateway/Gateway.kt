package myra.bot.voice.utils.gateway

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.websocket.send
import myra.bot.voice.gateway.models.Opcode
import myra.bot.voice.utils.toJson
import org.slf4j.Logger

abstract class Gateway(
    val url: String,
    val logger: Logger
) {
    val client = HttpClient(CIO) {
        install(WebSockets)
        expectSuccess = true
    }

    var sequence: Int = 0
    var socket: DefaultClientWebSocketSession? = null

    internal suspend fun send(opcode: Opcode) {
        logger.debug(">>> ${opcode.toJson()}")
        socket?.send(opcode.toJson())
    }

}