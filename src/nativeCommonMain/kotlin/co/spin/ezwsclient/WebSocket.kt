package co.spin.ezwsclient

import kotlinx.cinterop.*
import platform.posix.*
import co.spin.utils.Log
import co.spin.utils.addrinfo
import co.spin.utils.connect
import co.spin.utils.getaddrinfo
import co.spin.utils.closesocket
import co.spin.utils.freeaddrinfo
import co.spin.utils.INVALID_SOCKET
import co.spin.utils.SOCKET_EWOULDBLOCK
import co.spin.utils.SOCKET_EAGAIN_EINPROGRESS
import co.spin.ezwsclient.WebSocket.ReadyStateValues.*

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
            var masking_key: UByteArray = UByteArray(4)
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
    var rxbuf = UByteArray(0)
    var txbuf = UByteArray(0)
    var receivedData = UByteArray(0)


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
        this.readyState = OPEN
        this.useMask = useMask
    }

    fun poll(timeout : Int){
        memScoped {
            if (readyState == CLOSED) {
                if (timeout > 0) {
                    val tv = alloc<timeval>().apply{ tv_sec = timeout/1000L; tv_usec = (timeout%1000) * 1000 }
                    select(0, null, null, null, tv.ptr)
                }
                return@memScoped
            }
            if (timeout != 0) {
                var rfds = alloc<fd_set>()
                var wfds = alloc<fd_set>()
                val tv = alloc<timeval>().apply{ tv_sec = timeout/1000L; tv_usec = (timeout%1000) * 1000 }
                posix_FD_ZERO(rfds.ptr)
                posix_FD_ZERO(wfds.ptr)
                posix_FD_SET(sockfd.toInt(), rfds.ptr);
                if (!txbuf.isEmpty()) { posix_FD_SET(sockfd.toInt(), wfds.ptr); }
                select(sockfd.toInt() + 1, rfds.ptr, wfds.ptr, null, if (timeout > 0) tv.ptr else null)
            }
            while (true) {
                // FD_ISSET(0, &rfds) will be true
                val N = rxbuf.size
                rxbuf= rxbuf.copyOf(N + 1500)
                var ret = 0L
                rxbuf.usePinned { pinned: Pinned<UByteArray> ->
                    ret = recv(sockfd.toInt(), pinned.addressOf(0) + N, 1500, 0)
                }
                if (false) {
                } else if (ret < 0 && (posix_errno() == SOCKET_EWOULDBLOCK || posix_errno() == SOCKET_EAGAIN_EINPROGRESS)) {
                    rxbuf= rxbuf.copyOf(N)
                    break;
                } else if (ret <= 0) {
                    rxbuf= rxbuf.copyOf(N)
                    closesocket(sockfd)
                    readyState = CLOSED
                    Log.error(if (ret < 0) "Connection error!"  else "Connection closed!")
                    break
                } else {
                    rxbuf= rxbuf.copyOf(N + ret.toInt())
                }
            }
            while (!txbuf.isEmpty()) {
                var ret =0L
                txbuf.usePinned{ pinned->
                    ret = send(sockfd.toInt(), pinned.addressOf(0), txbuf.size.toULong(), 0)
                }
                if (false) { } // ??
                else if (ret < 0 && (posix_errno() == SOCKET_EWOULDBLOCK || posix_errno() == SOCKET_EAGAIN_EINPROGRESS)) {
                    break
                }
                else if (ret <= 0) {
                    closesocket(sockfd);
                    readyState = CLOSED;
                    Log.error(if (ret < 0) "Connection error!"  else "Connection closed!")
                    break
                }
                else {
                    val remaining = UByteArray(txbuf.size - ret.toInt())
                    txbuf = txbuf.copyInto(remaining,0,txbuf.size - ret.toInt())
                }
            }
            if (txbuf.isEmpty() && readyState == CLOSING) {
                closesocket(sockfd)
                readyState = CLOSED
            }
        }
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


