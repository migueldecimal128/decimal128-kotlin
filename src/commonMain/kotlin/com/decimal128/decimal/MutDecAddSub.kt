package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min

internal fun mutDecAddImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    verify { x.digitLen <= 76 } // x is allowed more digits because of FMA
    verify { y.digitLen <= 38 }
    val qMax = max(x.qExp, y.qExp)
    when {
        qMax < NON_FINITE_INF -> {
            val qMin = min(x.qExp, y.qExp)
            when {
                x.bitLen == 0 && y.bitLen == 0 -> {
                    // IEEE 754: Handle sign of -0 + -0 = -0
                    val sign = if (x.sign == ySign) {
                        ySign  // Both same sign → use that sign
                    } else {
                        ctx.isRoundTowardNegative()  // Different signs → +0 except roundTowardNegative
                    }
                    z.setZero(sign, qMin, ctx)
                }

                y.bitLen == 0 && x.qExp == qMin -> {
                    z.set(x)
                    z.finalize(ctx)
                }

                y.bitLen == 0 -> {
                    val gap = x.qExp - y.qExp
                    val headroom = ctx.precision - x.digitLen
                    // FMA adding 0 will lead to -headroom
                    val shiftLeft = max(0, min(headroom, gap))
                    z.type = STEAL_TYPE_FNZ
                    z.qExp = x.qExp - shiftLeft
                    z.sign = x.sign
                    c256SetScaleUpPow10(z, x, shiftLeft, ctx.tmps.pentad1)
                    // we could be here because of FMA, so need to finalize
                    z.finalize(ctx)
                }

                x.bitLen == 0 && y.qExp == qMin -> {
                    z.set(y)
                    z.sign = ySign
                }

                x.bitLen == 0 -> {
                    val gap = y.qExp - x.qExp
                    val headroom = ctx.precision - y.digitLen
                    val shiftLeft = min(headroom, gap)
                    z.qExp = y.qExp - shiftLeft
                    z.sign = ySign
                    c256SetScaleUpPow10(z, y, shiftLeft, ctx.tmps.pentad1)
                }

                else -> addFnzImpl(z, x, ySign, y, ctx)
//                        x.qExp == y.qExp -> unscaledAddFnzImpl(z, x, ySign, y, ctx)
//                        else -> scaledAddFnzImpl(z, x, ySign, y, ctx)
            }
        }

        qMax == NON_FINITE_INF -> infiniteAddImpl(z, x, ySign, y, ctx)
        else -> z.setNaNOperand(x, y, ctx)
    }
    return z
}

private fun addFnzImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    return if (x.qExp == y.qExp)
        unscaledAddFnzImpl(z, x, ySign, y, ctx)
    else
        scaledAddFnzImpl(z, x, ySign, y, ctx)
}

private fun unscaledAddFnzImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
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
                z.sign = isRoundTowardNegative
            }
        }
    }
    return z.finalize(ctx)
}

private fun scaledAddFnzImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    val qX = x.qExp
    val qY = y.qExp
    verify { qX != qY }
    val xSign = x.sign
    val qMax = max(qX, qY)
    verify { qMax < MIN_SPECIAL_VALUE }
    verify { x.bitLen > 0 && y.bitLen > 0 }
    val pentad = ctx.tmps.pentad1
    val residue: Residue
    if (xSign == ySign) {
        residue = MagnitudeAddSub.magScaledAdd(z, xSign, x, y, ctx)
    } else {
        val cmp = x.magnitudeCompareTo(y, pentad)
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

private fun infiniteAddImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    val qX = x.qExp
    val qY = y.qExp
    verify { qX == NON_FINITE_INF || qY == NON_FINITE_INF }
    if (qX == qY && x.sign != ySign) {
        z.setNaN(ctx)
        return ctx.signalInvalid(z)
    } else {
        z.setInfinite(if (qX == NON_FINITE_INF) x.sign else ySign)
        return z
    }
}
