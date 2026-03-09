package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

internal fun d128AddImpl(x: Decimal, y: Decimal): Decimal =
    d128AddImpl(x, y, DecContext.current())

internal fun d128AddImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val ySign = y.sign
    return if (bothFnz(x, y)) {
        addFnzFnz(x, ySign, y, ctx)
    } else when (binopSignatureOf(x, y)) {
        ZER_ZER -> addZeroZero(x, ySign, y, ctx)
        ZER_FNZ -> scaleToMinExp(ySign, y, x.qExp, ctx)
        ZER_INF, FNZ_INF -> y

        FNZ_ZER -> scaleToMinExp(x.sign, x, y.qExp, ctx)
        FNZ_FNZ -> throw IllegalStateException()
        //FNZ_INF -> y

        INF_ZER, INF_FNZ -> x
        //INF_FNZ -> x
        INF_INF -> addInfInf(x, ySign, y, ctx)

        NAN_FOUND -> nanOperandFound(x, y, ctx)
    }
}

internal fun d128SubImpl(x: Decimal, y: Decimal): Decimal =
    d128SubImpl(x, y, DecContext.current())

internal fun d128SubImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val ySignNegated = ! y.sign
    return if (bothFnz(x, y)) {
        addFnzFnz(x, ySignNegated, y, ctx)
    } else when (binopSignatureOf(x, y)) {
        ZER_ZER -> addZeroZero(x, ySignNegated, y, ctx)
        ZER_FNZ -> scaleToMinExp(ySignNegated, y, x.qExp, ctx)
        ZER_INF, FNZ_INF -> y.negate()

        FNZ_ZER -> scaleToMinExp(x.sign, x, y.qExp, ctx)
        FNZ_FNZ -> throw IllegalStateException()
        //FNZ_INF -> y.negate()

        INF_ZER, INF_FNZ -> x
        //INF_FNZ -> x
        INF_INF -> addInfInf(x, ySignNegated, y, ctx)

        NAN_FOUND -> nanOperandFound(x, y, ctx)
    }
}

private fun addZeroZero(x: Decimal, ySign: Boolean, y: Decimal, ctx: DecContext): Decimal {
    // Both operands are zero. This is where the special rules apply.
    // IEEE754-2019 6.3 The sign bit
    // However, under all rounding-direction attributes,
    // when x is zero, x + x and x − (−x) have the sign of x.
    val xSign = x.sign
    val xQ = x.qExp
    val yQ = y.qExp
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
        if (y.sign == isRoundTowardNegative)
            return y
        qMin = yQ
    }
    return Decimal.zero(ctx.isRoundTowardNegative(), qMin, ctx)
}

private fun addInfInf(x: Decimal, ySign: Boolean, y: Decimal, ctx: DecContext): Decimal =
    if (x.sign == ySign)
        x
    else
        ctx.signalInvalid(Decimal.NaN)

private fun addFnzFnz(x: Decimal, ySign: Boolean, y: Decimal, ctx: DecContext) =
    if (x.qExp == y.qExp)
        addFnzFnzUnscaled(x, ySign, y, ctx)
    else
        addFnzFnzScaled(x, ySign, y, ctx)

private fun addFnzFnzUnscaled(x: Decimal, ySign: Boolean, y: Decimal, ctx: DecContext): Decimal {
    val xSign = x.sign
    if (xSign == ySign)
        return addUnscaledMagnitudes(ySign, x, y, ctx)
    val cmp = c128UnscaledCompare(x, y)
    return when {
        (cmp > 0) -> unscaledSub(xSign, x, y)
        (cmp < 0) -> unscaledSub(ySign, y, x)
        else -> Decimal.zero(ctx.isRoundTowardNegative(), x.qExp, ctx)
    }
}

