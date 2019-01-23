package co.spin

import co.spin.ezwsclient.ReadyStateValues
import co.spin.ezwsclient.WebSocket
import co.spin.utils.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@ExperimentalUnsignedTypes
class EasySocketWrapper(url: Url, delegate: SocketDelegate) : WebSocketWrapper(url, delegate) {


    /*!< Queue used for receiving messages. */
    private val receiveQueue : ThreadPool = ThreadPool(1)

    /*!< The mutex used when sending/polling messages over the socket. */
    private val socketMutex =  Mutex()

    /*!< The underlying socket EasySocketWrapper wraps. */
    private var socket: WebSocket? = null



    /**
     * Open the websocket connection.
     */
    override suspend fun open() {
        socket = co.spin.ezwsclient.WebSocket.fromUrl(url)
        if (socket==null){
            state = SocketState.SocketClosed
            delegate?.webSocketDidError(this, "")
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


        val callback = {message : String ->
            receiveQueue.enqueue {
                delegate?.webSocketDidReceive(this, message)

            }
        }
        val j = GlobalScope.launch(EzSocketDispatchers.Default) {
            while (shouldContinueLoop) {
                //Log.debug { "${ws.readyState.name}" }
                when (ws.readyState) {
                    ReadyStateValues.CLOSED -> {
                        state = SocketState.SocketClosed
                        delegate?.webSocketDidClose(this@EasySocketWrapper, 0, "", true)

                        // We got a CLOSED so the loop should stop.
                        shouldContinueLoop = false
                    }
                    ReadyStateValues.CLOSING -> {
                        state = SocketState.SocketClosing
                        GlobalScope.launch(EzSocketDispatchers.Default) {
                            socketMutex.withLock {
                                ws.poll()
                                ws.dispatchBinary(callback)
                            }
                        }
                    }
                    ReadyStateValues.CONNECTING -> {
                        state = SocketState.SocketConnecting
                        GlobalScope.launch(EzSocketDispatchers.Default) {
                            socketMutex.withLock {
                                Log.debug { "connecting in lock" }
                                ws.poll()
                                ws.dispatchBinary(callback)
                            }
                        }
                    }
                    ReadyStateValues.OPEN -> {
                        state = SocketState.SocketOpen
                        if (!triggeredWebsocketJoinedCallback) {
                            triggeredWebsocketJoinedCallback = true;
                            delegate?.webSocketDidOpen(this@EasySocketWrapper)
                        }
                        GlobalScope.launch(EzSocketDispatchers.Default) {
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
        GlobalScope.launch(EzSocketDispatchers.Default) {
            socketMutex.withLock {
                if (socket != null && state == SocketState.SocketOpen) {
                    socket!!.sendMessage(message)
                }
            }
        }
    }



}