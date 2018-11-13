package co.spin.ezwsclient

class WebSocketOpenSSL(useMask : Boolean) : WebSocket(useMask) {


    override fun connect(hostname : String, port : Int): ULong {
    return 1u
    }
}