private fun addUnscaledMagnitudes(sign: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
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

private fun addFnzFnzScaled(x: Decimal, ySign: Boolean, y: Decimal, ctx: DecContext): Decimal {
    val xSign = x.sign
    if (xSign == ySign)
        return addFnzScaledMagnitudes(ySign, x, y, ctx)
    // signs differ ... subtract scaled magnitudes
    val cmpMag = x.magnitudeCompareTo(y)
    return when {
        cmpMag > 0 -> subFnzScaledMagnitude(xSign, x, y, ctx)
        cmpMag < 0 -> subFnzScaledMagnitude(ySign, y, x, ctx)
        else -> Decimal.zero(false, min(x.qExp, y.qExp), ctx)
    }
}

private fun addFnzScaledMagnitudes(resultSign: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val flip = x.qExp > y.qExp
    val m = if (flip) x else y
    val n = if (flip) y else x
    val qDelta = m.qExp - n.qExp
    verify { qDelta >= 0 }
    val headroom = ctx.precision - m.digitLen
    val n1 = n.dw1; val n0 = n.dw0
    var dw1Sum = m.dw1
    var dw0Sum = m.dw0
    val shiftLeft = min(headroom, qDelta)
    if (shiftLeft > 0) {
        val (dw1T, dw0T) = umul128xPow10to128(dw1Sum, dw0Sum, shiftLeft)
        dw1Sum = dw1T
        dw0Sum = dw0T
    }
    val qAlign = m.qExp - shiftLeft
    val shiftRight = qAlign - n.qExp
    val residue: Residue
    when {
        shiftRight == 0 -> {
            verify { shiftLeft > 0 }
            dw0Sum += n0
            dw1Sum += n1 + if (unsignedLT(dw0Sum, n0)) 1L else 0L
            residue = Residue.EXACT
        }
        shiftRight >= n.digitLen -> { // fully swamped
            if (shiftRight > n.digitLen)
                residue = Residue.LT_HALF
            else
                residue = Residue.fromValuePow10(n1, n0, n.digitLen)
        }
        else -> {
            val tmpPair = ctx.tmps.dwQuad1
            residue = c128ScaleDownPow10(tmpPair, n1, n0, shiftRight)
            dw0Sum += tmpPair.dw0
            dw1Sum += tmpPair.dw1 + if (unsignedLT(dw0Sum, tmpPair.dw0)) 1L else 0L
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
 * 3. fully swamped
 *
 * Otherwise, use wide 256-bit ALU
 */
private fun subFnzScaledMagnitude(sign: Boolean, m: Decimal, s: Decimal, ctx: DecContext): Decimal {
    verify { m.magnitudeCompareTo(s) > 0 }
    verify { s.isNotZero() }
    verify { m.qExp != s.qExp }

    if (m.qExp < s.qExp) {
        // case 1: |m| > |s|, m.qExp < s.qExp
        // simple case: scale s up and subtract, always exact
        val qDelta = s.qExp - m.qExp
        verify { qDelta < ctx.precision }
        return d128FusedSubtractMulPow10(sign, m, s, qDelta)
    }
    // case 2: |m| > |s|, m.qExp > s.qExp

    verify { ctx.precision < 38 }
    run {
        val headroom = 38 - m.digitLen
        val gap = m.qExp - s.qExp
        if (headroom >= gap) {
            // case 2a
            // m has enough headroom to scale and align with s.qExp
            return d128FusedMulPow10Subtract(sign, m, gap, s, ctx)
        }
    }

    // case 3: fully swamped
    run {
        // in this case, headroom and shift are based upon ctx.precision

        val headroomP = 1 + ctx.precision - m.digitLen
        if (headroomP < 1) return@run  // no room for guard digit, skip to fullWidthAdd
        val qAlignP = m.qExp - headroomP
        val shiftSRight = qAlignP - s.qExp
        if (shiftSRight >= s.digitLen) {
            val residueInverse =
                if (shiftSRight > s.digitLen) Residue.GT_HALF
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
    verify { x.bitLen >= y.bitLen }

    val d0 = x.dw0 - y.dw0
    val carry0 = if (unsignedCmp(d0, x.dw0) > 0) 1L else 0L
    val d1 = x.dw1 - y.dw1 - carry0
    val diff = Decimal(sign, x.qExp, d1, d0)
    return diff
}