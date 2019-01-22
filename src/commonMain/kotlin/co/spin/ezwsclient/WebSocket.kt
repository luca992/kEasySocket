package co.spin.ezwsclient

import co.spin.Url
import kotlinx.coroutines.Job

enum class ReadyStateValues { CLOSING, CLOSED, CONNECTING, OPEN }

expect open class WebSocket (url: Url, useMask: Boolean = true, origin: String = "") {

    var readyState: ReadyStateValues

    fun send(buf: String?) : Long

    fun poll(timeout : Int = 0)

    fun dispatchBinary(callback: (String) -> Unit): Job

    fun sendMessage(message: String)

    fun sendBinary(message: String)

    fun sendBinary(message: ByteArray)

    fun sendPing()

    fun close()


    companion object {

        fun fromUrl(url : Url, origin: String = ""): WebSocket

        fun fromUrlNoMask(url : Url, origin: String): WebSocket

    }
}


