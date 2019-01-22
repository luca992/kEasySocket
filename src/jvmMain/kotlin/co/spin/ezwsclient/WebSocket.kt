package co.spin.ezwsclient

import co.spin.Url
import kotlinx.coroutines.Job
import co.spin.ezwsclient.ReadyStateValues.*


actual open class WebSocket actual constructor(url: Url, useMask: Boolean, origin: String) {

    actual var readyState: ReadyStateValues = OPEN

    actual fun send(buf: String?) : Long { TODO()}

    actual fun poll(timeout : Int) { TODO()}

    actual fun dispatchBinary(callback: (String) -> Unit): Job { TODO()}

    actual fun sendMessage(message: String) { TODO()}

    actual fun sendBinary(message: String) { TODO()}

    actual fun sendBinary(message: ByteArray) { TODO()}

    actual fun sendPing() { TODO()}

    actual fun close() { TODO()}


    actual companion object {

        actual fun fromUrl(url : Url, origin: String): WebSocket { TODO()}

        actual fun fromUrlNoMask(url : Url, origin: String): WebSocket { TODO()}

    }
}


