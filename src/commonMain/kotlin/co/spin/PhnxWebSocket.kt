package co.spin

enum class SocketState {
    SocketConnecting,
    SocketOpen,
    SocketClosing,
    SocketClosed
}

abstract class PhnxWebSocket(
        var url: Url,
        var delegate: SocketDelegate?) {
    /**
     * Open the websocket connection.
     */
    abstract suspend fun open()

    /**
     * Close the websocket connection.
     */
    abstract fun close()

    /**
     * Send a message over websockets.
     */
    abstract fun send(message: String)
}