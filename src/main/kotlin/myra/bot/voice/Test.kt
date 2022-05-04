package myra.bot.voice

import myra.bot.voice.gateway.models.hello.VoiceState

suspend fun main() {
    VoiceApi.apply {
        token = "ODcxNjk0NDY5OTc0NTkzNjI3.YQfCvA.llaswdiV4PrNNGy6ORLaULCD_Y8"
        connectGateway()
    }

    val server1 = VoiceState(
        guildId = "851809328650518568",
        channelId = "876079099943202926",
        selfMute = false,
        selfDeaf = false
    )

    val server2 = VoiceState(
        guildId = "642809436515074053",
        channelId = "712567184727212094",
        selfMute = false,
        selfDeaf = false
    )

    VoiceApi.connect(server1)
    VoiceApi.connect(server2)

    while (true) {

    }
}