package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.TWO
import java.math.BigInteger.TEN
import java.util.*

class TestCoeffFusedMulAbsDiff {

    class TC(val biX: BigInteger, val biY: BigInteger, val biA: BigInteger) {
        val biAbsDiff = biX.multiply(biY).subtract(biA).abs()

        constructor(x: String, y:String, a:String) : this(BigInteger(x), BigInteger(y), BigInteger(a))
    }

    val cases = arrayOf(
        TC(ONE.shiftLeft(256).subtract(ONE), TWO, ONE.shiftLeft(256).subtract(ONE)),
        TC("1", "41953078857", "69141105809440508213736970815833952828572312190904827688854686"),
        TC("9", "9", "99"),
        TC("99", "99", "999"),
        TC("2", "10000000000000000000000000000000000000", "99999999999999999999"),
        TC("9999999999999999919", "9999999999999999919", "1"),
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
                for (k in 0..<35) {
                    val biA = BigInteger.TEN.pow(k)
                    for (deltaX in deltas) {
                        val biXdelta = biX.add(deltaX)
                        for (deltaY in deltas) {
                            val biYdelta = biY.add(deltaY)
                            for (deltaA in deltas) {
                                val biAdelta = biA.add(deltaA)
                                val tc = TC(biXdelta, biYdelta, biAdelta)
                                if (tc.biAbsDiff.bitLength() <= 256)
                                    test1(tc)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val case = TC(randBi(), randBi(), randBi())
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
        val expected = case.biAbsDiff
        if (expected.bitLength() > 256)
            return
        val coeffX = Coeff(case.biX)
        val coeffY = Coeff(case.biY)
        val coeffA = Coeff(case.biA)
        val coeffExpected = Coeff(case.biAbsDiff)
        val coeffAbsDiff = Coeff()
        //println("$coeffX (${coeffX.digitCount}) * $coeffY (${coeffY.digitCount}) - $coeffA (${coeffA.digitCount}) = expected:$expected")
        coeffAbsDiff.fusedMulAbsDiff(coeffX, coeffY, coeffA)
        val observed = coeffAbsDiff.toBigInteger()
        if (! observed.equals(expected))
            println("$coeffX (${coeffX.digitCount}) * $coeffY (${coeffY.digitCount}) - $coeffA (${coeffA.digitCount}) = $coeffAbsDiff (${coeffAbsDiff.digitCount})  expected:$expected")
        assert (observed.equals(expected))

        coeffAbsDiff.fusedMulAbsDiff(coeffY, coeffX, coeffA)
        val observed2 = coeffAbsDiff.toBigInteger()
        if (! observed2.equals(expected))
            println("$coeffX ($coeffY (${coeffY.digitCount}) * ${coeffX.digitCount}) - $coeffA (${coeffA.digitCount}) = $coeffAbsDiff (${coeffAbsDiff.digitCount})  expected:$expected")
        assert (observed2.equals(expected))

        val rnd = random.nextInt(6)
        when (rnd) {
            0 -> {
                coeffX.fusedMulAbsDiff(coeffX, coeffY, coeffA); assert(coeffX.toBigInteger().equals(expected))
            }

            1 -> {
                coeffY.fusedMulAbsDiff(coeffX, coeffY, coeffA); assert(coeffY.toBigInteger().equals(expected))
            }

            2 -> {
                coeffA.fusedMulAbsDiff(coeffX, coeffY, coeffA); assert(coeffA.toBigInteger().equals(expected))
            }

            3 -> {
                coeffX.fusedMulAbsDiff(coeffY, coeffX, coeffA); assert(coeffX.toBigInteger().equals(expected))
            }

            4 -> {
                coeffY.fusedMulAbsDiff(coeffY, coeffX, coeffA); assert(coeffY.toBigInteger().equals(expected))
            }

            5 -> {
                coeffA.fusedMulAbsDiff(coeffY, coeffX, coeffA); assert(coeffA.toBigInteger().equals(expected))
            }
        }

    }

}
