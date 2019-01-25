package co.spin.utils

import kotlinx.cinterop.*
import platform.posix.*

expect class SocketT
expect class TimeValT
expect val INVALID_SOCKET : SocketT
expect val SOCKET_EAGAIN_EINPROGRESS : Int
expect val SOCKET_EWOULDBLOCK : Int
expect class addrinfo
expect fun getaddrinfo(pNodeName: String?,
                       pServiceName: String?,
                       pHints: CValuesRef<addrinfo>?,
                       ppResult: CValuesRef<CPointerVar<addrinfo>>) : Int
expect fun connect(connect: SocketT, name : CPointer<sockaddr>?, namelen: ULong) : Int

expect fun closesocket(s: SocketT)
expect fun freeaddrinfo(addr: CPointer<addrinfo>)
expect fun select(nfds : Int, readfds: CValuesRef<fd_set>?, writefds: CValuesRef<fd_set>?, exceptfds: CValuesRef<fd_set>?, timeval : CValuesRef<timeval>?) : Int
expect fun recv(s: SocketT, buf: CPointer<UByteVar>?, len: ULong, flags: Int): Long
expect fun send(s: SocketT, buf: CPointer<UByteVar>?, len: ULong, flags: Int): Long
expect fun setsockopt(s: SocketT, level: Int, option_name: Int, option_value: CPointer<IntVar>, option_len : Int) : Int
expect fun fcntl(s: SocketT)



