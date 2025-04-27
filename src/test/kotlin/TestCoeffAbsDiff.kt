package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestCoeffAbsDiff {

    class TC(val biA:BigInteger, val biB:BigInteger) {
        val biAbsDiff = biA.subtract(biB).abs()
        val isEqual = biA == biB
        val isReversed = biA < biB
        constructor(a:String, b:String) :this(BigInteger(a), BigInteger(b))

    }

    val cases = arrayOf(
        TC(BigInteger.ONE.shiftLeft(32).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(32).subtract(BigInteger.ONE)),
        TC(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)),
        TC(BigInteger.ONE.shiftLeft(96).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(96).subtract(BigInteger.ONE)),
        TC(BigInteger.ONE.shiftLeft(250).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(250).subtract(BigInteger.ONE)),

        TC(BigInteger.ONE, BigInteger.ONE.shiftLeft(192)),
        TC(BigInteger.ONE, BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)),
        TC(BigInteger.ONE, BigInteger.ONE.shiftLeft(64)),
        TC(BigInteger.ONE, BigInteger.ONE.shiftLeft(128)),
        TC(BigInteger.ONE, BigInteger.ONE.shiftLeft(192)),
        TC("22314046076771935944280046840374065293", "1"),
        TC("2231404607677193594428004684037406529390", "1"),
        TC("223140460767719359442800468403740652939", "1"),
        TC("1", "223140460767719359442800468403740652939"),
        TC("3255443143888146669276642975534954", "223140460767719359442800468403740652939"),
        TC("1", "0"),
        TC("2", "1"),
        TC("0", "688532143920808120673146908602245010982565719894737"),
        TC("4323480046920433710361467845758581556765", "436004"),
        TC(BigInteger.ONE.shiftLeft(250).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(250).subtract(BigInteger.ONE)),
        TC(BigInteger.ONE.shiftLeft(250).subtract(BigInteger.ONE), BigInteger.ONE),
        TC(BigInteger.ONE.shiftLeft(250), BigInteger.ONE.shiftLeft(250)),
        TC(BigInteger.ONE.shiftLeft(64), BigInteger.ONE),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val biA = randBi()
            val biB = randBi()
            val case = TC(biA, biB)

            test1(case)
        }

    }

    val random = Random()

    fun randBi() :BigInteger {
        val bitLength = random.nextInt(0, 255)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun test1(case: TC) {
        val biExpected = case.biAbsDiff
        if (biExpected.bitLength() > 256)
            return // outside our supported range
        val coeffA = Coeff(case.biA)
        val coeffB = Coeff(case.biB)
        val yin = Coeff()
        val yang = Coeff()
        //println("$coeffA - $coeffB = expected:$biExpected")
        val yinResidue = yin.absDiff(coeffA, coeffB)
        val yangResidue = yang.absDiff(coeffB, coeffA)

        val biYin = yin.toBigInteger()
        val biYang = yang.toBigInteger()
        if (! biYin.equals(biExpected))
            println("$coeffA - $coeffB = yin:$biYin yinResidue:$yinResidue expected:$biExpected")
        if (! biYang.equals(biExpected))
            println("$coeffB - $coeffA = yang:$biYang yangResidue:$yangResidue expected:$biExpected")
        if (case.isEqual) {
            assert(!yinResidue.isNegated())
            assert(!yangResidue.isNegated())
        } else {
            assertEquals(case.isReversed, yinResidue.isNegated())
            assertEquals(case.isReversed, ! yangResidue.isNegated())
        }

        assert (biYin.equals(biExpected))
        assert (biYang.equals(biExpected))

    }

}