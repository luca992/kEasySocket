package co.spin.utils

import kotlinx.cinterop.*
import platform.posix.*

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
    return getaddrinfo(pNodeName,pServiceName,pHints,ppResult)
}
actual fun connect(connect: ULong, name : CPointer<sockaddr>?, namelen: ULong) : ULong{
    var r =  connect(connect.toInt(), name, namelen.toUInt()).toULong()
    if (r.toInt() == Int.MAX_VALUE){
        // work around for *nix which returns -1(Int) if error
        r = INVALID_SOCKET
    }
    return r
}
actual fun closesocket(s: ULong){
    close(s.toInt())
}
actual fun freeaddrinfo(addr: CPointer<addrinfo>) {
    freeaddrinfo(addr)
}
actual fun select(nfds : Int, readfds: CValuesRef<fd_set>?, writefds: CValuesRef<fd_set>?, exceptfds:CValuesRef<fd_set>?, timeval : CValuesRef<timeval>?) : Int  =
        select(nfds,readfds,writefds,exceptfds,timeval)
actual fun recv(s: ULong, buf: CPointer<UByteVar>?, len: Int, flags: Int) : Long =
    recv(s,buf,len,flags)

actual fun send(s: ULong, buf: CPointer<UByteVar>?, len: ULong, flags: Int) : Long =
    recv(s.toInt(),buf,len,flags)

