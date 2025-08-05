package com.decimal128

actual fun umulHigh(x: Long, y: Long): Long = Math.unsignedMultiplyHigh(x, y)

actual fun umulHigh(x: ULong, y: ULong): ULong = umulHigh(x.toLong(), y.toLong()).toULong()