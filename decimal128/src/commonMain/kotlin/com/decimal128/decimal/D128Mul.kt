// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal fun d128MulImpl(x: Decimal, y: Decimal): Decimal =
    d128MulImpl(x, y, DecContext.current())

internal fun d128MulImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal; val ySteal = y.steal
    val prodSignFlag = stealSignFlag(xSteal) xor stealSignFlag(ySteal)
    val prodExp = stealQExp(xSteal) + stealQExp(ySteal)
    val signature = binopSignatureOf(xSteal, ySteal)
    return if (signature == FNZ_FNZ) {
        val prodBitLen = stealBitLen(xSteal) + stealBitLen(ySteal)
        mulFnzFnz(prodSignFlag, prodExp, prodBitLen, x, y, ctx)
    } else when (signature) {
        ZER_ZER,
        ZER_FNZ,
        FNZ_ZER -> Decimal.zero(prodSignFlag, prodExp)

        ZER_INF,
        INF_ZER -> ctx.signalInvalidOperation(InvalidCause.MAGNITUDE_SUBTRACTION_OF_INFINITIES)

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
        return decFinalizeFinite(prodSignFlag, prodExp, p1, p0, ctx)
    }
    return mulFnzFnz256(x, y, ctx)
}

private fun mulFnzFnz256(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal; val ySteal = y.steal
    val tmps = ctx.tmps
    val m = tmps.mdecBridge1.set(x)
    val n = tmps.mdecBridge2.set(y)
    val p = tmps.mdecBridgeResult
    c256SetMul(p, m, n, tmps.pentad)
    p.finalizeFnz(
        stealSignFlag(xSteal) xor stealSignFlag(ySteal),
        stealQExp(xSteal) + stealQExp(ySteal), ctx)
    val d = Decimal.from(p)
    return d
}

internal fun d128SqrImpl(x: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> {
            if (stealBitLen(xSteal) <= 64) {
                val xDw0 = x.dw0
                val p0 = xDw0 * xDw0
                val p1 = unsignedMulHi(xDw0, xDw0) + ((x.dw1 * xDw0) shl 1)
                return decFinalizeFinite(false, stealQExp(xSteal) shl 1, p1, p0, ctx)
            } else {
                val tmps = ctx.tmps
                val m = tmps.mdecBridge1.set(x)
                val z = tmps.mdecBridge2
                z.setSquare(m, ctx)
                return Decimal.from(z)
            }
        }
        STEAL_TYP_ZER -> return Decimal.zero(false, stealQExp(xSteal) shl 1)
        STEAL_TYP_INF -> return Decimal.POS_INFINITY
        else -> return nanOperandFound(x, ctx)
    }
}

