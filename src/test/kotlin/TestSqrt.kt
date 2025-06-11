package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.MathContext

class TestSqrt{

    class TC(val bd: BigDecimal) {
        constructor(str: String) : this(BigDecimal(str))
        val sqrt = bd.sqrt(MathContext.DECIMAL128)
    }

    val tcs = arrayOf (
        TC("1"),
        TC("4"),
        TC("16"),
        TC("256"),
        TC("400"),
        TC("40000"),
        TC("4000000"),
        TC("2"),
        TC("625"),
        TC("900"),
        TC("10000"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        val dec = Dec34()
        val decSqrt = Dec34()
        dec.set(tc.bd)
        setSqrt(decSqrt, dec)
    }

    fun setSqrt(sqrt: Dec34, radicand: Dec34) {
        val qPreferred = radicand.qExp shr 1
        if (radicand.coeffIsZero()) {
            sqrt.setZero()
            sqrt.qExp = qPreferred
        }
        val radicandScaled = Coeff()
        val scaleUp = 66 - radicand.digitLen + (radicand.digitLen and 1)
        //val scaleUp = 12
        radicandScaled.coeffSetScaleUpPow10(radicand, scaleUp)
        println("radicand:$radicand radicandScaled:$radicandScaled")

        val bitCount0 = Math.min(53, radicandScaled.bitLen)
        val bitIndex0 = radicandScaled.bitLen - bitCount0
        val topBits0 = CoeffBits.getDwordAtBitIndex(radicandScaled, bitIndex0)
        val dbl0 = Math.scalb(topBits0.toDouble(), bitIndex0)
        // g == guess
        val guess0Double = Math.sqrt(dbl0)
        val rawGuess0 = guess0Double.toRawBits()
        val guess0Mantissa = ((rawGuess0 and ((1L shl 52) - 1)) or (1L shl 52)) - 1 // ensure floor
        val guess0Exp = ((rawGuess0 ushr 52).toInt() and 0x7FF) - 1023

        val guess0Coeff = Coeff(guess0Mantissa)
        guess0Coeff.coeffSetShiftLeft(guess0Coeff, Math.max(guess0Exp - 52, 0))
        println(" --> dbl0:$dbl0 doubleGuess0:$guess0Double coeffGuess0:$guess0Coeff")

        val guess0squared = Coeff()
        guess0squared.coeffSetMul(guess0Coeff, guess0Coeff)
        val residual0 = Coeff()
        CoeffSub.coeffSubUnscaled(residual0, radicandScaled, guess0squared)
        println(" --> residual0:$residual0")

        val residual0BitCount = Math.min(53, residual0.bitLen)
        val residual0BitIndex = residual0.bitLen - residual0BitCount
        val residualTopBits = CoeffBits.getDwordAtBitIndex(residual0, residual0BitIndex)
        val residual0Double = Math.scalb(residualTopBits.toDouble(), residual0BitIndex)

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

        val guess1squared = Coeff()
        guess1squared.coeffSetMul(guess1Coeff, guess1Coeff)
        val residual1 = Coeff()
        CoeffSub.coeffSubUnscaled(residual1, radicandScaled, guess1squared)
        println(" --> residual1:$residual1")

        val guess1x2 = Coeff()
        guess1x2.coeffSetShiftLeft(guess1Coeff, 1)
        val delta1 = Coeff()
        val residue = delta1.coeffSetDiv(residual1, guess1x2)

        val guess2 = Coeff()
        guess2.coeffSetAdd(guess1Coeff, delta1)
        println(" --> guess2:$guess2")

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