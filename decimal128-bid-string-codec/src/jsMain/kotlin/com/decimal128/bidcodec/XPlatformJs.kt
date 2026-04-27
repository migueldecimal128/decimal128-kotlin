@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.bidcodec
// XPlatformJs.kt

internal actual inline fun unsignedMulHi(x: Long, y: Long): Long {
    val xULong = x.toULong()
    val yULong = y.toULong()
    val xLo = xULong and 0xFFFFFFFFUL
    val xHi = xULong shr 32
    val yLo = yULong and 0xFFFFFFFFUL
    val yHi = yULong shr 32

    val pp00 = xLo * yLo
    val pp01 = xHi * yLo
    val pp10 = xLo * yHi
    val pp11 = xHi * yHi

    val mid = pp01 + pp10  // may overflow
    val midCarry = if (mid < pp01) 1UL else 0UL  // carry from mid overflow

    val midWithLo = (pp00 shr 32) + (mid and 0xFFFFFFFFUL)
    // midWithLo cannot overflow: max is (2^32-1) + (2^32-1) < 2^33

    return (pp11 + (mid shr 32) + (midCarry shl 32) + (midWithLo shr 32)).toLong()
}

internal actual inline fun unsignedCmp(x: Long, y: Long): Int =
    (x xor Long.MIN_VALUE).compareTo(y xor Long.MIN_VALUE)

