package co.spin.utils

import kotlinx.cinterop.*
import platform.posix.*

//actual typealias SocketT = Int
actual val INVALID_SOCKET /*: SocketT */ : ULong = ULong.MAX_VALUE
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
