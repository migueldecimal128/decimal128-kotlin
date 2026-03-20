package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.TEN
import java.util.*

class TestC256BitLength {

    val verbose = false

    class TC(val biA: BigInteger) {
        val digitLength = biA.toString().length
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
        TC(TEN.pow(76).subtract(ONE))
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
            val biX = TEN.pow(i)
            for (deltaX in deltas) {
                val biA = biX.add(deltaX)
                if (biA.toString().length <= 76) {
                    val tc = TC(biA)
                    test1(tc)
                }
            }
        }

        for (i in 0..252) {
            val bi = ONE.shiftLeft(i)
            val tc = TC(bi)
            test1(tc)
        }
        for (i in 0..253) {
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

    fun randBi_76() : BigInteger {
        var bi: BigInteger
        do {
            val bitLength = random.nextInt(0, 254)
            bi = BigInteger(bitLength, random)
        } while (bi.toString().length > 76)
        return bi
    }

    fun randBi() : BigInteger = randBi_76()

    fun test1(case: TC) {
        if (case.digitLength > 76)
            return
        val expected = case.bitLength
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
