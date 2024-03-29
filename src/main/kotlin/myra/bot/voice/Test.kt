package myra.bot.voice

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.coroutineScope
import myra.bot.voice.gateway.commands.VoiceStateUpdate
import myra.bot.voice.voice.VoiceConnection
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun main() = coroutineScope {
    VoiceApi.apply {
        token = "ODcxNjk0NDY5OTc0NTkzNjI3.YQfCvA.llaswdiV4PrNNGy6ORLaULCD_Y8"
        connectGateway()
    }.awaitReady()

    val playerManager = DefaultAudioPlayerManager()
    playerManager.registerSourceManager(YoutubeAudioSourceManager())
    val track = suspendCoroutine<AudioTrack> { c ->
        playerManager.loadItem(
            "https://www.youtube.com/watch?v=H0n7HoBpP6s",
            FunctionalResultHandler(
                { c.resume(it) },
                { c.resume(it.tracks.first()) },
                {},
                {}
            )
        )
    }

    val server = VoiceStateUpdate(
        guildId = "642809436515074053",
        channelId = "712567184727212094",
        selfMute = false,
        selfDeaf = false)

    val connection: VoiceConnection = VoiceApi.connect(server)
    connection.openConnection()

    val player = playerManager.createPlayer()
    player.playTrack(track)

   connection.udp?.audioProvider?.provide { player.provide()?.data }

    while (true) {
    }
}