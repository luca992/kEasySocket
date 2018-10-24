package co.spin

abstract class SocketDelegate {
    /**
     * Callback received when Websocket is connected.
     */
    abstract fun webSocketDidOpen(socket: WebSocket)

    /**
     * Callback received when Websocket receives a message.
     */
    abstract fun webSocketDidReceive(socket: WebSocket, message: String)

    /**
     * Callback received when Websocket has an error.
     */
    abstract fun webSocketDidError(socket: WebSocket, error: String)

    /**
     * Callback received when Websocket closes.
     */
    abstract fun webSocketDidClose(socket: WebSocket, code: Int, reason :String, wasClean: Boolean)

}