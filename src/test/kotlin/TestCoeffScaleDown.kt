package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger
import java.util.*
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE
import java.math.BigDecimal
import java.math.MathContext

class TestCoeffScaleDown {

    class TC(val biA:BigInteger, val pow10:Int, val sign:Boolean,
             val roundingDirection:RoundingDirection, val biExpected:BigInteger, val expectedInexact:Boolean) {

        constructor(x:String, pow10:Int,
                    sign:Boolean, roundingDirection:RoundingDirection, expected:String, expectedInexact:Boolean) :
                this(BigInteger(x), pow10, sign, roundingDirection, BigInteger(expected), expectedInexact)
    }

    val cases = arrayOf(
        TC("74", 1, false, ROUND_TIES_TO_EVEN, "7", true),
        TC("94", 1, false, ROUND_TIES_TO_EVEN, "9", true),
        TC("14", 1, false, ROUND_TIES_TO_EVEN, "1", true),
        TC("84", 1, false, ROUND_TIES_TO_EVEN, "8", true),
        TC("95", 1, false, ROUND_TIES_TO_EVEN, "9", true),
        TC("96", 1, false, ROUND_TIES_TO_EVEN, "9", true),
        TC("0", 2, false, ROUND_TIES_TO_EVEN, "0", false),
        TC("1234567890", 1, false, ROUND_TIES_TO_EVEN, "123456789", false),
        TC("0", 1, false, ROUND_TIES_TO_EVEN, "0", false),
        TC("0", 1, true, ROUND_TIES_TO_EVEN, "0", false),

        TC("1", 1, false, ROUND_TIES_TO_EVEN, "0", true),
        TC("4", 1, false, ROUND_TIES_TO_EVEN, "0", true),
        TC("5", 1, false, ROUND_TIES_TO_EVEN, "0", true),
        TC("6", 1, false, ROUND_TIES_TO_EVEN, "1", true),
        TC("6", 1, false, ROUND_TIES_TO_EVEN, "1", true),
        TC("1234567890", 1, false, ROUND_TIES_TO_EVEN, "123456789", false),
        TC("1234567890", 2, false, ROUND_TIES_TO_EVEN, "12345679", true),
        TC("1234567890", 7, false, ROUND_TIES_TO_EVEN, "123", true),
        TC("1234500000", 6, false, ROUND_TIES_TO_EVEN, "1234", true),
        TC("1234500000", 6, false, ROUND_TIES_TO_AWAY, "1235", true),

        TC("1234567890123456789012345678901234567890", 30, false, ROUND_TIES_TO_EVEN, "1234567890", true),
        TC("1234567890123456789012345678901234567890", 39, false, ROUND_TIES_TO_EVEN, "1", true),
        TC("1234567890123456789012345678901234567890", 36, false, ROUND_TIES_TO_EVEN, "1235", true),
        TC("1234567890123456789012345678901234567890", 36, false, ROUND_TIES_TO_AWAY, "1235", true),
        TC("1234567890123456789012345678901234567890", 36, false, ROUND_TOWARD_ZERO, "1234", true),
        TC("1234567890123456789012345678901234567890", 36, false, ROUND_TOWARD_POSITIVE, "1235", true),
        TC("1234567890123456789012345678901234567890", 36, false, ROUND_TOWARD_NEGATIVE, "1234", true),
        TC("1234567890123456789012345678901234567890", 36, true, ROUND_TOWARD_POSITIVE, "1234", true),
        TC("1234567890123456789012345678901234567890", 36, true, ROUND_TOWARD_NEGATIVE, "1235", true),
        TC("1234567890123456789012345678901234567890", 36, true, ROUND_TOWARD_ZERO, "1234", true),
        TC("1234567890123456789012345678901234567890", 36, true, ROUND_TIES_TO_EVEN, "1235", true),
        TC("1234567890123456789012345678901234567890", 36, true, ROUND_TIES_TO_AWAY, "1235", true),

        TC("1234567890123456789012345678901234567890123456789012345678901234567890", 40, false, ROUND_TIES_TO_EVEN, "123456789012345678901234567890", true),
        TC("1234567890123456789012345678901234567890123456789012345678901234567890", 1, false, ROUND_TIES_TO_EVEN, "123456789012345678901234567890123456789012345678901234567890123456789", false),
    )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }
/*
    val deltas = arrayOf(BigInteger.ONE.negate(), BigInteger.ZERO, BigInteger.ONE)

    @Test
    fun testBoundaries() {
        for (i in 0..<77) {
            val biX = BigInteger.TEN.pow(i)
            for (pow10 in 0..<77-i) {
                for (deltaX in deltas) {
                    val biA = biX.add(deltaX)
                    val tc = TC(biA, pow10)
                    if (tc.biProduct.bitLength() <= 256)
                        test1(tc)
                }
            }
        }
    }
*/
    val random = Random()

    @Test
    fun testRandomMul() {
        for (i in 0..<1000000) {
            val bi = randBi()
            val pow10 = randPow(bi)
            val roundingDirection = RoundingDirection.fromValue(random.nextInt(5))
            val sign = random.nextBoolean()
            val bd = if (sign) BigDecimal(bi).negate() else BigDecimal(bi)
            val bdScaled = bd.scaleByPowerOfTen(-pow10)
            val bdRounded = bdScaled.setScale(0, roundingDirection.mapToRoundingMode())
            val inexact = bdScaled.compareTo(bdRounded) != 0
            val biRoundedAbs = bdRounded.toBigIntegerExact().abs()
            val case = TC(bi, pow10, sign, roundingDirection, biRoundedAbs, inexact)
            test1(case)
        }

    }

    fun randPow(bi: BigInteger) : Int {
        val biDigitCount = bi.toString().length
        val maxPow = biDigitCount + 2
        val randPow = random.nextInt(maxPow)
        return randPow
    }

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 10)
        val bi = BigInteger(bitLength, random)
        return bi
    }



    fun test1(case: TC) {
        val expected = case.biExpected
        if (expected.bitLength() > 256) {
            println("product would overflow ... skipped")
            return
        }
        val coeffA = Coeff(case.biA)
        val coeffObserved = Coeff()
        val pow10 = case.pow10
        val ctx = Decimal128Context(case.roundingDirection)
        println("$coeffA (${coeffA.digitCount}) / 10**$pow10 = ${case.roundingDirection} expected:$expected expectedInexact:${case.expectedInexact}")
        coeffObserved.scalePow10(coeffA, -pow10, case.sign, ctx)
        val observed = coeffObserved.toBigInteger()
        if (! observed.equals(expected))
            println("$coeffA (${coeffA.digitCount}) / 10**$pow10 = $coeffObserved (${coeffObserved.digitCount}) ${case.roundingDirection} expected:$expected expectedInexact:${case.expectedInexact}")
        if (case.expectedInexact != ctx.inexact)
            println("INEXACT mismatch $coeffA (${coeffA.digitCount}) / 10**$pow10 = $coeffObserved (${coeffObserved.digitCount})  ${case.roundingDirection} inexact expected:${case.expectedInexact} observed:${ctx.inexact}")
        assert (observed.equals(expected))
        assertEquals (case.expectedInexact, ctx.inexact)
    }

}