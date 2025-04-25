package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestCoeffAddUnrounded {

    class TC(val biA:BigInteger, val biB:BigInteger) {
        val biSum = biA.add(biB)

        constructor(a:String, b:String) : this(BigInteger(a), BigInteger(b))
    }

    val cases = arrayOf(
        TC("10000000000000000000000000000000000000000000000000000000000000000000000000000", "1"),
        TC("11002044283145426452", "83"),
        TC("9898648910501246606", "8919245473293500599"),
        TC("96391704742582514987", "0"),
        TC("300000000000000000000000000000000000000", "400000000000000000000000000000000000000"),
        TC("715519456384878388153883883486068107173", "1"),
        TC("43643123891145784062610756767344509631", "671876332493732604091273126718723597542"),
        TC(BigInteger.ONE.shiftLeft(250).subtract(BigInteger.ONE),BigInteger.ONE.shiftLeft(250).subtract(BigInteger.ONE)),
            TC(BigInteger.ONE.shiftLeft(250).subtract(BigInteger.ONE), BigInteger.ONE),
        TC(BigInteger.ONE.shiftLeft(250), BigInteger.ONE.shiftLeft(250)),
        TC(BigInteger.ONE.shiftLeft(64), BigInteger.ZERO),
        TC("0", "1"), TC("1", "2"),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
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
        val expected = case.biSum
        if (expected.bitLength() > 256)
            return
        val coeffA = Coeff(case.biA)
        val coeffB = Coeff(case.biB)
        val coeffC = Coeff()
        coeffC.add(coeffA, coeffB)
        val oldDigitCount = coeffC.digitCount
        if (! coeffC.isValidDigitCount()) {
            val digitCount = coeffC.digitCount
            println("bad digit count $coeffA + $coeffB = $coeffC was $oldDigitCount should be $digitCount")
            throw RuntimeException()
        }
        val biC = coeffC.toBigInteger()
        if (! biC.equals(expected))
            println("$coeffA + $coeffB = $coeffC   expected:$expected")
        assert (biC.equals(expected))

        coeffA.add(coeffA, coeffB)
        assert (coeffA.toBigInteger().equals(expected))
    }

}