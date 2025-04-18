package com.decimal128

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import java.math.BigInteger
import java.util.*

private val divisor = 100000000L
private val DIVISOR = BigInteger("" + divisor)
private val S = 89
// Using S = 89 (so that M fits in 63 bits)
// Compute M = ceil(2^S / 100000000)
// For S = 89, 2^89 / 100000000 ≈ 6189700196426901.37, so M = 6189700196426902.
// val M = BigInteger.valueOf(6189700196426902L)
private val twoPowS = BigInteger("2").pow(S)
private val M = (twoPowS + DIVISOR - BigInteger.ONE) / DIVISOR
private val m = M.longValueExact()

fun mul63x63_hi64(x:Long, y:Long) :Long {
    val xL = x and 0xFFFFFFFFL
    val xH = x ushr 32
    val yL = y and 0xFFFFFFFFL
    val yH = y ushr 32
    val pLL = xL * yL
    val pLH = xL * yH
    val pHL = xH * yL
    val pHH = xH * yH

    val s1 = pLL ushr 32
    val s2 = pLH + pHL + s1
    val c2 = (((pLH and pHL) or ((pLH or pHL) and s2.inv())) ushr 63) shl 32
    val s3 = (s2 ushr 32) + c2 + pHH
    return s3
}

fun divMod1e8(x:Long) :Pair<Long, Long> {
    val USE_BIGINTEGER = false
    if (USE_BIGINTEGER) {
        val X = BigInteger.valueOf(x)
        val Q = (X * M).shiftRight(S)
        val R = X - (Q * DIVISOR)
        return Pair(Q.longValueExact(), R.longValueExact())
    } else {
        val q = mul63x63_hi64(x, m) ushr (S - 64)
        val r = x - (q * divisor)
        return Pair(q, r)
    }
}

val random = Random()

fun test1(x:Long, y:Long) {
    val X = BigInteger.valueOf(x)
    val Y = BigInteger.valueOf(y)
    val expected = ((X * Y) shr 64).longValueExact()

    val observed = mul63x63_hi64(x, y)
    //println("x:$x y:$y expected:$expected observed:$observed")

    assert(expected == observed)
}

fun test1() {
    val x = random.nextLong() and Long.MAX_VALUE
    val y = random.nextLong() and Long.MAX_VALUE
    test1(x, y)
}

fun mainX() {
    println("M=$M S=$S")
    test1(Long.MAX_VALUE, Long.MAX_VALUE)
    for (i in 0..100000000)
        test1()
}


fun main() {
    // Divisor:100,000,000

    val t = mutableSetOf<BigInteger>()
    var pow = BigInteger.ONE
    for (x in 0..18) {
        t.add(pow - BigInteger.ONE)
        t.add(pow)
        t.add(pow + BigInteger.ONE)
        pow *= BigInteger.TEN
    }

    // Sort the set to test in increasing order.
    val sortedT = t.toList().sorted()

    // Test the reciprocal multiplication method on every value in t.
    for (x in sortedT) {
        val expectedQ = x / DIVISOR
        val expectedR = x % DIVISOR
        val (quotient, remainder) = divMod1e8(x.longValueExact())
        val observedQ = BigInteger.valueOf(quotient)
        val observedR = BigInteger.valueOf(remainder.toLong())

        println("x = $x")
        println("  Expected quotient = $expectedQ remainder = $expectedR")
        println("  Approx quotient = $observedQ  remainder = $observedR")
        println("  Match: ${expectedQ == observedQ}  ${expectedR == observedR}")
        assert(expectedQ == observedQ)
        assert(expectedR == observedR)
        println()
    }

}
