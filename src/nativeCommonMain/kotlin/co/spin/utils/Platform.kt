package co.spin.utils

import kotlinx.cinterop.*
import platform.posix.*

//expect class SocketT
expect val INVALID_SOCKET /*: SocketT */ : ULong
expect val SOCKET_EAGAIN_EINPROGRESS : Int
expect val SOCKET_EWOULDBLOCK : Int
expect class addrinfo
expect fun getaddrinfo(pNodeName: String?,
                       pServiceName: String?,
                       pHints: CValuesRef<addrinfo>?,
                       ppResult: CValuesRef<CPointerVar<addrinfo>>) : Int
expect fun connect(connect: ULong, name : CPointer<sockaddr>?, namelen: ULong) : ULong

expect fun closesocket(s: ULong)
expect fun freeaddrinfo(addr: CPointer<addrinfo>)




