package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.Decimal.Companion.bothFnz
import com.decimal128.decimal.Decimal.Companion.newZero

internal fun divImpl(x: Decimal, y: Decimal): Decimal =
    divImpl(x, y, DecContext.current())

internal fun divImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    return if (bothFnz(x, y)) {
        divFnzFnz(x, y, ctx)
    } else when (BinopSignature.of(x, y)) {
        ZER_ZER -> divZeroZero(x, y, ctx)
        ZER_FNZ -> newZero(x.sign xor y.sign, x.qExp - y.qExp, ctx)
        ZER_INF -> newZero(x.sign xor y.sign, ctx.qTiny, ctx)

        FNZ_ZER -> divFnzZero(x, y, ctx)
        FNZ_FNZ -> throw IllegalStateException()
        FNZ_INF -> newZero(x.sign xor y.sign, ctx.qTiny, ctx)

        INF_ZER -> if (x.sign xor y.sign) Decimal.NEG_INFINITY else Decimal.POS_INFINITY
        INF_FNZ -> if (x.sign xor y.sign) Decimal.NEG_INFINITY else Decimal.POS_INFINITY
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
