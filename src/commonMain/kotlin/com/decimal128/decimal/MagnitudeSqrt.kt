package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private val verbose = false

fun mutDecSqrtPosFnz(sqrt: MutDec, radicand: MutDec, ctx: DecContext): MutDec {
    val rSteal = radicand.steal
    val rQExp = stealQExp(rSteal)
    val rDigitLen = stealDigitLen(rSteal)
    val qPreferred = rQExp shr 1
    verify { radicand.bitLen != 0 }
    val tmps = ctx.tmps
    val coeffRadicandScaled = tmps.c256
    val pentad = ctx.tmps.pentad1
    val scaleUp = 70 - rDigitLen + (rDigitLen and 1) + (rQExp and 1)
    c256SetScaleUpPow10(coeffRadicandScaled, radicand, scaleUp, pentad)
    if (verbose)
        println("radicand:$radicand radicandScaled:$coeffRadicandScaled")

    val dRadicandScaled = coeffRadicandScaled.c256ToFloorDouble()

    val dGuess0 = sqrt(dRadicandScaled)
    val rawGuess0 = dGuess0.toRawBits()
    var guess0Significand = ((rawGuess0 and ((1L shl 52) - 1)) or (1L shl 52))
    val guess0Exp = ((rawGuess0 ushr 52).toInt() and 0x7FF) - 1023
    // FIXME - what to do about these tmp allocations?
    val coeffGuess0 = C256()
    val coeffGuess0Squared = C256()
    val coeffResidual0 = C256()
    val coeffDelta0 = C256()
    val coeffGuess1 = C256()
    val coeffGuess1Squared = C256()
    var corrections = -1
    while (true) {
        ++corrections
        --guess0Significand
        coeffGuess0.c256Set64(guess0Significand)
        coeffGuess0.c256SetShiftLeft(coeffGuess0, max(guess0Exp - 52, 0))
        if (verbose)
            println(" --> dbl0:$dRadicandScaled doubleGuess0:$dGuess0 coeffGuess0:$coeffGuess0")

        c256SetSqr(coeffGuess0Squared, coeffGuess0, Pentad())
        if (c256UnscaledCompare(coeffRadicandScaled, coeffGuess0Squared) < 0)
            continue

        c256SetSubUnscaled(coeffResidual0, coeffRadicandScaled, coeffGuess0Squared)
        if (verbose)
            println(" --> residual0:$coeffResidual0")

        val dResidual0 = coeffResidual0.c256ToFloorDouble()

        val dRecip2xGuess0 = 0.5 / dGuess0
        val dDelta0 = dResidual0 * dRecip2xGuess0
        val delta0Raw = dDelta0.toRawBits()
        val delta0Significand = ((delta0Raw and ((1L shl 52) - 1)) or (1L shl 52))
        val delta0Exp = ((delta0Raw ushr 52).toInt() and 0x7FF) - 1023
        coeffDelta0.c256Set64(delta0Significand - 1)
        coeffDelta0.c256MutateDecrement()
        coeffDelta0.c256SetShiftLeft(coeffDelta0, max(delta0Exp - 52, 0))

        c256SetAddUnscaled(coeffGuess1, coeffGuess0, coeffDelta0, pentad)
        if (verbose)
            println(" --> guess1Coeff:$coeffGuess1")

        c256SetSqr(coeffGuess1Squared, coeffGuess1, pentad)
        if (verbose)
            println(" ==> coeffRadicandScaled:$coeffRadicandScaled coeffGuess1Squared:$coeffGuess1Squared")
        if (c256UnscaledCompare(coeffRadicandScaled, coeffGuess1Squared) >= 0)
            break
    }
    //if (corrections > 0) {
    //    if (corrections == 1)
    //        ++corrections1
    //    else
    //        ++correctionsGT1
    //    println("corrections:$corrections total:$total corrections1:$corrections1 correctionsGT1:$correctionsGT1")
    //}

    val coeffResidual1 = C256()
    c256SetSubUnscaled(coeffResidual1, coeffRadicandScaled, coeffGuess1Squared)
    if (verbose)
        println(" --> residual1:$coeffResidual1")

    val ddT = coeffGuess1.c256ToNewDoubleDouble()
    ddT.mutate2x()
    ddT.mutateInvFast()
    val ddResidual1 = coeffResidual1.c256ToNewDoubleDouble()
    val ddDelta1 = DoubleDouble.newMulApprox(ddResidual1, ddT)
    val coeffDelta1 = C256()
    coeffDelta1.c256Set(ddDelta1)

    val coeffGuess2 = C256()
    c256SetAddUnscaled(coeffGuess2, coeffGuess1, coeffDelta1, pentad)

    if (verbose)
        println(" ==> coeffGuess2:$coeffGuess2")

    val coeffGuess2Squared = C256()
    c256SetMul(coeffGuess2Squared, coeffGuess2, coeffGuess2, pentad)
    val residual2 = C256()
    c256SetSubUnscaled(residual2, coeffRadicandScaled, coeffGuess2Squared)

    if (verbose)
        println(" ==> residual2:$residual2")

    if (verbose)
        println(" --> scaleUp:$scaleUp preferred:$qPreferred")

    sqrt.c256Set(coeffGuess2)

    if (verbose)
        println(" --> sqrt:$sqrt")

    val residue2 = if (residual2.c256IsZero()) Residue.EXACT else Residue.LT_HALF
    var qZ = (radicand.qExp - scaleUp) / 2
    var ntz = sqrt.dw0.countTrailingZeroBits()
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
                        val q = unsignedMulHi(t, M) ushr 3
                        val r = t - (q * 10)
                        if (r != 0L)
                            break
                        ++pow10Count
                        t = q
                    }
                    if (pow10Count > 0) {
                        c256SetScaleDownPow10(sqrt, sqrt, pow10Count, pentad)
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
            c256SetScaleDownPow10(sqrt, sqrt, 1, pentad)
            ++qZ
        }
    }
    return sqrt.roundAndFinalizeFnz(false, qZ, residue2, ctx)
}
