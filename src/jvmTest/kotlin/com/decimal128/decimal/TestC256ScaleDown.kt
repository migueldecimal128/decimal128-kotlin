package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger
import java.util.*
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_POSITIVE
import java.math.BigDecimal
import java.math.RoundingMode
import java.lang.Math.min

class TestC256ScaleDown {

    val verbose = false

    class TC(
        val biA: BigInteger, val pow10: Int, val sign: Boolean, val decRounding: DecRounding)
     {

        constructor(
            x: String, pow10: Int, sign: Boolean,
            decRounding: DecRounding
        ) :
                this(BigInteger(x), pow10, sign, decRounding)

         val bd = if (sign) BigDecimal(biA).negate() else BigDecimal(biA)
         val bdScaled = bd.scaleByPowerOfTen(-pow10)
         val bdRounded = bdScaled.setScale(0, decRounding.mapToRoundingMode())
         val inexact = bdScaled.compareTo(bdRounded) != 0
         val biRoundedAbs = bdRounded.toBigIntegerExact().abs()
         val biUnrounded = bdScaled.setScale(0, RoundingMode.DOWN).toBigIntegerExact().abs()
         //FIXME ... currently not testing rounding because
         // coeffDiv and coeffScaleDownPow10 return the Residue
         val biExpected = biUnrounded

         fun finalDigitCount() =
             if (biExpected.signum() == 0) 0 else biExpected.abs().toString().length
    }

    val bi1 = BigInteger(0x111111111111L.toString())
    val bi2 = BigInteger(0x222222222222L.toString())
    val bi3 = BigInteger(0x333333333333L.toString())
    val bi4 = BigInteger(0x444444444444L.toString())
    val bi5 = BigInteger(0x5555555555555555L.toString())
    val bi54321 = bi5.shiftLeft(192).or(bi4.shiftLeft(144)).or(bi3.shiftLeft(96)).or(bi2.shiftLeft(48)).or(bi1)

    val bi5321 = bi5.shiftLeft(144).or(bi3.shiftLeft(96)).or(bi2.shiftLeft(48)).or(bi1)

    val cases = arrayOf(
        TC("313632014659747341935386347255122675838030894814755919430800302558372603616", 37, false, ROUND_TIES_TO_AWAY),
        TC(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE), 2, false, ROUND_TIES_TO_EVEN),
        TC("599", 2, false, ROUND_TIES_TO_EVEN),
        TC("2", 1, false, ROUND_TIES_TO_EVEN),
        TC("10", 1, false, ROUND_TIES_TO_EVEN),
        TC("5500000000000001", 15, false, ROUND_TIES_TO_EVEN),
        TC("5000000000000000", 15, false, ROUND_TIES_TO_EVEN),
        TC("5500000000000000", 14, false, ROUND_TIES_TO_EVEN),
        TC("5500000000000000", 15, false, ROUND_TIES_TO_EVEN),
        TC("8399566454088370176858046088293", 0, true, ROUND_TIES_TO_EVEN),
        TC("511", 0, true, ROUND_TIES_TO_EVEN),
        TC(bi5321.shiftLeft(1), 1, true, ROUND_TIES_TO_EVEN),
        TC("99999999999999999999999999999999999999999999", 1, true, ROUND_TIES_TO_EVEN),
        TC(bi54321, 1, true, ROUND_TIES_TO_EVEN),
        TC("1087", 2, true, ROUND_TOWARD_ZERO),
        TC("18010334491269082087", 2, true, ROUND_TOWARD_ZERO),
        TC(BigInteger.ONE.shiftLeft(66), 1, false, ROUND_TIES_TO_EVEN),
        TC("8833659103727869972", 4, false, ROUND_TOWARD_POSITIVE),
        TC("2155134570088770723972178854394", 3, false, ROUND_TIES_TO_EVEN),
        TC("78694514899641471", 8, false, ROUND_TOWARD_ZERO),
        TC("218013830959062861903198505440629832955", 9, false, ROUND_TIES_TO_EVEN),
        TC("8833659103727869972", 4, false, ROUND_TOWARD_NEGATIVE),
        TC("8833659103727869972", 4, true, ROUND_TOWARD_POSITIVE),
        TC("8833659103727869972", 4, true, ROUND_TOWARD_NEGATIVE),
        TC("2243325502234968696719", 9, false, ROUND_TIES_TO_EVEN),
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
        for (qDigitCount in Q_MIN..<Q_MAXX) {
            val biQ = BigInteger.TEN.pow(qDigitCount)
            for (xPow10 in K_MIN..<min(K_MAXX, qDigitCount + 2)) {
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
            val coeff = C256(dw3, dw2, dw1, dw0)
            val biQ = coeff.coeffToBigInteger()
            for (xPow10 in K_MIN..<K_MAXX) {
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
            var tc: TC
            do {
                val bi = randBi()
                val pow10 = randPow(bi)
                tc = buildTestCase(bi, pow10)
            } while (tc.finalDigitCount() > 38)
            test1(tc)
        }

    }


    fun buildTestCase(bi:BigInteger, xPow10:Int) : TC {
        val sign = random.nextBoolean()
        val decRounding = DecRounding.fromValue(random.nextInt(5))
        val tc = TC(bi, xPow10, sign, decRounding)
        return tc
    }

    fun randPow(bi: BigInteger) : Int {
        val biDigitCount = bi.toString().length
        val maxPow = min(K_MAXX - 1, biDigitCount + 2)
        val randPow = random.nextInt(maxPow)
        return randPow
    }

    fun randBi() : BigInteger {
        while (true) {
            val bitLength = random.nextInt(0, 257)
            val bi = BigInteger(bitLength, random)
            if (bi.toString().length < Q_MAXX)
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
        val coeffA = newCoeff(case.biA)
        val coeffObserved = C256()
        val pow10 = case.pow10
        val env = DecContext().with(case.decRounding)
        if (verbose)
            println("$coeffA (${coeffA.digitLen}) / 10**$pow10 = sign:$sign ${case.decRounding} expected:$expected")
        c256SetScaleDownPow10(coeffObserved, coeffA, pow10)
        val observed = coeffObserved.coeffToBigInteger()
        if (! observed.equals(expected))
            println("$coeffA (${coeffA.digitLen}) / 10**$pow10 = $coeffObserved (${coeffObserved.digitLen}) sign:$sign ${case.decRounding} expected:$expected")
        assertEquals(expected, observed)
    }

}