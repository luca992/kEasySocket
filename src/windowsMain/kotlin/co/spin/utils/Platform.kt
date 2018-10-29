package co.spin.utils

import kotlinx.cinterop.*
import platform.windows.addrinfo
import platform.posix.sockaddr
import platform.windows.getaddrinfo
import platform.windows.closesocket
import platform.windows.freeaddrinfo


//actual typealias SocketT = ULong
actual val INVALID_SOCKET /*: SocketT */ : ULong = ULong.MAX_VALUE
actual typealias addrinfo = platform.windows.addrinfo
actual fun getaddrinfo(pNodeName: String?,
                       pServiceName: String?,
                       pHints: CValuesRef<addrinfo>?,
                       ppResult: CValuesRef<CPointerVar<addrinfo>>) : Int {
    return getaddrinfo(pNodeName,pServiceName,pHints,ppResult)
}
actual fun connect(connect: ULong, name : CPointer<sockaddr>?, namelen: ULong) : ULong{
    return connect(connect, name, namelen)
}
actual fun closesocket(s: ULong){
    closesocket(s)
}
actual fun freeaddrinfo(addr: CPointer<addrinfo>) {
    freeaddrinfo(addr)
}