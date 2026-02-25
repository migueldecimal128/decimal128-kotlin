package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.FNZ_FNZ
import com.decimal128.decimal.BinopSignature.FNZ_INF
import com.decimal128.decimal.BinopSignature.FNZ_ZER
import com.decimal128.decimal.BinopSignature.INF_FNZ
import com.decimal128.decimal.BinopSignature.INF_INF
import com.decimal128.decimal.BinopSignature.INF_ZER
import com.decimal128.decimal.BinopSignature.NAN_FOUND
import com.decimal128.decimal.BinopSignature.ZER_FNZ
import com.decimal128.decimal.BinopSignature.ZER_INF
import com.decimal128.decimal.BinopSignature.ZER_ZER
import kotlin.math.max
import kotlin.math.min

internal fun fmaImpl(x: Decimal, y: Decimal, a: Decimal): Decimal =
    fmaImpl(x, y, a, DecContext.current())

internal fun fmaImpl(x: Decimal, y: Decimal, a: Decimal, ctx: DecContext): Decimal {
    if (x.isFiniteNonZero() && y.isFiniteNonZero() && a.isFinite())
        return fmaFnzFnzFinite(x, y, a, ctx)
    if (a.isNaN())
        return fmaNanAddend(x, y, a, ctx)
    return when (BinopSignature.of(x, y)) {
        ZER_ZER -> fmaZeroProd(x, y, a, ctx)
        ZER_FNZ -> fmaZeroProd(x, y, a, ctx)
        ZER_INF -> ctx.signalInvalid(Decimal.NaN)

        FNZ_ZER -> fmaZeroProd(x, y, a, ctx)
        FNZ_FNZ -> {
            verify { a.isInfinite() }
            a
        }
        FNZ_INF, INF_FNZ, INF_INF ->
            fmaInfProd(x.sign xor y.sign, a, ctx)

        INF_ZER -> ctx.signalInvalid(Decimal.NaN)
        //INF_FNZ -> fmaInfProd(x.sign xor y.sign, a, ctx)
        //INF_INF -> fmaInfProd(x.sign xor y.sign, a, ctx)

        NAN_FOUND -> nanOperandFound(x, y, ctx)
    }
}

private fun fmaFnzFnzFinite(x: Decimal, y: Decimal, a: Decimal, ctx: DecContext): Decimal {
    val decTmps = ctx.decTmps
    val i = decTmps.mdecBridge1.set(x)
    val j = decTmps.mdecBridge2.set(y)
    val k = decTmps.mdecBridge3.set(a)
    val result = decTmps.mdecResult.setFma(i, j, k, ctx)
    return Decimal.from(result)
}

/**
 * IEEE754-2019 7.2 Invalid Operation
 *
 * c) fusedMultiplyAdd: fusedMultiplyAdd(0, ∞, c) or fusedMultiplyAdd(∞, 0, c) unless c is a quiet
 * NaN; if c is a quiet NaN then it is implementation defined whether the invalid operation exception
 * is signaled
 *
 * I will check and signal.
 */
private fun fmaNanAddend(x: Decimal, y: Decimal, a: Decimal, ctx: DecContext): Decimal {
    val qX = x.qExp
    val qY = y.qExp
    val qA = a.qExp
    val maxQ = max(max(qX, qY), qA)
    verify { maxQ >= NON_FINITE_QNAN }
    val theNaN = if (maxQ == qX) x else if (maxQ == qY) y else a
    if (maxQ == NON_FINITE_QNAN) {
        if ((x.isZero() && y.isInfinite()) ||(x.isInfinite() && y.isZero()))
            return ctx.signalInvalid(theNaN)
        return theNaN
    }
    val quietedNaN = Decimal.qNaN(theNaN.sign, theNaN.dw1, theNaN.dw0)
    return ctx.signalInvalid(quietedNaN)
}

private fun fmaZeroProd(x: Decimal, y: Decimal, a: Decimal, ctx: DecContext): Decimal {
    verify { x.isZero() || y.isZero() }
    verify { !a.isNaN() }
    val prodSign = x.sign xor y.sign
    val prodQ = x.qExp + y.qExp
    if (a.isZero()) {
        val fmaSign =
            (prodSign and a.sign) or ((prodSign xor a.sign) and ctx.isRoundTowardNegative())
        return Decimal.newZero(fmaSign, min(prodQ, a.qExp), ctx)
    }
    if (a.isFiniteNonZero() && prodQ < a.qExp)
        return rescaleToMinQExpImpl(a, prodQ, ctx)
    return a
}

private fun fmaInfProd(infSign: Boolean, a: Decimal, ctx: DecContext): Decimal {
    verify { !a.isNaN() }
    if (a.isFinite() || a.sign == infSign)
        return Decimal.infinity(infSign)
    return ctx.signalInvalid(Decimal.NaN)
}

internal fun rescaleToMinQExpImpl(x: Decimal, qNew: Int, ctx: DecContext): Decimal {
    val headroom = ctx.precision - x.digitLen
    val qDelta = min(x.qExp - qNew, headroom)
    if (qDelta <= 0)
        return x
    val t = ctx.decTmps.mdecBridge1.set(x)
    val r = ctx.decTmps.mdecResult
    c256SetScaleUpPow10(r, t, qDelta)
    r.qExp = x.qExp - qDelta
    r.sign = x.sign
    return Decimal.from(r.finalize(ctx))
}