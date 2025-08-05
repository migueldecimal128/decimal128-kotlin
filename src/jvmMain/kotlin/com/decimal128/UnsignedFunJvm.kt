package com.decimal128

@Suppress("NOTHING_TO_INLINE")
actual inline fun umulHigh(x: Long, y: Long): Long = Math.unsignedMultiplyHigh(x, y)

@Suppress("NOTHING_TO_INLINE")
actual inline fun umulHigh(x: ULong, y: ULong): ULong = umulHigh(x.toLong(), y.toLong()).toULong()

@Suppress("NOTHING_TO_INLINE")
actual inline fun unsignedCompare(x: Long, y: Long): Int = java.lang.Long.compareUnsigned(x, y)

@Suppress("NOTHING_TO_INLINE")
actual inline fun unsignedCompare(x: Int, y: Int): Int = java.lang.Integer.compareUnsigned(x, y)

@Suppress("NOTHING_TO_INLINE")
actual inline fun unsignedLT(x: Long, y: Long): Boolean = (x xor Long.MIN_VALUE) < (y xor Long.MIN_VALUE)
