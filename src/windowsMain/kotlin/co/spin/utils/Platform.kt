package co.spin.utils

import kotlinx.cinterop.*
import platform.windows.addrinfo
import platform.windows.getaddrinfo


actual typealias SocketT = UInt
actual typealias addrinfo = platform.windows.addrinfo
actual fun getaddrinfo(pNodeName: String?,
                       pServiceName: String?,
                       pHints: CValuesRef<addrinfo>?,
                       ppResult: CValuesRef<CPointerVar<addrinfo>>) : Int {
    return getaddrinfo(pNodeName,pServiceName,pHints,ppResult)
}
