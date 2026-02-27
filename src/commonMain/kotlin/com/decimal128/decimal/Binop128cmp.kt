package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import kotlin.math.abs

private fun cmpMagnitudeFnzFnz(x: Decimal, y: Decimal): Int {
    val x1 = x.dw1; val x0 = x.dw0; val xQ = x.qExp
    val y1 = y.dw1; val y0 = y.dw0; val yQ = y.qExp
    if (xQ == yQ)
        return ucmp128(x1, x0, y1, y0)
    val cmpSci = x.eExp.compareTo(y.eExp)
    if (cmpSci != 0)
        return cmpSci
    val qDelta = xQ - yQ
    val qDeltaAbs = abs(qDelta)
    val pow10BitLen = pow10BitLen(qDeltaAbs)
    val (dw1Pow10, dw0Pow10) = pow10_128(qDeltaAbs)
    if (qDelta > 0) {
        // x.qExp is larger
        // scale up x.coefficient
        if (pow10BitLen <= 64)
            return -ucmp128_128x64(y1, y0, x1, x0, dw0Pow10)
        return -ucmp128_128x64(y1, y0, dw1Pow10, dw0Pow10, x0)
    } else {
        // scale up y
        if (pow10BitLen <= 64)
            return ucmp128_128x64(x1, x0, y1, y0, dw0Pow10)
        return ucmp128_128x64(x1, x0, dw1Pow10, dw0Pow10, y0)
    }
}

private fun cmpMagnitudeNanFound(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    TODO()
}

private fun cmpNanFound(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    TODO()
}

fun cmpTotalOrderImpl_x(x: Decimal, y: Decimal, ctx: DecContext): Int {
    if (x.sign != y.sign)
        return if (x.sign) -1 else 1
    val negateMask = -x.signBit // 0 or -1
    return (cmpTotalOrderMagnitudeImpl_x(x, y) xor negateMask) - negateMask
}

private fun cmpTotalOrderMagnitudeImpl_x(x: Decimal, y: Decimal): Int {
    return if (bothFnz(x, y)) {
        cmpTotalOrderMagnitudeFnzFnz(x, y)
    } else when (BinopSignature.of(x, y)) {
        ZER_ZER -> x.qExp.compareTo(y.qExp)
        ZER_FNZ -> -1
        ZER_INF -> -1

        FNZ_ZER -> 1
        FNZ_FNZ -> throw IllegalStateException()
        FNZ_INF -> -1

        INF_ZER -> 1
        INF_FNZ -> 1
        INF_INF -> 0
        NAN_FOUND -> cmpTotalOrderMagnitudeNanFound(x, y)
    }
}

private fun cmpTotalOrderMagnitudeFnzFnz(x: Decimal, y: Decimal): Int {
    val cmp = cmpMagnitudeFnzFnz(x, y)
    if (cmp != 0)
        return cmp
    // If x and y represent the same floating-point datum:
    //  i) If x and y have negative sign,
    //    totalOrder(x, y) is true if and only if the exponent of x ≥ the exponent of y
    //  ii) otherwise,
    //    totalOrder(x, y) is true if and only if the exponent of x ≤ the exponent of y.
    return x.qExp.compareTo(y.qExp)
}

private fun cmpTotalOrderMagnitudeNanFound(x: Decimal, y: Decimal): Int {
    return when {
        x.qExp < NON_FINITE_QNAN -> -1
        y.qExp < NON_FINITE_QNAN -> 1
        // if both are the same NaN, then compare payloads
        x.qExp == y.qExp -> ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
        // strange ... sNaN sorts less than qNaN
        // TODO ... should I consider swapping qNaN and sNaN qExp values?
        x.qExp == NON_FINITE_QNAN -> -1
        else -> 1
    }
}
