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

class TestSqrtDoubleDouble{

    val verbose = false

    class TC(val bd: BigDecimal) {
        constructor(str: String) : this(BigDecimal(str))
        val sqrt0 = bd.sqrt(MathContext.DECIMAL128)
        val sqrt0Squared = sqrt0.multiply(sqrt0)
        val isPerfect = sqrt0Squared.compareTo(bd) == 0
        val zeroPadding = 34 - sqrt0.precision()
        val scale = bd.scale()
        val sqrt = if (isPerfect) {
            if ((scale and 1) != 0 && (bd.unscaledValue().mod(BigInteger.TEN).signum() == 0)) {
                sqrt0.setScale((scale + 1) / 2)
            } else {
                sqrt0
            }
        } else {
            sqrt0.setScale(sqrt0.scale() + zeroPadding, RoundingMode.UNNECESSARY)
        }
    }

    val tcs = arrayOf (
        TC("+59952631752260E5262"),
        TC("1.5134773972131969E69"),
        TC("100E-2"),
        TC("+10E-1"),
        TC("+1000E-3"),
        TC("+10E-1839"),
        TC("625"),
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
        val tc = TC("+10E-1")
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
        val dec = Decimal()
        val decSqrt = Decimal()
        dec.set(tc.bd)
        setSqrt(decSqrt, dec)
        val expected = tc.sqrt
        assertEquals(expected.unscaledValue(), decSqrt.coeffToBigInteger())
        assertEquals(-expected.scale(), decSqrt.qExp)
    }

    var total = 0L
    var corrections1 = 0L
    var correctionsGT1 = 0L

    fun setSqrt(sqrt: Decimal, radicand: Decimal) {
        ++total
        val qPreferred = radicand.qExp shr 1
        if (radicand.coeffIsZero()) {
            val sciExp = radicand.sciExp()
            if (verbose)
                println("zero radicand:$radicand sciExp:$sciExp")
            sqrt.setZero()
            sqrt.qExp = qPreferred
            sqrt.sign = radicand.sign
            return
        }
        val coeffRadicandScaled = Coeff()
        val scaleUp = 70 - radicand.digitLen + (radicand.digitLen and 1) + (radicand.qExp and 1)
        coeffRadicandScaled.coeffSetScaleUpPow10(radicand, scaleUp)
        if (verbose)
            println("radicand:$radicand radicandScaled:$coeffRadicandScaled")

        val dRadicandScaled = coeffRadicandScaled.coeffToFloorDouble()

        val dGuess0 = Math.sqrt(dRadicandScaled)
        val rawGuess0 = dGuess0.toRawBits()
        var guess0Significand = ((rawGuess0 and ((1L shl 52) - 1)) or (1L shl 52))
        val guess0Exp = ((rawGuess0 ushr 52).toInt() and 0x7FF) - 1023
        val coeffGuess0 = Coeff()
        val coeffGuess0Squared = Coeff()
        val coeffResidual0 = Coeff()
        val coeffDelta0 = Coeff()
        val coeffGuess1 = Coeff()
        val coeffGuess1Squared = Coeff()
        var corrections = -1
        while (true) {
            ++corrections
            --guess0Significand
            coeffGuess0.coeffSet64(guess0Significand)
            coeffGuess0.coeffSetShiftLeft(coeffGuess0, Math.max(guess0Exp - 52, 0))
            if (verbose)
                println(" --> dbl0:$dRadicandScaled doubleGuess0:$dGuess0 coeffGuess0:$coeffGuess0")

            coeffGuess0Squared.coeffSetSqr(coeffGuess0)
            if (coeffRadicandScaled.coeffUnscaledCompareTo(coeffGuess0Squared) < 0)
                continue

            coeffResidual0.coeffSetSub(coeffRadicandScaled, coeffGuess0Squared)
            if (verbose)
                println(" --> residual0:$coeffResidual0")

            val dResidual0 = coeffResidual0.coeffToFloorDouble()

            val dRecip2xGuess0 = 0.5 / dGuess0
            val dDelta0 = dResidual0 * dRecip2xGuess0
            val delta0Raw = dDelta0.toRawBits()
            val delta0Significand = ((delta0Raw and ((1L shl 52) - 1)) or (1L shl 52))
            val delta0Exp = ((delta0Raw ushr 52).toInt() and 0x7FF) - 1023
            coeffDelta0.coeffSet64(delta0Significand - 1)
            coeffDelta0.coeffMutateDecrement()
            coeffDelta0.coeffSetShiftLeft(coeffDelta0, Math.max(delta0Exp - 52, 0))

            coeffGuess1.coeffSetAdd(coeffGuess0, coeffDelta0)
            if (verbose)
                println(" --> guess1Coeff:$coeffGuess1")

            coeffGuess1Squared.coeffSetSqr(coeffGuess1)
            if (verbose)
                println(" ==> coeffRadicandScaled:$coeffRadicandScaled coeffGuess1Squared:$coeffGuess1Squared")
            if (coeffRadicandScaled.coeffUnscaledCompareTo(coeffGuess1Squared) >= 0)
                break
            }
        if (corrections > 0) {
            if (corrections == 1)
                ++corrections1
            else
                ++correctionsGT1
            println("corrections:$corrections total:$total corrections1:$corrections1 correctionsGT1:$correctionsGT1")
        }
        val coeffResidual1 = Coeff()
        CoeffSub.coeffSubUnscaled(coeffResidual1, coeffRadicandScaled, coeffGuess1Squared)
        if (verbose)
            println(" --> residual1:$coeffResidual1")

        val ddT = coeffGuess1.coeffToNewDoubleDouble()
        ddT.mutateDouble()
        ddT.mutateInvFast()
        val ddResidual1 = coeffResidual1.coeffToNewDoubleDouble()
        val ddDelta1 = DoubleDouble.newMulApprox(ddResidual1, ddT)
        val coeffDelta1 = Coeff()
        coeffDelta1.coeffSet(ddDelta1)

        val coeffGuess2 = Coeff()
        coeffGuess2.coeffSetAdd(coeffGuess1, coeffDelta1)

        if (verbose)
            println(" ==> coeffGuess2:$coeffGuess2")

        val coeffGuess2Squared = Coeff()
        coeffGuess2Squared.coeffSetMul(coeffGuess2, coeffGuess2)
        val residual2 = Coeff()
        residual2.coeffSetSub(coeffRadicandScaled, coeffGuess2Squared)

        if (verbose)
            println(" ==> residual2:$residual2")

        if (verbose)
            println(" --> scaleUp:$scaleUp preferred:$qPreferred")

        sqrt.coeffSet(coeffGuess2)
        sqrt.qExp = -scaleUp / 2
        sqrt.sign = 0

        if (verbose)
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
        sqrt.roundAndFinalize(residue2, DecimalContext())

        if (verbose)
            println(" --> sqrt:$sqrt")


    }

}
