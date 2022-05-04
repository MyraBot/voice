package myra.bot.voice.connection.udp

import myra.bot.voice.connection.gateway.VoiceGateway

class VoiceConnection {

    val gateway = VoiceGateway()
    var udp: UdpSocket? = null

}