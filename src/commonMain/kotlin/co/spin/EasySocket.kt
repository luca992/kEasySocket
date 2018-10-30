package co.spin

//import kotlinx.coroutines.sync.*

class EasySocket(url: String, delegate: SocketDelegate) : WebSocket(url, delegate) {


    /*!< Queue used for receiving messages. */
    private val receiveQueue : ThreadPool

    /*!< The mutex used when sending/polling messages over the socket. */
    //private lateinit var socketMutex: Mutex;

    /*!< The underlying socket EasySocket wraps. */
    private lateinit var socket: WebSocket;

    /*!< Keep track of Socket State.
      This is used instead of easywsclient's SocketState. */
    private var state: SocketState

    init {
        receiveQueue = ThreadPool(1)
        state = SocketState.SocketClosed
    }


    override fun open() = Unit
    override fun close() = Unit
    override fun send(message: String) = Unit
    override fun getSocketState(): SocketState = state
    override fun setURL(url: String) = Unit



}