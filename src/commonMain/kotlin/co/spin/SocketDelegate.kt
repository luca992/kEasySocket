package co.spin

abstract class SocketDelegate {
    /**
     * Callback received when Websocket is connected.
     */
    abstract fun webSocketDidOpen(socket: WebSocketWrapper)

    /**
     * Callback received when Websocket receives a message.
     */
    abstract fun webSocketDidReceive(socket: WebSocketWrapper, message: String)

    /**
     * Callback received when Websocket has an error.
     */
    abstract fun webSocketDidError(socket: WebSocketWrapper, error: String)

    /**
     * Callback received when Websocket closes.
     */
    abstract fun webSocketDidClose(socket: WebSocketWrapper, code: Int, reason :String, wasClean: Boolean)

}