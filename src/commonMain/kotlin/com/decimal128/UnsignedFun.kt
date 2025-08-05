package com.decimal128

expect inline fun umulHigh(x: Long, y: Long): Long

expect inline fun umulHigh(x: ULong, y: ULong): ULong

expect inline fun unsignedCompare(x: Long, y: Long): Int

expect inline fun unsignedCompare(x: Int, y: Int): Int

expect inline fun unsignedLT(x: Long, y: Long): Boolean

