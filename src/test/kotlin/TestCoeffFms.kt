package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestCoeffFms {

    val verbose = false

    class TC(val biX: BigInteger, val biY: BigInteger, val biS: BigInteger) {
        val biResult = biX.multiply(biY).subtract(biS)

        constructor(x: String, y:String, a:String) : this(BigInteger(x), BigInteger(y), BigInteger(a))
    }

    val cases = arrayOf(
        TC("1669490949391031905145964367996983202265", "2054250785867918762649", "0"),
        TC("2", "99999999999999999999999999999999999999", "0"),

        TC("10", "10", "100"),
        TC("1", "2", "0"),
        TC("1", "1", "1"),
        TC("10", "10", "100"),
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
                                if (tc.biResult.bitLength() <= 256)
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
            val case = TC(randBi256(), randBi256(), randBi128())
            test1(case)
        }

    }

    val random = Random()

    fun randBi256() : BigInteger {
        val bitLength = random.nextInt(0, 256)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun randBi128() : BigInteger {
        val bitLength = random.nextInt(0, 128)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun test1(case: TC) {
        val expected = case.biResult
        if (expected.bitLength() > 256)
            return
        if (expected.signum() < 0)
            return
        //if (case.biS.bitLength() > 128)
         //   return
        val coeffX = Coeff(case.biX)
        val coeffY = Coeff(case.biY)
        val coeffS = Coeff(case.biS)
        val coeffExpected = Coeff(case.biResult)
        val coeffResult = Coeff()
        if (verbose)
            println("$coeffX (${coeffX.bitLen} bits) * $coeffY (${coeffY.bitLen} bits) - $coeffS (${coeffS.bitLen} bits) = expected:$expected (${expected.bitLength()} bits)")
        coeffResult.coeffFms(coeffX, coeffY, coeffS)
        val biProd = coeffResult.coeffToBigInteger()
        if (! biProd.equals(expected))
            println("$coeffX (${coeffX.bitLen} bits) * $coeffY (${coeffY.bitLen} bits) - $coeffS (${coeffS.bitLen} bits) = $coeffResult (${coeffResult.bitLen} bits)  expected:$expected (${expected.bitLength()} bits)")
        assert (biProd.equals(expected))

        val rnd = random.nextInt(3)
        if (rnd == 0) {
            coeffX.coeffFms(coeffX, coeffY, coeffS)
            assert(coeffX.coeffToBigInteger().equals(expected))
        } else if (rnd == 1) {
            coeffY.coeffFms(coeffY, coeffX, coeffS)
            assert(coeffY.coeffToBigInteger().equals(expected))
        } else {
            coeffS.coeffFms(coeffX, coeffY, coeffS)
            assert(coeffS.coeffToBigInteger().equals(expected))
        }

    }

}
