package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.lang.Long.numberOfTrailingZeros
import java.lang.Math.unsignedMultiplyHigh
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.util.*
import java.lang.Math.max
import java.lang.Math.min
import kotlin.math.sqrt

class TestSqrt{

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
            //sqrt0.setScale(sqrt0.scale() + zeroPadding, RoundingMode.UNNECESSARY)
            sqrt0
        }
    }

    val tcs = arrayOf (
        TC("1.8317919805335444813E-270"),
        TC("200"),
        TC("200"),
        TC("2e2"),
        TC("20e1"),
        TC("+10E-3"),
        TC("+2139362027E-4288"),
        TC("625"),
        TC("2"),
        TC("4"),
        TC("16"),
        TC("256"),
        TC("400"),
        TC("40000"),
        TC("4000000"),
        //TC("+10E-1"),
        //TC("+10E-1839"),
        //TC("+0.0E4019"),
        TC("+2139362027"),
        TC("1e1"),
        TC("1"),
        TC("2"),
        TC("10"),
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

    @Test
    fun testSqrt2() {
        val md2 = MutDec().set(2)
        val ctx38 = DecContext.decimal128Extended38()
        val sqrt = MutDec()
        mutDecSqrtPosFnz_38(sqrt, md2, ctx38)
    }

    @Test
    fun testSqrt20() {
        val md2 = MutDec().set(20)
        val ctx38 = DecContext.decimal128Extended38()
        val sqrt = MutDec()
        mutDecSqrtPosFnz_38(sqrt, md2, ctx38)
    }

    @Test
    fun testSqrt200() {
        val md2 = MutDec().set(200)
        val ctx38 = DecContext.decimal128Extended38()
        val sqrt = MutDec()
        mutDecSqrtPosFnz_38(sqrt, md2, ctx38)
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
        if (tc.bd.signum() == 0)
            return
        val dec = MutDec()
        val decSqrt = MutDec()
        dec.set(tc.bd)
        val ctx34 = DecContext.decimal128IEEE()
        mutDecSqrtPosFnz_38(decSqrt, dec, ctx34)
        val expected = tc.sqrt
        assertEquals(expected.unscaledValue(), decSqrt.coeffToBigInteger())
        assertEquals(-expected.scale(), decSqrt.qExp)
    }

    fun setSqrt(sqrt: MutDec, radicand: MutDec) {
        val tmps = DecTmps()

        val qPreferred = radicand.qExp shr 1
        if (radicand.c256IsZero()) {
            val sciExp = radicand.sciExp()
            if (verbose)
                println("zero radicand:$radicand sciExp:$sciExp")
            sqrt.setZero()
            sqrt.qExp = qPreferred
            sqrt.sign = radicand.sign
            return
        }
        val radicandScaled = C256()
        val scaleUp = 70 - radicand.digitLen + (radicand.digitLen and 1) + (radicand.qExp and 1)
        val tmpPentad = Pentad()
        c256SetScaleUpPow10(radicandScaled, radicand, scaleUp, tmpPentad)
        if (verbose)
            println("radicand:$radicand radicandScaled:$radicandScaled")

        val dbl0 = radicandScaled.c256ToFloorDouble()
        // g == guess
        val guess0Double = Math.nextDown(Math.sqrt(dbl0))
        //val rawGuess0 = guess0Double.toRawBits()
        //val guess0Mantissa = ((rawGuess0 and ((1L shl 52) - 1)) or (1L shl 52)) - 1 // ensure floor
        //val guess0Exp = ((rawGuess0 ushr 52).toInt() and 0x7FF) - 1023

        //val guess0Coeff = Coeff(guess0Mantissa)
        //guess0Coeff.coeffSetShiftLeft(guess0Coeff, max(guess0Exp - 52, 0))
        val guess0Coeff = C256()
        guess0Coeff.c256Set(guess0Double)
        if (verbose)
            println(" --> dbl0:$dbl0 doubleGuess0:$guess0Double coeffGuess0:$guess0Coeff")
        guess0Coeff.c256MutateDecrement()

        val guess0Squared = C256()
        c256SetMul(guess0Squared, guess0Coeff, guess0Coeff, tmpPentad)
        if (verbose)
            println(" --> coeffGuess0:$guess0Coeff guess0Squared:$guess0Squared")
        while (c256UnscaledCompare(guess0Squared, radicandScaled) > 0) {
            guess0Coeff.c256MutateDecrement()
            c256SetMul(guess0Squared, guess0Coeff, guess0Coeff, tmpPentad)
        }
        val residual0 = C256()
        c256SetSubUnscaled(residual0, radicandScaled, guess0Squared)
        if (verbose)
            println(" --> residual0:$residual0")

        /*
        val residual0BitCount = min(53, residual0.bitLen)
        val residual0BitIndex = residual0.bitLen - residual0BitCount
        val residualTopBits = CoeffBits.getDwordAtBitIndex(residual0, residual0BitIndex)
        val residual0Double = Math.scalb(residualTopBits.toDouble(), residual0BitIndex)
         */
        val residual0Double = residual0.c256ToFloorDouble()

        val inv2Guess0 = 0.5 / guess0Double
        val delta0Double = residual0Double * inv2Guess0
        val delta0Raw = delta0Double.toRawBits()
        val delta0Mantissa = ((delta0Raw and ((1L shl 52) - 1)) or (1L shl 52))
        val delta0Exp = ((delta0Raw ushr 52).toInt() and 0x7FF) - 1023
        val delta0Coeff = C256(delta0Mantissa)
        delta0Coeff.c256SetShiftLeft(delta0Coeff, max(delta0Exp - 52, 0))

        val guess1Coeff = C256()
        c256SetAddUnscaled(guess1Coeff, guess0Coeff, delta0Coeff, Pentad())
        if (verbose)
            println(" --> guess1Coeff:$guess1Coeff")

        val guess1Squared = C256()
        c256SetMul(guess1Squared, guess1Coeff, guess1Coeff, tmpPentad)
        while (c256UnscaledCompare(guess1Squared, radicandScaled) > 0) {
            guess1Coeff.c256MutateDecrement()
            c256SetMul(guess1Squared, guess1Coeff, guess1Coeff, tmpPentad)
        }
        val residual1 = C256()
        c256SetSubUnscaled(residual1, radicandScaled, guess1Squared)
        if (verbose)
            println(" --> residual1:$residual1")

        val guess1x2 = C256()
        guess1x2.c256SetShiftLeft(guess1Coeff, 1)
        val delta1 = C256()
        val residue1 = c256SetDiv(delta1, residual1, guess1x2, tmps)

        val guess2 = C256()
        c256SetAddUnscaled(guess2, guess1Coeff, delta1, Pentad())
        if (verbose)
            println(" --> guess2:$guess2")

        val guess2Squared = C256()
        c256SetSqr(guess2Squared, guess2, Pentad())
        val residual2 = C256()
        c256SetSubUnscaled(residual2, radicandScaled, guess2Squared)
        if (verbose)
            println(" --> residual2:$residual2")

        if (verbose)
            println(" --> scaleUp:$scaleUp preferred:$qPreferred")

        sqrt.c256Set(guess2)
        sqrt.qExp = -scaleUp / 2
        sqrt.sign = false

        if (verbose)
            println(" --> sqrt:$sqrt")

        val residue2 = if (residual2.c256IsZero()) Residue.EXACT else Residue.LT_HALF
        var qZ = (radicand.qExp - scaleUp) / 2
        var ntz = numberOfTrailingZeros(sqrt.dw0)
        if (residue2 == Residue.EXACT && qZ < qPreferred && ntz > 0) {
            if (qZ + 1 < qPreferred) {
                val quot = C256()
                do {
                    val deltaQ = qPreferred - qZ
                    val chunk = min(min(9, deltaQ), ntz)
                    val chunkRemainder = barrettDivModPow10(quot, sqrt, chunk)
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
                            c256SetScaleDownPow10(sqrt, sqrt, pow10Count, Pentad())
                            qZ += pow10Count
                        }
                        break
                    } else {
                        sqrt.c256Set(quot)
                        ntz -= chunk
                        qZ += chunk
                    }
                } while (qZ < qPreferred && ntz > 0)
            } else if (c256IsMultipleOf10(sqrt)) {
                c256SetScaleDownPow10(sqrt, sqrt, 1, Pentad())
                ++qZ
            }
        }
        sqrt.type = if (sqrt.c256IsZero()) STEAL_TYP_ZER else STEAL_TYP_FNZ
        sqrt.qExp = qZ
        sqrt.roundAndFinalizeFnz(residue2, DecContext.decimal128Kotlin())

        if (verbose)
            println(" --> sqrt:$sqrt")

        /*
        val bitCount1 = min(53, coeff0Diff.bitLen)
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
        coeff1.coeffSetShiftLeft(coeff1, max(dblSqrt1Exp - 52, 0))
        println(" --> dbl1:$dbl1 dblSqrt1:$dblSqrt1, dblSqrt1Mantissa:$dblSqrt1Mantissa dblSqrt1Exp:$dblSqrt1Exp coeff1:$coeff1")

        val coeff1squared = Coeff()
        coeff1squared.coeffSetMul(coeff1, coeff1)
        val coeff1Diff = Coeff()
        CoeffSub.coeffSubUnscaled(coeff1Diff, coeff0Diff, coeff1squared)
        println(" --> coeff1Diff:$coeff1Diff")

        val bitCount2 = min(53, coeff1Diff.bitLen)
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
        coeff2.coeffSetShiftLeft(coeff2, max(dblSqrt2Exp - 52, 0))
        println(" --> dbl2:$dbl2 dblSqrt2:$dblSqrt2, dblSqrt2Mantissa:$dblSqrt2Mantissa dblSqrt2Exp:$dblSqrt2Exp coeff2:$coeff2")

        val coeff2squared = Coeff()
        coeff2squared.coeffSetMul(coeff2, coeff2)
        val coeff2Diff = Coeff()
        CoeffSub.coeffSubUnscaled(coeff2Diff, coeff1Diff, coeff2squared)
        println(" --> coeff2Diff:$coeff2Diff")

        val bitCount3 = min(53, coeff2Diff.bitLen)
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
        coeff3.coeffSetShiftLeft(coeff3, max(dblSqrt3Exp - 52, 0))
        println(" --> dbl3:$dbl3 dblSqrt3:$dblSqrt3, dblSqrt3Mantissa:$dblSqrt3Mantissa dblSqrt3Exp:$dblSqrt3Exp coeff3:$coeff3")

        val coeff3squared = Coeff()
        coeff3squared.coeffSetMul(coeff3, coeff3)
        val coeff3Diff = Coeff()
        CoeffSub.coeffSubUnscaled(coeff3Diff, coeff2Diff, coeff3squared)
        println(" --> coeff3Diff:$coeff3Diff")


         */

    }

    fun mutDecSqrtPosFnz_38(sqrt: MutDec, radicand: MutDec, ctx: DecContext, reduceToPreferredQExp: Boolean = true): MutDec {
        if (verbose)
            println("---> radicand:$radicand")
        val rSteal = radicand.steal
        val rQExp = stealQExp(rSteal)
        val rDigitLen = stealDigitLen(rSteal)
        verify { radicand.bitLen != 0 }
        val tmps = ctx.tmps
        val coeffRadicandScaled = C256()
        val pentad = ctx.tmps.pentad1
        val scaleUp = 48 - rDigitLen + ((rDigitLen xor rQExp) and 1)
        c256SetScaleUpPow10(coeffRadicandScaled, radicand, scaleUp, pentad)
        if (verbose)
            println("radicand:$radicand radicandScaled:$coeffRadicandScaled")

        val dRadicandScaled = coeffRadicandScaled.c256ToFloorDouble()

        val x0 = sqrt(dRadicandScaled)
        if (verbose)
            println("x0:$x0")
        val ddX0 = DoubleDouble(x0, 0.0)
        val ddRadicandScaled = coeffRadicandScaled.c256ToNewDoubleDouble()
        val ddProd = DoubleDouble.newMulApprox(ddX0, ddX0)
        val ddResidual = DoubleDouble.newSub(ddRadicandScaled, ddProd)
        val ddInv2X0 = DoubleDouble(x0 * 2.0, 0.0)
        ddInv2X0.mutateInvFast()
        val ddDelta = DoubleDouble.newMulApprox(ddResidual, ddInv2X0)
        val ddX1 = DoubleDouble.newAdd(ddX0, ddDelta)
        val coeffX1 = C256().c256Set(ddX1)
        if (verbose)
            println(" ==> coeffX1:$coeffX1")

        val coeffX1Squared = C256()
        c256SetSqr(coeffX1Squared, coeffX1, pentad)
        val coeff2X1 = C256()
        c256SetAddUnscaled(coeff2X1, coeffX1, coeffX1, pentad)
        if (verbose)
            println(" ==> coeffX1Squared:$coeffX1Squared coeffX1 * 2:$coeff2X1")
        val residual = C256()
        val lessThan = c256UnscaledCompare(coeffX1Squared, coeffRadicandScaled) < 0
        if (lessThan)
            c256SetSubUnscaled(residual, coeffRadicandScaled, coeffX1Squared)
        else
            c256SetSubUnscaled(residual, coeffX1Squared, coeffRadicandScaled)
        c256SetScaleUpPow10(residual, residual, 20, pentad)
        if (verbose)
            println(" ==> scaledUpResidual:$residual")
        c256SetDiv(residual, residual, coeff2X1, tmps)
        if (verbose)
            println(" ==> residualAfterDivision:$residual")
        val coeffX1Scaled = C256()
        c256SetScaleUpPow10(coeffX1Scaled, coeffX1, 20, pentad)
        sqrt.setOne()
        if (lessThan)
            c256SetAddUnscaled(sqrt, coeffX1Scaled, residual, pentad)
        else
            c256SetSubUnscaled(sqrt, coeffX1Scaled, residual)
        if (verbose)
            println(" ==> sqrt:$sqrt")
        val sqrtQExp = -((scaleUp shr 1) + 20) + (rQExp shr 1)
        val sqrt = sqrt.finalizeFnz(false, sqrtQExp, ctx)
        if (verbose)
            println(" ==> sqrt:$sqrt")
        if (reduceToPreferredQExp) {
            val mdCrossCheck = MutDec().setSquare(sqrt, ctx)
            val qExpPreferred = rQExp shr 1
            val maxToStrip = qExpPreferred - sqrt.qExp
            if (maxToStrip > 0)
                sqrt.setStripTrailingZeros(sqrt, ctx, maxToStrip)
        }
        return sqrt
    }


}