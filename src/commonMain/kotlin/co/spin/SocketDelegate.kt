package co.spin

abstract class SocketDelegate {
    /**
     * Callback received when Websocket is connected.
     */
    abstract fun webSocketDidOpen(socket: WebSocketInterface)

    /**
     * Callback received when Websocket receives a message.
     */
    abstract fun webSocketDidReceive(socket: WebSocketInterface, message: String)

    /**
     * Callback received when Websocket has an error.
     */
    abstract fun webSocketDidError(socket: WebSocketInterface, error: String)

    /**
     * Callback received when Websocket closes.
     */
    abstract fun webSocketDidClose(socket: WebSocketInterface, code: Int, reason :String, wasClean: Boolean)

}