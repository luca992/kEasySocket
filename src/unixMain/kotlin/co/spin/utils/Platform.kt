package co.spin.utils

import kotlinx.cinterop.*
import platform.posix.*
import platform.osx.*

//actual typealias SocketT = Int
actual typealias TimeValT = Long
actual val INVALID_SOCKET /*: SocketT */ : ULong = ULong.MAX_VALUE
actual val SOCKET_EAGAIN_EINPROGRESS : Int = EAGAIN
actual val SOCKET_EWOULDBLOCK : Int = EWOULDBLOCK
actual typealias addrinfo = platform.posix.addrinfo
actual fun getaddrinfo(pNodeName: String?,
                       pServiceName: String?,
                       pHints: CValuesRef<addrinfo>?,
                       ppResult: CValuesRef<CPointerVar<addrinfo>>) : Int {
    return platform.posix.getaddrinfo(pNodeName,pServiceName,pHints,ppResult)
}
actual fun connect(connect: ULong, name : CPointer<sockaddr>?, namelen: ULong) : ULong{
    var r =  platform.posix.connect(connect.toInt(), name, namelen.toUInt()).toULong()
    if (r.toInt() == Int.MAX_VALUE){
        // work around for *nix which returns -1(Int) if error
        r = INVALID_SOCKET
    }
    return r
}
actual fun closesocket(s: ULong){
    platform.posix.close(s.toInt())
}
actual fun freeaddrinfo(addr: CPointer<addrinfo>) {
    freeaddrinfo(addr)
}
actual fun select(nfds : Int, readfds: CValuesRef<fd_set>?, writefds: CValuesRef<fd_set>?, exceptfds:CValuesRef<fd_set>?, timeval : CValuesRef<timeval>?) : Int  =
        platform.posix.select(nfds,readfds,writefds,exceptfds,timeval)
actual fun recv(s: ULong, buf: CPointer<UByteVar>?, len: ULong, flags: Int) : Long {
    val r = platform.posix.recv(s.toInt(), buf, len, flags)
    //val message = (buf as? CPointer<ByteVar>?)?.toKString()
    //if (!message!!.isBlank()) Log.debug{"Receiving: ${message?.trim()}"}
    return r
}

actual fun send(s: ULong, buf: CPointer<UByteVar>?, len: ULong, flags: Int) : Long {
    val message = (buf as? CPointer<ByteVar>?)?.toKString()
    Log.debug{"Sending: ${message?.trim()}"}
    return platform.posix.send(s.toInt(),buf,len,flags)
}

@ExperimentalUnsignedTypes
actual fun setsockopt(s: ULong, level: Int, option_name: Int, option_value: CPointer<IntVar>, option_len : Int) =
    platform.posix.setsockopt(s.toInt(), level, option_name, option_value, option_len.toUInt())


actual fun fcntl(s: ULong) {
    platform.posix.fcntl(s.toInt(), F_SETFL, O_NONBLOCK)
}

