package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.util.*

class TestCoeffBitLength {

    val verbose = false

    class TC(val biA: BigInteger) {
        val bitLength = biA.bitLength()
        constructor(a: String) : this(BigInteger(a))
    }

    val cases = arrayOf(
        TC("0"),
        TC("1"),
        TC(ONE.shiftLeft(64).subtract(ONE)),
        TC(ONE.shiftLeft(64)),
        TC(ONE.shiftLeft(128).subtract(ONE)),
        TC(ONE.shiftLeft(128)),
        TC(ONE.shiftLeft(192).subtract(ONE)),
        TC(ONE.shiftLeft(192)),
        TC(ONE.shiftLeft(256).subtract(ONE)),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    val deltas = arrayOf(ONE.negate(), BigInteger.ZERO, ONE)

    @Test
    fun testBoundaries() {
        for (i in 0..<77) {
            val biX = BigInteger.TEN.pow(i)
            for (deltaX in deltas) {
                val biA = biX.add(deltaX)
                val tc = TC(biA)
                if (tc.bitLength <= 256)
                    test1(tc)
            }
        }

        for (i in 0..255) {
            val bi = ONE.shiftLeft(i)
            val tc = TC(bi)
            test1(tc)
        }
        for (i in 0..256) {
            val bi = ONE.shiftLeft(i).subtract(ONE)
            val tc = TC(bi)
            test1(tc)
        }
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val case = TC(randBi())
            test1(case)
        }

    }

    val random = Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 256)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun test1(case: TC) {
        val expected = case.bitLength
        if (expected > 256)
            return
        val coeffA = Coeff(case.biA)
        if (verbose)
            println("$coeffA (${coeffA.digitLen}) bitLength expected:$expected")
        val observed = coeffA.bitLen
        if (expected != observed)
            println("$coeffA (${coeffA.digitLen}) observed:$observed bitLength expected:$expected")
        assertEquals(expected, observed)
    }

}
