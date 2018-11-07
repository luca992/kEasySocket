package co.spin
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

enum class ChannelState { CLOSED, ERRORED, JOINING, JOINED }

typealias OnOpen = () -> Unit
typealias OnClose = (event: String) -> Unit
typealias OnError = (error: String) -> Unit
typealias OnMessage = (json: JsonElement) -> Unit
typealias OnReceive = (message: JsonElement, ref: Long) -> Unit
typealias After = () -> Unit