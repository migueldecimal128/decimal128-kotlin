package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestCoeffientMul {

    class TC(val biA: BigInteger, val biB: BigInteger) {
        val biProduct = biA.multiply(biB)

        constructor(a: String, b:String) : this(BigInteger(a), BigInteger(b))
    }

    val cases = arrayOf(
        TC("9999999999999999919", "9999999999999999919"),
        TC("9999999999999999918", "99999999999999999920"),
        TC("6075018513315086291545187086261960270921991288466", "16192"),
        TC(BigInteger.ONE.shiftLeft(64), BigInteger.ZERO),
        TC("10000000000000000000000000000000000000000000000000000000000000000000000000000", "1"),

        TC("1", "10000000000000000000"),
        TC("0", "10000000000000000000"),
        TC("0", "1000000000000000000"),
        TC("1", "1000000000000000000"),

        TC("1000000000000000001", "99999999999999999999999999999999999999999999999"),
        TC(BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE), BigInteger.ONE),
        TC("9999999999999999918", "99999999999999999920"),

        TC("0", "1"),
        TC("1", "1"),
        TC("3", "5"),
        TC("999999999", "999999999"),
        TC("999999999", "9999999999"),
        TC("999999999", "99999999999"),
        TC("999999999999999918", "999999999999999918"),
        TC("9999999999999999919", "999999999999999918"),
        TC("9999999999999999919", "9999999999999999919"),
        TC("9999999999999999918", "99999999999999999920"),
        TC(BigInteger.ONE.shiftLeft(250).subtract(BigInteger.ONE),BigInteger.ONE.shiftLeft(250).subtract(BigInteger.ONE)),
        TC(BigInteger.ONE.shiftLeft(200).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(2)),
        TC(BigInteger.ONE.shiftLeft(250), BigInteger.ONE.shiftLeft(250)),
        TC(BigInteger.ONE.shiftLeft(64), BigInteger.ZERO),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    val deltas = arrayOf(BigInteger.ONE.negate(), BigInteger.ZERO, BigInteger.ONE)

    @Test
    fun testBoundaries() {
        for (i in 0..<77) {
            val biX = BigInteger.TEN.pow(i)
            for (j in 0..<(44-i)) {
                val biY = BigInteger.TEN.pow(j)
                for (deltaX in deltas) {
                    val biA = biX.add(deltaX)
                    for (deltaY in deltas) {
                        val biB = biY.add(deltaY)
                        val tc = TC(biA, biB)
                        if (tc.biProduct.bitLength() <= 256)
                            test1(tc)
                    }
                }
            }
        }
    }

    @Test
    fun testRandomMul() {
        for (i in 0..<1000000) {
            val case = TC(randBi(), randBi())
            test1(case)
        }

    }

    val random = Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 255)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun test1(case: TC) {
        val expected = case.biProduct
        if (expected.bitLength() > 256)
            return
        val coeffA = Coeff(case.biA)
        val coeffB = Coeff(case.biB)
        val coeffC = Coeff()
        //println("$coeffA (${coeffA.digitCount}) * $coeffB (${coeffB.digitCount}) = expected:$expected")
        coeffC.mul(coeffA, coeffB)
        val biC = coeffC.toBigInteger()
        if (! biC.equals(expected))
            println("$coeffA (${coeffA.digitCount}) * $coeffB (${coeffB.digitCount}) = $coeffC (${coeffC.digitCount})  expected:$expected")
        assert (biC.equals(expected))

        val oldDigitCount = coeffC.digitCount
        if (! coeffC.isValidDigitCount()) {
            val digitCount = coeffC.digitCount
            println("bad digit count $coeffA * $coeffB = $coeffC was $oldDigitCount should be $digitCount")
            throw RuntimeException()
        }

        coeffA.mul(coeffA, coeffB)
        assert (coeffA.toBigInteger().equals(expected))
    }

}
