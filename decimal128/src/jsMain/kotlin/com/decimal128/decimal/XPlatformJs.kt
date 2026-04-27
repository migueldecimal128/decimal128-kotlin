@file:Suppress("NOTHING_TO_INLINE")
// XPlatformJs.kt
package com.decimal128.decimal

actual inline fun unsignedMulHi(x: Long, y: Long): Long {
    val xLo = x and 0xFFFFFFFFL
    val xHi = x ushr 32
    val yLo = y and 0xFFFFFFFFL
    val yHi = y ushr 32

    val pp00 = xLo * yLo
    val pp01 = xHi * yLo
    val pp10 = xLo * yHi
    val pp11 = xHi * yHi

    val mid = pp01 + pp10  // may overflow
    val midCarryShifted = if (unsignedLT(mid, pp01)) (1L shl 32) else 0L

    val midWithLo = (pp00 ushr 32) + (mid and 0xFFFFFFFFL)
    // midWithLo cannot overflow: max is (2^32-1) + (2^32-1) < 2^33

    return pp11 + (mid ushr 32) + midCarryShifted + (midWithLo ushr 32)
}

actual inline fun unsignedDiv(x: Long, y: Long): Long =
    (x.toULong() / y.toULong()).toLong()

actual inline fun unsignedRem(x: Long, y: Long): Long =
    (x.toULong() % y.toULong()).toLong()

actual inline fun unsignedCmp(x: Long, y: Long): Int =
    (x xor Long.MIN_VALUE).compareTo(y xor Long.MIN_VALUE)

actual inline fun mathFma(a: Double, b: Double, c: Double): Double =
    mathFmaCorrect(a, b, c)

private fun twoProd(a: Double, b: Double): Pair<Double, Double> {
    val p = a * b
    val c = 134217729.0 * a  // 2^27 + 1
    val aHi = c - (c - a)
    val aLo = a - aHi
    val d = 134217729.0 * b
    val bHi = d - (d - b)
    val bLo = b - bHi
    val err = ((aHi * bHi - p) + aHi * bLo + aLo * bHi) + aLo * bLo
    return Pair(p, err)
}

private fun twoSum(a: Double, b: Double): Pair<Double, Double> {
    val s = a + b
    val v = s - a
    val err = (a - (s - v)) + (b - v)
    return Pair(s, err)
}

fun mathFmaCorrect(a: Double, b: Double, c: Double): Double {
    // 1. Get the exact product (p + err)
    val (p, pErr) = twoProd(a, b)

    // 2. Sum the exact product with c using an expansion
    // We need to sum three terms: p, pErr, and c
    val (s1, e1) = twoSum(p, c)

    // 3. The final result is the main sum plus the accumulation of all errors
    // This is where the "fusion" happens mathematically
    return s1 + (e1 + pErr)
}
