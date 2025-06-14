package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.lang.Long.numberOfTrailingZeros
import java.lang.Math.unsignedMultiplyHigh
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.util.*
import kotlin.math.nextDown

class TestSqrt{

    class TC(val bd: BigDecimal) {
        constructor(str: String) : this(BigDecimal(str))
        val sqrt0 = bd.sqrt(MathContext.DECIMAL128)
        val sqrt0Squared = sqrt0.multiply(sqrt0)
        val isPerfect = sqrt0Squared.compareTo(bd) == 0
        val zeroPadding = 34 - sqrt0.precision()
        val sqrt = if (isPerfect) sqrt0 else sqrt0.setScale(sqrt0.scale() + zeroPadding, RoundingMode.UNNECESSARY)
    }

    val tcs = arrayOf (
        TC("625"),
        //TC("+10E-1"),
        //TC("+10E-1839"),
        TC("+0.0E4019"),
        TC("+2139362027"),
        TC("+2139362027E-4288"),
        TC("1e1"),
        TC("1"),
        TC("2"),
        TC("10"),
        TC("4"),
        TC("16"),
        TC("256"),
        TC("400"),
        TC("40000"),
        TC("4000000"),
        TC("2"),
        TC("900"),
        TC("10000"),
        TC("1234567890123456789012345678901234"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    @Test
    fun testProblemChild() {
        val tc = TC("+10E-3")
        test1(tc)
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val tc = TC(randBd())
            test1(tc)
        }

    }

    val random = Random()

    fun randBd() : BigDecimal {
        val bitLength = random.nextInt(0, 112)
        val bi = BigInteger(bitLength, random)
        val exp = random.nextInt(3*4096) - 6176
        val bd = BigDecimal(bi).scaleByPowerOfTen(exp)
        return bd
    }

    fun test1(tc: TC) {
        val dec = Dec34()
        val decSqrt = Dec34()
        dec.set(tc.bd)
        setSqrt(decSqrt, dec)
        val expected = tc.sqrt
        assertEquals(expected.unscaledValue(), decSqrt.coeffToBigInteger())
        assertEquals(-expected.scale(), decSqrt.qExp)
    }

    fun setSqrt(sqrt: Dec34, radicand: Dec34) {
        val qPreferred = radicand.qExp shr 1
        if (radicand.coeffIsZero()) {
            val sciExp = radicand.sciExp()
            println("zero radicand:$radicand sciExp:$sciExp")
            sqrt.setZero()
            sqrt.qExp = qPreferred
            sqrt.sign = radicand.sign
            return
        }
        val radicandScaled = Coeff()
        val scaleUp = 70 - radicand.digitLen + (radicand.digitLen and 1) + (radicand.qExp and 1)
        radicandScaled.coeffSetScaleUpPow10(radicand, scaleUp)
        println("radicand:$radicand radicandScaled:$radicandScaled")

        val dbl0 = radicandScaled.coeffToFloorDouble()
        // g == guess
        val guess0Double = Math.sqrt(dbl0).nextDown()
        //val rawGuess0 = guess0Double.toRawBits()
        //val guess0Mantissa = ((rawGuess0 and ((1L shl 52) - 1)) or (1L shl 52)) - 1 // ensure floor
        //val guess0Exp = ((rawGuess0 ushr 52).toInt() and 0x7FF) - 1023

        //val guess0Coeff = Coeff(guess0Mantissa)
        //guess0Coeff.coeffSetShiftLeft(guess0Coeff, Math.max(guess0Exp - 52, 0))
        val guess0Coeff = Coeff()
        guess0Coeff.coeffSet(guess0Double)
        println(" --> dbl0:$dbl0 doubleGuess0:$guess0Double coeffGuess0:$guess0Coeff")
        guess0Coeff.coeffMutateDecrement()

        val guess0Squared = Coeff()
        guess0Squared.coeffSetMul(guess0Coeff, guess0Coeff)
        println(" --> coeffGuess0:$guess0Coeff guess0Squared:$guess0Squared")
        while (guess0Squared.coeffUnscaledCompareTo(radicandScaled) > 0) {
            guess0Coeff.coeffMutateDecrement()
            guess0Squared.coeffSetMul(guess0Coeff, guess0Coeff)
        }
        val residual0 = Coeff()
        CoeffSub.coeffSubUnscaled(residual0, radicandScaled, guess0Squared)
        println(" --> residual0:$residual0")

        /*
        val residual0BitCount = Math.min(53, residual0.bitLen)
        val residual0BitIndex = residual0.bitLen - residual0BitCount
        val residualTopBits = CoeffBits.getDwordAtBitIndex(residual0, residual0BitIndex)
        val residual0Double = Math.scalb(residualTopBits.toDouble(), residual0BitIndex)
         */
        val residual0Double = residual0.coeffToFloorDouble()

        val inv2Guess0 = 0.5 / guess0Double
        val delta0Double = residual0Double * inv2Guess0
        val delta0Raw = delta0Double.toRawBits()
        val delta0Mantissa = ((delta0Raw and ((1L shl 52) - 1)) or (1L shl 52))
        val delta0Exp = ((delta0Raw ushr 52).toInt() and 0x7FF) - 1023
        val delta0Coeff = Coeff(delta0Mantissa)
        delta0Coeff.coeffSetShiftLeft(delta0Coeff, Math.max(delta0Exp - 52, 0))

        val guess1Coeff = Coeff()
        guess1Coeff.coeffSetAdd(guess0Coeff, delta0Coeff)
        println(" --> guess1Coeff:$guess1Coeff")

        val guess1Squared = Coeff()
        guess1Squared.coeffSetMul(guess1Coeff, guess1Coeff)
        while (guess1Squared.coeffUnscaledCompareTo(radicandScaled) > 0) {
            guess1Coeff.coeffMutateDecrement()
            guess1Squared.coeffSetMul(guess1Coeff, guess1Coeff)
        }
        val residual1 = Coeff()
        CoeffSub.coeffSubUnscaled(residual1, radicandScaled, guess1Squared)
        println(" --> residual1:$residual1")

        val guess1x2 = Coeff()
        guess1x2.coeffSetShiftLeft(guess1Coeff, 1)
        val delta1 = Coeff()
        val residue1 = delta1.coeffSetDiv(residual1, guess1x2)

        val guess2 = Coeff()
        guess2.coeffSetAdd(guess1Coeff, delta1)
        println(" --> guess2:$guess2")

        val guess2Squared = Coeff()
        guess2Squared.coeffSetMul(guess2, guess2)
        val residual2 = Coeff()
        CoeffSub.coeffSubUnscaled(residual2, radicandScaled, guess2Squared)
        println(" --> residual2:$residual2")

        println(" --> scaleUp:$scaleUp preferred:$qPreferred")

        sqrt.coeffSet(guess2)
        sqrt.qExp = -scaleUp / 2
        sqrt.sign = 0

        println(" --> sqrt:$sqrt")

        val residue2 = if (residual2.coeffIsZero()) Residue.EXACT else Residue.LT_HALF
        var qZ = (radicand.qExp - scaleUp) / 2
        var ntz = numberOfTrailingZeros(sqrt.dw0)
        if (residue2 == Residue.EXACT && qZ < qPreferred && ntz > 0) {
            if (qZ + 1 < qPreferred) {
                val quot = Coeff()
                do {
                    val deltaQ = qPreferred - qZ
                    val chunk = Math.min(Math.min(9, deltaQ), ntz)
                    val chunkRemainder = DivBarrett.barrettDivModPow10(quot, sqrt, chunk)
                    if (chunkRemainder > 0) {
                        var pow10Count = 0
                        var t = chunkRemainder
                        val M = 0xCCCCCCCCCCCCCCCDuL.toLong()
                        while (true) {
                            // val q = t / 10
                            // val r = t % 10
                            val q = unsignedMultiplyHigh(t, M) ushr 3
                            val r = t - (q * 10)
                            if (r != 0L)
                                break
                            ++pow10Count
                            t = q
                        }
                        if (pow10Count > 0) {
                            sqrt.coeffSetScaleDownPow10(sqrt, pow10Count)
                            qZ += pow10Count
                        }
                        break
                    } else {
                        sqrt.coeffSet(quot)
                        ntz -= chunk
                        qZ += chunk
                    }
                } while (qZ < qPreferred && ntz > 0)
            } else if (sqrt.coeffIsMultipleOf10()) {
                sqrt.coeffSetScaleDownPow10(sqrt, 1)
                ++qZ
            }
        }
        sqrt.qExp = qZ
        sqrt.roundAndFinalize(residue2, 0, Decimal128Context())

        println(" --> sqrt:$sqrt")

        /*
        val bitCount1 = Math.min(53, coeff0Diff.bitLen)
        val bitIndex1 = coeff0Diff.bitLen - bitCount1
        val topBits1 = CoeffBits.getDwordAtBitIndex(coeff0Diff, bitIndex1)
        val dbl1 = Math.scalb(topBits1.toDouble(), bitIndex1)
        val dblSqrt1 = Math.sqrt(dbl1)
        val logB1 = Math.getExponent(dblSqrt1)
        val delta1 = logB1 - 52
        val dblRaw1 = dblSqrt1.toRawBits()
        val dblSqrt1Mantissa = ((dblRaw1 and ((1L shl 52) - 1)) or (1L shl 52)) - 1 // ensure floor
        val dblSqrt1Exp = ((dblRaw1 ushr 52).toInt() and 0x7FF) - 1023

        val coeff1 = Coeff()
        coeff1.coeffSet64(dblSqrt1Mantissa)
        coeff1.coeffSetShiftLeft(coeff1, Math.max(dblSqrt1Exp - 52, 0))
        println(" --> dbl1:$dbl1 dblSqrt1:$dblSqrt1, dblSqrt1Mantissa:$dblSqrt1Mantissa dblSqrt1Exp:$dblSqrt1Exp coeff1:$coeff1")

        val coeff1squared = Coeff()
        coeff1squared.coeffSetMul(coeff1, coeff1)
        val coeff1Diff = Coeff()
        CoeffSub.coeffSubUnscaled(coeff1Diff, coeff0Diff, coeff1squared)
        println(" --> coeff1Diff:$coeff1Diff")

        val bitCount2 = Math.min(53, coeff1Diff.bitLen)
        val bitIndex2 = coeff1Diff.bitLen - bitCount2
        val topBits2 = CoeffBits.getDwordAtBitIndex(coeff1Diff, bitIndex2)
        val dbl2 = Math.scalb(topBits2.toDouble(), bitIndex2)
        val dblSqrt2 = Math.sqrt(dbl2)
        val logB2 = Math.getExponent(dblSqrt2)
        val delta2 = logB2 - 52
        val dblRaw2 = dblSqrt2.toRawBits()
        val dblSqrt2Mantissa = ((dblRaw2 and ((1L shl 52) - 1)) or (1L shl 52)) - 1 // ensure floor
        val dblSqrt2Exp = ((dblRaw2 ushr 52).toInt() and 0x7FF) - 1023

        val coeff2 = Coeff()
        coeff2.coeffSet64(dblSqrt2Mantissa)
        coeff2.coeffSetShiftLeft(coeff2, Math.max(dblSqrt2Exp - 52, 0))
        println(" --> dbl2:$dbl2 dblSqrt2:$dblSqrt2, dblSqrt2Mantissa:$dblSqrt2Mantissa dblSqrt2Exp:$dblSqrt2Exp coeff2:$coeff2")

        val coeff2squared = Coeff()
        coeff2squared.coeffSetMul(coeff2, coeff2)
        val coeff2Diff = Coeff()
        CoeffSub.coeffSubUnscaled(coeff2Diff, coeff1Diff, coeff2squared)
        println(" --> coeff2Diff:$coeff2Diff")

        val bitCount3 = Math.min(53, coeff2Diff.bitLen)
        val bitIndex3 = coeff2Diff.bitLen - bitCount3
        val topBits3 = CoeffBits.getDwordAtBitIndex(coeff2Diff, bitIndex3)
        val dbl3 = Math.scalb(topBits3.toDouble(), bitIndex3)
        val dblSqrt3 = Math.sqrt(dbl3)
        val logB3 = Math.getExponent(dblSqrt3)
        val delta3 = logB3 - 52
        val dblRaw3 = dblSqrt3.toRawBits()
        val dblSqrt3Mantissa = ((dblRaw3 and ((1L shl 52) - 1)) or (1L shl 52)) - 1 // ensure floor
        val dblSqrt3Exp = ((dblRaw3 ushr 52).toInt() and 0x7FF) - 1023

        val coeff3 = Coeff()
        coeff3.coeffSet64(dblSqrt3Mantissa)
        coeff3.coeffSetShiftLeft(coeff3, Math.max(dblSqrt3Exp - 52, 0))
        println(" --> dbl3:$dbl3 dblSqrt3:$dblSqrt3, dblSqrt3Mantissa:$dblSqrt3Mantissa dblSqrt3Exp:$dblSqrt3Exp coeff3:$coeff3")

        val coeff3squared = Coeff()
        coeff3squared.coeffSetMul(coeff3, coeff3)
        val coeff3Diff = Coeff()
        CoeffSub.coeffSubUnscaled(coeff3Diff, coeff2Diff, coeff3squared)
        println(" --> coeff3Diff:$coeff3Diff")


         */

    }

}