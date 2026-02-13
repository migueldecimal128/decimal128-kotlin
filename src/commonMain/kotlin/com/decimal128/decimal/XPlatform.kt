@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

expect inline fun unsignedMulHi(x: Long, y: Long): Long

inline fun unsignedMulHi(x: ULong, y: ULong): ULong = unsignedMulHi(x.toLong(), y.toLong()).toULong()

expect inline fun unsignedDiv(x: Long, y: Long): Long

expect inline fun unsignedMod(x: Long, y: Long): Long

expect inline fun unsignedCmp(x: Long, y: Long): Int

expect inline fun unsignedCmp(x: Int, y: Int): Int

inline fun unsignedLT(x: Long, y: Long): Boolean = (x xor Long.MIN_VALUE) < (y xor Long.MIN_VALUE)

inline fun unsignedGE(x: Long, y: Long): Boolean = (x xor Long.MIN_VALUE) >= (y xor Long.MIN_VALUE)

inline fun unsignedLT_option1(x: Long, y: Long): Boolean = unsignedCmp(x, y) < 0

inline fun unsignedLT_option2(x: Long, y: Long): Boolean = (x xor Long.MIN_VALUE) < (y xor Long.MIN_VALUE)

expect inline fun scalb(d: Double, n: Int): Double

expect inline fun arraycopy(src: ByteArray, srcIndex: Int, dst: ByteArray, dstIndex: Int, len: Int)

expect inline fun arraycopy(src: IntArray, srcIndex: Int, dst: IntArray, dstIndex: Int, len: Int)

expect inline fun shiftUp1(x: ByteArray, srcIndex: Int, len: Int)

expect inline fun shiftUp(x: ByteArray, srcIndex: Int, dstIndex: Int, len: Int)

expect inline fun shiftUp1(x: IntArray, srcIndex: Int, len: Int)

expect inline fun shiftUp(x: IntArray, srcIndex: Int, dstIndex: Int, len: Int)

expect inline fun shiftDown1(x: ByteArray, srcIndex: Int, len: Int)

expect inline fun shiftDown(x: ByteArray, srcIndex: Int, dstIndex: Int, len: Int)

expect inline fun shiftDown1(x: IntArray, srcIndex: Int, len: Int)

expect inline fun shiftDown(x: IntArray, srcIndex: Int, dstIndex: Int, len: Int)


