# 🎺 Voice

A Kotlin Discord Voice API wrapper kept very, *very* simple. This is for demonstration purpose only and **shouldn't be used in production**!
I made this library to understand how the Voice API works and get a basic example down.

The library handles the voice sockets as well as the main gateway. Both are really simple and do not feature full support for everything.

## 🏗️ Example

The example uses [Lavaplayer](https://github.com/walkyst/lavaplayer-fork) to get opus encoded audio.

```kotlin
suspend fun main() = coroutineScope {
    VoiceApi.apply {
        token = "YOUR_BOT_TOKEN"
        connectGateway()
    }.awaitReady()

    val playerManager = DefaultAudioPlayerManager()
    playerManager.registerSourceManager(YoutubeAudioSourceManager())
    val track = suspendCoroutine<AudioTrack> { c ->
        playerManager.loadItem(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
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
        selfDeaf = false
    )

    val connection: VoiceConnection = VoiceApi.connect(server)
    connection.openConnection()

    val player = playerManager.createPlayer()
    player.playTrack(track)

   connection.udp?.audioProvider?.provide { player.provide()?.data }

    while (true) {
    }
}
```

## 📚 Libraries used

* [Ktor](https://github.com/ktorio/ktor) - responsible for websockets
* [xsalsa20poly1305](https://github.com/codahale/xsalsa20poly1305) - Voice encryption
* [Lavaplayer](https://github.com/walkyst/lavaplayer-fork) - Getting Opus audio
* [Logback](https://github.com/qos-ch/logback) - Logging
* [Kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization) - Serializing/Deserializing opus packets