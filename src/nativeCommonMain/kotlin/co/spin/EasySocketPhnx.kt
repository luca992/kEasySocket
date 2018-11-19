package co.spin

import co.spin.ezwsclient.WebSocket
import co.spin.utils.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@ExperimentalUnsignedTypes
class EasySocketPhnx(url: Url, delegate: SocketDelegate) : PhnxWebSocket(url, delegate) {


    /*!< Queue used for receiving messages. */
    private val receiveQueue : ThreadPool = ThreadPool(1)

    /*!< The mutex used when sending/polling messages over the socket. */
    private val socketMutex =  Mutex()

    /*!< The underlying socket EasySocketPhnx wraps. */
    private var socket: WebSocket? = null

    /*!< Keep track of Socket State.
      This is used instead of easywsclient's SocketState. */
    var state: SocketState = SocketState.SocketClosed


    /**
     * Open the websocket connection.
     */
    override suspend fun open() {
        socket = co.spin.ezwsclient.WebSocket.fromUrl(url)
        if (socket==null){
            state = SocketState.SocketClosed
            delegate?.webSocketDidError(this@EasySocketPhnx, "")
            socket = null
            return
        }


        val ws = socket!!
        // This worker thread will continue to loop as long as the Websocket
        // is connected. Once we get a CLOSED message, this will be set to
        // false and the loop (and thread) will be exited.
        var shouldContinueLoop = true

        // We use this flag to track if we've triggered the webSocketDidOpen
        // yet. The first time we encounter CONNECTED while looping, trigger
        // the callback and then set this to true so we only do it once.
        var triggeredWebsocketJoinedCallback = false


        val callback = {message :UByteArray ->
            receiveQueue.enqueue {
                delegate?.webSocketDidReceive(this@EasySocketPhnx, message.toByteArray().stringFromUtf8())

            }
        }
        val j = GlobalScope.launch(TDispatchers.Default) {
            while (shouldContinueLoop) {
                Log.debug { "${ws.readyState.name}" }
                when (ws.readyState) {
                    WebSocket.ReadyStateValues.CLOSED -> {
                        state = SocketState.SocketClosed
                        delegate?.webSocketDidClose(this@EasySocketPhnx, 0, "", true)

                        // We got a CLOSED so the loop should stop.
                        shouldContinueLoop = false
                    }
                    WebSocket.ReadyStateValues.CLOSING -> {
                        state = SocketState.SocketClosing
                        GlobalScope.launch(TDispatchers.Default) {
                            socketMutex.withLock {
                                ws.poll()
                                ws.dispatchBinary(callback)
                            }
                        }
                    }
                    WebSocket.ReadyStateValues.CONNECTING -> {
                        state = SocketState.SocketConnecting
                        GlobalScope.launch(TDispatchers.Default) {
                            socketMutex.withLock {
                                Log.debug { "connecting in lock" }
                                ws.poll()
                                ws.dispatchBinary(callback)
                            }
                        }
                    }
                    WebSocket.ReadyStateValues.OPEN -> {
                        state = SocketState.SocketOpen
                        if (!triggeredWebsocketJoinedCallback) {
                            triggeredWebsocketJoinedCallback = true;
                            delegate?.webSocketDidOpen(this@EasySocketPhnx)
                        }
                        GlobalScope.launch(TDispatchers.Default) {
                            socketMutex.withLock {
                                ws.poll()
                                ws.dispatchBinary(callback)
                            }
                        }
                    }
                }
                delay(500L)
            }
        }
        j.join()

        Log.debug { "DONE???" }
        state = SocketState.SocketClosed
        socket = null

    }

    /**
     * Close the websocket connection.
     */
    override fun close() {
        state = SocketState.SocketClosed;
        // Was already closed or never opened.
        socket?.close()
    }

    /**
     * Send a message over websockets.
     */
    override fun send(message: String) {
        GlobalScope.launch(TDispatchers.Default) {
            socketMutex.withLock {
                if (socket != null && state == SocketState.SocketOpen) {
                    socket!!.send(message)
                }
            }
        }
    }



}