package co.spin.utils

import kotlinx.cinterop.*

expect class SocketT
expect class addrinfo
expect fun getaddrinfo(pNodeName: String?,
                       pServiceName: String?,
                       pHints: CValuesRef<addrinfo>?,
                       ppResult: CValuesRef<CPointerVar<addrinfo>>) : Int


