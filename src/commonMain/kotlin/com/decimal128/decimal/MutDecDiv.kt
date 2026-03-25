package com.decimal128.decimal

import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO
import kotlin.math.min

internal fun mutDecDivImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val binopSignature = binopSignatureOf(x.type, y.type)
    val quotientSign = x.sign xor y.sign
    if (binopSignature == FNZ_FNZ)
        return mutDecDivFnzFnz(z, quotientSign, x, y, ctx)
    else when (binopSignature) {
        ZER_ZER -> ctx.setNanSignalInvalid(z, InvalidOperationReason.DIV_ZERO_BY_ZERO)
        ZER_FNZ -> z.setZero(quotientSign, x.qExp - y.qExp)
        ZER_INF,
        FNZ_INF -> z.setZero(quotientSign, Q_TINY)
        FNZ_ZER -> ctx.signalDivByZero(z.setInfinite(quotientSign))
        INF_ZER,
        INF_FNZ -> z.setInfinite(quotientSign)
        INF_INF -> ctx.setNanSignalInvalid(z, InvalidOperationReason.DIV_INF_BY_INF)
        else -> z.setNaNOperand(x, y, ctx)
    }
    return z
}

internal fun mutDecDivIntImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val binopSignature = binopSignatureOf(x.type, y.type)
    if (binopSignature == FNZ_FNZ) {
        setDivIntFnzFnz(z, x, y, ctx)
    } else {
        val quotientSign = x.sign xor y.sign
        when (binopSignature) {
            ZER_ZER -> ctx.setNanSignalInvalid(z, InvalidOperationReason.DIV_ZERO_BY_ZERO)
            INF_INF -> ctx.setNanSignalInvalid(z, InvalidOperationReason.DIV_INF_BY_INF)
            FNZ_INF,
            ZER_FNZ,
            ZER_INF -> z.setZero(quotientSign)
            INF_ZER,
            INF_FNZ -> z.setInfinite(quotientSign)
            FNZ_ZER -> ctx.signalDivByZero(z.setInfinite(quotientSign))
            else -> z.setNaNOperand(x, y, ctx)
        }
    }
    return z
}

internal fun setDivIntFnzFnz(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val truncCtx = ctx.withRoundingAndNewFlags(ROUND_TOWARD_ZERO)
    z.setDiv(x, y, truncCtx)
    z.setRoundToIntegralExact(z, truncCtx)
    // Normalize integer toward qExp = 0 using available precision
    val zQ = z.qExp
    verify { zQ >= 0 }
    if (zQ > 0) {
        if (!z.isZero()) { // truncation could generate z == 0
            val headroom = ctx.precision - z.digitLen
            if (headroom < zQ) // 1234567890123456789012345678901234 / .000000001
                return ctx.setNanSignalInvalid(z, InvalidOperationReason.DIV_INT_OVERFLOWS_COEFFICIENT)
            c256SetScaleUpPow10(z, z, zQ, ctx.tmps.pentad1)
        }
        z.qExp = 0
    }
    return z
}

internal fun mutDecReciprocalImpl(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
    val quotientSign = x.sign
    when (stealTyp(x.type)) {
        STEAL_TYP_FNZ -> {
            return mutDecInv(z, quotientSign, x, ctx)
        }
        STEAL_TYP_ZER -> ctx.signalDivByZero(z.setInfinite(quotientSign))
        STEAL_TYP_INF -> z.setZero(quotientSign)
        else -> z.setNaNOperand(x, ctx)
    }
    return z
}

fun mutDecSetRemTruncImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): Boolean {
    val binopSignature = binopSignatureOf(x.type, y.type)
    if (binopSignature == FNZ_FNZ) {
        return setRemTruncFnzFnz(z, x, y, ctx)
    } else {
        when (binopSignature) {
            ZER_FNZ -> z.setZero(x.sign, min(x.qExp, y.qExp))

            FNZ_ZER -> ctx.setNanSignalInvalid(z, InvalidOperationReason.DIV_BY_ZERO_IN_REMAINDER_OP)
            ZER_ZER -> ctx.setNanSignalInvalid(z, InvalidOperationReason.DIV_ZERO_BY_ZERO)

            INF_ZER,
            INF_FNZ,
            INF_INF -> ctx.setNanSignalInvalid(z, InvalidOperationReason.INF_NUMERATOR_IN_REMAINDER_OP)
            ZER_INF,
            FNZ_INF -> z.set(x)
            else -> z.setNaNOperand(x, y, ctx)
        }
    }
    return false
}

fun setRemTruncFnzFnz(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): Boolean {
    verify { x.bitLen != 0 && y.bitLen != 0 }
    // Compute n = nearest integer to x/y (ties to even)
    // setRemainder is an EXACT operation, so we will use a temp
    // environment so that INEXACT flag/trap does not get signaled.
    // use INTERNAL_TMP_ENV so that flag-setting
    val truncCtx = ctx.withRoundingAndNewFlags(ROUND_TOWARD_ZERO)
    val n = ctx.tmps.mdecArg1.setDiv(x, y, truncCtx)
    if (n.qExp < 0)
        n.setRoundToIntegralExact(n, truncCtx)

    // save xSign ... in case of aliasing this === x
    val xSign = x.sign
    // Compute r = x - n*y
    // (-n) * y + x
    n.sign = !n.sign // negate n
    val quotientIsOdd = (n.dw0.toInt() and 1) != 0
    z.setFma(n, y, x, truncCtx)
    if (z.isZero())
        z.setZero(x.sign, min(x.qExp, y.qExp))

    return quotientIsOdd
}

fun mutDecCompare754Impl(x: MutDec, y: MutDec, isSignaling: Boolean, ctx: DecContext): Compare754Result {
    val binopSignature = binopSignatureOf(x.type, y.type)
    if (binopSignature == FNZ_FNZ) {
        return Compare754Result(x.compareNumericMagnitudeTo(y))
    }
    val xSign = x.sign
    val ySign = y.sign
    return when (binopSignature) {
        ZER_ZER -> IEEE754_EQ

        ZER_FNZ -> if (ySign) IEEE754_LT else IEEE754_GT
        FNZ_ZER -> if (xSign) IEEE754_GT else IEEE754_LT

        INF_INF -> when {
            xSign == ySign -> IEEE754_EQ
            xSign -> IEEE754_LT
            else -> IEEE754_GT
        }

        INF_FNZ,
        INF_ZER -> if (xSign) IEEE754_LT else IEEE754_GT
        FNZ_INF,
        ZER_INF -> if (ySign) IEEE754_GT else IEEE754_LT

        else -> { // NAN_FOUND
            if (isSignaling) {
                val guiltyParty = when {
                    x.isSignaling() -> x
                    y.isSignaling() -> y
                    x.isNaN() -> x
                    else -> y
                }
                ctx.operandIsSignalingNaN(guiltyParty)
            }
            IEEE754_UNORDERED
        }
    }
}