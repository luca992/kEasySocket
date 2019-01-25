package co.spin.utils

import kotlinx.cinterop.*
import platform.windows.addrinfo
import platform.posix.sockaddr
import platform.windows.getaddrinfo
import platform.windows.closesocket
import platform.windows.freeaddrinfo
import platform.windows.WSAEINPROGRESS
import platform.windows.WSAEWOULDBLOCK
import platform.windows.*
import platform.windows.select
import platform.windows.setsockopt
import platform.posix.timeval
import platform.posix.fd_set


actual typealias SocketT = ULong
actual typealias TimeValT = Int
actual val INVALID_SOCKET : SocketT = ULong.MAX_VALUE
actual val SOCKET_EAGAIN_EINPROGRESS : Int = WSAEINPROGRESS
actual val SOCKET_EWOULDBLOCK : Int = WSAEWOULDBLOCK
actual typealias addrinfo = platform.windows.addrinfo
actual fun getaddrinfo(pNodeName: String?,
                       pServiceName: String?,
                       pHints: CValuesRef<addrinfo>?,
                       ppResult: CValuesRef<CPointerVar<addrinfo>>) : Int {
    return getaddrinfo(pNodeName,pServiceName,pHints,ppResult)
}
actual fun connect(connect: SocketT, name : CPointer<sockaddr>?, namelen: ULong) : Int {
    return connect(connect, name, namelen)
}
actual fun closesocket(s: SocketT){
    closesocket(s)
}
actual fun freeaddrinfo(addr: CPointer<addrinfo>) {
    freeaddrinfo(addr)
}

actual fun select(nfds : Int, readfds: CValuesRef<fd_set>?, writefds: CValuesRef<fd_set>?, exceptfds:CValuesRef<fd_set>?, timeval : CValuesRef<timeval>?) : Int  =
        platform.windows.select(nfds,readfds,writefds,exceptfds,timeval)
actual fun recv(s: SocketT, buf: CPointer<UByteVar>?, len: ULong, flags: Int) : Long =
        platform.windows.recv(s,buf,len,flags).toLong().toLong()

actual fun send(s: SocketT, buf: CPointer<UByteVar>?, len: ULong, flags: Int) : Long =
        platform.windows.send(s,buf,len.toInt(),flags).toLong()

actual fun setsockopt(s: ULong, level: Int, option_name: Int, option_value: CPointer<IntVar>, option_len : Int) =
        platform.windows.setsockopt(s,level,option_name,option_value.pointed.value.toChar().toString(),option_len)

actual fun fcntl(s: SocketT) {
    memScoped{
        val on = alloc<UIntVar>()
        on.value  = 1u
        ioctlsocket(s, FIONBIO.toInt(), on.ptr);
    }
}