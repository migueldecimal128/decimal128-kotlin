package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestCoeffFma {

    class TC(val biX: BigInteger, val biY: BigInteger, val biA: BigInteger) {
        val biProduct = biX.multiply(biY).add(biA)

        constructor(x: String, y:String, a:String) : this(BigInteger(x), BigInteger(y), BigInteger(a))
    }

    val cases = arrayOf(
        TC("1", "0", "1"),
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
                                if (tc.biProduct.bitLength() <= 256)
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
        val expected = case.biProduct
        if (expected.bitLength() > 256)
            return
        val coeffX = Coeff(case.biX)
        val coeffY = Coeff(case.biY)
        val coeffA = Coeff(case.biA)
        val coeffExpected = Coeff(case.biProduct)
        val coeffProd = Coeff()
        //println("$coeffX (${coeffX.digitCount}) * $coeffY (${coeffY.digitCount}) + $coeffA (${coeffA.digitCount}) = expected:$expected")
        coeffProd.fma(coeffX, coeffY, coeffA)
        val biProd = coeffProd.toBigInteger()
        if (! biProd.equals(expected))
            println("$coeffX (${coeffX.digitCount}) * $coeffY (${coeffY.digitCount}) + $coeffA (${coeffA.digitCount}) = $coeffProd (${coeffProd.digitCount})  expected:$expected")
        assert (biProd.equals(expected))

        val oldDigitCount = coeffProd.digitCount
        if (! coeffProd.isValidDigitCount()) {
            val digitCount = coeffProd.digitCount
            println("bad digit count $coeffX * $coeffY = $coeffProd was $oldDigitCount should be $digitCount")
            throw RuntimeException()
        }

        val rnd = random.nextInt(3)
        if (rnd == 0) {
            coeffX.fma(coeffX, coeffY, coeffA)
            assert(coeffX.toBigInteger().equals(expected))
        } else if (rnd == 1) {
            coeffY.fma(coeffX, coeffY, coeffA)
            assert(coeffY.toBigInteger().equals(expected))
        } else {
            coeffA.fma(coeffX, coeffY, coeffA)
            assert(coeffA.toBigInteger().equals(expected))
        }

    }

}
