package co.spin

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import io.ktor.client.features.BadResponseStatusException

import kotlinx.coroutines.EzSocketDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex


@ExperimentalUnsignedTypes
actual class EasySocketInterface
actual constructor(actual override var url: Url,
                   actual override var delegate: SocketDelegate?) : WebSocketInterface {



    val client = HttpClient(CIO).config {
        install(WebSockets)
    }


    actual override var state: SocketState = SocketState.SocketClosed

    private var websocketSession: DefaultClientWebSocketSession? = null

    private val defaultClientWebSocketSession : suspend DefaultClientWebSocketSession.() -> Unit = {
        websocketSession = this
        state = SocketState.SocketOpen
        delegate?.webSocketDidOpen(this@EasySocketInterface)
        try {
            for (message in incoming.map { it as? Frame.Text }.filterNotNull()) {
                delegate?.webSocketDidReceive(this@EasySocketInterface, message.readText())
            }
        } catch (e: ClosedReceiveChannelException) {
            println("onClose ${closeReason.await()}")
            state = SocketState.SocketClosed
            val reason = closeReason.await()
            delegate?.webSocketDidClose(
                    this@EasySocketInterface,
                    reason?.code?.toInt() ?: 0,
                    reason.toString(),
                    true
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            state = SocketState.SocketClosed
            val reason = closeReason.await()
            delegate?.webSocketDidError(
                    this@EasySocketInterface,
                    reason.toString()
            )

        }
    }


    /**
     * Open the websocket connection.
     */
    actual override suspend fun open() {
        try {
            state = SocketState.SocketConnecting
            client.wss(method = HttpMethod.Get, host = url.host, port = url.port, path = url.path,
                    block = defaultClientWebSocketSession)
        } catch (e: BadResponseStatusException) {
            e.printStackTrace()
            state = SocketState.SocketClosed
            delegate?.webSocketDidError(
                    this@EasySocketInterface,
                    e.message.toString()
            )
        }
        catch (t: Throwable) {
            t.printStackTrace()
            state = SocketState.SocketClosed
            delegate?.webSocketDidError(
                    this@EasySocketInterface,
                    t.message.toString()
            )
        }
    }

    /**
     * Close the websocket connection.
     */
    actual override fun close() {
        state = SocketState.SocketClosed
        client.close()
        delegate?.webSocketDidClose(this@EasySocketInterface, 0, "", true)
    }

    /**
     * Send a message over websockets.
     */
    actual override fun send(message: String) {
        GlobalScope.launch(EzSocketDispatchers.Default) {
            if (state == SocketState.SocketOpen) {
                try {
                    websocketSession?.send(message)
                } catch (t: Throwable){
                    t.printStackTrace()
                }
            }
        }
    }



}