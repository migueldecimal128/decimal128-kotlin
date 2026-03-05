package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestC256Fma {

    class TC(val biX: BigInteger, val biY: BigInteger, val biA: BigInteger) {
        val biProduct = biX.multiply(biY).add(biA)

        constructor(x: String, y:String, a:String) : this(BigInteger(x), BigInteger(y), BigInteger(a))
    }

    val cases = arrayOf(
        TC("1", "2", "0"),
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
        val coeffX = newCoeff(case.biX)
        val coeffY = newCoeff(case.biY)
        val coeffA = newCoeff(case.biA)
        val coeffExpected = newCoeff(case.biProduct)
        val coeffProd = C256()
        //println("$coeffX (${coeffX.digitCount}) * $coeffY (${coeffY.digitCount}) + $coeffA (${coeffA.digitCount}) = expected:$expected")
        c256SetFma(coeffProd, coeffX, coeffY, coeffA)
        val biProd = coeffProd.coeffToBigInteger()
        if (! biProd.equals(expected))
            println("$coeffX (${coeffX.digitLen}) * $coeffY (${coeffY.digitLen}) + $coeffA (${coeffA.digitLen}) = $coeffProd (${coeffProd.digitLen})  expected:$expected")
        assert (biProd.equals(expected))

        val oldDigitCount = coeffProd.digitLen
        if (! coeffProd.c256HasValidLengths()) {
            val digitCount = coeffProd.digitLen
            println("bad digit count $coeffX * $coeffY = $coeffProd was $oldDigitCount should be $digitCount")
            throw RuntimeException()
        }

        val rnd = random.nextInt(3)
        if (rnd == 0) {
            c256SetFma(coeffX, coeffX, coeffY, coeffA)
            assert(coeffX.coeffToBigInteger().equals(expected))
        } else if (rnd == 1) {
            c256SetFma(coeffY, coeffX, coeffY, coeffA)
            assert(coeffY.coeffToBigInteger().equals(expected))
        } else {
            c256SetFma(coeffA, coeffX, coeffY, coeffA)
            assert(coeffA.coeffToBigInteger().equals(expected))
        }

    }

}
