package co.spin

abstract class SocketDelegate {
    /**
     * Callback received when Websocket is connected.
     */
    abstract fun webSocketDidOpen(socket: PhnxWebSocket)

    /**
     * Callback received when Websocket receives a message.
     */
    abstract fun webSocketDidReceive(socket: PhnxWebSocket, message: String)

    /**
     * Callback received when Websocket has an error.
     */
    abstract fun webSocketDidError(socket: PhnxWebSocket, error: String)

    /**
     * Callback received when Websocket closes.
     */
    abstract fun webSocketDidClose(socket: PhnxWebSocket, code: Int, reason :String, wasClean: Boolean)

}