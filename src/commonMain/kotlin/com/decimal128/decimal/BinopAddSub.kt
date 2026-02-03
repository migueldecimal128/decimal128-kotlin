package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.C128Compare.c128UnscaledCompare
import com.decimal128.decimal.Decimal.Companion.bothFnz
import kotlin.math.max
import kotlin.math.min

internal fun addImpl(x: Decimal, y: Decimal): Decimal =
    addImpl(x, y, DecContext.current())

internal fun addImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    return if (bothFnz(x, y)) {
        addFnzFnz(x, y.sign, y, ctx)
    } else when (BinopSignature.of(x, y)) {
        ZER_ZER -> addZeroZero(x, y.sign, y, ctx)
        ZER_FNZ -> scaleToMinExp(y.sign, y, x.qExp, ctx)
        ZER_INF -> y

        FNZ_ZER -> scaleToMinExp(x.sign, x, y.qExp, ctx)
        FNZ_FNZ -> throw IllegalStateException()
        FNZ_INF -> y

        INF_ZER -> x
        INF_FNZ -> x
        INF_INF -> addInfInf(x, y.sign, y, ctx)

        NAN_FOUND -> nanOperandFound(x, y, ctx)
    }
}

internal fun subImpl(x: Decimal, y: Decimal): Decimal =
    subImpl(x, y, DecContext.current())

internal fun subImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    return if (bothFnz(x, y)) {
        addFnzFnz(x, !y.sign, y, ctx)
    } else when (BinopSignature.of(x, y)) {
        ZER_ZER -> addZeroZero(x, !y.sign, y, ctx)
        ZER_FNZ -> scaleToMinExp(!y.sign, y, x.qExp, ctx)
        ZER_INF -> y.negate()

        FNZ_ZER -> scaleToMinExp(x.sign, x, y.qExp, ctx)
        FNZ_FNZ -> throw IllegalStateException()
        FNZ_INF -> y.negate()

        INF_ZER -> x
        INF_FNZ -> x
        INF_INF -> addInfInf(x, !y.sign, y, ctx)

        NAN_FOUND -> nanOperandFound(x, y, ctx)
    }
}

private fun addZeroZero(x: Decimal, ySign: Boolean, y: Decimal, ctx: DecContext): Decimal {
    // Both operands are zero. This is where the special rules apply.
    if (x.sign == ySign) {
        // Rule: x + x = x. Preserves the sign of zero. (-0) + (-0) = -0.
        return if (x.qExp <= y.qExp) x else y // return min qExp
    }
    // Rule: (+0) + (-0). The signs are different.
    // Result is +0 unless rounding is roundTowardNegative.
    val isRoundTowardNegative = ctx.isRoundTowardNegative()
    var qMin = x.qExp
    if (x.qExp <= y.qExp) {
        if (x.sign == isRoundTowardNegative)
            return x
    } else {
        if (y.sign == isRoundTowardNegative)
            return y
        qMin = y.qExp
    }
    return Decimal.newZero(ctx.isRoundTowardNegative(), qMin, ctx)
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
    if (x.sign == ySign)
        return addUnscaledMagnitudes(ySign, x, y, ctx)
    val cmp = c128UnscaledCompare(x, y)
    return when {
        (cmp > 0) -> C128AddSub.c128UnscaledSub(x.sign, x, y)
        (cmp < 0) -> C128AddSub.c128UnscaledSub(ySign, y, x)
        else -> Decimal.newZero(ctx.isRoundTowardNegative(), x.qExp, ctx)
    }
}

private fun addUnscaledMagnitudes(resultSign: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val sumBitLen = max(x.bitLen, y.bitLen) + 1
    val sum = if (sumBitLen < ctx.decFormat.maxBitLen) {
        val x0 = x.dw0
        val y0 = y.dw0
        val s0 = x0 + y0
        val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
        val x1 = x.dw1
        val y1 = y.dw1
        val s1 = x1 + y1 + carry0
        Decimal.from(resultSign, s1, s0, x.qExp)
    } else {
        val m = ctx.decTemps.mdecArg1.set(x)
        m.sign = resultSign
        val n = ctx.decTemps.mdecArg2.set(y)
        n.sign = resultSign
        val mdecSum = ctx.decTemps.mdecResult.setAdd(m, n, ctx)
        Decimal.from(mdecSum)
    }
    return sum
}

private fun addFnzFnzScaled(x: Decimal, ySign: Boolean, y: Decimal, ctx: DecContext): Decimal {
    if (x.sign == ySign)
        return addScaledMagnitudes(ySign, x, y, ctx)
    // signs differ ... subtract scaled magnitudes
    val cmpMag = x.magnitudeCompareTo(y)
    return when {
        cmpMag > 0 -> subScaledMagnitudes(x.sign, x, y, ctx)
        cmpMag < 0 -> subScaledMagnitudes(ySign, y, x, ctx)
        else -> Decimal.newZero(x.sign && ySign, min(x.qExp, y.qExp), ctx)
    }
}

private fun addScaledMagnitudes(resultSign: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val flip = x.qExp > y.qExp
    val m = if (flip) x else y
    val n = if (flip) y else x
    val qDelta = m.qExp - n.qExp
    verify { qDelta >= 0 }
    val headroom = ctx.precision - m.digitLen
    return if (qDelta <= headroom) {
        // we can resolve this in our D128 world
        val shiftLeft = min(qDelta, headroom)
        D128Pow10.fmaCoeffPow10(resultSign, m, shiftLeft, n)
    } else {
        fullWidthAdd(resultSign, x, resultSign, y, ctx)
    }
}

private fun fullWidthAdd(xSign: Boolean, x: Decimal, ySign: Boolean, y: Decimal, ctx: DecContext): Decimal {
    val arg1 = ctx.decTemps.mdecArg1.set(x)
    arg1.sign = xSign
    val arg2 = ctx.decTemps.mdecArg2.set(y)
    arg2.sign = ySign
    val mdecSum = ctx.decTemps.mdecResult.setAdd(arg1, arg2, ctx)
    val sum = Decimal.from(mdecSum)
    return sum
}

private fun subScaledMagnitudes(sign: Boolean, m: Decimal, s: Decimal, ctx: DecContext): Decimal {
    // non-zero with different signs ... subtract magnitudes
    verify { m.magnitudeCompareTo(s) > 0 }
    verify { s.isNotZero() }
    verify { m.qExp != s.qExp }
    if (m.qExp < s.qExp) {
        // TC("22E1", "-2E2"),
        // signs opposite, |m| > |s|, but m.qExp < s.qExp
        // scale s before subtraction
        val qDelta = s.qExp - m.qExp
        verify { qDelta < PRECISION_34 }
        return D128Pow10.fusedSubtractMulPow10(sign, m, s, qDelta)
    } else {
        // |m| > |s| && m.qExp > s.qExp
        val headroom = ctx.precision - m.digitLen
        val qDelta = m.qExp - s.qExp
        if (headroom >= qDelta) {
            // 12E3, -4
            // m has enough headroom to scale and align with s.qExp
            return D128Pow10.fusedMulPow10Subtract(sign, m, qDelta, s)
        }
        val qAlign = m.qExp - headroom
        val shiftRight = qAlign - s.qExp
        if (shiftRight >= s.digitLen) {
            // s is fully swamped
            // this becomes a rounding/residue problem
            // FIXME
            //  This requires residue and rounding in the D128 world
            //  I'm not ready to tackle that yet
        }

    }
    return fullWidthAdd(sign, m, !sign, s, ctx)
}

