package co.spin

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TDispatchers
import kotlinx.coroutines.launch
import co.spin.ezwsclient.WebSocket
import co.spin.utils.Log
import kotlinx.coroutines.withContext

@ExperimentalUnsignedTypes
class EasySocket(url: String, delegate: SocketDelegate) : co.spin.WebSocket(url, delegate) {


    /*!< Queue used for receiving messages. */
    private val receiveQueue : ThreadPool

    /*!< The mutex used when sending/polling messages over the socket. */
    //private lateinit var socketMutex: Mutex;

    /*!< The underlying socket EasySocket wraps. */
    private var socket: WebSocket? = null

    /*!< Keep track of Socket State.
      This is used instead of easywsclient's SocketState. */
    private var state: SocketState

    init {
        receiveQueue = ThreadPool(1)
        state = SocketState.SocketClosed
    }


    override fun open() {
        socket = co.spin.ezwsclient.WebSocket.fromUrl(url)
        if (socket==null){
            state = SocketState.SocketClosed
            GlobalScope.launch(TDispatchers.Default) {
                delegate.webSocketDidError(this@EasySocket, "")
            }
            socket = null
            return
        }

        GlobalScope.launch(TDispatchers.Default) {
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
                Log.info {message}
                receiveQueue.enqueue {
                    delegate.webSocketDidReceive(this@EasySocket, message.toByteArray().stringFromUtf8())

                }
            }

            while (shouldContinueLoop) {
                when (ws.readyState) {
                    WebSocket.ReadyStateValues.CLOSED -> {
                        state = SocketState.SocketClosed
                        delegate.webSocketDidClose(this@EasySocket, 0, "", true)

                        // We got a CLOSED so the loop should stop.
                        shouldContinueLoop = false
                    }
                    WebSocket.ReadyStateValues.CLOSING-> {
                        state = SocketState.SocketClosing;
                        //std::lock_guard<std::mutex> guard(this->socketMutex);
                        ws.poll()
                        ws.dispatchBinary(callback)
                    }
                    WebSocket.ReadyStateValues.CONNECTING-> {
                        state = SocketState.SocketConnecting
                        //std::lock_guard<std::mutex> guard(this->socketMutex);
                        ws.poll()
                        ws.dispatchBinary(callback)
                    }
                    WebSocket.ReadyStateValues.OPEN -> {
                        state = SocketState.SocketOpen
                        if (!triggeredWebsocketJoinedCallback) {
                            triggeredWebsocketJoinedCallback = true;
                            getSocketState()
                            delegate.webSocketDidOpen(this@EasySocket);

                        }

                        //std::lock_guard<std::mutex> guard(this->socketMutex);
                        ws.poll()
                        ws.dispatchBinary(callback)
                    }
                    else -> {
                    }
                }
            }

            state = SocketState.SocketClosed
            socket = null
        }
    }
    override fun close() {
        state = SocketState.SocketClosed;
        // Was already closed or never opened.
        socket?.close()
    }

    override fun send(message: String) {
        GlobalScope.launch(TDispatchers.Default) {
            //std::lock_guard<std::mutex> guard(this->socketMutex);
            if (socket != null && state == SocketState.SocketOpen) {
                socket!!.send(message)
            }
        }
    }
    override fun getSocketState(): SocketState = state



}