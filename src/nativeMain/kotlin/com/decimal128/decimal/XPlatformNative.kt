@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import platform.posix.fma

actual inline fun unsignedMulHi(x: Long, y: Long): Long {
    val xULong = x.toULong()
    val yULong = y.toULong()
    val xLo = xULong and 0xFFFFFFFFUL
    val xHi = xULong shr 32
    val yLo = yULong and 0xFFFFFFFFUL
    val yHi = yULong shr 32

    val lo = xLo * yLo
    val mid1 = xHi * yLo
    val mid2 = xLo * yHi
    val hi = xHi * yHi

    val mid = (lo shr 32) + mid1 + mid2
    return (hi + (mid shr 32)).toLong()
}

actual inline fun unsignedDiv(x: Long, y: Long): Long =
    (x.toULong() / y.toULong()).toLong()

actual inline fun unsignedRem(x: Long, y: Long): Long =
    (x.toULong() % y.toULong()).toLong()

actual inline fun unsignedCmp(x: Long, y: Long): Int =
    (x xor Long.MIN_VALUE).compareTo(y xor Long.MIN_VALUE)

actual inline fun unsignedCmp(x: Int, y: Int): Int =
    (x xor Int.MIN_VALUE).compareTo(y xor Int.MIN_VALUE)

actual inline fun mathFma(a: Double, b: Double, c: Double): Double = fma(a, b, c)
