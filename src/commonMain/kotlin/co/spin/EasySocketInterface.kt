package co.spin


@ExperimentalUnsignedTypes
expect class EasySocketInterface(url: Url,
                                 delegate: SocketDelegate?) : WebSocketInterface {


    override var url: Url
    override var delegate: SocketDelegate?
    override var state: SocketState


    /**
     * Open the websocket connection.
     */
    override suspend fun open()
    override fun close()
    override fun send(message: String)


}