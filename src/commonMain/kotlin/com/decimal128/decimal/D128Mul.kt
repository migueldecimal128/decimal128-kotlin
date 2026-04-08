// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import kotlin.math.min

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
        INF_ZER -> ctx.signalInvalid(InvalidOperationReason.MAGNITUDE_SUBTRACTION_OF_INFINITIES)

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
                return decFinalizeFinite(false, p1, p0, stealQExp(xSteal) shl 1, ctx)
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

internal fun d128PowImpl(x: Decimal, pow: Int, ctx: DecContext): Decimal {
    val xSteal = x.steal
    return when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> powImplFNZ(x, pow, ctx)
        STEAL_TYP_ZER -> powImplZER(x, pow, ctx)
        STEAL_TYP_INF -> powImplINF(x, pow)
        else -> nanOperandFound(x, ctx)
    }
}

private inline fun powImplINF(x: Decimal, pow: Int): Decimal {
    val resultSign = x.signBit and (pow and 1) != 0
    return when {
        pow > 0 -> Decimal.infinity(resultSign)
        pow < 0 -> Decimal.zero(resultSign)
        else -> Decimal.ONE
    }
}


private inline fun powImplZER(x: Decimal, pow: Int, ctx: DecContext): Decimal {
    val resultSign = x.signBit and (pow and 1) != 0
    return when {
        pow > 0 -> {
            // clamp power to prevent overflow with multiplication
            val pClamp = min(pow, 9999)
            val zQ = x.qExp * pClamp
            Decimal.zero(resultSign, zQ)
        }
        pow < 0 -> ctx.signalDivByZero(Decimal.infinity(resultSign))
        else -> Decimal.ONE
    }
}

private inline fun powImplFNZ(x: Decimal, pow: Int, ctx: DecContext): Decimal {
    when {
        pow > 0 -> when {
            pow == 1 -> return x
            pow == 2 -> return x.square()
        }
        pow == 0 -> return Decimal.ONE
    }
    val tmps = ctx.tmps
    val tmp1 = tmps.mdecBridge1.set(x)
    val tmp2 = tmps.mdecBridge2.setPow(tmp1, pow, ctx)
    return Decimal.from(tmp2)
}
