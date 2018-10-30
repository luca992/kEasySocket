package co.spin.ezwsclient

import kotlinx.cinterop.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.*
import platform.posix.*
import co.spin.utils.Log
import co.spin.utils.TimeValT
import co.spin.utils.addrinfo
import co.spin.utils.connect
import co.spin.utils.getaddrinfo
import co.spin.utils.closesocket
import co.spin.utils.freeaddrinfo
import co.spin.utils.select
import co.spin.utils.send
import co.spin.utils.recv
import co.spin.utils.INVALID_SOCKET
import co.spin.utils.SOCKET_EWOULDBLOCK
import co.spin.utils.SOCKET_EAGAIN_EINPROGRESS
import co.spin.ezwsclient.WebSocket.ReadyStateValues.*
import kotlinx.coroutines.TDispatchers

fun UByte.shl(b: Int) = (toInt() shl b.toInt()).toUByte()

interface Callback_Imp{
    operator fun Callback_Imp.invoke()
}

const val SOCKET_ERROR  : Long = -1L


class WebSocket{
    enum class ReadyStateValues { CLOSING, CLOSED, CONNECTING, OPEN }

    private data class WsHeaderType (
            var header_size: UInt = 0u,
            var fin: Boolean = false,
            var mask: Boolean = false,
            var opcode: OpcodeType = OpcodeType.CLOSE,
            var N0: Int = 0,
            var N: ULong = 0u,
            var masking_key: UByteArray = UByteArray(4)
    ) {
        enum class OpcodeType(val value: UByte){
            CONTINUATION(0x0u),
            TEXT_FRAME(0x1u),
            BINARY_FRAME(0x2u),
            CLOSE(8u),
            PING(9u),
            PONG(0xau);
            companion object {
                fun valueOf(value: UByte): OpcodeType = OpcodeType.values().first{it.value == value }
            }
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
                    val tv = alloc<timeval>().apply{ tv_sec = timeout/(1000 as TimeValT); tv_usec = (timeout%1000) * 1000 }
                    select(0, null, null, null, tv.ptr)
                }
                return@memScoped
            }
            if (timeout != 0) {
                var rfds = alloc<fd_set>()
                var wfds = alloc<fd_set>()
                val tv = alloc<timeval>().apply{ tv_sec = timeout/(1000 as TimeValT); tv_usec = (timeout%1000) * 1000 }
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
                    ret = recv(sockfd, pinned.addressOf(0) + N, 1500, 0)
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
                    ret = send(sockfd, pinned.addressOf(0), txbuf.size.toULong(), 0)
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

    fun dispatchBinary() : ReceiveChannel<UByteArray> = GlobalScope.produce<UByteArray>(TDispatchers.Default) {
        // TODO: consider acquiring a lock on rxbuf...
        while (true) {
            val ws = WsHeaderType()
            if (rxbuf.size < 2) { return@produce /* Need at least 2 */ }
            val data = rxbuf// peek, but don't consume
            ws.fin = (data[0].and(0x80u)) == 0x80.toUByte()
            ws.opcode = WsHeaderType.OpcodeType.valueOf(data[0].and(0x0fu))
            ws.mask = data[1].and(0x80u) == 0x80.toUByte()
            ws.N0 = data[1].and(0x7fu).toInt()
            ws.header_size = 2u + (if (ws.N0 == 126) 2u else 0u) + (if (ws.N0 == 127) 8u else 0u) + (if(ws.mask) 4u else 0u)
            if (rxbuf.size < ws.header_size.toInt()) { return@produce; /* Need: ws.header_size - rxbuf.size */ }
            var i = 0// free memory
            // just feed
            /* Need: ws.header_size+ws.N - rxbuf.size */

            // We got a whole message, now do something with it:
            when {
                ws.N0 < 126 -> {
                    ws.N = ws.N0.toULong()
                    i = 2
                }
                ws.N0 == 126 -> {
                    ws.N = 0u
                    ws.N = ws.N.or(data[2].toULong().shl(8))
                    ws.N = (data[3].toULong()).shl(0)
                    i = 4
                }
                ws.N0 == 127 -> {
                    ws.N = 0u
                    ws.N = (data[2].toULong()).shl(56)
                    ws.N = (data[3].toULong()).shl(48)
                    ws.N = (data[4].toULong()).shl(40)
                    ws.N = (data[5].toULong()).shl(32)
                    ws.N = (data[6].toULong()).shl(24)
                    ws.N = (data[7].toULong()).shl(16)
                    ws.N = (data[8].toULong()).shl(8)
                    ws.N = (data[9].toULong()).shl(0)
                    i = 10
                }
            }
            if (ws.mask) {
                ws.masking_key[0] = (data[i+0]).shl(0)
                ws.masking_key[1] = (data[i+1]).shl(0)
                ws.masking_key[2] = (data[i+2]).shl(0)
                ws.masking_key[3] = (data[i+3]).shl(0)
            }
            else {
                ws.masking_key[0] = 0u
                ws.masking_key[1] = 0u
                ws.masking_key[2] = 0u
                ws.masking_key[3] = 0u
            }
            if (rxbuf.size < ws.header_size.toInt() +ws.N.toLong()) { return@produce; /* Need: ws.header_size+ws.N - rxbuf.size */ }

            // We got a whole message, now do something with it:
            if (false) { }
            else if (
                    ws.opcode == WsHeaderType.OpcodeType.TEXT_FRAME
                    || ws.opcode == WsHeaderType.OpcodeType.BINARY_FRAME
                    || ws.opcode == WsHeaderType.OpcodeType.CONTINUATION
            ) {
                if (ws.mask) {
                    for (i in 0..ws.N.toInt()) {
                        rxbuf[i+ws.header_size.toInt()] =  rxbuf[i+ws.header_size.toInt()].xor(ws.masking_key[i.and(0x3)])
                    }
                }
                receivedData = receivedData.copyOf(receivedData.size + ws.N.toInt())
                receivedData = rxbuf.copyInto(receivedData,receivedData.size,ws.header_size.toInt(), ws.header_size.toInt()+ws.N.toInt())// just feed
                if (ws.fin) {
                    this.send(receivedData)
                    receivedData = UByteArray(0)
                }
            }
            else if (ws.opcode == WsHeaderType.OpcodeType.PING) {
                if (ws.mask) {
                    for (i in 0..ws.N.toInt()) {
                        rxbuf[i+ws.header_size.toInt()] =  rxbuf[i+ws.header_size.toInt()].xor(ws.masking_key[i.and(0x3)])
                    }
                }
                //std::string data(rxbuf.begin()+ws.header_size, rxbuf.begin()+ws.header_size+(size_t)ws.N);
                //sendData(WsHeaderType.OpcodeType.PONG, data.size, data.begin(), data.end());
            }
            else if (ws.opcode == WsHeaderType.OpcodeType.PONG) { }
            else if (ws.opcode == WsHeaderType.OpcodeType.CLOSE) { close(); }
            else { Log.error("ERROR: Got unexpected WebSocket message.\n"); close(); }

            rxbuf = UByteArray(0)
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


