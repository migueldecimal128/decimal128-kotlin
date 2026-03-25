package com.decimal128.decimal

import kotlin.math.min

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
        ZER_INF -> ctx.signalInvalid(InvalidOperationReason.MUL_ZERO_BY_INFINITY)

        FNZ_FNZ -> {
            verify { a.isInfinite() }
            a
        }
        FNZ_INF, INF_FNZ, INF_INF ->
            fmaInfProd(x.signFlag xor y.signFlag, a, ctx)

        INF_ZER -> ctx.signalInvalid(InvalidOperationReason.MUL_ZERO_BY_INFINITY)
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
    verify { stealIsNAN(a.steal) }
    val hasSNAN = stealIsSNAN(stealX) or stealIsSNAN(stealY) or stealIsSNAN(stealA)
    val targetNAN = if (hasSNAN) STEAL_NAN_SNAN else STEAL_NAN_QNAN
    val theNAN =
        if ((stealX and STEAL_NAN_MASK) == targetNAN) x
        else if ((stealY and STEAL_NAN_MASK) == targetNAN) y
        else a

    if (!hasSNAN) {
        if ((stealIsZER(stealX) and stealIsINF(stealY)) or (stealIsINF(stealX) and stealIsZER(stealY)))
            return ctx.signalInvalid(InvalidOperationReason.MUL_ZERO_BY_INFINITY, theNAN)
        return theNAN
    }
    val quietedNaN = Decimal.qNaN(theNAN.signFlag, theNAN.dw1, theNAN.dw0)
    return ctx.signalInvalid(InvalidOperationReason.SNAN_OPERAND, quietedNaN)
}

private fun fmaZeroProd(x: Decimal, y: Decimal, a: Decimal, ctx: DecContext): Decimal {
    val stealX = x.steal
    val stealY = y.steal
    val stealA = a.steal
    verify { stealIsZER(stealX) || stealIsZER(stealY) }
    verify { !stealIsNAN(stealA) }
    val prodSign = stealSignFlag(stealX) xor stealSignFlag(stealY)
    val prodQ = stealQExp(stealX) + stealQExp(stealY)
    if (stealIsZER(stealA)) {
        val fmaSign =
            (prodSign and stealSignFlag(stealA)) or ((prodSign xor stealSignFlag(stealA)) and ctx.isRoundTowardNegative())
        return Decimal.zero(fmaSign, min(prodQ, stealQExp(stealA)))
    }
    if (stealIsFNZ(stealA) && prodQ < stealQExp(stealA))
        return scaleToMinQExp(stealA, a, prodQ, ctx)
    return a
}

private fun fmaInfProd(infSign: Boolean, a: Decimal, ctx: DecContext): Decimal {
    verify { !a.isNaN() }
    if (a.isFinite() || a.signFlag == infSign)
        return Decimal.infinity(infSign)
    return ctx.signalInvalid(InvalidOperationReason.MAGNITUDE_SUBTRACTION_OF_INFINITIES)
}

