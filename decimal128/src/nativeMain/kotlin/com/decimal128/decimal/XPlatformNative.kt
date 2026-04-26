@file:Suppress("NOTHING_TO_INLINE")
// XPlatformNative.kt
package com.decimal128.decimal

import platform.posix.fma
import com.decimal128.unsignedmulhi.unsigned_mul_hi

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual inline fun unsignedMulHi(x: Long, y: Long): Long =
    unsigned_mul_hi(x.toULong(), y.toULong()).toLong()

actual inline fun unsignedDiv(x: Long, y: Long): Long =
    (x.toULong() / y.toULong()).toLong()

actual inline fun unsignedRem(x: Long, y: Long): Long =
    (x.toULong() % y.toULong()).toLong()

actual inline fun unsignedCmp(x: Long, y: Long): Int =
    (x xor Long.MIN_VALUE).compareTo(y xor Long.MIN_VALUE)

actual inline fun mathFma(a: Double, b: Double, c: Double): Double = fma(a, b, c)
