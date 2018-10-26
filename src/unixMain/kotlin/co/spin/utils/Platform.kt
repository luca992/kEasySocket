package co.spin.utils

import kotlinx.cinterop.*
import platform.posix.*

actual typealias SocketT = Int
actual val INVALID_SOCKET : SocketT = -1
actual typealias addrinfo = platform.posix.addrinfo
actual fun getaddrinfo(pNodeName: String?,
                       pServiceName: String?,
                       pHints: CValuesRef<addrinfo>?,
                       ppResult: CValuesRef<CPointerVar<addrinfo>>) : Int {
    return getaddrinfo(pNodeName,pServiceName,pHints,ppResult)
}
actual fun closesocket(s: SocketT){
    close(s)
}
