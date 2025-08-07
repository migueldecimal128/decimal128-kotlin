package com.decimal128.decimal

@Suppress("NOTHING_TO_INLINE")
actual inline fun unsignedMulHi(x: Long, y: Long): Long = Math.unsignedMultiplyHigh(x, y)

@Suppress("NOTHING_TO_INLINE")
actual inline fun unsignedDiv(x: Long, y: Long): Long = java.lang.Long.divideUnsigned(x, y)

@Suppress("NOTHING_TO_INLINE")
actual inline fun unsignedMod(x: Long, y: Long): Long = java.lang.Long.remainderUnsigned(x, y)

@Suppress("NOTHING_TO_INLINE")
actual inline fun unsignedCmp(x: Long, y: Long): Int = java.lang.Long.compareUnsigned(x, y)

@Suppress("NOTHING_TO_INLINE")
actual inline fun unsignedCmp(x: Int, y: Int): Int = java.lang.Integer.compareUnsigned(x, y)

@Suppress("NOTHING_TO_INLINE")
actual inline fun unsignedLT(x: Long, y: Long): Boolean = (x xor Long.MIN_VALUE) < (y xor Long.MIN_VALUE)
