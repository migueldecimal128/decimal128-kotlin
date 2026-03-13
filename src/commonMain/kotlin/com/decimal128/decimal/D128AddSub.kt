// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

internal fun d128AddImpl(x: Decimal, y: Decimal): Decimal =
    d128AddSubImpl(x, y.steal, y, DecContext.current())

internal fun d128SubImpl(x: Decimal, y: Decimal): Decimal =
    d128AddSubImpl(x, y.steal xor Int.MIN_VALUE, y, DecContext.current())

internal fun d128AddImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    d128AddSubImpl(x, y.steal, y, ctx)

internal fun d128SubImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    d128AddSubImpl(x, y.steal xor Int.MIN_VALUE, y, ctx)

internal fun d128AddSubImpl(x: Decimal, ySteal: Int, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal
    val signature = binopSignatureOf(xSteal, ySteal)
    return if (signature == FNZ_FNZ) {
        addFnzFnz(xSteal, x, ySteal, y, ctx)
    } else when (signature) {
        ZER_ZER -> addZeroZero(xSteal, x, ySteal, y, ctx)
        ZER_FNZ -> scaleToMinExp(ySteal, y, stealQexp(xSteal), ctx)
        ZER_INF, FNZ_INF -> Decimal.infinity(stealSignFlag(ySteal))

        FNZ_ZER -> scaleToMinExp(xSteal, x, stealQexp(ySteal), ctx)
        //FNZ_INF -> y

        INF_ZER, INF_FNZ -> x
        //INF_FNZ -> x
        INF_INF -> addInfInf(xSteal, x, ySteal, y, ctx)

        else -> nanOperandFound(x, y, ctx)
    }
}

private fun addZeroZero(xSteal: Int, x: Decimal, ySteal: Int, y: Decimal, ctx: DecContext): Decimal {
    // Both operands are zero. This is where the special rules apply.
    // IEEE754-2019 6.3 The sign bit
    // However, under all rounding-direction attributes,
    // when x is zero, x + x and x − (−x) have the sign of x.
    val xSign = stealSignFlag(xSteal)
    val ySign = stealSignFlag(ySteal)
    val xQ = stealQexp(xSteal)
    val yQ = stealQexp(ySteal)
    if (xSign == ySign) {
        // Rule: x + x = x. Preserves the sign of zero. (-0) + (-0) = -0.
        return if (xQ <= yQ) x else y // return min qExp
    }
    // Rule: (+0) + (-0). The signs are different.
    // Result is +0 unless rounding is roundTowardNegative.
    val isRoundTowardNegative = ctx.isRoundTowardNegative()
    var qMin = xQ
    if (xQ <= yQ) {
        if (xSign == isRoundTowardNegative)
            return x
    } else {
        // yes, this should be y.sign, not ySign
        if (y.sign == isRoundTowardNegative)
            return y
        qMin = yQ
    }
    return Decimal.zero(isRoundTowardNegative, qMin, ctx)
}

private fun addInfInf(xSteal: Int, x: Decimal, ySteal: Int, y: Decimal, ctx: DecContext): Decimal =
    if (xSteal == ySteal)
        x
    else
        ctx.signalInvalid(Decimal.NaN)

private inline fun addFnzFnz(xSteal: Int, x: Decimal, ySteal: Int, y: Decimal, ctx: DecContext) =
    if (stealQexp(xSteal) == stealQexp(ySteal))
        addFnzFnzUnscaled(xSteal, x, ySteal, y, ctx)
    else
        addFnzFnzScaled(xSteal, x, ySteal, y, ctx)

private inline fun addFnzFnzUnscaled(xSteal: Int, x: Decimal, ySteal: Int, y: Decimal, ctx: DecContext): Decimal {
    val xSign = stealSignFlag(xSteal)
    val ySign = stealSignFlag(ySteal)
    if (xSign == ySign)
        return addUnscaledMagnitudes(ySign, x, y, ctx)
    val cmp = c128UnscaledCompare(x, y)
    return when {
        (cmp > 0) -> unscaledSub(xSign, x, y)
        (cmp < 0) -> unscaledSub(ySign, y, x)
        else -> Decimal.zero(ctx.isRoundTowardNegative(), x.qExp(), ctx)
    }
}

private inline fun addUnscaledMagnitudes(sign: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    verify { max(x.bitLen(), y.bitLen()) + 1 <= 128 }
    val x0 = x.dw0
    val y0 = y.dw0
    val s0 = x0 + y0
    val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
    val x1 = x.dw1
    val y1 = y.dw1
    val s1 = x1 + y1 + carry0
    return decFinalizeFinite(sign, s1, s0, x.qExp(), ctx)
}

private inline fun addFnzFnzScaled(xSteal: Int, x: Decimal, ySteal: Int, y: Decimal, ctx: DecContext): Decimal {
    val xSign = stealSignFlag(xSteal)
    val ySign = stealSignFlag(ySteal)
    if (xSign == ySign)
        return addFnzScaledMagnitudes(ySign, x, y, ctx)
    // signs differ ... subtract scaled magnitudes
    val cmpMag = x.magnitudeCompareTo(y)
    return when {
        cmpMag > 0 -> subFnzScaledMagnitude(xSign, x, y, ctx)
        cmpMag < 0 -> subFnzScaledMagnitude(ySign, y, x, ctx)
        else -> Decimal.zero(false, min(stealQexp(xSteal), stealQexp(ySteal)), ctx)
    }
}

