package co.spin.utils

import kotlinx.cinterop.*
import platform.posix.*

actual fun FD_ZERO(set: CValuesRef<fd_set>, scope: AutofreeScope){
    posix_FD_ZERO(set)
}

actual fun FD_SET(fd: Int, set: CValuesRef<fd_set>, scope: AutofreeScope){
    posix_FD_SET(fd,set)
}