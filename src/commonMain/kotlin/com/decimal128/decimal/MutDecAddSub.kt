package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min

internal fun mutDecAddImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    verify { x.digitLen <= 76 } // x is allowed more digits because of FMA
    verify { y.digitLen <= 38 }
    val binopSignature = binopSignatureOf(x.type, y.type)
    if (binopSignature == FNZ_FNZ) {
        addFnzFnz(z, x, ySign, y, ctx)
    }else {
        val xSign = x.sign
        when (binopSignature) {
            ZER_ZER -> addZerZer(z, x, ySign, y, ctx)
            INF_ZER,
            INF_FNZ -> z.setInfinite(xSign)
            ZER_INF,
            FNZ_INF -> z.setInfinite(ySign)
            INF_INF -> {
                if (x.sign != ySign) {
                    z.setNaN(ctx)
                    return ctx.signalInvalid(z)
                }
                z.setInfinite(ySign)
            }
            FNZ_ZER -> return setScaleToMinQexp(z, x.sign, x, y.qExp, ctx)
            ZER_FNZ -> return setScaleToMinQexp(z, ySign, y, x.qExp, ctx)
            else -> z.setNaNOperand(x, y, ctx)
        }
    }
    return z
}

private fun addFnzFnz(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    return if (x.qExp == y.qExp)
        unscaledAddFnzFnz(z, x, ySign, y, ctx)
    else
        scaledAddFnzFnz(z, x, ySign, y, ctx)
}

private fun unscaledAddFnzFnz(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    verify { x.bitLen > 0 && y.bitLen > 0 }  // Optional: could remove in production
    verify { x.qExp == y.qExp }
    val xSign = x.sign
    z.type = STEAL_TYPE_FNZ
    z.qExp = x.qExp
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
        z.sign = xSign
    } else {
        val cmp = c256UnscaledCompare(x, y)
        when {
            (cmp > 0) -> {
                c256SetSubUnscaled(z, x, y)
                z.sign = if (z.bitLen > 0) xSign else isRoundTowardNegative
            }

            (cmp < 0) -> {
                c256SetSubUnscaled(z, y, x)
                z.sign = if (z.bitLen > 0) ySign else isRoundTowardNegative
            }

            else -> {
                z.c256SetZero()
                z.type = STEAL_TYPE_ZER
                z.sign = isRoundTowardNegative
            }
        }
    }
    return z.finalize(ctx)
}

private fun scaledAddFnzFnz(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    verify { x.isFiniteNonZero() && y.isFiniteNonZero()}
    val qX = x.qExp
    val qY = y.qExp
    verify { qX != qY }
    val xSign = x.sign
    val pentad = ctx.tmps.pentad1
    val residue: Residue
    if (xSign == ySign) {
        residue = MagnitudeAddSub.magScaledAdd(z, xSign, x, y, ctx)
    } else {
        val cmp = x.compareNumericMagnitudeTo(y, pentad)
        when {
            cmp > 0 ->
                residue = MagnitudeAddSub.magScaledSub(z, xSign, x, y, ctx)
            cmp < 0 ->
                residue = MagnitudeAddSub.magScaledSub(z, ySign, y, x, ctx)

            else -> {
                // Magnitudes are equal and signs opposite → exact cancellation
                // IEEE 754: sign is +0 except when rounding toward negative
                z.setZero(ctx.isRoundTowardNegative(), min(qX, qY), ctx)
                return z // I don't think I need to finalize in this case
            }
        }
    }
    return z.roundAndFinalize(residue, ctx)
}

private fun addZerZer(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    // IEEE 754: Handle sign of -0 + -0 = -0
    val sign = if (x.sign == ySign) {
        ySign  // Both same sign → use that sign
    } else {
        ctx.isRoundTowardNegative()  // Different signs → +0 except roundTowardNegative
    }
    return z.setZero(sign, min(x.qExp, y.qExp), ctx)
}

internal fun setScaleToMinQexp(z: MutDec, xSign: Boolean, x: MutDec, otherExp: Int, ctx: DecContext): MutDec {
    val xQ = x.qExp
    val delta = xQ - otherExp
    val headroom = max(0, ctx.precision - x.digitLen)
    if (delta <= 0 || headroom == 0) {
        z.set(x)
    } else {
        val shiftLeft = min(headroom, delta)
        c256SetScaleUpPow10(z, x, shiftLeft, ctx.tmps.pentad1)
        z.type = STEAL_TYPE_FNZ
        z.qExp = xQ - shiftLeft
    }
    z.sign = xSign
    return z.finalize(ctx)
}