private fun addFnzScaledMagnitudes(resultSign: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val flip = x.qExp() > y.qExp()
    val m = if (flip) x else y
    val n = if (flip) y else x
    val mSteal = m.steal
    val nSteal = n.steal
    val mQ = stealQexp(mSteal)
    val nQ = stealQexp(nSteal)
    val qDelta = mQ - nQ
    verify { qDelta >= 0 }
    val mDigitLen = stealDigitLen(mSteal)
    val headroom = ctx.precision - mDigitLen
    val n1 = n.dw1; val n0 = n.dw0
    var dw1Sum = m.dw1
    var dw0Sum = m.dw0
    val shiftLeft = min(headroom, qDelta)
    if (shiftLeft > 0) {
        val (dw1T, dw0T) = umul128xPow10to128(dw1Sum, dw0Sum, shiftLeft)
        dw1Sum = dw1T
        dw0Sum = dw0T
    }
    val qAlign = mQ - shiftLeft
    val shiftRight = qAlign - nQ
    val residue: Residue
    val nDigitLen = stealDigitLen(nSteal)
    when {
        shiftRight == 0 -> {
            verify { shiftLeft > 0 }
            dw0Sum += n0
            dw1Sum += n1 + if (unsignedLT(dw0Sum, n0)) 1L else 0L
            residue = Residue.EXACT
        }
        shiftRight >= nDigitLen -> { // fully swamped
            if (shiftRight > nDigitLen)
                residue = Residue.LT_HALF
            else
                residue = Residue.fromValuePow10(n1, n0, nDigitLen)
        }
        else -> {
            val tmpDwQuad = ctx.tmps.pentad1
            residue = c128ScaleDownPow10(tmpDwQuad, n1, n0, shiftRight)
            dw0Sum += tmpDwQuad.dw0
            dw1Sum += tmpDwQuad.dw1 + if (unsignedLT(dw0Sum, tmpDwQuad.dw0)) 1L else 0L
        }
    }
    return decRoundAndFinalizeFinite(resultSign, dw1Sum, dw0Sum, residue, qAlign, ctx)
}

/**
 * Scaled subtraction of FNZ finite non-zero values.
 *
 * m == minuend
 * s == subtrahend
 *
 * We can handle the following cases in the 128-bit world
 * 1. m.qExp() < s.qExp() ... scale s.coefficient up by qDelta
 * 2. sufficient headroom in 38 digits to completely cover the gap
 * 3. fully swamped
 *
 * Otherwise, use wide 256-bit ALU
 */
private fun subFnzScaledMagnitude(sign: Boolean, m: Decimal, s: Decimal, ctx: DecContext): Decimal {
    verify { m.magnitudeCompareTo(s) > 0 }
    verify { s.isNotZero() }
    verify { m.qExp() != s.qExp() }

    val mSteal = m.steal
    val sSteal = s.steal
    val qM = stealQexp(mSteal)
    val qS = stealQexp(sSteal)
    val precision = ctx.precision

    if (qM < qS) {
        // case 1: |m| > |s|, m.qExp < s.qExp
        // simple case: scale s up and subtract, always exact
        val qDelta = qS - qM
        verify { qDelta < precision }
        return d128FusedSubtractMulPow10(sign, m, s, qDelta)
    }
    // case 2: |m| > |s|, m.qExp > s.qExp

    verify { precision < 38 }
    val mDigitLen = stealDigitLen(mSteal)
    run {
        val headroom = 38 - mDigitLen
        val gap = qM - qS
        if (headroom >= gap) {
            // case 2a
            // m has enough headroom to scale and align with s.qExp
            return d128FusedMulPow10Subtract(sign, m, gap, s, ctx)
        }
    }

    // case 3: fully swamped
    run {
        // in this case, headroom and shift are based upon ctx.precision
        val sDigitLen = stealDigitLen(sSteal)
        val headroomP = 1 + precision - mDigitLen
        if (headroomP < 1) return@run  // no room for guard digit, skip to fullWidthAdd
        val qAlignP = qM - headroomP
        val shiftSRight = qAlignP - qS
        if (shiftSRight >= sDigitLen) {
            val residueInverse =
                if (shiftSRight > sDigitLen) Residue.GT_HALF
                else Residue.fromValuePow10(s.dw1, s.dw0, shiftSRight).subtractionInverse()
            verify { residueInverse != EXACT }
            val (dw1S, dw0S) = umul128xPow10to128(m.dw1, m.dw0, headroomP)
            val dw1T = dw1S - if (dw0S == 0L) 1L else 0L
            val dw0T = dw0S - 1
            return decRoundAndFinalizeFinite(sign, dw1T, dw0T, residueInverse, qAlignP, ctx)
        }
    }
    // fall back to wide 256-bit ALU
    return fullWidthSub(sign, m, s, ctx)
}

private fun fullWidthSub(sign: Boolean, m: Decimal, s: Decimal, ctx: DecContext): Decimal {
    val decTmps = ctx.tmps
    val arg1 = decTmps.mdecBridge1.set(m)
    val arg2 = decTmps.mdecBridge2.set(s)
    val mdecDiff = decTmps.mdecResult
    val residue = MagnitudeAddSub.magScaledSub(mdecDiff, sign, arg1, arg2, ctx)
    mdecDiff.roundAndFinalize(residue, ctx)
    val diff = Decimal.from(mdecDiff)
    return diff
}

private fun unscaledSub(sign: Boolean, x: Decimal, y: Decimal): Decimal {
    verify { x.validate() }
    verify { y.validate() }
    verify { x.bitLen() >= y.bitLen() }

    val x0 = x.dw0

    val d0 = x0 - y.dw0
    val carry0 = if (unsignedCmp(d0, x0) > 0) 1L else 0L
    val d1 = x.dw1 - y.dw1 - carry0
    val diff = Decimal(sign, x.qExp(), d1, d0)
    return diff
}
