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
import com.decimal128.decimal.Decimal.Companion.NEG_ONE
import com.decimal128.decimal.Decimal.Companion.NaN
import com.decimal128.decimal.Decimal.Companion.POS_ONE
import com.decimal128.decimal.Decimal.Companion.ZERO
import com.decimal128.decimal.Decimal.Companion.hasNaN
import kotlin.math.abs

private val mapToDecimal: Array<Decimal> =
    arrayOf(NEG_ONE, ZERO, POS_ONE, NaN)

// TODO - in MutDec I combined the different types of comparisons
//  into a common routine. Consider doing that for Decimal in order
//  to reduce code size and redundancy.

internal fun cmpImpl(x: Decimal, y: Decimal): Decimal =
    cmpImpl(x, y, DecContext.current())

internal fun cmpImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    if (hasNaN(x, y))
        return nanOperandFound(x, y, ctx)
    if (x.isZero() && y.isZero())
        return ZERO
    if (x.sign != y.sign)
        return if (x.sign) NEG_ONE else POS_ONE
    val cmpMag =
        if (bothFnz(x, y)) {
            cmpMagnitudeFnzFnz(x, y)
        } else when (BinopSignature.of(x, y)) {
            ZER_ZER -> throw IllegalStateException()
            ZER_FNZ -> -1
            ZER_INF -> -1

            FNZ_ZER -> 1
            FNZ_FNZ -> throw IllegalStateException()
            FNZ_INF -> -1

            INF_ZER -> 1
            INF_FNZ -> 1
            INF_INF -> 0

            NAN_FOUND -> throw IllegalStateException()
        }
    val negateMask = x.signMask
    val t = (cmpMag xor negateMask) - negateMask
    return mapToDecimal[t + 1]
}

private fun cmpFnzFnz(x: Decimal, y: Decimal): Int {
    if (x.sign != y.sign)
        return if (x.sign) -1 else 1
    val negateMask = x.signMask // 0 or -1
    return (cmpMagnitudeFnzFnz(x, y) xor negateMask) - negateMask
}

private fun cmpMagnitudeFnzFnz(x: Decimal, y: Decimal): Int {
    val x1 = x.dw1; val x0 = x.dw0
    val xQ = x.qExp
    val y1 = y.dw1; val y0 = y.dw0
    val yQ = y.qExp
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

internal fun cmpSignalingImpl(x: Decimal, y: Decimal): Decimal =
    cmpSignalingImpl(x, y, DecContext.current())

internal fun cmpSignalingImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    if (hasNaN(x, y))
        return nanOperandFound(x, y, ctx, alwaysSignal = true)
    return cmpImpl(x, y, ctx)
}

internal fun cmpMagnitudeImpl(x: Decimal, y: Decimal): Decimal =
    cmpMagnitudeImpl(x, y, DecContext.current())

fun cmpMagnitudeImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val cmp = if (bothFnz(x, y)) {
        cmpMagnitudeFnzFnz(x, y)
    } else when (BinopSignature.of(x, y)) {
        ZER_ZER -> 0
        ZER_FNZ -> -1
        ZER_INF -> -1

        FNZ_ZER -> 1
        FNZ_FNZ -> throw IllegalStateException()
        FNZ_INF -> -1

        INF_ZER -> 1
        INF_FNZ -> 1
        INF_INF -> 0

        NAN_FOUND -> return nanOperandFound(x, y, ctx)
    }
    return mapToDecimal[(cmp + 1) and 0x03]
}

internal fun cmpTotalOrderImpl(x: Decimal, y: Decimal, ctx: DecContext): Int {
    if (x.sign != y.sign)
        return if (x.sign) -1 else 1
    val negateMask = -x.signBit // 0 or -1
    return (cmpTotalOrderMagnitudeImpl(x, y, ctx) xor negateMask) - negateMask
}

fun cmpTotalOrderMagnitudeImpl(x: Decimal, y: Decimal, ctx: DecContext): Int {
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
    val xQ = x.qExp
    val yQ = y.qExp
    return when {
        xQ < NON_FINITE_QNAN -> -1
        yQ < NON_FINITE_QNAN -> 1
        // if both are the same NaN, then compare payloads
        xQ == yQ -> ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
        xQ == NON_FINITE_QNAN -> 1
        else -> -1
    }
}
