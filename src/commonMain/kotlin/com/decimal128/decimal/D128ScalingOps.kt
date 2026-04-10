package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.decimalFNZ
import kotlin.math.min

internal fun stripTrailingZerosImpl(x: Decimal, ctx: DecContext, maxToStrip: Int = 99): Decimal {
    val stealX = x.steal
    return when {
        stealIsFNZ(stealX) -> {
            val t = ctx.tmps.mdecBridge1.set(x)
            val r = ctx.tmps.mdecBridgeResult.setStripTrailingZeros(t, ctx, maxToStrip)
            Decimal.from(r)
        }
        stealIsZER(stealX) -> Decimal.zero(stealSignFlag(stealX))
        stealIsINF(stealX) -> x
        else -> nanOperandFound(x, ctx)
    }
}

internal fun withScaleImpl(x: Decimal, decimalScale: Int, ctx: DecContext): Decimal {
    val xSteal = x.steal
    if (stealIsFNZ(xSteal)) {
        val xQ = stealQExp(xSteal)
        val xDigitLen = stealDigitLen(xSteal)
        val qDesired = -decimalScale
        val qDelta = xQ - qDesired
        when {
            qDelta > 0 -> { // add fractional zeros
                val headroom = min(ctx.precision - xDigitLen, xQ - Q_TINY)
                if (qDelta <= headroom) {
                    val pentad = ctx.tmps.pentad
                    umul128xPow10to128(pentad, x.dw1, x.dw0, qDelta)
                    return decimalFNZ(x.signFlag, xQ - qDelta, pentad.dw1, pentad.dw0)
                }
                return ctx.signalInvalid(InvalidOperationReason.UNABLE_TO_SCALE)
            }

            qDelta < 0 -> { // remove fractional zeros
                val t = ctx.tmps.mdecBridge1.set(x)
                val r = ctx.tmps.mdecBridge2.setStripTrailingZeros(t, ctx, -qDelta)
                if (r.qExp == -decimalScale)
                    return Decimal.from(r)
                return ctx.signalInvalid(InvalidOperationReason.UNABLE_TO_SCALE)
            }

            else -> return x
        }
    }
    if (stealIsZER(xSteal))
        return Decimal.zero(x.signFlag, -decimalScale)
    if (stealIsINF(xSteal))
        return x
    verify { stealIsNAN(xSteal) }
    return nanOperandFound(x, ctx)
}
