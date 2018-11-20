package co.spin

import co.spin.utils.Log
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

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
        val url: Url,
        /*!< The interval at which to send heartbeats to server. */
        val heartBeatInterval: Int = 1,
        /*!
        The underlying WebSocket interface. This can be used with a
        different library provided the WebSocket interface is implemented.*/
        var socket: EasySocketPhnx? = null) : SocketDelegate() {

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

    /*!< Ref to keep track of for each WebSocket message. */
    private var ref = 0L

    /**
     *  \brief Stops the heartbeating.
     *
     *  \return void
     */
    private fun discardHeartBeatTimerSuspended() {
        setCanSendHeartBeatSuspended(false)
    }

    private fun discardHeartBeatTimer() {
        setCanSendHeartBeat(false)
    }

    /*!< Flag indicating whether or not to continue sending heartbeats. */
    private var canSendHeartbeat : Boolean = false

    /**
     *  \brief Stops trying to reconnect the WebSocket.
     *
     *  \return void
     */
    private fun discardReconnectTimer() {
        setCanReconnect(false)
    }

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
    private fun disconnectSocket() {
        socket?.apply {
            delegate = null
            close()
        }
        socket = null
    }

    /**
     *  \brief Function called when WebSocket opens.
     *
     *  \return void
     */
    private fun onConnOpen() {
        discardReconnectTimer()

        // After the socket connection is opened, continue to send heartbeats
        // to keep the connection alive.
        if (heartBeatInterval > 0) {

            GlobalScope.launch(EzSocketDispatchers.Default) {
                // Use sleep_for to wait specified time (or sleep_until).
                delay(heartBeatInterval * 1000L /*interval in seconds*/)
                setCanSendHeartBeatSuspended(true)
                while (true) {
                    delay(heartBeatInterval * 1000L /*interval in seconds*/)
                    if (canSendHeartbeat) {
                        Log.debug { "enqueue sendHeartbeat" }
                        pool.enqueue { sendHeartbeat() }
                    } else {
                        break
                    }
                }
            }
        }

        for (callback in openCallbacks) {
            callback()
        }

        delegate?.phxSocketDidOpen()
    }

    /**
     *  \brief Function called when WebSocket closes.
     *
     *  \param event The event that caused the close.
     *  \return void
     */
    private fun onConnClose(event: String) {
        triggerChanError(event)

        // When connection is closed, attempt to reconnect.
        if (reconnectOnError) {
            if (!reconnecting) {
                reconnecting = true
                canReconnect = true

                GlobalScope.launch(EzSocketDispatchers.Default) {
                    // Use sleep_for to wait specified time (or sleep_until).
                    delay(RECONNECT_INTERVAL * 1000L /*interval in seconds*/)
                    pool.enqueue {
                        if (canReconnect) {
                            canReconnect = false
                            GlobalScope.launch(EzSocketDispatchers.Default) {
                                reconnect()
                            }
                        }
                        reconnecting = false
                    }
                }
            }
        }

        discardHeartBeatTimer()

        for (callback in closeCallbacks) {
            callback(event)
        }

        delegate?.phxSocketDidClose(event)
    }

    /**
     *  \brief Function called when there was an error with the connection.
     *
     *  \param error The error message.
     *  \return void
     */
    private fun onConnError(error: String) {
        discardHeartBeatTimer()

        for (callback in errorCallbacks) {
            callback(error)
        }

        delegate?.phxSocketDidReceiveError(error)

        onConnClose(error)
    }

    /**
     *  \brief Function called when WebSocket receives a message.
     *
     *  \param rawMessage The message as a std::string.
     *  \return void
     */
    private fun onConnMessage(rawMessage: String) {
        val json = JsonTreeParser(rawMessage).read() as JsonObject
        val jsonTopic = json.get("topic").content
        val jsonEvent = json.get("event").content
        val jsonRef = json.getPrimitiveOrNull("ref")
        val jsonPayload = json.get("payload").jsonObject

        // Ref can be null, so check for it first.
        var ref = -1L
        if (jsonRef!=null) {
            ref = jsonRef.long
        }

        for (channel in channels) {
            if (channel.topic == jsonTopic) {
                channel.triggerEvent(jsonEvent, jsonPayload, ref)
            }
        }

        for (callback in messageCallbacks) {
            callback(json)
        }
    }

    /**
     *  \brief Triggers a "phx_error" event to all channels.
     *
     *  \param error The error message.
     *  \return void
     */
    private fun triggerChanError(error: String) {
        for (channel in channels) {
            channel.triggerEvent("phx_error", JsonPrimitive(error), 0)
        }

    }

    /**
     *  \brief Sends a heartbeat to keep Websocket connection alive.
     *
     *  \return void
     */
    private fun sendHeartbeat() {
        push(JsonObject(mapOf(
                "topic" to JsonPrimitive("phoenix") ,
                "event" to JsonPrimitive("heartbeat"),
                "payload" to JsonObject(mapOf()),
                "ref" to JsonPrimitive(makeRef())))
        )
    }

    /**
     *  \brief Sets this->canSendHeartbeat.
     *
     *  This is intended to be a semi-thread safe way to set this flag.
     *
     *  \param canSendHeartbeat Indicating whether or not this socket can
     *  continue sending heartbeats.
     *  \return void
     */
    private fun setCanSendHeartBeatSuspended(canSendHeartbeat: Boolean) {
        pool.enqueue {
            this.canSendHeartbeat = canSendHeartbeat
        }
    }

    private fun setCanSendHeartBeat(canSendHeartbeat: Boolean) {
        pool.enqueue {
            this.canSendHeartbeat = canSendHeartbeat
        }
    }

    /**
     *  \brief Sets this->canReconnect.
     *
     *  This is intended to be a semi-thread safe way to set this flag.
     *
     *  \param canReconnect Indicating whether or not the socket can reconnect.
     *  \return void
     */
    private fun setCanReconnect(canReconnect: Boolean) {
        pool.enqueue { this.canReconnect = canReconnect }
    }

    // SocketDelegate
    override fun webSocketDidOpen(socket: PhnxWebSocket) {
        pool.enqueue {
            onConnOpen()
        }
    }
    override fun webSocketDidReceive(socket: PhnxWebSocket, message: String) {
        pool.enqueue { onConnMessage(message) }
    }
    override fun webSocketDidError(socket: PhnxWebSocket, error: String) {
        pool.enqueue { onConnError(error) }
    }
    override fun webSocketDidClose(socket: PhnxWebSocket, code: Int, reason: String, wasClean: Boolean) {
        pool.enqueue { onConnClose(reason) }
    }


    /**
     *  \brief Connects the Websocket.
     *
     *  \param params List of params to be formatted into Websocket URL.
     *  \return void
     */
    fun connect() : Job {
        setCanReconnect(false)

        // The socket hasn't been instantiated with a custom WebSocket.
        if (socket==null) {
            this.socket = EasySocketPhnx(url, this)
        }
        return GlobalScope.launch {
            socket!!.open()
        }
    }

    /**
     *  \brief Disconnects the socket connection.
     *
     *  \return void
     */
    fun disconnect() {
        discardHeartBeatTimer()
        discardReconnectTimer()
        disconnectSocket()
    }

    /**
     *  \brief Reconnects the socket after disconnection.
     *
     *  The reconnection happens on a timer controlled by RECONNECT_INTERVAL.
     *
     *  \return void
     */
    private suspend fun reconnect() {
        disconnectSocket()
        connect()
    }

    /**
     *  \brief Adds a callback on open.
     *
     *  \param callback
     *  \return void
     */
    fun onOpen(callback: OnOpen) {
        openCallbacks.add(callback)
    }

    /**
     *  \brief Adds a callback on close.
     *
     *  \param callback
     *  \return void
     */
    fun onClose(callback: OnClose) {
        closeCallbacks.add(callback)
    }

    /**
     *  \brief Adds a callback on error.
     *
     *  \param callback
     *  \return void
     */
    fun onError(callback: OnError) {
        errorCallbacks.add(callback)
    }

    /**
     *  \brief Adds a callback on message.
     *
     *  \param callback
     *  \return void
     */
    fun onMessage(callback: OnMessage) {
        messageCallbacks.add(callback)
    }

    /**
     *  \brief Flag indicating whether or not socket is connected.
     *
     *  \return bool Indicating connected status.
     */
    fun isConnected() : Boolean = socketState() == SocketState.SocketOpen


    /**
     *  \brief Make a unique reference per message sent to Phoenix Server.
     *
     *  \return Long
     */
    fun makeRef() : Long {
        return ref++
    }

    /**
     *  \brief The current state of the socket connection.
     *
     *  \return SocketState
     */
    fun socketState(): SocketState {
        if (socket == null) {
            return SocketState.SocketClosed;
        }
        return socket!!.state
    }

    /**
     *  \brief Send data through websockets.
     *
     *  \param data The json data to send.
     *  \return void
     */
    fun push(data: JsonElement) {
        try {
            println("JSON: ${data.toString()}")
            socket?.send(data.toString())
        } catch ( e:Exception) {
            e.printStackTrace()
        }
    }

    /**
     *  \brief Adds PhxChannel to list of channels.
     *
     *  \return void
     */
    fun addChannel(channel: PhxChannel) {
        channels.add(channel)
    }

    /**
     *  \brief Removes PhxChannel from list of channels.
     *
     *  \return void
     */
    fun removeChannel(channel: PhxChannel) {
        channels = channels.filter{ it!=channel}.toMutableList()
    }

    /**
     *  \brief Sets the PhxSocketDelegate.
     *
     *  this->delegate will be weakly held by PhxSocket.
     */
    fun setDelegate(delegate: PhxSocketDelegate) {
        this.delegate = delegate
    }
}