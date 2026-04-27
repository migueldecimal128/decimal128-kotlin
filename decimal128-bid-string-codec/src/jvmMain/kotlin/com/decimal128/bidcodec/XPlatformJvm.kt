@file:Suppress("NOTHING_TO_INLINE")
// XPlatformJvm.kt
package com.decimal128.bidcodec

internal actual inline fun unsignedMulHi(x: Long, y: Long): Long = Math.unsignedMultiplyHigh(x, y)

internal actual inline fun unsignedCmp(x: Long, y: Long): Int = java.lang.Long.compareUnsigned(x, y)

