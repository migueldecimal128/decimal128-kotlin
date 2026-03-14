package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestC256Fms {

    val verbose = false

    class TC(val biX: BigInteger, val biY: BigInteger, val biS: BigInteger) {
        val biIntermediateProd = biX.multiply(biY)
        val biResult = biIntermediateProd.subtract(biS)

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
        for (i in 0..76) {
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
                                if (tc.biIntermediateProd.toString().length <= 76 && tc.biResult.toString().length <= 76)
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
            val case = TC(randBi253(), randBi253(), randBi128())
            test1(case)
        }

    }

    val random = Random()

    fun randBi253() : BigInteger {
        val bitLength = random.nextInt(0, 253)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun randBi128() : BigInteger {
        val bitLength = random.nextInt(0, 128)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun test1(case: TC) {
        val intermediateProd = case.biIntermediateProd
        if (intermediateProd.toString().length > 76)
            return
        val expected = case.biResult
        if (expected.toString().length > 76)
            return
        if (expected.signum() < 0)
            return
        //if (case.biS.bitLength() > 128)
         //   return
        val coeffX = newCoeff(case.biX)
        val coeffY = newCoeff(case.biY)
        val coeffS = newCoeff(case.biS)
        val coeffExpected = newCoeff(case.biResult)
        val coeffResult = C256()
        val pentad = Pentad()
        if (verbose)
            println("$coeffX (${coeffX.bitLen} bits) * $coeffY (${coeffY.bitLen} bits) - $coeffS (${coeffS.bitLen} bits) = expected:$expected (${expected.bitLength()} bits)")
        c256SetFms(coeffResult, coeffX, coeffY, coeffS, pentad)
        val biProd = coeffResult.coeffToBigInteger()
        if (! biProd.equals(expected))
            println("$coeffX (${coeffX.bitLen} bits) * $coeffY (${coeffY.bitLen} bits) - $coeffS (${coeffS.bitLen} bits) = $coeffResult (${coeffResult.bitLen} bits)  expected:$expected (${expected.bitLength()} bits)")
        assert (biProd.equals(expected))

        val rnd = random.nextInt(3)
        if (rnd == 0) {
            c256SetFms(coeffX, coeffX, coeffY, coeffS, pentad)
            assert(coeffX.coeffToBigInteger().equals(expected))
        } else if (rnd == 1) {
            c256SetFms(coeffY, coeffY, coeffX, coeffS, pentad)
            assert(coeffY.coeffToBigInteger().equals(expected))
        } else {
            c256SetFms(coeffS, coeffX, coeffY, coeffS, pentad)
            assert(coeffS.coeffToBigInteger().equals(expected))
        }

    }

}
