package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.Decimal.Companion.bothFnz
import com.decimal128.decimal.Decimal.Companion.newZero
import kotlin.math.min

internal fun divImpl(x: Decimal, y: Decimal): Decimal =
    divImpl(x, y, DecContext.current())

internal fun divImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    return if (bothFnz(x, y)) {
        divFnzFnz(x, y, ctx)
    } else when (BinopSignature.of(x, y)) {
        ZER_ZER -> divZeroZero(x, y, ctx)
        ZER_FNZ -> newZero(x.sign xor y.sign, x.qExp - y.qExp, ctx)
        ZER_INF,
        FNZ_INF -> newZero(x.sign xor y.sign, ctx.qTiny, ctx)

        FNZ_ZER -> divFnzZero(x, y, ctx)
        FNZ_FNZ -> throw IllegalStateException()

        INF_ZER,
        INF_FNZ -> Decimal.infinity(x.sign xor y.sign)
        INF_INF -> divInfInf(x, y, ctx)

        NAN_FOUND -> nanOperandFound(x, y, ctx)
    }
}

private fun divZeroZero(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    ctx.signalInvalid(Decimal.NaN)

private fun divInfInf(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    ctx.signalInvalid(Decimal.NaN)

private fun divFnzZero(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val r = if (x.sign xor y.sign) Decimal.NEG_INFINITY else Decimal.POS_INFINITY
    return ctx.signalDivByZero(r)
}

private fun divFnzFnz(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val dividend = ctx.decTemps.mdecArg1.set(x)
    val divisor = ctx.decTemps.mdecArg2.set(y)
    val quotient = ctx.decTemps.mdecResult.setDiv(dividend, divisor, ctx)
    return Decimal.from(quotient)
}

internal fun divIntImpl(x: Decimal, y: Decimal): Decimal =
    divIntImpl(x, y, DecContext.current())

internal fun divIntImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    return if (bothFnz(x, y)) {
        divIntFnzFnz(x, y, ctx)
    } else when (BinopSignature.of(x, y)) {
        ZER_ZER -> divZeroZero(x, y, ctx)
        ZER_FNZ,
        ZER_INF,
        FNZ_INF -> Decimal.zero(x.sign xor y.sign)

        FNZ_ZER -> divFnzZero(x, y, ctx)
        FNZ_FNZ -> throw IllegalStateException()

        INF_ZER,
        INF_FNZ -> Decimal.infinity(x.sign xor y.sign)
        INF_INF -> divInfInf(x, y, ctx)

        NAN_FOUND -> nanOperandFound(x, y, ctx)
    }
    // otherwise, divInt() == div()
}

private fun divIntFnzFnz(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val dividend = ctx.decTemps.mdecArg1.set(x)
    val divisor = ctx.decTemps.mdecArg2.set(y)
    val quotient = ctx.decTemps.mdecResult.setDivInt(dividend, divisor, ctx)
    return Decimal.from(quotient)
}

internal fun remTruncImpl(x: Decimal, y: Decimal): Decimal =
    remImpl(isTrunc = true, x, y, DecContext.current())

internal fun remTruncImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    remImpl(isTrunc = true, x, y, ctx)

internal fun remNearImpl(x: Decimal, y: Decimal): Decimal =
    remImpl(isTrunc = false, x, y, DecContext.current())

internal fun remNearImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    remImpl(isTrunc = false, x, y, ctx)

internal fun remImpl(isTrunc: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    return if (bothFnz(x, y)) {
        remFnzFnz(isTrunc, x, y, ctx)
    } else when (BinopSignature.of(x, y)) {
        ZER_FNZ -> newZero(x.sign, min(x.qExp, y.qExp), ctx)
        FNZ_INF,
        ZER_INF -> x

        FNZ_FNZ, // Illegal state, we checked above
        ZER_ZER,
        FNZ_ZER,
        INF_ZER,
        INF_FNZ,
        INF_INF -> ctx.signalInvalid(Decimal.NaN)

        NAN_FOUND -> nanOperandFound(x, y, ctx)
    }
}

private fun remFnzFnz(isTrunc: Boolean, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val dividend = ctx.decTemps.mdecArg2.set(x)
    val divisor = ctx.decTemps.mdecArg3.set(y)
    val quotient =
        if (isTrunc)
            ctx.decTemps.mdecResult.setRemainderTruncate(dividend, divisor, ctx)
        else
            ctx.decTemps.mdecResult.setRemainderNear(dividend, divisor, ctx)
    return Decimal.from(quotient)
}
