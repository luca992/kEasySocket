package co.spin
import kotlinx.serialization.json.JSON

enum class ChannelState { CLOSED, ERRORED, JOINING, JOINED }

typealias OnOpen = () -> Unit
typealias OnClose = (event: String) -> Unit
typealias OnError = (error: String) -> Unit
typealias OnMessage = (json: JSON) -> Unit
typealias OnReceive = (message: JSON, ref: Long) -> Unit
typealias After = () -> Unit