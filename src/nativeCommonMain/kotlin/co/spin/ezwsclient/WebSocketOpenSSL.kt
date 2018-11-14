package co.spin.ezwsclient

class WebSocketOpenSSL(url: Url, useMask : Boolean) : WebSocket(url, useMask) {


    override fun connect(hostname : String, port : Int): ULong {
    return 1u
    }
}