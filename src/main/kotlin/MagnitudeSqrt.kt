package com.decimal128

import java.lang.Long.numberOfTrailingZeros
import java.lang.Math.unsignedMultiplyHigh
import kotlin.math.max
import kotlin.math.min

object MagnitudeSqrt {
    val verbose = false

    fun magSqrt(sqrt: Decimal, radicand: Decimal): Residue {
        val qPreferred = radicand.qExp shr 1
        if (radicand.coeffIsZero()) {
            sqrt.coeffSetZero()
            sqrt.qExp = qPreferred
            sqrt.sign = radicand.sign
            return Residue.EXACT
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
            coeffGuess0.coeffSetShiftLeft(coeffGuess0, max(guess0Exp - 52, 0))
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
            coeffDelta0.coeffSetShiftLeft(coeffDelta0, max(delta0Exp - 52, 0))

            coeffGuess1.coeffSetAdd(coeffGuess0, coeffDelta0)
            if (verbose)
                println(" --> guess1Coeff:$coeffGuess1")

            coeffGuess1Squared.coeffSetSqr(coeffGuess1)
            if (verbose)
                println(" ==> coeffRadicandScaled:$coeffRadicandScaled coeffGuess1Squared:$coeffGuess1Squared")
            if (coeffRadicandScaled.coeffUnscaledCompareTo(coeffGuess1Squared) >= 0)
                break
        }
        //if (corrections > 0) {
        //    if (corrections == 1)
        //        ++corrections1
        //    else
        //        ++correctionsGT1
        //    println("corrections:$corrections total:$total corrections1:$corrections1 correctionsGT1:$correctionsGT1")
        //}

        val coeffResidual1 = Coeff()
        CoeffSub.coeffSubUnscaled(coeffResidual1, coeffRadicandScaled, coeffGuess1Squared)
        if (verbose)
            println(" --> residual1:$coeffResidual1")

        val ddT = coeffGuess1.coeffToNewDoubleDouble()
        ddT.mutate2x()
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
        sqrt.sign = false

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
                    val chunk = min(min(9, deltaQ), ntz)
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
        return residue2
    }
}