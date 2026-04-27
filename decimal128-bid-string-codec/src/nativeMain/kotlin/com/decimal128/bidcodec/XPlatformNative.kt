@file:Suppress("NOTHING_TO_INLINE")
// XPlatformNative.kt
package com.decimal128.bidcodec

import platform.posix.fma
import com.decimal128.unsignedmulhi.unsigned_mul_hi
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual inline fun unsignedMulHi(x: Long, y: Long): Long =
    unsigned_mul_hi(x.toULong(), y.toULong()).toLong()

internal actual inline fun unsignedCmp(x: Long, y: Long): Int =
    (x xor Long.MIN_VALUE).compareTo(y xor Long.MIN_VALUE)

