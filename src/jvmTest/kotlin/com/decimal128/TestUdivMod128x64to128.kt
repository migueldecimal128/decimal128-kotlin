package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.util.*

class TestUdivMod128x64to128 {

    val verbose = false

    class TC(val biX: BigInteger, val biY: BigInteger) {
        constructor(strX: String, strY: String) :
                this(BigInteger(strX), BigInteger(strY))
        val xBitLen = biX.bitLength()
        val yBitLen = biY.bitLength()
        val x1 = biX.shiftRight(64).toLong()
        val x0 = biX.toLong()
        val y0 = biY.toLong()
        val pair = biX.divideAndRemainder(biY)
        val quot = pair[0]
        val rem = pair[1]
        val q1 = quot.shiftRight(64).toLong()
        val q0 = quot.toLong()
        val r0 = rem.toLong()
    }

    val tcs = arrayOf(
        //TC(ONE.shiftLeft(64), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(64), ONE.shiftLeft(32).add(ONE)),
        TC(ONE.shiftLeft(127), ONE.shiftLeft(32).add(ONE)),
        TC(ONE.shiftLeft(127), ONE.shiftLeft(32).add(ONE)),
        TC("58862472429443935685763286105747889", "89918751114211992"),
        TC("2", "1"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        if (tc.xBitLen > 128)
            return
        if (tc.yBitLen > 64)
            return
        if (tc.yBitLen == 0)
            return

        if (verbose)
            println("${tc.biX} / ${tc.biY} => ${tc.quot} : ${tc.rem}")
        val x1 = tc.x1
        val x0 = tc.x0
        val y0 = tc.y0

        val (q1, q0, r0) = udivMod128x64to128(x1, x0, y0)
        assertEquals(tc.q0, q0)
        assertEquals(tc.q1, q1)
        assertEquals(tc.r0, r0)
    }

    val random = Random()

    @Test
    fun testRandom() {
        for (i in 0..<10000000) {
            val x = randBi128()
            val y = randBi64()
            if (y.bitLength() > 0) {
                val tc = TC(x, y)
                test1(tc)
            }
        }
    }

    fun randBi128() : BigInteger {
        val bitLength = random.nextInt(0, 129)
        val bi128 = BigInteger(bitLength, random)
        return bi128
    }

    fun randBi64() : BigInteger {
        val bitLength = random.nextInt(0, 65)
        val bi64 = BigInteger(bitLength, random)
        return bi64
    }

}