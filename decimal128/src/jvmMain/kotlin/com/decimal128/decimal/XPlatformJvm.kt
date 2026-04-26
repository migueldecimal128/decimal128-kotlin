@file:Suppress("NOTHING_TO_INLINE")
// XPlatformJvm.kt
package com.decimal128.decimal

actual inline fun unsignedMulHi(x: Long, y: Long): Long = Math.unsignedMultiplyHigh(x, y)

actual inline fun unsignedDiv(x: Long, y: Long): Long = java.lang.Long.divideUnsigned(x, y)

actual inline fun unsignedRem(x: Long, y: Long): Long = java.lang.Long.remainderUnsigned(x, y)

actual inline fun unsignedCmp(x: Long, y: Long): Int = java.lang.Long.compareUnsigned(x, y)

actual inline fun mathFma(a: Double, b: Double, c: Double): Double = Math.fma(a, b, c)
