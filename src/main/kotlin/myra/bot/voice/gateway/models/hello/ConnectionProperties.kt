package myra.bot.voice.gateway.models.hello

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionProperties(
    @SerialName("\$os") val os: String = "linux",
    @SerialName("\$browser") val browser: String = "idkyet",
    @SerialName("\$device") val device: String = "idkyet"
)