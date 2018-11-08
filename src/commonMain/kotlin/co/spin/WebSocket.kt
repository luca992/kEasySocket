package co.spin

enum class SocketState {
    SocketConnecting,
    SocketOpen,
    SocketClosing,
    SocketClosed
}

abstract class WebSocket(
        var url: String,
        var delegate: SocketDelegate?) {
    /**
     * Open the websocket connection.
     */
    abstract fun open()

    /**
     * Close the websocket connection.
     */
    abstract fun close()

    /**
     * Send a message over websockets.
     */
    abstract fun send(message: String)

    /**
     * Get SocketState.
     */
    abstract fun getSocketState(): SocketState

}