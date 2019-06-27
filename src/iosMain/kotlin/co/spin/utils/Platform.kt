package co.spin.utils

import kotlinx.cinterop.*
import platform.posix.*

actual fun FD_ZERO(set: CValuesRef<fd_set>, scope: AutofreeScope){
    for(i in 0..31) {
        set.getPointer(scope).pointed.fds_bits[i] = 0
    }
}

actual fun FD_SET(fd: Int, set: CValuesRef<fd_set>, scope: AutofreeScope){
    val intOffset = fd / 32
    val bitOffset = fd % 32
    val mask = 1.shl(bitOffset)
    val fds_bits = set.getPointer(scope).pointed.fds_bits
    when (intOffset) {
        0 -> fds_bits[0] = fds_bits[0].or(mask)
        1 -> fds_bits[1] = fds_bits[1].or(mask)
        2 -> fds_bits[2] = fds_bits[2].or(mask)
        3 -> fds_bits[3] = fds_bits[3].or(mask)
        4 -> fds_bits[4] = fds_bits[4].or(mask)
        5 -> fds_bits[5] = fds_bits[5].or(mask)
        6 -> fds_bits[6] = fds_bits[6].or(mask)
        7 -> fds_bits[7] = fds_bits[7].or(mask)
        8 -> fds_bits[8] = fds_bits[8].or(mask)
        9 -> fds_bits[9] = fds_bits[9].or(mask)
        10 -> fds_bits[10] = fds_bits[10].or(mask)
        11 -> fds_bits[11] = fds_bits[11].or(mask)
        12 -> fds_bits[12] = fds_bits[12].or(mask)
        13 -> fds_bits[13] = fds_bits[13].or(mask)
        14 -> fds_bits[14] = fds_bits[14].or(mask)
        15 -> fds_bits[15] = fds_bits[15].or(mask)
        16 -> fds_bits[16] = fds_bits[16].or(mask)
        17 -> fds_bits[17] = fds_bits[17].or(mask)
        18 -> fds_bits[18] = fds_bits[18].or(mask)
        19 -> fds_bits[19] = fds_bits[19].or(mask)
        20 -> fds_bits[20] = fds_bits[20].or(mask)
        21 -> fds_bits[21] = fds_bits[21].or(mask)
        22 -> fds_bits[22] = fds_bits[22].or(mask)
        23 -> fds_bits[23] = fds_bits[23].or(mask)
        24 -> fds_bits[24] = fds_bits[24].or(mask)
        25 -> fds_bits[25] = fds_bits[25].or(mask)
        26 -> fds_bits[26] = fds_bits[26].or(mask)
        27 -> fds_bits[27] = fds_bits[27].or(mask)
        28 -> fds_bits[28] = fds_bits[28].or(mask)
        29 -> fds_bits[29] = fds_bits[29].or(mask)
        30 -> fds_bits[30] = fds_bits[30].or(mask)
        31 -> fds_bits[31] = fds_bits[31].or(mask)
        else -> return
    }
}


