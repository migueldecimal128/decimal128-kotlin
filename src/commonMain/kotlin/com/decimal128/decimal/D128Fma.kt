package com.decimal128.decimal

import kotlin.math.min

internal fun fmaImpl(x: Decimal, y: Decimal, a: Decimal): Decimal =
    fmaImpl(x, y, a, DecContext.current())

internal fun fmaImpl(x: Decimal, y: Decimal, a: Decimal, ctx: DecContext): Decimal {
    val signatureXY = binopSignatureOf(x.steal, y.steal)
    if (signatureXY == FNZ_FNZ && a.isFinite())
        return fmaFnzFnzFinite(x, y, a, ctx)
    if (a.isNaN())
        return fmaNanAddend(x, y, a, ctx)
    return when (signatureXY) {
        ZER_ZER,
        ZER_FNZ,
        FNZ_ZER -> fmaZeroProd(x, y, a, ctx)
        ZER_INF -> ctx.signalInvalid(Decimal.NaN)

        FNZ_FNZ -> {
            verify { a.isInfinite() }
            a
        }
        FNZ_INF, INF_FNZ, INF_INF ->
            fmaInfProd(x.sign xor y.sign, a, ctx)

        INF_ZER -> ctx.signalInvalid(Decimal.NaN)
        //INF_FNZ -> fmaInfProd(x.sign xor y.sign, a, ctx)
        //INF_INF -> fmaInfProd(x.sign xor y.sign, a, ctx)

        else -> nanOperandFound(x, y, ctx)
    }
}

private fun fmaFnzFnzFinite(x: Decimal, y: Decimal, a: Decimal, ctx: DecContext): Decimal {
    val decTmps = ctx.tmps
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
    val stealX = x.steal
    val stealY = y.steal
    val stealA = a.steal
    verify { stealIsNAN(stealX) or stealIsNAN(stealY) or stealIsNAN(stealA) }
    val hasSNAN = stealIsSNAN(stealX) or stealIsSNAN(stealY) or stealIsSNAN(stealA)
    val targetNAN = if (hasSNAN) STEAL_NAN_SNAN else STEAL_NAN_QNAN
    val theNAN =
        if ((stealX and STEAL_NAN_MASK) == targetNAN) x
        else if ((stealY and STEAL_NAN_MASK) == targetNAN) y
        else a

    if (!hasSNAN) {
        if ((stealIsZER(stealX) and stealIsINF(stealY)) or (stealIsINF(stealX) and stealIsZER(stealY)))
            return ctx.signalInvalid(theNAN)
        return theNAN
    }
    val quietedNaN = Decimal.qNaN(theNAN.sign, theNAN.dw1, theNAN.dw0)
    return ctx.signalInvalid(quietedNaN)
}

private fun fmaZeroProd(x: Decimal, y: Decimal, a: Decimal, ctx: DecContext): Decimal {
    verify { x.isZero() || y.isZero() }
    verify { !a.isNaN() }
    val prodSign = x.sign xor y.sign
    val prodQ = x.qExp() + y.qExp()
    if (a.isZero()) {
        val fmaSign =
            (prodSign and a.sign) or ((prodSign xor a.sign) and ctx.isRoundTowardNegative())
        return Decimal.zero(fmaSign, min(prodQ, a.qExp()), ctx)
    }
    if (a.isFiniteNonZero() && prodQ < a.qExp())
        return rescaleToMinQExpImpl(a, prodQ, ctx)
    return a
}

private fun fmaInfProd(infSign: Boolean, a: Decimal, ctx: DecContext): Decimal {
    verify { !a.isNaN() }
    if (a.isFinite() || a.sign == infSign)
        return Decimal.infinity(infSign)
    return ctx.signalInvalid(Decimal.NaN)
}

private fun rescaleToMinQExpImpl(x: Decimal, qNew: Int, ctx: DecContext): Decimal {
    val headroom = ctx.precision - x.digitLen
    val qDelta = min(x.qExp() - qNew, headroom)
    if (qDelta <= 0)
        return x
    val t = ctx.tmps.mdecBridge1.set(x)
    val r = ctx.tmps.mdecResult
    c256SetScaleUpPow10(r, t, qDelta, ctx.tmps.dwQuad1)
    r.qExp = x.qExp() - qDelta
    r.sign = x.sign
    return Decimal.from(r.finalize(ctx))
}