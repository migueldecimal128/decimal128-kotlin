// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.Residue.Companion.LT_HALF
import kotlin.math.max
import kotlin.math.min

internal fun mutDecAddImpl(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    verify { x.digitLen <= 77 } // x is allowed more digits because of FMA
    verify { y.digitLen <= 77 }
    val xSteal = x.steal
    val ySteal = y.steal
    val binopSignature = binopSignatureOf(xSteal, ySteal)
    if (binopSignature == FNZ_FNZ) {
        if (stealQExp(xSteal) == stealQExp(ySteal))
            addAlignedFnzFnz(z, x, ySign, y, ctx)
        else
            addUnalignedFnzFnz(z, x, ySign, y, ctx)
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
                    return ctx.setNanSignalInvalidOperation(z, InvalidCause.MAGNITUDE_SUBTRACTION_OF_INFINITIES)
                }
                z.setInfinite(ySign)
            }
            FNZ_ZER -> return setScaleToMinQexp(z, xSign, x, stealQExp(ySteal), ctx)
            ZER_FNZ -> return setScaleToMinQexp(z, ySign, y, stealQExp(xSteal), ctx)
            else -> z.setNanOperandFound(x, y, ctx)
        }
    }
    return z
}

private /*inline*/ fun addAlignedFnzFnz(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
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
    val pentad = ctx.tmps.pentad
    if (xSign == ySign) {
        c256SetAddAligned(z, x, y, pentad)
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

private /*inline*/ fun addUnalignedFnzFnz(z: MutDec, x: MutDec, ySign: Boolean, y: MutDec, ctx: DecContext): MutDec {
    verify { x.isFiniteNonZero() && y.isFiniteNonZero()}
    val xQ = x.qExp
    val yQ = y.qExp
    verify { xQ != yQ }
    val xSign = x.sign
    val residue: Residue
    if (xSign == ySign) {
        residue = mutDecAddMagUnalignedFnzFnz(z, xSign, x, y, ctx)
    } else {
        val cmp = mutDecCmpMagFnzFnz(x, y)
        when {
            cmp > 0 ->
                residue = mutDecSubMagUnalignedFnzFnz(z, xSign, x, y, ctx)
            cmp < 0 ->
                residue = mutDecSubMagUnalignedFnzFnz(z, ySign, y, x, ctx)

            else -> {
                // Magnitudes are equal and signs opposite → exact cancellation
                // IEEE 754: sign is +0 except when rounding toward negative
                z.setZero(ctx.isRoundTowardNegative(), min(xQ, yQ))
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
    // it is not the case the xSign == x.sign because this may be part of a subtraction operation
    var zQ = x.qExp
    val delta = zQ - otherExp
    val headroom = max(0, ctx.precision - x.digitLen)
    if (delta <= 0 || headroom == 0) {
        z.c256Set(x)
    } else {
        val shiftLeft = min(headroom, delta)
        c256SetMulPow10(z, x, shiftLeft, ctx.tmps.pentad)
        zQ -= shiftLeft
    }
    return z.finalizeFnz(xSign, zQ, ctx)
}

private fun mutDecAddMagUnalignedFnzFnz(z: MutDec, sign: Boolean, x: MutDec, y: MutDec, ctx: DecContext): Residue {
    verify { stealQExp(x.steal) != stealQExp(y.steal) } // the unscaled case should have been caught earlier
    verify { x.isFiniteNonZero() && y.isFiniteNonZero()}

    val flipFlop = stealQExp(x.steal) > stealQExp(y.steal)
    val m = if (flipFlop) x else y
    val n = if (flipFlop) y else x
    val mSteal = m.steal
    val nSteal = n.steal
    val mQ = stealQExp(mSteal)
    val nQ = stealQExp(nSteal)

    val mDigitLen = stealDigitLen(mSteal)
    val nDigitLen = stealDigitLen(nSteal)
    val qDelta = mQ - nQ
    verify { qDelta > 0 }
    val pentad = ctx.tmps.pentad
    val headroom = ctx.precision - mDigitLen
    val shiftLeft = min(max(headroom, 0), qDelta)
    val qAlign = mQ - shiftLeft
    val shiftRight = qAlign - nQ
    val residue: Residue
    if (shiftRight == 0) {
        verify { shiftLeft > 0 }
        c256SetFusedMulPow10Add(z, m, shiftLeft, n, pentad)
        residue = EXACT
    } else if (shiftRight >= nDigitLen) {
        // perform in this order to avoid aliasing z === n issue
        if (shiftRight > nDigitLen) {
            residue = LT_HALF
        } else {
            residue = Residue.fromDecade(n)
        }
        c256SetMulPow10(z, m, shiftLeft, pentad)
    } else {
        // shift right required ... shift left maybe
        // shift right first into our destination
        // then do a fused scaling, allowing us to
        // perform this op without allocating of temp variables
        val t: MutDec
        if (m === z) {
            t = MutDec()
        } else {
            t = z
        }
        residue = c256SetScaleDownPow10(t, n, shiftRight, pentad)
        if (shiftLeft > 0)
            c256SetFusedMulPow10Add(z, m, shiftLeft, t)
        else
            c256SetAddAligned(z, m, t, pentad)
    }
    z.steal = stealEncodeFNZ(sign, qAlign, stealPackedLengths(z.steal))
    return residue
}

// uses Guard digit
// decrements when non-exact so that standard round and finalize routine can be called
// m == minuend  s == subtrahend
internal fun mutDecSubMagUnalignedFnzFnz(z: MutDec, mSign: Boolean, m: MutDec, s: MutDec, ctx: DecContext): Residue {
    verify { m.isFiniteNonZero() && s.isFiniteNonZero() }
    verify { m.qExp != s.qExp }
    verify { mutDecCmpMagFnzFnz(m, s) > 0 }
    val tmps = ctx.tmps
    val pentad = tmps.pentad
    val mQ = m.qExp
    val mDigitLen = m.digitLen
    val sQ = s.qExp
    val sDigitLen = s.digitLen
    val gap = mQ - sQ
    if (gap > 0) {
        val headroomWithGuard: Int
        if (sDigitLen > ctx.precision) {
            // It is possible for y.digitLen > precision because
            // of intermediate result of a FMA operation.
            // In this case we might have to scale x.coeff up to
            // x.digitLen == y.digitLen
            // This will not exceed our 256-bit ALU capacity
            headroomWithGuard = sDigitLen - mDigitLen
        } else {
            headroomWithGuard = 1 + ctx.precision - mDigitLen  // Standard with guard
        }
        val shiftMLeft = min(gap, max(0, headroomWithGuard))

        val qAlign = mQ - shiftMLeft
        val shiftSRight = qAlign - sQ
        verify { shiftSRight >= 0 }

        val residue: Residue
        if (shiftSRight == 0) {
            verify { shiftMLeft > 0 }
            c256SetSubScaled(z, m, shiftMLeft, s, pentad) // z = (x * 10^shiftXLeft) - y
            residue = EXACT
        } else if (shiftSRight >= sDigitLen) {
            if (shiftSRight > sDigitLen) {
                residue = Residue.GT_HALF // actually Residue.LT_HALF.subtractionInverse()
            } else {
                residue = Residue.fromDecade(s).subtractionInverse()
            }
            verify { residue != EXACT }
            if (shiftMLeft > 0) {
                c256SetMulPow10(z, m, shiftMLeft, ctx.tmps.pentad)
            } else {
                z.c256Set(m)
            }
            // decrement and let the residue possibly round it back up
            z.c256MutateDecrement()
        } else { // shiftYRight > 0
            // There is overlap and there will be residue.
            // align x by shiftLeftX
            //
            val tmpY = tmps.c256
            val residueT = c256SetScaleDownPow10(tmpY, s, shiftSRight, pentad)
            if (shiftMLeft > 0)
                c256SetSubScaled(z, m, shiftMLeft, tmpY, pentad)
            else
                c256SetSubUnscaled(z, m, tmpY)
            if (residueT != EXACT)
                z.c256MutateDecrement()
            residue = residueT.subtractionInverse()
        }
        z.steal = stealEncodeFNZ(mSign, qAlign, stealPackedLengths(z.steal))
        return residue

    } else {
        // this branch is relatively simple
        // abs(x) > abs(y)
        // x.qExp < y.qExp
        // this can only happen if x's coefficient
        // is larger than y
        // 99999e0 - 1e1
        // just adjust y's coefficient and subtract
        c256FusedSubMulPow10(z, m, s, -gap, pentad)
        z.steal = stealEncodeFNZ(mSign, mQ, stealPackedLengths(z.steal))
        return EXACT
    }
}
