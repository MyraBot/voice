package myra.bot.voice

import myra.bot.voice.gateway.commands.VoiceStateUpdate
import myra.bot.voice.voice.VoiceConnection

suspend fun main() {
    VoiceApi.apply {
        token = "ODcxNjk0NDY5OTc0NTkzNjI3.YQfCvA.llaswdiV4PrNNGy6ORLaULCD_Y8"
        connectGateway()
    }.await()

    val server = VoiceStateUpdate(
        guildId = "642809436515074053",
        channelId = "712567184727212094",
        selfMute = false,
        selfDeaf = false
    )
    val connection: VoiceConnection = VoiceApi.connect(server)
    connection.openVoiceGatewayConnection()

    while (true) {

    }
}