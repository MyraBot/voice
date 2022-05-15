package myra.bot.voice

import myra.bot.voice.voice.udp.VoiceConnection
import myra.bot.voice.gateway.commands.VoiceStateUpdate

suspend fun main() {
    VoiceApi.apply {
        token = "ODcxNjk0NDY5OTc0NTkzNjI3.YQfCvA.llaswdiV4PrNNGy6ORLaULCD_Y8"
        connectGateway()
    }.await()


    val server1 = VoiceStateUpdate(
        guildId = "851809328650518568",
        channelId = "876079099943202926",
        selfMute = false,
        selfDeaf = false
    )

    val server2 = VoiceStateUpdate(
        guildId = "642809436515074053",
        channelId = "712567184727212094",
        selfMute = false,
        selfDeaf = false
    )

    /*
    val mainConnection: VoiceConnection = VoiceApi.connect(server1).apply {

    }*/
    val testConnection: VoiceConnection = VoiceApi.connect(server2)



    while (true) {

    }
}