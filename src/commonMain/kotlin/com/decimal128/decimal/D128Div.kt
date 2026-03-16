package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.zero
import kotlin.math.min

internal fun d128DivImpl(x: Decimal, y: Decimal): Decimal =
    d128DivImpl(x, y, DecContext.current())

internal fun d128DivImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal; val ySteal = y.steal
    val signature = binopSignatureOf(xSteal, ySteal)
    return if (signature == FNZ_FNZ) {
        divFnzFnz(x, y, ctx)
    } else {
        val resultSign = stealSignFlag(xSteal) xor stealSignFlag(ySteal)
        when (signature) {
            ZER_ZER -> divZeroZero(x, y, ctx)
            ZER_FNZ -> zero(resultSign, x.qExp() - y.qExp(), ctx)
            ZER_INF,
            FNZ_INF -> zero(resultSign, ctx.qTiny, ctx)

            FNZ_ZER -> divFnzZero(x, y, ctx)

            INF_ZER,
            INF_FNZ -> Decimal.infinity(resultSign)
            INF_INF -> divInfInf(x, y, ctx)

            else -> nanOperandFound(x, y, ctx)
        }
    }
}

private fun divZeroZero(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    ctx.signalInvalid(InvalidOpReason.DIV_ZERO_BY_ZERO, Decimal.NaN)

private fun divInfInf(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    ctx.signalInvalid(InvalidOpReason.DIV_INF_BY_INF, Decimal.NaN)

private fun divFnzZero(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val r = if (x.sign xor y.sign) Decimal.NEG_INFINITY else Decimal.POS_INFINITY
    return ctx.signalDivByZero(r)
}

private fun divFnzFnz(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    // not much can be done here ... perhaps some
    return divFnzFnz256(x, y, ctx)
}

private fun divFnzFnz256(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val dividend = ctx.tmps.mdecBridge1.set(x)
    val divisor = ctx.tmps.mdecBridge2.set(y)
    val quotient = ctx.tmps.mdecResult
    val residue = MagnitudeDiv.magDivFnzFnz(quotient, x.sign xor y.sign, dividend, divisor, ctx)
    quotient.roundAndFinalize(residue, ctx)
    return Decimal.from(quotient, ctx)
}

internal fun divIntImpl(x: Decimal, y: Decimal): Decimal =
    divIntImpl(x, y, DecContext.current())

internal fun divIntImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val signature = binopSignatureOf(x.steal, y.steal)
    return if (signature == FNZ_FNZ) {
        divIntFnzFnz(x, y, ctx)
    } else when (signature) {
        ZER_ZER -> divZeroZero(x, y, ctx)
        ZER_FNZ,
        ZER_INF,
        FNZ_INF -> Decimal.zero(x.sign xor y.sign)

        FNZ_ZER -> divFnzZero(x, y, ctx)

        INF_ZER,
        INF_FNZ -> Decimal.infinity(x.sign xor y.sign)
        INF_INF -> divInfInf(x, y, ctx)

        else -> nanOperandFound(x, y, ctx)
    }
    // otherwise, divInt() == div()
}

private fun divIntFnzFnz(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val dividend = ctx.tmps.mdecBridge1.set(x)
    val divisor = ctx.tmps.mdecBridge2.set(y)
    val quotient = ctx.tmps.mdecResult.setDivIntFnzFnz(dividend, divisor, ctx)
    return Decimal.from(quotient)
}

internal fun d128RemTruncImpl(x: Decimal, y: Decimal): Decimal =
    remImpl(isTrunc = true, x, y, DecContext.current())

internal fun d128RemTruncImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    remImpl(isTrunc = true, x, y, ctx)

internal fun d128RemNearImpl(x: Decimal, y: Decimal): Decimal =
    remImpl(isTrunc = false, x, y, DecContext.current())

internal fun remNearImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    remImpl(isTrunc = false, x, y, ctx)

internal fun remImpl(isTrunc: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val signature = binopSignatureOf(x.steal, y.steal)
    return if (signature == FNZ_FNZ) {
        remFnzFnz(isTrunc, x, y, ctx)
    } else when (signature) {
        ZER_FNZ -> zero(x.sign, min(x.qExp(), y.qExp()), ctx)
        FNZ_INF,
        ZER_INF -> x

        ZER_ZER,
        FNZ_ZER,
        INF_ZER,
        INF_FNZ,
        INF_INF -> ctx.signalInvalid(InvalidOpReason.DIV_INF_BY_INF, Decimal.NaN)

        else -> nanOperandFound(x, y, ctx)
    }
}

private fun remFnzFnz(isTrunc: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val dividend = ctx.tmps.mdecBridge1.set(x)
    val divisor = ctx.tmps.mdecBridge2.set(y)
    val quotient =
        if (isTrunc)
            ctx.tmps.mdecResult.setRemainderTruncate(dividend, divisor, ctx)
        else
            ctx.tmps.mdecResult.setRemainderNear(dividend, divisor, ctx)
    return Decimal.from(quotient)
}
