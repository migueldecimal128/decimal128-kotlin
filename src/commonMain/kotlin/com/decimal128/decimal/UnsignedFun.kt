package com.decimal128.decimal

expect inline fun unsignedMulHi(x: Long, y: Long): Long

expect inline fun unsignedDiv(x: Long, y: Long): Long

expect inline fun unsignedMod(x: Long, y: Long): Long

expect inline fun unsignedCmp(x: Long, y: Long): Int

expect inline fun unsignedCmp(x: Int, y: Int): Int

expect inline fun unsignedLT(x: Long, y: Long): Boolean

