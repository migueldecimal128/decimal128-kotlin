package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger
import java.util.*
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import java.math.BigDecimal
import java.math.RoundingMode

class TestCoeffScaleDown {

    val verbose = false

    class TC(
        val biA: BigInteger, val pow10: Int, val sign: Boolean, val roundingDirection: RoundingDirection)
     {

        constructor(
            x: String, pow10: Int, sign: Boolean,
            roundingDirection: RoundingDirection
        ) :
                this(BigInteger(x), pow10, sign, roundingDirection)

         val bd = if (sign) BigDecimal(biA).negate() else BigDecimal(biA)
         val bdScaled = bd.scaleByPowerOfTen(-pow10)
         val bdRounded = bdScaled.setScale(0, roundingDirection.mapToRoundingMode())
         val inexact = bdScaled.compareTo(bdRounded) != 0
         val biRoundedAbs = bdRounded.toBigIntegerExact().abs()
         val biUnrounded = bdScaled.setScale(0, RoundingMode.DOWN).toBigIntegerExact().abs()
         //FIXME ... currently not testing rounding because
         // coeffDiv and coeffScaleDownPow10 return the Residue
         val biExpected = biUnrounded
    }

    val cases = arrayOf(
        TC("8833659103727869972", 4, false, ROUND_TOWARD_POSITIVE),
        TC("8833659103727869972", 4, false, ROUND_TOWARD_NEGATIVE),
        TC("8833659103727869972", 4, true, ROUND_TOWARD_POSITIVE),
        TC("8833659103727869972", 4, true, ROUND_TOWARD_NEGATIVE),
        TC("2243325502234968696719", 9, false, ROUND_TIES_TO_EVEN),
        TC(BigInteger.ONE.shiftLeft(66), 1, false, ROUND_TIES_TO_EVEN),
        TC("316413813903600685246406848", 8, false, ROUND_TIES_TO_EVEN),
        TC("1598575705195620085819330297435841000492438", 6, false, ROUND_TOWARD_ZERO),
        TC("1087", 2, true, ROUND_TOWARD_ZERO),
        TC("1087", 2, false, ROUND_TIES_TO_EVEN),
        TC("18010334491269082087", 2, true, ROUND_TOWARD_ZERO),
        TC("3482748081595369130101", 16, false, ROUND_TIES_TO_EVEN),
        TC("167457751028870756383096012673692132664", 39, false, ROUND_TIES_TO_AWAY),
        TC("74", 1, false, ROUND_TIES_TO_EVEN),
    )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    val deltas = arrayOf(BigInteger.ONE.negate(), BigInteger.ZERO, BigInteger.ONE)

    @Test
    fun testProblemChild() {
        val tc = TC("8833659103727869972", 4, false, ROUND_TOWARD_POSITIVE)
        test1(tc)
    }

    @Test
    fun testDecimalBoundaries() {
        for (qDigitCount in MIN_DIVIDEND_DIGIT_COUNT..<MAX_DIVIDEND_DIGIT_COUNT) {
            val biQ = BigInteger.TEN.pow(qDigitCount)
            for (xPow10 in MIN_DIVISOR_POW10..<Math.min(MAX_DIVISOR_POW10, qDigitCount + 2)) {
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

    @Test
    fun testBinaryBoundaries() {
        val quads = longArrayOf( // littleEndian order
            (1L shl 62), 0, 0, 0,
            -1, -1, -1, -1,
            -1, 0, 0,0,
            -1, -1, 0, 0,
            -1, -1, -1, 0,
            -1, -1, -1, -1,
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1,
            0, 0, 0, (1L shl 62),
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
                    if (biA.bitLength() <= 64) {
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
        val sign = random.nextBoolean()
        val roundingDirection = RoundingDirection.fromValue(random.nextInt(5))
        val tc = TC(bi, xPow10, sign, roundingDirection)
        return tc
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
        val sign = case.sign
        val coeffA = Coeff(case.biA)
        val coeffObserved = Coeff()
        val pow10 = case.pow10
        val ctx = Decimal128Context(case.roundingDirection)
        if (verbose)
            println("$coeffA (${coeffA.digitLen}) / 10**$pow10 = sign:$sign ${case.roundingDirection} expected:$expected")
        coeffObserved.scaleDownPow10(coeffA, pow10)
        val observed = coeffObserved.coeffToBigInteger()
        if (! observed.equals(expected))
            println("$coeffA (${coeffA.digitLen}) / 10**$pow10 = $coeffObserved (${coeffObserved.digitLen}) sign:$sign ${case.roundingDirection} expected:$expected")
        assertEquals(expected, observed)
    }

}