package co.spin.ezwsclient

import kotlinx.cinterop.*
import platform.posix.*
import platform.posix.size_t
import platform.windows.addrinfo
import platform.windows.getaddrinfo
import platform.windows.freeaddrinfo
import co.spin.utils.Log

interface Callback_Imp{
    operator fun Callback_Imp.invoke()
}

const val INVALID_SOCKET  : Int = -1
const val SOCKET_ERROR  : Int = -1

class WebSocket{
    enum class ReadyStateValues { CLOSING, CLOSED, CONNECTING, OPEN }
    //var readyState : ReadyStateValues

    private fun hostname_connect(hostname : String, port : Int) : Int {
        memScoped {
            val hints : addrinfo = alloc<addrinfo>()
            var result : CPointer<addrinfo> = alloc<addrinfo>().ptr
            var p : CPointer<addrinfo>? = alloc<addrinfo>().ptr
            var ret : Int = 0

            var sockfd = INVALID_SOCKET;
            memset(hints.ptr, 0, sizeOf<addrinfo>().convert<size_t>());
            hints.ai_family = AF_UNSPEC;
            hints.ai_socktype = SOCK_STREAM;
            ret = getaddrinfo(hostname, port.toString(), hints.ptr, cValuesOf(result))
            if (ret != 0)
            {
                Log.error("getaddrinfo: $ret")
                return 1;
            }
            p = result
            while (p != null)
            {
                sockfd = socket(p.pointed.ai_family, p.pointed.ai_socktype, p.pointed.ai_protocol);
                if (sockfd == INVALID_SOCKET) {
                    continue; }
                if (connect(sockfd, p.pointed.ai_addr, p.pointed.ai_addrlen) != SOCKET_ERROR) {
                    break;
                }
                close(sockfd);
                sockfd = INVALID_SOCKET;
                p = p.pointed.ai_next
            }
            freeaddrinfo(result);
            return sockfd;
        }
    }


    constructor(){
        init_sockets()

    }

    fun poll(timeout : Int){TODO() }

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


