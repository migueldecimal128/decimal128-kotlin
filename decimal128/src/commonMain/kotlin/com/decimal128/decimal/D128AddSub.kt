// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.decimalFinite
import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.Residue.Companion.LT_HALF
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
        ZER_ZER -> addZerZer(xSteal, x, ySteal, y, ctx)
        ZER_FNZ -> scaleToMinQExp(ySteal, y, stealQExp(xSteal), ctx)
        ZER_INF, FNZ_INF -> Decimal.infinity(stealSignFlag(ySteal))

        FNZ_ZER -> scaleToMinQExp(xSteal, x, stealQExp(ySteal), ctx)
        //FNZ_INF -> y

        INF_ZER, INF_FNZ -> x
        //INF_FNZ -> x
        INF_INF -> addInfInf(xSteal, x, ySteal, ctx)

        else -> nanOperandFound(x, y, ctx)
    }
}

private fun addZerZer(xSteal: Int, x: Decimal, ySteal: Int, y: Decimal, ctx: DecContext): Decimal {
    // Both operands are zero. This is where the special rules apply.
    // IEEE754-2019 6.3 The sign bit
    // However, under all rounding-direction attributes,
    // when x is zero, x + x and x − (−x) have the sign of x.
    val xSign = stealSignFlag(xSteal)
    val ySign = stealSignFlag(ySteal)
    val xQ = stealQExp(xSteal)
    val yQ = stealQExp(ySteal)
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
        if (y.signFlag == isRoundTowardNegative)
            return y
        qMin = yQ
    }
    return Decimal.zero(isRoundTowardNegative, qMin)
}

private fun addInfInf(xSteal: Int, x: Decimal, ySteal: Int, ctx: DecContext): Decimal =
    if (xSteal == ySteal)
        x
    else
        ctx.signalInvalidOperation(InvalidOperationReason.MAGNITUDE_SUBTRACTION_OF_INFINITIES)

private inline fun addFnzFnz(xSteal: Int, x: Decimal, ySteal: Int, y: Decimal, ctx: DecContext) =
    if (stealQExp(xSteal) == stealQExp(ySteal))
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
        else -> Decimal.zero(ctx.isRoundTowardNegative(), x.qExp)
    }
}

private inline fun addUnscaledMagnitudes(sign: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    verify { max(x.bitLen, y.bitLen) + 1 <= 128 }
    val x0 = x.dw0
    val y0 = y.dw0
    val s0 = x0 + y0
    val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
    val x1 = x.dw1
    val y1 = y.dw1
    val s1 = x1 + y1 + carry0
    return decFinalizeFinite(sign, s1, s0, x.qExp, ctx)
}

private inline fun addFnzFnzScaled(xSteal: Int, x: Decimal, ySteal: Int, y: Decimal, ctx: DecContext): Decimal {
    val xSign = stealSignFlag(xSteal)
    val ySign = stealSignFlag(ySteal)
    if (xSign == ySign)
        return addFnzScaledMagnitudes(ySign, x, y, ctx)
    // signs differ ... subtract scaled magnitudes
    val cmpMag = x.compareNumericMagnitudeTo(y)
    return when {
        cmpMag > 0 -> subFnzScaledMagnitude(xSign, x, y, ctx)
        cmpMag < 0 -> subFnzScaledMagnitude(ySign, y, x, ctx)
        else -> Decimal.zero(false, min(stealQExp(xSteal), stealQExp(ySteal)))
    }
}

