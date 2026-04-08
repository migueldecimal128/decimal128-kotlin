package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min
import kotlin.math.nextDown
import kotlin.math.sqrt

/**
 * Computes the square root of a positive, finite, non-zero decimal value.
 *
 * Uses a three-stage Newton-Raphson approach:
 * 1. Double-precision seed via [kotlin.math.sqrt], giving ~15 significant digits.
 * 2. One DoubleDouble NR refinement step, giving ~25-30 significant digits.
 * 3. One 256-bit integer NR refinement step (with 20-digit scale-up for division),
 *    giving ~43 significant digits before rounding.
 *
 * The radicand is scaled up by [scaleUp] powers of 10 before computation so that
 * the coefficient is a large integer suitable for integer arithmetic. The final
 * result exponent is adjusted accordingly.
 *
 * @param sqrt the output [MutDec] to store the result in
 * @param radicand the input value; must be positive, finite, and non-zero
 * @param ctx the [DecContext] controlling precision and rounding
 * @param reduceToPreferredQExp if true, strips trailing zeros to reduce to the
 *   preferred exponent derived from the radicand's exponent
 * @return [sqrt] containing the square root of [radicand], rounded to [ctx] precision
 */
fun mutDecSqrtPosFnz(sqrt: MutDec, radicand: MutDec, ctx: DecContext, reduceToPreferredQExp: Boolean = true): MutDec {
    val rSteal = radicand.steal
    val rQExp = stealQExp(rSteal)
    val rDigitLen = stealDigitLen(rSteal)
    verify { radicand.bitLen != 0 }
    val tmps = ctx.tmps
    // FIXME ... this code contains a number of allocations of C256() tmps
    val coeffRadicandScaled = C256()
    val pentad = tmps.pentad
    val scaleUp = 48 - rDigitLen + ((rDigitLen xor rQExp) and 1)
    c256SetScaleUpPow10(coeffRadicandScaled, radicand, scaleUp, pentad)

    val dRadicandScaled = coeffRadicandScaled.c256ToFloorDouble()

    val x0 = sqrt(dRadicandScaled)
    val ddX0 = DoubleDouble(x0, 0.0)
    val ddRadicandScaled = coeffRadicandScaled.c256ToNewDoubleDouble()
    val ddProd = DoubleDouble.newMulApprox(ddX0, ddX0)
    val ddResidual = DoubleDouble.newSub(ddRadicandScaled, ddProd)
    val ddInv2X0 = DoubleDouble(x0 * 2.0, 0.0)
    ddInv2X0.mutateInvFast()
    val ddDelta = DoubleDouble.newMulApprox(ddResidual, ddInv2X0)
    val ddX1 = DoubleDouble.newAdd(ddX0, ddDelta)
    val coeffX1 = C256().c256Set(ddX1)

    val coeffX1Squared = C256()
    c256SetSqr(coeffX1Squared, coeffX1, pentad)
    val coeff2X1 = C256()
    c256SetAddUnscaled(coeff2X1, coeffX1, coeffX1, pentad)
    val residual = C256()
    val lessThan = c256UnscaledCompare(coeffX1Squared, coeffRadicandScaled) < 0
    if (lessThan)
        c256SetSubUnscaled(residual, coeffRadicandScaled, coeffX1Squared)
    else
        c256SetSubUnscaled(residual, coeffX1Squared, coeffRadicandScaled)
    c256SetScaleUpPow10(residual, residual, 20, pentad)
    c256SetDiv(residual, residual, coeff2X1, tmps)
    val coeffX1Scaled = C256()
    c256SetScaleUpPow10(coeffX1Scaled, coeffX1, 20, pentad)
    sqrt.setOne()
    if (lessThan)
        c256SetAddUnscaled(sqrt, coeffX1Scaled, residual, pentad)
    else
        c256SetSubUnscaled(sqrt, coeffX1Scaled, residual)
    val sqrtQExp = -((scaleUp shr 1) + 20) + (rQExp shr 1)
    val sqrt = sqrt.finalizeFnz(false, sqrtQExp, ctx)
    if (reduceToPreferredQExp) {
        val qExpPreferred = rQExp shr 1
        val maxToStrip = qExpPreferred - sqrt.qExp
        if (maxToStrip > 0)
            sqrt.setStripTrailingZeros(sqrt, ctx, maxToStrip)
    }
    return sqrt
}
