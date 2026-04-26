@file:Suppress("NOTHING_TO_INLINE")
// XPlatformJs.kt
package com.decimal128.decimal

actual inline fun unsignedMulHi(x: Long, y: Long): Long {
    val xULong = x.toULong()
    val yULong = y.toULong()
    val xLo = xULong and 0xFFFFFFFFUL
    val xHi = xULong shr 32
    val yLo = yULong and 0xFFFFFFFFUL
    val yHi = yULong shr 32

    val pp00 = xLo * yLo
    val pp01 = xHi * yLo
    val pp10 = xLo * yHi
    val pp11 = xHi * yHi

    val mid = pp01 + pp10  // may overflow
    val midCarry = if (mid < pp01) 1UL else 0UL  // carry from mid overflow

    val midWithLo = (pp00 shr 32) + (mid and 0xFFFFFFFFUL)
    // midWithLo cannot overflow: max is (2^32-1) + (2^32-1) < 2^33

    return (pp11 + (mid shr 32) + (midCarry shl 32) + (midWithLo shr 32)).toLong()
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
