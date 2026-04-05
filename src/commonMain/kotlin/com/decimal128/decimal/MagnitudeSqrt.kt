package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min
import kotlin.math.nextDown
import kotlin.math.sqrt

private val verbose = false

fun mutDecSqrtPosFnz(sqrt: MutDec, radicand: MutDec, ctx: DecContext, reduceToPreferredQExp: Boolean = true): MutDec =
    mutDecSqrtPosFnz_38(sqrt, radicand, ctx, reduceToPreferredQExp)

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

fun mutDecSqrtPosFnz_1(sqrt: MutDec, radicand: MutDec, ctx: DecContext): MutDec {
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
            ++qZ
        }
    }
    return sqrt.roundAndFinalizeFnz(false, qZ, residue2, ctx)
}

fun mutDecSqrtPosFnz_2(sqrt: MutDec, radicand: MutDec, ctx: DecContext): MutDec {
    val rSteal = radicand.steal
    val rQExp = stealQExp(rSteal)
    val rDigitLen = stealDigitLen(rSteal)
    val qPreferred = rQExp shr 1
    verify { radicand.bitLen != 0 }
    val tmps = ctx.tmps
    val coeffRadicandScaled = tmps.c256
    val pentad = ctx.tmps.pentad1
    val scaleUp = 76 - rDigitLen + ((rDigitLen xor rQExp) and 1)
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
            ++qZ
        }
    }
    return sqrt.roundAndFinalizeFnz(false, qZ, residue2, ctx)
}

fun mutDecSqrtPosFnz_3(sqrt: MutDec, radicand: MutDec, ctx: DecContext): MutDec {
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
    val dRadicandScaled = coeffRadicandScaled.c256ToFloorDouble()

    val dGuess0 = sqrt(dRadicandScaled).nextDown() // ensure that sqrt is < actual
    val ddGuess0 = DoubleDouble(dGuess0, 0.0)
    val ddRadicandScaled = coeffRadicandScaled.c256ToNewDoubleDouble()
    val ddProd = DoubleDouble.newMulApprox(ddGuess0, ddGuess0)
    val ddResidual = DoubleDouble.newSub(ddRadicandScaled, ddProd)
    val ddT = DoubleDouble.newAdd(ddGuess0, ddGuess0)
    ddT.mutateInvFast()
    ddT.setMulApprox(ddResidual, ddT)
    val ddGuess1 = DoubleDouble.newAdd(ddGuess0, ddT)


    val coeffGuess1 = C256()
    coeffGuess1.c256Set(ddGuess1)
    if (verbose)
        println(" ==> coeffGuess1:$coeffGuess1")


    TODO()
}