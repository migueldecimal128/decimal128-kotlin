@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.bidcodec


internal expect inline fun unsignedMulHi(x: Long, y: Long): Long

internal expect inline fun unsignedCmp(x: Long, y: Long): Int

internal inline fun unsignedLT(dwA: Long, dwB: Long): Boolean =
    (dwA xor Long.MIN_VALUE) < (dwB xor Long.MIN_VALUE)

