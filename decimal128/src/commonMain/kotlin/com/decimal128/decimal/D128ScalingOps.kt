package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.decimalFNZ
import com.decimal128.decimal.InvalidCause.QUANTIZE_EXACTLY_ONE_OPERAND_IS_INFINITE
import com.decimal128.decimal.InvalidCause.QUANTUM_SCALE_OUT_OF_RANGE
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

internal fun withScaleImpl(x: Decimal, decimalScale: Int): Decimal {
    val xSteal = x.steal
    return when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> withScaleImplFNZ(x, decimalScale)
        STEAL_TYP_ZER -> {
            val targetQExp = -decimalScale
            if (targetQExp >= Q_TINY && targetQExp <= Q_MAX)
                Decimal.zero(stealSignFlag(xSteal), -decimalScale)
            else
                signalInvalidOperation(QUANTUM_SCALE_OUT_OF_RANGE)
        }
        STEAL_TYP_INF -> signalInvalidOperation(QUANTIZE_EXACTLY_ONE_OPERAND_IS_INFINITE)
        else -> // STEAL_TYP_NAN
            nanOperandFound(x)
    }
}

private fun withScaleImplFNZ(x: Decimal, decimalScale: Int): Decimal {
    val ctx = DecContext.current()
    val qDesired = -decimalScale
    if (qDesired < Q_TINY || qDesired > Q_MAX)
        return ctx.signalInvalidOperation(QUANTUM_SCALE_OUT_OF_RANGE)
    val xSteal = x.steal
    val xQ = stealQExp(xSteal)
    val xDigitLen = stealDigitLen(xSteal)
    val qDelta = qDesired - xQ
    when {
        qDelta < 0 -> { // add fractional zeros
            val headroom = min(ctx.precision - xDigitLen, xQ - Q_TINY)
            if (-qDelta <= headroom) {
                val pentad = ctx.tmps.pentad
                umul128xPow10to128(pentad, x.dw1, x.dw0, -qDelta)
                return decimalFNZ(x.signFlag, xQ + qDelta, pentad.dw1, pentad.dw0)
            }
            return ctx.signalInvalidOperation(InvalidCause.UNABLE_TO_SCALE)
        }

        qDelta > 0 -> { // scale and round as needed
            val t1 = ctx.tmps.mdecBridge1.set(x)
            val residue = c256SetScaleDownPow10(t1, t1, qDelta, ctx.tmps.pentad)
            t1.roundAndFinalizeFinite(stealSignFlag(xSteal), qDesired, residue, ctx.decRounding, ctx)
            return Decimal.from(t1)
        }

        else -> return x
    }
}

internal fun quantizeImpl(x: Decimal, y: Decimal): Decimal {
    val xSteal = x.steal
    val ySteal = y.steal
    val signature = binopSignatureOf(xSteal, ySteal)
    return when (signature) {
        FNZ_FNZ,
        FNZ_ZER -> withScaleImplFNZ(x, -stealQExp(ySteal))

        ZER_FNZ,
        ZER_ZER -> Decimal.zero(stealSignFlag(xSteal), stealQExp(ySteal))

        // IEEE754-2019 5.3.2
        // Otherwise if only one operand is infinite then the invalid operation
        // exception is signaled and the result is a NaN.
        // If both operands are infinite then the result is
        // canonical ∞ with the sign of x.
        INF_FNZ,
        INF_ZER,
        FNZ_INF,
        ZER_INF -> signalInvalidOperation(QUANTIZE_EXACTLY_ONE_OPERAND_IS_INFINITE)

        INF_INF -> x
        else -> nanOperandFound(x, y)
    }
}

