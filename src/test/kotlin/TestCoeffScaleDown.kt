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

class TestCoeffScaleDown {

    val verbose = false

    class TC(val biA:BigInteger, val pow10:Int, val sign:Boolean,
             val roundingDirection:RoundingDirection, val biExpected:BigInteger, val expectedInexact:Boolean) {

        constructor(x:String, pow10:Int,
                    sign:Boolean, roundingDirection:RoundingDirection, expected:String, expectedInexact:Boolean) :
                this(BigInteger(x), pow10, sign, roundingDirection, BigInteger(expected), expectedInexact)
    }

    val cases = arrayOf(
        TC("1234567890123456789012345678901234567890123456789012345678901234567890", 40, false, ROUND_TIES_TO_EVEN, "123456789012345678901234567890", true),
        TC("115792089237316195423570985008687907853269984665640564039457584007913129639935", 4, false, ROUND_TIES_TO_EVEN, "11579208923731619542357098500868790785326998466564056403945758400791312964", true),
        TC("115792089237316195423570985008687907853269984665640564039457584007913129639935", 3, false, ROUND_TIES_TO_EVEN, "115792089237316195423570985008687907853269984665640564039457584007913129640", true),
        TC("115792089237316195423570985008687907853269984665640564039457584007913129639935", 1, false, ROUND_TIES_TO_EVEN, "11579208923731619542357098500868790785326998466564056403945758400791312963994", true),
        TC("1234567890123456789012345678901234567890123456789012345678901234567890", 40, false, ROUND_TIES_TO_EVEN, "123456789012345678901234567890", true),
        TC("167457751028870756383096012673692132664", 39, false, ROUND_TIES_TO_AWAY, "0", true),
        TC("1000000000000000000", 1, false, ROUND_TIES_TO_EVEN, "100000000000000000", false),
        TC("74", 1, false, ROUND_TIES_TO_EVEN, "7", true),
        TC("75", 1, false, ROUND_TIES_TO_EVEN, "8", true),
        TC("76", 1, false, ROUND_TIES_TO_EVEN, "8", true),
        TC("740", 2, false, ROUND_TIES_TO_EVEN, "7", true),
        TC("750", 2, false, ROUND_TIES_TO_EVEN, "8", true),
        TC("760", 2, false, ROUND_TIES_TO_EVEN, "8", true),
        TC("741", 2, false, ROUND_TIES_TO_EVEN, "7", true),
        TC("751", 2, false, ROUND_TIES_TO_EVEN, "8", true),
        TC("761", 2, false, ROUND_TIES_TO_EVEN, "8", true),
        TC("1000000000000000009", 1, false, ROUND_TIES_TO_EVEN, "100000000000000001", true),
        TC("115792089237316195423570985008687907853269984665640564039457584007913129639935", 1, false, ROUND_TIES_TO_EVEN, "11579208923731619542357098500868790785326998466564056403945758400791312963994", true),
        TC("1000000000000000000", 1, false, ROUND_TIES_TO_EVEN, "100000000000000000", false),
        TC("100000000000000000", 1, false, ROUND_TIES_TO_EVEN, "10000000000000000", false),
        TC("0", 1, false, ROUND_TIES_TO_EVEN, "0", false),
        TC("115792089237316195423570985008687907853269984665640564039457584007913129639935", 1, false, ROUND_TIES_TO_EVEN, "11579208923731619542357098500868790785326998466564056403945758400791312963994", true),
        TC("115792089237316195423570985008687907853269984665640564039457584007913129639935", 44, false, ROUND_TIES_TO_EVEN, "1157920892373161954235709850086879", true),
        TC("161027067925926009762976744537943203828", 39, false, ROUND_TIES_TO_AWAY, "0", true),
        TC("167457751028870756383096012673692132664", 39, false, ROUND_TIES_TO_AWAY, "0", true),
        TC("1234567890123456789012345678901234567890123456789012345678901234567890", 40, false, ROUND_TIES_TO_EVEN, "123456789012345678901234567890", true),
        TC("74", 1, false, ROUND_TIES_TO_EVEN, "7", true),
        TC("94", 1, false, ROUND_TIES_TO_EVEN, "9", true),
        TC("14", 1, false, ROUND_TIES_TO_EVEN, "1", true),
        TC("84", 1, false, ROUND_TIES_TO_EVEN, "8", true),
        TC("95", 1, false, ROUND_TIES_TO_EVEN, "10", true),
        TC("96", 1, false, ROUND_TIES_TO_EVEN, "10", true),
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

    val deltas = arrayOf(BigInteger.ONE.negate(), BigInteger.ZERO, BigInteger.ONE)

    @Test
    fun testDecimalBoundaries() {
        for (qDigitCount in MIN_DIVIDEND_DIGIT_COUNT..<MAX_DIVIDEND_DIGIT_COUNT) {
            val biQ = BigInteger.TEN.pow(qDigitCount)
            for (xPow10 in MIN_DIVISOR_POW10..<Math.min(MIN_DIVISOR_POW10, qDigitCount + 2)) {
                for (deltaX in deltas) {
                    val biA = biQ.add(deltaX)
                    val case = buildTestCase(biA, xPow10)
                    test1(case)
                }
            }
        }
    }

    @Test
    fun testBinaryBoundaries() {
        val quads = longArrayOf(
            -1, -1, -1, -1,
            -1, 0, 0,0,
            -1, -1, 0, 0,
            -1, -1, -1, 0,
            -1, -1, -1, -1,
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1,
        )

        for (i in quads.indices step 4) {
            val dw0 = quads[i + 0]
            val dw1 = quads[i + 1]
            val dw2 = quads[i + 2]
            val dw3 = quads[i + 3]
            val biQ = Ular.toBigInteger(dw3, dw2, dw1, dw0)
            for (xPow10 in MIN_DIVISOR_POW10..<MAX_DIVISOR_POW10) {
                for (deltaX in deltas) {
                    val biA = biQ.add(deltaX)
                    if (biA.bitLength() <= 256) {
                        val case = buildTestCase(biA, xPow10)
                        test1(case)
                    }
                }
            }
        }
    }

    val random = Random()

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val bi = randBi()
            val pow10 = randPow(bi)
            val case = buildTestCase(bi, pow10)
            test1(case)
        }

    }

    fun buildTestCase(bi:BigInteger, xPow10:Int) : TC {
        val roundingDirection = RoundingDirection.fromValue(random.nextInt(5))
        val sign = random.nextBoolean()
        val bd = if (sign) BigDecimal(bi).negate() else BigDecimal(bi)
        val bdScaled = bd.scaleByPowerOfTen(-xPow10)
        val bdRounded = bdScaled.setScale(0, roundingDirection.mapToRoundingMode())
        val inexact = bdScaled.compareTo(bdRounded) != 0
        val biRoundedAbs = bdRounded.toBigIntegerExact().abs()
        val case = TC(bi, xPow10, sign, roundingDirection, biRoundedAbs, inexact)
        return case
    }

    fun randPow(bi: BigInteger) : Int {
        val biDigitCount = bi.toString().length
        val maxPow = Math.min(MAX_DIVISOR_POW10 - 1, biDigitCount + 2)
        val randPow = random.nextInt(maxPow)
        return randPow
    }

    fun randBi() : BigInteger {
        while (true) {
            val bitLength = random.nextInt(0, 257)
            val bi = BigInteger(bitLength, random)
            if (bi.toString().length < MAX_DIVIDEND_DIGIT_COUNT)
                return bi
        }
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
        if (verbose)
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