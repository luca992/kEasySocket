package co.spin.utils

import kotlinx.cinterop.*

expect class SocketT
expect val INVALID_SOCKET : SocketT
expect class addrinfo
expect fun getaddrinfo(pNodeName: String?,
                       pServiceName: String?,
                       pHints: CValuesRef<addrinfo>?,
                       ppResult: CValuesRef<CPointerVar<addrinfo>>) : Int
expect fun closesocket(s: SocketT)

