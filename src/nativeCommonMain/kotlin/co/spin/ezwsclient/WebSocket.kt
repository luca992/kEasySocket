package co.spin.ezwsclient

import kotlinx.cinterop.*
import platform.posix.*
import platform.posix.size_t
import co.spin.utils.Log
import co.spin.utils.addrinfo
import co.spin.utils.connect
import co.spin.utils.getaddrinfo
import co.spin.utils.closesocket
import co.spin.utils.freeaddrinfo
import co.spin.utils.INVALID_SOCKET

interface Callback_Imp{
    operator fun Callback_Imp.invoke()
}

const val SOCKET_ERROR  : Long = -1L


class WebSocket{
    enum class ReadyStateValues { CLOSING, CLOSED, CONNECTING, OPEN }

    private data class WsHeaderType (
            var header_size: UInt,
            var fin: Boolean,
            var mask: Boolean,
            var opcode: OpcodeType,
            var N0: Int,
            var N: ULong,
            var masking_key: MutableList<UByte> = MutableList <UByte>(4,{0u})
    ) {
        enum class OpcodeType(val rgb: Int){
            CONTINUATION(0x0),
            TEXT_FRAME(0x1),
            BINARY_FRAME(0x2),
            CLOSE(8),
            PING(9),
            PONG(0xa),
        }
    }
    var rxbuf = mutableListOf<UByte>()
    var txbuf = mutableListOf<UByte>()
    var receivedData = mutableListOf<UByte>()


    var sockfd: /*socketT*/ULong
    var readyState: ReadyStateValues
    var useMask: Boolean

    private fun hostname_connect(hostname : String, port : Int) : ULong {
        init_sockets()
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
                return 1u;
            }
            p = result
            while (p != null)
            {
                sockfd = socket(p.pointed.ai_family, p.pointed.ai_socktype, p.pointed.ai_protocol).toULong()
                if (sockfd.toInt() == Int.MAX_VALUE){
                    // work around for *nix which returns -1(Int) if error
                    sockfd = INVALID_SOCKET
                }
                if (sockfd == INVALID_SOCKET) {
                    continue; }
                if (connect(sockfd, p.pointed.ai_addr, p.pointed.ai_addrlen.toULong()) != SOCKET_ERROR.toULong()) {
                    break;
                }
                closesocket(sockfd);
                sockfd = INVALID_SOCKET;
                p = p.pointed.ai_next
            }
            freeaddrinfo(result);
            return sockfd;
        }
    }


    constructor(sockfd : ULong, useMask : Boolean){
        this.sockfd = sockfd
        this.readyState = ReadyStateValues.OPEN
        this.useMask = useMask
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


