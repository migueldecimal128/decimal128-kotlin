package com.decimal128.decimal

import com.decimal128.decimal.U256Bits.calcBitLen64
import com.decimal128.decimal.U256Bits.calcBitLen128
import com.decimal128.decimal.U256Bits.calcBitLen192
import com.decimal128.decimal.U256Bits.calcBitLen256
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.util.*

class TestC256BitLength {

    val verbose = false

    class TC(val biA: BigInteger) {
        val bitLength = biA.bitLength()
        constructor(a: String) : this(BigInteger(a))
    }

    val cases = arrayOf(
        TC("170141183460469231731687303715884105728"),
        TC("9999999999999999999999999999999999999999999999999999999999"),
        TC("37303532557864644374188297275281322713417"),
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
        val coeffA = newCoeff(case.biA)
        if (verbose)
            println("$coeffA (${coeffA.digitLen}) bitLength expected:$expected")
        val observed = coeffA.bitLen
        if (expected != observed)
            println("$coeffA (${coeffA.digitLen}) observed:$observed bitLength expected:$expected")
        assertEquals(expected, observed)

        val bitLen2 = calcBitLen256(coeffA.dw3, coeffA.dw2, coeffA.dw1, coeffA.dw0)
        assertEquals(expected, bitLen2)

        if (expected <= 192) {
            val bitLen3 = calcBitLen192(coeffA.dw2, coeffA.dw1, coeffA.dw0)
            assertEquals(expected, bitLen3)

            if (expected <= 128) {
                val bitLen4 = calcBitLen128(coeffA.dw1, coeffA.dw0)
                assertEquals(expected, bitLen4)

                if (expected <= 64) {
                    val bitLen5 = calcBitLen64(coeffA.dw0)
                    assertEquals(expected, bitLen5)

                }
            }
        }
    }
}
