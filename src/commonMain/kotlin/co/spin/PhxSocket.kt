package co.spin

import kotlinx.serialization.json.JsonElement

abstract class PhxSocketDelegate {
    abstract fun phxSocketDidOpen()
    abstract fun phxSocketDidClose(event: String)
    abstract fun phxSocketDidReceiveError(error: String)
}


const val RECONNECT_INTERVAL =  5
const val POOL_SIZE = 1


/**
 *  \brief Constructor with custom WebSocket implementation.
 *
 *  \param url The URL to connect to.
 *  \param interval The heartbeat interval.
 *  \param socket the Custom WebSocket implementation.
 *  \return return type
 */
class PhxSocket(
        /*!< Websocket URL to connect to. */
        val url: String,
        /*!< The interval at which to send heartbeats to server. */
        val heartBeatInterval: Int = 1,
        /*!
        The underlying WebSocket interface. This can be used with a
        different library provided the WebSocket interface is implemented.*/
        val socket: WebSocket? = null) : SocketDelegate() {

    /*!< Single Thread Thread Pool used for synchronization. */
    private var pool =  ThreadPool(POOL_SIZE)

    /*! Delegate that can listen in on Phoenix related callbacks. */
    private var delegate : PhxSocketDelegate? = null


    /*!< Flag indicating whether or not to reconnect when socket errors out. */
    private var reconnectOnError = true

    /*!< The list of channels interested in sending messages over this socket.
     */
    private var channels = mutableListOf<PhxChannel>()

    /*!< List of callbacks when socket opens. */
    private var openCallbacks = mutableListOf<OnOpen>()

    /*!< List of callbacks when socket closes. */
    private var closeCallbacks = mutableListOf<OnClose>()

    /*!< List of callbacks when socket errors out. */
    private var errorCallbacks = mutableListOf<OnError>()

    /*!< List of callbacks when socket receives a messages. */
    private var messageCallbacks = mutableListOf<OnMessage>()

    /*!< These params are used to pass arguments into the Websocket URL. */
    private var params = mapOf<String, String>()

    /*!< Ref to keep track of for each WebSocket message. */
    private var ref = 0

    /**
     *  \brief Stops the heartbeating.
     *
     *  \return void
     */
    private fun discardHeartBeatTimer() = TODO()

    /*!< Flag indicating whether or not to continue sending heartbeats. */
    private fun canSendHeartbeat() : Boolean = TODO()

    /**
     *  \brief Stops trying to reconnect the WebSocket.
     *
     *  \return void
     */
    private fun discardReconnectTimer() = TODO()

    /*!< Flag indicating whether or not socket can reconnect to server. */
    private var canReconnect = false

    /*!< Flag indicating whether or not we are in the process of reconnecting.
     */
    private var reconnecting = false

    /**
     *  \brief Disconnects the socket.
     *
     *  \return void
     */
    private fun disconnectSocket() = TODO()

    /**
     *  \brief Function called when WebSocket opens.
     *
     *  \return void
     */
    private fun onConnOpen() = TODO()

    /**
     *  \brief Function called when WebSocket closes.
     *
     *  \param event The event that caused the close.
     *  \return void
     */
    private fun onConnClose(event: String) = TODO()

    /**
     *  \brief Function called when there was an error with the connection.
     *
     *  \param error The error message.
     *  \return void
     */
    private fun onConnError(error: String) = TODO()

    /**
     *  \brief Function called when WebSocket receives a message.
     *
     *  \param rawMessage The message as a std::string.
     *  \return void
     */
    private fun onConnMessage(rawMessage) = TODO()

    /**
     *  \brief Triggers a "phx_error" event to all channels.
     *
     *  \param error The error message.
     *  \return void
     */
    private fun triggerChanError(error: String) = TODO()

    /**
     *  \brief Sends a heartbeat to keep Websocket connection alive.
     *
     *  \return void
     */
    private fun sendHeartbeat() = TODO()

    /**
     *  \brief Sets this->canSendHeartbeat.
     *
     *  This is intended to be a semi-thread safe way to set this flag.
     *
     *  \param canSendHeartbeat Indicating whether or not this socket can
     *  continue sending heartbeats.
     *  \return void
     */
    private fun setCanSendHeartBeat(canSendHeartbeat: Boolean) = TODO()

    /**
     *  \brief Sets this->canReconnect.
     *
     *  This is intended to be a semi-thread safe way to set this flag.
     *
     *  \param canReconnect Indicating whether or not the socket can reconnect.
     *  \return void
     */
    private fun setCanReconnect(canReconnect: Boolean) = TODO()

    // SocketDelegate
    private fun webSocketDidOpen(socket: WebSocket) = TODO()
    private fun webSocketDidReceive(socket: WebSocket, message: String) = TODO()
    private fun webSocketDidError(socket: WebSocket, error: String) = TODO()
    private fun webSocketDidClose(socket: WebSocket, code: Int, reason: String, wasClean: Boolean) = TODO()
    // SocketDelegate
    public:
    /**
     *  \brief Connects the Websocket.
     *
     *  \return void
     */
    fun connect() = TODO()

    /**
     *  \brief Connects the Websocket.
     *
     *  \param params List of params to be formatted into Websocket URL.
     *  \return void
     */
    fun connect(params: Map<String, String>) = TODO()

    /**
     *  \brief Disconnects the socket connection.
     *
     *  \return void
     */
    fun disconnect() = TODO()

    /**
     *  \brief Reconnects the socket after disconnection.
     *
     *  The reconnection happens on a timer controlled by RECONNECT_INTERVAL.
     *
     *  \return void
     */
    fun reconnect() = TODO()

    /**
     *  \brief Adds a callback on open.
     *
     *  \param callback
     *  \return void
     */
    fun onOpen(callback: OnOpen) = TODO()

    /**
     *  \brief Adds a callback on close.
     *
     *  \param callback
     *  \return void
     */
    fun onClose(callback: OnClose) = TODO()

    /**
     *  \brief Adds a callback on error.
     *
     *  \param callback
     *  \return void
     */
    fun onError(callback: OnError) = TODO()

    /**
     *  \brief Adds a callback on message.
     *
     *  \param callback
     *  \return void
     */
    fun onMessage(callback: OnMessage) = TODO()

    /**
     *  \brief Flag indicating whether or not socket is connected.
     *
     *  \return bool Indicating connected status.
     */
    fun isConnected() : Boolean = TODO()

    /**
     *  \brief Make a unique reference per message sent to Phoenix Server.
     *
     *  \return int64_t
     */
    fun makeRef() : Long = TODO()

    /**
     *  \brief The current state of the socket connection.
     *
     *  \return SocketState
     */
    fun socketState(): SocketState = TODO()

    /**
     *  \brief Send data through websockets.
     *
     *  \param data The json data to send.
     *  \return void
     */
    fun push(data: JsonElement) = TODO()

    /**
     *  \brief Adds PhxChannel to list of channels.
     *
     *  \return void
     */
    fun addChannel(channel: PhxChannel) = TODO()

    /**
     *  \brief Removes PhxChannel from list of channels.
     *
     *  \return void
     */
    fun removeChannel(channel: PhxChannel) = TODO()

    /**
     *  \brief Sets the PhxSocketDelegate.
     *
     *  this->delegate will be weakly held by PhxSocket.
     */
    fun setDelegate(delegate: PhxSocketDelegate) = TODO()
};