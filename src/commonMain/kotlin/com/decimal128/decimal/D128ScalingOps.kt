package com.decimal128.decimal

import kotlin.math.min

internal fun stripTrailingZerosImpl(x: Decimal, ctx: DecContext, maxToStrip: Int = 99): Decimal {
    val stealX = x.steal
    return when {
        stealIsFNZ(stealX) -> {
            val t = ctx.tmps.mdecBridge1.set(x)
            val r = ctx.tmps.mdecResult.setStripTrailingZeros(t, ctx, maxToStrip)
            Decimal.from(r)
        }
        stealIsZER(stealX) -> Decimal.zero(stealSignFlag(stealX))
        stealIsINF(stealX) -> x
        else -> nanOperandFound(x, ctx)
    }
}

internal fun withScale(x: Decimal, decimalScale: Int, ctx: DecContext): Decimal {
    val qX = x.qExp
    val qDesired = -decimalScale
    val digitLen = x.digitLen
    when {
        qX < NON_FINITE_INF && x.digitLen > 0 -> {
            val qDelta = qX - qDesired
            when {
                qDelta > 0 -> { // add fractional zeros
                    val headroom = min(ctx.precision - digitLen, qX - ctx.qTiny)
                    if (qDelta <= headroom) {
                        val (dw1, dw0) = umul128xPow10to128(x.dw1, x.dw0, qDelta)
                        return Decimal(x.sign, qX - qDelta, dw1, dw0)
                    }
                    return ctx.signalInvalid(Decimal.NaN)
                }
                qDelta < 0 -> { // remove fractional zeros
                    val t = ctx.tmps.mdecBridge1.set(x)
                    val r = ctx.tmps.mdecBridge2.setStripTrailingZeros(t, ctx, -qDelta)
                    if (r.qExp == -decimalScale)
                        return Decimal.from(r)
                    return ctx.signalInvalid(Decimal.NaN)
                }
                else -> return x
            }
        }
        qX < NON_FINITE_INF -> return Decimal.zero(x.sign, -decimalScale, ctx)
        qX == NON_FINITE_INF -> return x
        else -> return nanOperandFound(x, ctx)
    }
}
