package co.spin.ezwsclient


interface Callback_Imp{
    operator fun Callback_Imp.invoke()
}

const val INVALID_SOCKET  : UInt = UInt.MAX_VALUE
const val SOCKET_ERROR  : Int = -1

class WebSocket{
    enum class ReadyStateValues { CLOSING, CLOSED, CONNECTING, OPEN }
    //var readyState : ReadyStateValues


    constructor()
    fun poll(timeout : Int){
        TODO()
    }

    fun send(message: String){TODO()}
    fun sendBinary(message: String){TODO()}
    fun sendBinary(message: ByteArray){TODO()}
    fun sendPing(){TODO()}
    fun close(){TODO()}


    companion object {

        fun from_url(url :String, origin: String): WebSocket {
            TODO()
        }

        fun from_url_no_mask(url :String, origin: String): WebSocket {
            TODO()
        }
    }
}


