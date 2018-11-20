package co.spin.ezwsclient

import co.spin.Url
import kotlinx.cinterop.*
import openssl.*
import kotlinx.coroutines.*
import platform.posix.*
import co.spin.utils.fcntl
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
import co.spin.utils.setsockopt
import co.spin.utils.INVALID_SOCKET
import co.spin.utils.SOCKET_EWOULDBLOCK
import co.spin.utils.SOCKET_EAGAIN_EINPROGRESS
import co.spin.ezwsclient.WebSocket.ReadyStateValues.*
import kotlinx.coroutines.EzSocketDispatchers

fun UByte.shl(b: Int) = (toInt() shl b.toInt()).toUByte()
fun UByte.shr(b: Int) = (toInt() shr b.toInt()).toUByte()
fun UByte.toChar() = (toByte()).toChar()

interface Callback_Imp{
    operator fun Callback_Imp.invoke()
}

const val SOCKET_ERROR  : Long = -1L


@ExperimentalUnsignedTypes
open class WebSocket{
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
                fun valueOf(value: UByte): OpcodeType = OpcodeType.values().firstOrNull{
                    it.value == value } ?: TEXT_FRAME
            }
        }
    }

    private var url: Url
    private var origin: String

    var rxbuf = UByteArray(0)
    var txbuf = UByteArray(0)
    var receivedData = UByteArray(0)


    var sockfd: /*socketT*/ULong = ULong.MAX_VALUE
    var readyState: ReadyStateValues = OPEN
    var useMask: Boolean


    var sslctx : CPointerVar<SSL_CTX>? = null
    var cSSL : CPointerVar<SSL>? = null


    //https://stackoverflow.com/a/16328115/1363742
    fun InitializeSSL() {
       // SSL_load_error_strings()
        //SSL_library_init()
        //OPENSSL_add_all_algorithms()

    }

    fun DestroySSL() {
        //ERR_free_strings()
        //EVP_cleanup()
    }

    fun ShutdownSSL() {
        SSL_shutdown(cSSL?.value)
        SSL_free(cSSL?.value)
    }


    constructor(url : Url, useMask : Boolean = true, origin: String= ""){
        this.url = url
        this.useMask = useMask
        this.origin = origin
    }


    private fun init() {
        Log.debug{"easywsclient: connecting: host=${url.host} port=${url.port} path=${url.path}"}
        val success = connect(url.host, url.port)
        if (!success) {
            throw RuntimeException("Unable to connect to ${url.host}:${url.port}")
        }
        memScoped {
            // XXX: this should be done non-blocking,
            val line = UByteArray(256)
            val status = alloc<IntVar>()
            var i = 0
            Log.debug{"easywsclient: connecting now: host=${url.host} port=${url.port} path=${url.path}"}
            "GET ${url.path} HTTP/1.1\r\n".toUtf8().toUByteArray()
                    .usePinned { pinned ->
                        send(pinned.addressOf(0), pinned.get().size.toULong())
                    }
            if (url.port == 80) {
                "Host: ${url.host}\r\n".toUtf8().toUByteArray()
                        .usePinned { pinned ->
                            send(pinned.addressOf(0), pinned.get().size.toULong())
                        }
            }
            else {
                "Host: ${url.host}:${url.port}\r\n".toUtf8().toUByteArray()
                        .usePinned { pinned ->
                            send(pinned.addressOf(0), pinned.get().size.toULong())
                        }
            }
            "Upgrade: websocket\r\n".toUtf8().toUByteArray()
                    .usePinned { pinned ->
                        send(pinned.addressOf(0), pinned.get().size.toULong())
                    }
            "Connection: Upgrade\r\n".toUtf8().toUByteArray()
                    .usePinned { pinned ->
                        send(pinned.addressOf(0), pinned.get().size.toULong())
                    }
            if (!origin.isEmpty()) {
                "Origin: $origin\r\n".toUtf8().toUByteArray()
                        .usePinned { pinned ->
                            send(pinned.addressOf(0), pinned.get().size.toULong())
                        }
            }
            "Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==\r\n".toUtf8().toUByteArray()
                    .usePinned { pinned ->
                        send(pinned.addressOf(0), pinned.get().size.toULong())
                    }
            "Sec-WebSocket-Version: 13\r\n".toUtf8().toUByteArray()
                    .usePinned { pinned ->
                        send(pinned.addressOf(0), pinned.get().size.toULong())
                    }
            "\r\n".toUtf8().toUByteArray()
                    .usePinned { pinned ->
                        send(pinned.addressOf(0), pinned.get().size.toULong())
                    }
            line.usePinned{ pinned ->
                while (i < 2 || (i < 255 && line[i - 2].toChar() != '\r' && line[i - 1].toChar() != '\n')) {
                    val recv = recv(pinned.addressOf(i), 1u)
                    if (recv == 0L) {
                        throw Exception("recv returned 0")
                    }
                    ++i
                }
            }

            line[i] = 0u
            if (i == 255) { throw RuntimeException("ERROR: Got invalid status line connecting to: $url");}
            val sscanfResult = sscanf(line.toByteArray().stringFromUtf8(), "HTTP/1.1 %d", status.ptr)
            if (sscanfResult != 1 || status.value != 101) {
                Log.error{"ERROR: Got bad status connecting to $url: ${line.toByteArray().stringFromUtf8()}"}; return@memScoped
            }
            // TODO: verify response headers,
            while (true) {
                i = 0
                line.usePinned { pinned ->
                    while (i < 2 || (i < 255 && line[i-2].toChar() != '\r' && line[i-1].toChar() != '\n')) {
                        val recv = recv(pinned.addressOf(i), 1u).toInt()
                        if (recv == 0) {
                            throw Exception("recv returned 0")
                        }
                        ++i
                    }
                }
                if (line[0].toChar() == '\r' && line[1].toChar() == '\n') { break; }
            }
        }

        memScoped{
            val flag = alloc<IntVar>()
            flag.value  = 1
            setsockopt(sockfd, IPPROTO_TCP, TCP_NODELAY, flag.ptr, IntVar.size.toInt()) // Disable Nagle's algorithm
            return@memScoped
        }
        fcntl(sockfd)
        Log.debug{"Connected to: $url"}
    }

    protected open fun send(buf: CPointer<UByteVar>?, len: ULong) : Long {
        return send(sockfd,buf,len,0)
    }

    protected open fun recv(buf: CPointer<UByteVar>?, len: ULong) : Long {
        return recv(sockfd,buf,len,0)
    }

    protected open fun connect(hostname : String, port : Int): Boolean {
        this.sockfd = hostnameConnect(hostname, port)
        return sockfd != INVALID_SOCKET
    }

    protected fun hostnameConnect(hostname : String, port : Int) : ULong {
        init_sockets()
        return memScoped {
            val hints : addrinfo = alloc<addrinfo>()
            val result : CPointerVar<addrinfo> = alloc<CPointerVar<addrinfo>>()
            var p : CPointer<addrinfo>? = alloc<addrinfo>().ptr
            val ret : Int


            var sockfd = INVALID_SOCKET;
            memset(hints.ptr, 0, sizeOf<addrinfo>().convert<size_t>())
            hints.ai_family = AF_UNSPEC
            hints.ai_socktype = SOCK_STREAM

            val hn =hostname
            val ps= port.toString()
            ret = getaddrinfo(hn, ps, hints.ptr, result.ptr)
            if (ret != 0)
            {
                Log.error{"getaddrinfo: $ret"}
                return@memScoped 1u
            }
            p = result.value
            while (p != null)
            {
                sockfd = socket(p.pointed.ai_family, p.pointed.ai_socktype, p.pointed.ai_protocol).toULong()
                if (sockfd.toInt() == Int.MAX_VALUE){
                    // work around for *nix which returns -1(Int) if error
                    sockfd = INVALID_SOCKET
                }
                if (sockfd == INVALID_SOCKET) { continue; }
                if (connect(sockfd, p.pointed.ai_addr, p.pointed.ai_addrlen.toULong()) != SOCKET_ERROR.toULong()) {
                    break
                }
                closesocket(sockfd)
                sockfd = INVALID_SOCKET
                p = p.pointed.ai_next
            }
            freeaddrinfo(result.value)
            return@memScoped sockfd
        }
    }


    fun poll(timeout : Int = 0){
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
                    ret = recv(pinned.addressOf(0) + N, 1500u)
                }
                if (false) {
                } else if (ret < 0 && (posix_errno() == SOCKET_EWOULDBLOCK || posix_errno() == SOCKET_EAGAIN_EINPROGRESS)) {
                    rxbuf= rxbuf.copyOf(N)
                    break
                } else if (ret <= 0) {
                    rxbuf= rxbuf.copyOf(N)
                    closesocket(sockfd)
                    readyState = CLOSED
                    Log.error{if (ret < 0) "Recv: Connection error!"  else "Recv: Connection closed!"}
                    break
                } else {
                    rxbuf= rxbuf.copyOf(N + ret.toInt())
                }
            }
            while (!txbuf.isEmpty()) {
                var ret =0L
                txbuf.usePinned{ pinned->
                    ret = send(pinned.addressOf(0), txbuf.size.toULong())
                }
                if (false) { } // ??
                else if (ret < 0 && (posix_errno() == SOCKET_EWOULDBLOCK || posix_errno() == SOCKET_EAGAIN_EINPROGRESS)) {
                    break
                }
                else if (ret <= 0) {
                    closesocket(sockfd)
                    readyState = CLOSED
                    Log.error{if (ret < 0) "Send: Connection error!"  else "Send: Connection closed!"}
                    break
                }
                else {
                    val remaining = UByteArray(txbuf.size - ret.toInt())
                    //Log.debug { "${remaining.size}  : ${ret.toInt()}" }
                    txbuf = if (remaining.isEmpty()) remaining else txbuf.copyInto(remaining,0,txbuf.size - ret.toInt())
                }
            }
            if (txbuf.isEmpty() && readyState == CLOSING) {
                closesocket(sockfd)
                readyState = CLOSED
            }
        }
    }

    fun dispatchBinary(callback: (UByteArray) -> Unit): Job = GlobalScope.launch(EzSocketDispatchers.Default) {
        try {
            // TODO: consider acquiring a lock on rxbuf...
            while (true) {
                val ws = WsHeaderType()
                if (rxbuf.size < 2) {
                    return@launch /* Need at least 2 */
                }
                val data = rxbuf// peek, but don't consume
                ws.fin = (data[0].and(0x80u)) == 0x80.toUByte()
                ws.opcode = WsHeaderType.OpcodeType.valueOf(data[0].and(0x0fu))
                ws.mask = data[1].and(0x80u) == 0x80.toUByte()
                ws.N0 = data[1].and(0x7fu).toInt()
                ws.header_size = 2u + (if (ws.N0 == 126) 2u else 0u) + (if (ws.N0 == 127) 8u else 0u) + (if (ws.mask) 4u else 0u)
                if (rxbuf.size < ws.header_size.toInt()) {
                    return@launch; /* Need: ws.header_size - rxbuf.size */
                }
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
                        ws.N = ws.N.or((data[3].toULong()).shl(0))
                        i = 4
                    }
                    ws.N0 == 127 -> {
                        ws.N = 0u
                        ws.N = ws.N.or((data[2].toULong()).shl(56))
                        ws.N = ws.N.or((data[3].toULong()).shl(48))
                        ws.N = ws.N.or((data[4].toULong()).shl(40))
                        ws.N = ws.N.or((data[5].toULong()).shl(32))
                        ws.N = ws.N.or((data[6].toULong()).shl(24))
                        ws.N = ws.N.or((data[7].toULong()).shl(16))
                        ws.N = ws.N.or((data[8].toULong()).shl(8))
                        ws.N = ws.N.or((data[9].toULong()).shl(0))
                        i = 10
                    }
                }
                if (ws.mask) {
                    ws.masking_key[0] = (data[i + 0]).shl(0)
                    ws.masking_key[1] = (data[i + 1]).shl(0)
                    ws.masking_key[2] = (data[i + 2]).shl(0)
                    ws.masking_key[3] = (data[i + 3]).shl(0)
                } else {
                    ws.masking_key[0] = 0u
                    ws.masking_key[1] = 0u
                    ws.masking_key[2] = 0u
                    ws.masking_key[3] = 0u
                }
                if (rxbuf.size < ws.header_size.toInt() + ws.N.toLong()) {
                    return@launch; /* Need: ws.header_size+ws.N - rxbuf.size */
                }

                // We got a whole message, now do something with it:
                if (false) {
                } else if (
                        ws.opcode == WsHeaderType.OpcodeType.TEXT_FRAME
                        || ws.opcode == WsHeaderType.OpcodeType.BINARY_FRAME
                        || ws.opcode == WsHeaderType.OpcodeType.CONTINUATION
                ) {
                    if (ws.mask) {
                        for (i in 0..ws.N.toInt()) {
                            rxbuf[i + ws.header_size.toInt()] = rxbuf[i + ws.header_size.toInt()].xor(ws.masking_key[i.and(0x3)])
                        }
                    }
                    val oldReceivedDataSize = receivedData.size
                    receivedData = receivedData.copyOf(receivedData.size + ws.N.toInt())
                    receivedData = rxbuf.copyInto(receivedData, oldReceivedDataSize, ws.header_size.toInt(), ws.header_size.toInt() + ws.N.toInt())// just feed
                    if (ws.fin) {
                        Log.info { "Recieved: ${receivedData.toByteArray().stringFromUtf8()}" }
                        callback(receivedData)
                        receivedData = UByteArray(0)
                    }
                } else if (ws.opcode == WsHeaderType.OpcodeType.PING) {
                    if (ws.mask) {
                        for (i in 0..ws.N.toInt()) {
                            rxbuf[i + ws.header_size.toInt()] = rxbuf[i + ws.header_size.toInt()].xor(ws.masking_key[i.and(0x3)])
                        }
                    }
                    val pongData = UByteArray(ws.header_size.toInt() + ws.N.toInt()) { rxbuf[ws.header_size.toInt() + i] }
                    sendData(WsHeaderType.OpcodeType.PONG, pongData.size, pongData)
                } else if (ws.opcode == WsHeaderType.OpcodeType.PONG) {
                } else if (ws.opcode == WsHeaderType.OpcodeType.CLOSE) {
                    this@WebSocket.close()
                } else {
                    Log.error { "ERROR: Got unexpected WebSocket message.\n" }; this@WebSocket.close()
                }

                rxbuf = UByteArray(0)
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun send(message: String){
        val uft8Msg = message.toUtf8().toUByteArray()
        sendData(WsHeaderType.OpcodeType.TEXT_FRAME, uft8Msg.size, uft8Msg)
    }

    fun sendBinary(message: String){
        val uft8Msg = message.toUtf8().toUByteArray()
        sendData(WsHeaderType.OpcodeType.BINARY_FRAME, uft8Msg.size, uft8Msg)
    }
    fun sendBinary(message: ByteArray){
        sendData(WsHeaderType.OpcodeType.BINARY_FRAME, message.size, message.toUByteArray())
    }

    fun sendPing(){
        val empty = "".toUtf8().toUByteArray()
        sendData(WsHeaderType.OpcodeType.PING, empty.size, empty)
    }

    private fun sendData(type : WsHeaderType.OpcodeType, messageSize: Int, message: UByteArray) {
        val messageSizeUbyte = messageSize.toUByte()
        // TODO:
        // Masking key should (must) be derived from a high quality random
        // number generator, to mitigate attacks on non-WebSocket friendly
        // middleware:
        val masking_key = ubyteArrayOf(0x12u, 0x34u, 0x56u, 0x78u)
        // TODO: consider acquiring a lock on txbuf...
        if (readyState == CLOSING || readyState == CLOSED) { return; }
        val header = UByteArray(2 +
                (if(messageSizeUbyte >= 126u) 2 else 0) +
                (if(messageSizeUbyte >= 65536u) 6 else 0) +
                (if (useMask) 4 else 0)) {0u}
        header[0] = 0x80.toUByte().or(type.value)
        if (messageSizeUbyte < 126u) {
            header[1] = (messageSizeUbyte.and(0xffu)).or(if (useMask) 0x80u else 0u)
            if (useMask) {
                header[2] = masking_key[0]
                header[3] = masking_key[1]
                header[4] = masking_key[2]
                header[5] = masking_key[3]
            }
        }
        else if (messageSizeUbyte < 65536u) {
            header[1] = 126u.toUByte().or(if (useMask) 0x80u else 0u)
            header[2] = messageSizeUbyte.shr(8).and(0xffu)
            header[3] = messageSizeUbyte.shr(0).and(0xffu)
            if (useMask) {
                header[4] = masking_key[0]
                header[5] = masking_key[1]
                header[6] = masking_key[2]
                header[7] = masking_key[3]
            }
        }
        else { // TODO: run coverage testing here
            header[1] = 127.toUByte().or(if (useMask) 0x80u else 0u)
            header[2] = messageSizeUbyte.shr(56).and(0xffu)
            header[3] = messageSizeUbyte.shr(48).and(0xffu)
            header[4] = messageSizeUbyte.shr(40).and(0xffu)
            header[5] = messageSizeUbyte.shr(32).and(0xffu)
            header[6] = messageSizeUbyte.shr(24).and(0xffu)
            header[7] = messageSizeUbyte.shr(16).and(0xffu)
            header[8] = messageSizeUbyte.shr( 8).and(0xffu)
            header[9] = messageSizeUbyte.shr( 0).and(0xffu)
            if (useMask) {
                header[10] = masking_key[0]
                header[11] = masking_key[1]
                header[12] = masking_key[2]
                header[13] = masking_key[3]
            }
        }
        // N.B. - txbuf will keep growing until it can be transmitted over the socket:
        val txbufOldSize = txbuf.size
        txbuf= txbuf.copyOf(txbufOldSize+header.size+ message.size)
        header.copyInto(txbuf,txbufOldSize)
        message.copyInto(txbuf,txbufOldSize+header.size)
        if (useMask) {
            val message_offset = txbuf.size - messageSizeUbyte.toInt()
            for (i in 0 until messageSize){
                txbuf[message_offset + i] = txbuf[message_offset + i].xor(masking_key[i.and(0x3)])
            }
        }
    }

    open fun close(){
        if(readyState == CLOSING || readyState == CLOSED) { return; }
        readyState = CLOSING
        val txbufOldSize = txbuf.size
        val closeFrame = ubyteArrayOf(0x88u, 0x80u, 0x00u, 0x00u, 0x00u, 0x00u) // last 4 bytes are a masking key
        txbuf = txbuf.copyOf(txbuf.size + closeFrame.size)
        txbuf = closeFrame.copyInto(txbuf,txbufOldSize)
    }


    companion object {

        private fun webSocketForUrl(url: Url, useMask: Boolean) : WebSocket{
            return if (url.protocol == "wss"){
                WebSocketOpenSSL(url, useMask)
            } else {
                WebSocket(url, useMask)
            }

        }
        fun fromUrl(url : Url, origin: String = ""): WebSocket {
            val webSocket =  webSocketForUrl(url, true)
            webSocket.init()
            return webSocket
        }

        fun fromUrlNoMask(url : Url, origin: String): WebSocket {
            val webSocket =  webSocketForUrl(url, true)
            webSocket.init()
            return webSocket
        }

    }
}




