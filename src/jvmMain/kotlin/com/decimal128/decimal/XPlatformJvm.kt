@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

actual inline fun unsignedMulHi(x: Long, y: Long): Long = Math.unsignedMultiplyHigh(x, y)

actual inline fun unsignedDiv(x: Long, y: Long): Long = java.lang.Long.divideUnsigned(x, y)

actual inline fun unsignedMod(x: Long, y: Long): Long = java.lang.Long.remainderUnsigned(x, y)

actual inline fun unsignedCmp(x: Long, y: Long): Int = java.lang.Long.compareUnsigned(x, y)

actual inline fun unsignedCmp(x: Int, y: Int): Int = java.lang.Integer.compareUnsigned(x, y)

actual inline fun scalb(d: Double, n: Int): Double = java.lang.Math.scalb(d, n)

actual inline fun arraycopy(src: IntArray, srcIndex: Int, dst: IntArray, dstIndex: Int, len: Int) =
    System.arraycopy(src, srcIndex, dst, dstIndex, len)

actual inline fun arraycopy(src: ByteArray, srcIndex: Int, dst: ByteArray, dstIndex: Int, len: Int) =
    System.arraycopy(src, srcIndex, dst, dstIndex, len)

actual inline fun shiftUp1(x: ByteArray, srcIndex: Int, len: Int) =
    System.arraycopy(x, srcIndex, x, srcIndex + 1, len)

actual inline fun shiftUp(x: ByteArray, srcIndex: Int, dstIndex: Int, len: Int) =
    System.arraycopy(x, srcIndex, x, dstIndex, len)

actual inline fun shiftUp1(x: IntArray, srcIndex: Int, len: Int) =
    System.arraycopy(x, srcIndex, x, srcIndex + 1, len)

actual inline fun shiftUp(x: IntArray, srcIndex: Int, dstIndex: Int, len: Int) =
    System.arraycopy(x, srcIndex, x, srcIndex + 1, len)

actual inline fun shiftDown1(x: ByteArray, srcIndex: Int, len: Int) =
    System.arraycopy(x, srcIndex, x, srcIndex - 1, len)

actual inline fun shiftDown(x: ByteArray, srcIndex: Int, dstIndex: Int, len: Int) =
    System.arraycopy(x, srcIndex, x, dstIndex, len)

actual inline fun shiftDown1(x: IntArray, srcIndex: Int, len: Int) =
    System.arraycopy(x, srcIndex, x, srcIndex - 1, len)

actual inline fun shiftDown(x: IntArray, srcIndex: Int, dstIndex: Int, len: Int) =
    System.arraycopy(x, srcIndex, x, dstIndex, len)

actual inline fun mathFma(a: Double, b: Double, c: Double): Double = Math.fma(a, b, c)

actual inline fun mathUlp(a: Double): Double = Math.ulp(a)