// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal fun d128MulImpl(x: Decimal, y: Decimal): Decimal =
    d128MulImpl(x, y, DecContext.current())

internal fun d128MulImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal; val ySteal = y.steal
    val prodSignFlag = stealSignFlag(xSteal) xor stealSignFlag(ySteal)
    val prodExp = stealQexp(xSteal) + stealQexp(ySteal)
    val signature = binopSignatureOf(xSteal, ySteal)
    return if (signature == FNZ_FNZ) {
        val prodBitLen = stealBitLen(xSteal) + stealBitLen(ySteal)
        mulFnzFnz(prodSignFlag, prodExp, prodBitLen, x, y, ctx)
    } else when (signature) {
        ZER_ZER,
        ZER_FNZ,
        FNZ_ZER -> Decimal.zero(prodSignFlag, prodExp, ctx)

        ZER_INF,
        INF_ZER -> ctx.signalInvalid(InvalidOpReason.MAGNITUDE_SUBTRACTION_OF_INFINITIES, Decimal.NaN)

        FNZ_INF,
        INF_FNZ,
        INF_INF -> Decimal.infinity(prodSignFlag)

        else -> nanOperandFound(x, y, ctx)
    }
}

// fast-path iff ...
//  product bitLen strictly less than decFormat.maxBitLen
//  (equal bitLen could overflow coefficient decimal limit)
//
//  exponent on the upper end is easy, must be < qMax
//  exponent on the low end must be >= eMin, not qTiny
//  anything in the range [qTiny, eMin) is subnormal
//  and must be scaled, so not on the fast-path
private inline fun mulFnzFnz(prodSignFlag: Boolean, prodExp: Int, prodBitLen: Int, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    if (prodBitLen <= 128) {
        val p0 = x.dw0 * y.dw0
        val p1 = unsignedMulHi(x.dw0, y.dw0) + (x.dw1 * y.dw0) + (y.dw1 * x.dw0)
        return decFinalizeFinite(prodSignFlag, p1, p0, prodExp, ctx)
    }
    return mulFnzFnz256(x, y, ctx)
}

private fun mulFnzFnz256(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal; val ySteal = y.steal
    val tmps = ctx.tmps
    val m = tmps.mdecBridge1.set(x)
    val n = tmps.mdecBridge2.set(y)
    val p = tmps.mdecResult
    c256SetMul(p, m, n, tmps.pentad1)
    p.type = STEAL_TYPE_FNZ
    p.qExp = stealQexp(xSteal) + stealQexp(ySteal)
    p.sign = stealSignFlag(xSteal) xor stealSignFlag(ySteal)
    p.finalize(ctx)
    val d = Decimal.from(p)
    return d
}

