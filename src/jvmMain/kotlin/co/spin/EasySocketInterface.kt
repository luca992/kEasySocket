package co.spin

import co.spin.ezwsclient.ReadyStateValues
import co.spin.utils.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.features.websocket.*
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.filterNotNull
import kotlinx.coroutines.channels.map


@ExperimentalUnsignedTypes
actual class EasySocketInterface
actual constructor(actual override var url: Url,
                   actual override var delegate: SocketDelegate?) : WebSocketInterface {


    /*!< Queue used for receiving messages. */
    private val receiveQueue : ThreadPool = ThreadPool(1)

    /*!< The mutex used when sending/polling messages over the socket. */
    private val socketMutex =  Mutex()

    val client = HttpClient(CIO).config {
        install(WebSockets)
        install(Logging) {
            level = LogLevel.ALL
        }}


    actual override var state: SocketState = SocketState.SocketClosed

    val defaultClientWebSocketSession : suspend DefaultClientWebSocketSession.() -> Unit = {
        println("onConnect")
        try {
            while (true) {
                val text = (incoming.receive() as Frame.Text).readText()
                println("onMessage")
                println(text)
            }
        } catch (e: ClosedReceiveChannelException) {
            println("onClose ${closeReason.await()}")
        } catch (e: Throwable) {
            println("onError ${closeReason.await()}")
            e.printStackTrace()
        }
    }


    /**
     * Open the websocket connection.
     */
    actual override suspend fun open() {
        println(11111111111)
        try {
            client.wss(method = HttpMethod.Get, host = url.host, port = url.port, path = url.path,
                    block = defaultClientWebSocketSession)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        println(444444444444)

        /*       socket = co.spin.ezwsclient.WebSocket.fromUrl(url)
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
                               delegate?.webSocketDidClose(this@EasySocketInterface, 0, "", true)

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
                                   delegate?.webSocketDidOpen(this@EasySocketInterface)
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
       */
    }

    /**
     * Close the websocket connection.
     */
    actual override fun close() {
        state = SocketState.SocketClosed;
        // Was already closed or never opened.
        client.close()
    }

    /**
     * Send a message over websockets.
     */
    actual override fun send(message: String) {
        /*GlobalScope.launch(EzSocketDispatchers.Default) {
            socketMutex.withLock {
                if (socket != null && state == SocketState.SocketOpen) {
                    socket!!.sendMessage(message)
                }
            }
        }*/
    }



}