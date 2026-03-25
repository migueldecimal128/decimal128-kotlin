// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min

internal fun mutDecAddImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    verify { x.digitLen <= 76 } // x is allowed more digits because of FMA
    verify { y.digitLen <= 38 }
    val xSteal = x.steal
    val ySteal = y.steal
    val binopSignature = binopSignatureOf(xSteal, ySteal)
    if (binopSignature == FNZ_FNZ) {
        if (stealQExp(xSteal) == stealQExp(ySteal))
            unscaledAddFnzFnz(z, x, ySign, y, ctx)
        else
            scaledAddFnzFnz(z, x, ySign, y, ctx)
    }else {
        val xSign = stealSignFlag(xSteal)
        when (binopSignature) {
            ZER_ZER -> addZerZer(z, x, ySign, y, ctx)
            INF_ZER,
            INF_FNZ -> z.setInfinite(xSign)
            ZER_INF,
            FNZ_INF -> z.setInfinite(ySign)
            INF_INF -> {
                if (xSign != ySign) {
                    return ctx.setNanSignalInvalid(z, InvalidOperationReason.MAGNITUDE_SUBTRACTION_OF_INFINITIES)
                }
                z.setInfinite(ySign)
            }
            FNZ_ZER -> return setScaleToMinQexp(z, xSign, x, stealQExp(ySteal), ctx)
            ZER_FNZ -> return setScaleToMinQexp(z, ySign, y, stealQExp(xSteal), ctx)
            else -> z.setNaNOperand(x, y, ctx)
        }
    }
    return z
}

private inline fun unscaledAddFnzFnz(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    val xQ = stealQExp(xSteal)
    verify { xQ == stealQExp(ySteal) }
    val xSign = stealSignFlag(xSteal)
    var zSign: Boolean
    // IEEE754-2019 6.3 The sign bit
    // When the sum of two operands with opposite signs
    // (or the difference of two operands with like signs) is
    // exactly zero, the sign of that sum (or difference)
    // shall be +0 under all rounding-direction attributes except
    // roundTowardNegative; under that attribute, the sign of an
    // exact zero sum (or difference) shall be −0.
    val isRoundTowardNegative = ctx.isRoundTowardNegative()
    val pentad = ctx.tmps.pentad1
    if (xSign == ySign) {
        c256SetAddUnscaled(z, x, y, pentad)
        zSign = xSign
    } else {
        val cmp = c256UnscaledCompare(x, y)
        when {
            (cmp > 0) -> {
                c256SetSubUnscaled(z, x, y)
                zSign = if (z.bitLen > 0) xSign else isRoundTowardNegative
            }

            (cmp < 0) -> {
                c256SetSubUnscaled(z, y, x)
                zSign = if (z.bitLen > 0) ySign else isRoundTowardNegative
            }

            else -> {
                return z.setZero(isRoundTowardNegative, xQ)
            }
        }
    }
    return z.finalizeFnz(zSign, xQ, ctx)
}

private inline fun scaledAddFnzFnz(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    verify { x.isFiniteNonZero() && y.isFiniteNonZero()}
    val qX = x.qExp
    val qY = y.qExp
    verify { qX != qY }
    val xSign = x.sign
    val residue: Residue
    if (xSign == ySign) {
        residue = MagnitudeAddSub.magScaledAdd(z, xSign, x, y, ctx)
    } else {
        val cmp = mutDecCompareNumericMagnitude(x, y)
        when {
            cmp > 0 ->
                residue = MagnitudeAddSub.magScaledSub(z, xSign, x, y, ctx)
            cmp < 0 ->
                residue = MagnitudeAddSub.magScaledSub(z, ySign, y, x, ctx)

            else -> {
                // Magnitudes are equal and signs opposite → exact cancellation
                // IEEE 754: sign is +0 except when rounding toward negative
                z.setZero(ctx.isRoundTowardNegative(), min(qX, qY))
                return z // I don't think I need to finalize in this case
            }
        }
    }
    return z.roundAndFinalizeFnz(residue, ctx)
}

private inline fun addZerZer(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    // IEEE 754: Handle sign of -0 + -0 = -0
    val sign = if (x.sign == ySign) {
        ySign  // Both same sign → use that sign
    } else {
        ctx.isRoundTowardNegative()  // Different signs → +0 except roundTowardNegative
    }
    return z.setZero(sign, min(x.qExp, y.qExp))
}

internal fun setScaleToMinQexp(z: MutDec, xSign: Boolean, x: MutDec, otherExp: Int, ctx: DecContext): MutDec {
    var zQ = x.qExp
    val delta = zQ - otherExp
    val headroom = max(0, ctx.precision - x.digitLen)
    if (delta <= 0 || headroom == 0) {
        z.c256Set(x)
    } else {
        val shiftLeft = min(headroom, delta)
        c256SetScaleUpPow10(z, x, shiftLeft, ctx.tmps.pentad1)
        zQ -= shiftLeft
    }
    return z.finalizeFnz(xSign, zQ, ctx)
}