private fun addFnzScaledMagnitudes(resultSign: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val flip = x.qExp > y.qExp
    val m = if (flip) x else y
    val n = if (flip) y else x
    val mSteal = m.steal
    val nSteal = n.steal
    val mQ = stealQExp(mSteal)
    val nQ = stealQExp(nSteal)
    val qDelta = mQ - nQ
    verify { qDelta >= 0 }
    val mDigitLen = stealDigitLen(mSteal)
    val headroom = ctx.precision - mDigitLen
    val pentad = ctx.tmps.pentad
    val n1 = n.dw1; val n0 = n.dw0
    var dw1Sum = m.dw1
    var dw0Sum = m.dw0
    val shiftLeft = min(headroom, qDelta)
    if (shiftLeft > 0) {
        umul128xPow10to128(pentad, dw1Sum, dw0Sum, shiftLeft)
        dw1Sum = pentad.dw1
        dw0Sum = pentad.dw0
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
            residue = EXACT
        }
        shiftRight >= nDigitLen -> { // fully swamped
            if (shiftRight > nDigitLen)
                residue = LT_HALF
            else
                residue = Residue.fromValuePow10(n1, n0, nDigitLen)
        }
        else -> {
            val resultPentad = ctx.tmps.pentad
            residue = c128ScaleDownPow10(resultPentad, n1, n0, shiftRight)
            dw0Sum += resultPentad.dw0
            dw1Sum += resultPentad.dw1 + if (unsignedLT(dw0Sum, resultPentad.dw0)) 1L else 0L
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
 * 1. m.qExp < s.qExp ... scale s.coefficient up by qDelta
 * 2. sufficient headroom in 38 digits to completely cover the gap
 * 3. catastrophic cancellation is impossible because magnitudes
 *    differ enough.
 *    3a. Fully swamped ... no digits overlap
 *        3a1. there is a 0 to the right of the decimal point
 *        3a2. s is exactly to the right of the decimal point
 *    3b. partially swamped
 *
 * Otherwise, use wide 256-bit ALU
 */
private fun subFnzScaledMagnitude(sign: Boolean, m: Decimal, s: Decimal, ctx: DecContext): Decimal {
    // for all cases, |m| > |s| ... or we wouldn't be here
    verify { m.compareNumericMagnitudeTo(s) > 0 }
    verify { s.isNotZero() }
    verify { m.qExp != s.qExp }


    val mSteal = m.steal
    val sSteal = s.steal
    val qM = stealQExp(mSteal)
    val qS = stealQExp(sSteal)
    val precision = ctx.precision
    verify { precision <= 38 }

    if (qM < qS) {
        // case 1: |m| > |s|, m.qExp < s.qExp
        // simple case: scale s up and subtract, always exact
        val qDelta = qS - qM
        verify { qDelta < precision }
        return d128FusedSubtractMulPow10(sign, m, s, qDelta)
    }
    // case 2: |m| > |s|, m.qExp > s.qExp

    val mDigitLen = stealDigitLen(mSteal)
    val headroom38 = 38 - mDigitLen
    val gap38 = qM - qS
    if (headroom38 >= gap38) {
        // case 2 success
        // m has enough headroom to scale and align with s.qExp
        return d128FusedMulPow10Subtract(sign, m, gap38, s, ctx)
    }

    // case 3: Catastrophic Cancellation Impossible
    if (precision == 34 && isCatastrophicCancellationImpossible(mSteal, sSteal)) {
        verify { mDigitLen <= 34 }

        val sDigitLen = stealDigitLen(sSteal)
        val headroomPrime = 1 + precision - mDigitLen
        val pentad = ctx.tmps.pentad
        umul128xPow10to128(pentad, m.dw1, m.dw0, headroomPrime)
        val dw1MinuendPrime = pentad.dw1
        val dw0MinuendPrime = pentad.dw0
        val qAlign = qM - headroomPrime
        val shiftSRight = qAlign - qS
        val residue: Residue
        var dw1Diff: Long
        var dw0Diff: Long
        if (shiftSRight >= sDigitLen) {
            // case 3a: fully swamped
            residue =
                if (shiftSRight > sDigitLen) Residue.GT_HALF // case 3a1
                else Residue.fromValuePow10(s.dw1, s.dw0, shiftSRight).subtractionInverse() // case 3a2
            verify { residue != EXACT }
            dw1Diff = dw1MinuendPrime - if (dw0MinuendPrime == 0L) 1L else 0L
            dw0Diff = dw0MinuendPrime - 1
            //return decRoundAndFinalizeFinite(sign, dw1Diff, dw0Diff, residueInverse, qAlign, ctx)
        } else {
            // case 3b: partially swamped
            // residue here could be EXACT if the swamped digits were all zeros
            residue = c128ScaleDownPow10(pentad, s.dw1, s.dw0, shiftSRight).subtractionInverse()
            dw0Diff = dw0MinuendPrime - pentad.dw0
            dw1Diff = dw1MinuendPrime - pentad.dw1 - if (unsignedLT(dw0MinuendPrime, dw0Diff)) 1L else 0L
            if (residue != EXACT) {
                dw0Diff -= 1L
                if (dw0Diff == 0L)
                    dw1Diff -= 1L
            }
        }
        return decRoundAndFinalizeFinite(sign, dw1Diff, dw0Diff, residue, qAlign, ctx)
    }

    // fall back to wide 256-bit ALU for more precision
    // when catastrophic cancellation is possible.
    return fullWidthSub(sign, m, s, ctx)
}

private fun fullWidthSub(sign: Boolean, m: Decimal, s: Decimal, ctx: DecContext): Decimal {
    val decTmps = ctx.tmps
    val arg1 = decTmps.mdecBridge1.set(m)
    val arg2 = decTmps.mdecBridge2.set(s)
    val mdecDiff = decTmps.mdecBridgeResult
    val residue = mutDecMagScaledSub(mdecDiff, sign, arg1, arg2, ctx)
    mdecDiff.roundAndFinalizeFnz(sign, mdecDiff.qExp, residue, ctx)
    val diff = Decimal.from(mdecDiff)
    return diff
}

private fun unscaledSub(sign: Boolean, x: Decimal, y: Decimal): Decimal {
    verify { x.validate() }
    verify { y.validate() }
    verify { x.bitLen >= y.bitLen }

    val x0 = x.dw0

    val d0 = x0 - y.dw0
    val carry0 = if (unsignedCmp(d0, x0) > 0) 1L else 0L
    val d1 = x.dw1 - y.dw1 - carry0
    val diff = decimalFinite(sign, x.qExp, d1, d0)
    return diff
}
