@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

expect inline fun unsignedMulHi(x: Long, y: Long): Long

inline fun unsignedMulHi(x: ULong, y: ULong): ULong = unsignedMulHi(x.toLong(), y.toLong()).toULong()

expect inline fun unsignedDiv(x: Long, y: Long): Long

expect inline fun unsignedRem(x: Long, y: Long): Long

expect inline fun unsignedCmp(x: Long, y: Long): Int

expect inline fun unsignedCmp(x: Int, y: Int): Int

inline fun unsignedLT(x: Long, y: Long): Boolean = (x xor Long.MIN_VALUE) < (y xor Long.MIN_VALUE)

inline fun unsignedGT(x: Long, y: Long): Boolean = (x xor Long.MIN_VALUE) > (y xor Long.MIN_VALUE)

inline fun unsignedGE(x: Long, y: Long): Boolean = (x xor Long.MIN_VALUE) >= (y xor Long.MIN_VALUE)

inline fun unsignedLT_option1(x: Long, y: Long): Boolean = unsignedCmp(x, y) < 0

inline fun unsignedLT_option2(x: Long, y: Long): Boolean = (x xor Long.MIN_VALUE) < (y xor Long.MIN_VALUE)

expect fun mathFma(a: Double, b: Double, c: Double): Double


