// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.NEG_INFINITY
import com.decimal128.decimal.Decimal.Companion.POS_INFINITY

internal fun d128MulImpl(x: Decimal, y: Decimal): Decimal =
    d128MulImpl(x, y, DecContext.current())

internal fun d128MulImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal; val ySteal = y.steal
    val signature = binopSignatureOf(xSteal, ySteal)
    return if (signature == FNZ_FNZ) {
        mulFnzFnz(xSteal, x, ySteal, y, ctx)
    } else when (signature) {
        ZER_ZER,
        ZER_FNZ,
        FNZ_ZER -> mulZero(xSteal, x, ySteal, y, ctx)

        ZER_INF,
        INF_ZER -> mulInfZero(ctx)

        FNZ_INF,
        INF_FNZ,
        INF_INF -> mulInfNonzero(xSteal, x, ySteal, y)

        else -> nanOperandFound(x, y, ctx)
    }
}

private inline fun mulZero(xSteal: Int, x: Decimal, ySteal: Int, y: Decimal, ctx: DecContext): Decimal =
    Decimal.zero(stealSignFlag(xSteal) xor stealSignFlag(ySteal),
        stealQexp(xSteal) + stealQexp(ySteal), ctx)

private inline fun mulInfZero(ctx: DecContext): Decimal =
    ctx.signalInvalid(Decimal.NaN)

private fun mulInfNonzero(xSteal: Int, x: Decimal, ySteal: Int, y: Decimal): Decimal =
    if (stealSignFlag(xSteal) xor stealSignFlag(ySteal)) NEG_INFINITY else POS_INFINITY

// fast-path iff ...
//  product bitLen strictly less than decFormat.maxBitLen
//  (equal bitLen could overflow coefficient decimal limit)
//
//  exponent on the upper end is easy, must be < qMax
//  exponent on the low end must be >= eMin, not qTiny
//  anything in the range [qTiny, eMin) is subnormal
//  and must be scaled, so not on the fast-path
private inline fun mulFnzFnz(xSteal: Int, x: Decimal, ySteal: Int, y: Decimal, ctx: DecContext): Decimal {
    val prodBitLen = stealBitLen(xSteal) + stealBitLen(ySteal)
    val prodExp = stealQexp(xSteal) + stealQexp(ySteal)
    if (prodBitLen <= 128) {
        val p0 = x.dw0 * y.dw0
        val p1 = unsignedMulHi(x.dw0, y.dw0) + (x.dw1 * y.dw0) + (y.dw1 * x.dw0)
        val prodSign = x.sign xor y.sign
        return decFinalizeFinite(prodSign, p1, p0, prodExp, ctx)
    }
    return mulFnzFnz256(x, y, ctx)
}

private fun mulFnzFnz256(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal; val ySteal = y.steal
    val tmps = ctx.tmps
    val m = tmps.mdecBridge1.set(x)
    val n = tmps.mdecBridge2.set(y)
    val p = tmps.mdecResult
    c256SetMul(p, m, n, tmps.dwQuad1)
    p.type = STEAL_TYPE_FNZ
    p.qExp = stealQexp(xSteal) + stealQexp(ySteal)
    p.sign = stealSignFlag(xSteal) xor stealSignFlag(ySteal)
    p.finalize(ctx)
    val d = Decimal.from(p)
    return d
}

