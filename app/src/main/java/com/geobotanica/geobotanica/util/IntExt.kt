package com.geobotanica.geobotanica.util


/**
 * Calculate log base 2 of an integer that is expected to be a power of 2 (i.e. bit flags).
 * Useful for converting bit flags to enum ordinals.
 * Returns zero if receiver is not a power of 2.
 */
fun Int.log2(): Int {
    var temp = this
    var bitIndex = 0
    while (temp != 0) {
        if (temp == 1)
            return bitIndex
        temp = temp ushr 1
        bitIndex++
    }
    return 0
}