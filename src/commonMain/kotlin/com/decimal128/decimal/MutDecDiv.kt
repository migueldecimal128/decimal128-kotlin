package com.decimal128.decimal

import com.decimal128.decimal.roundAndFinalize
import kotlin.math.max

fun mutDecDivImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val binopSignature = binopSignatureOf(x.type, y.type)
    val quotientSign = x.sign xor y.sign
    if (binopSignature == FNZ_FNZ)
        z.roundAndFinalize(MagnitudeDiv.magDivFnzFnz(z, quotientSign, x, y, ctx), ctx)
    else when (binopSignature) {
        ZER_ZER -> ctx.setNanSignalInvalid(z, InvalidOperationReason.DIV_ZERO_BY_ZERO)
        ZER_FNZ -> z.setZero(quotientSign, x.qExp - y.qExp, ctx)
        ZER_INF,
        FNZ_INF -> z.setZero(quotientSign, Q_TINY, ctx)
        FNZ_ZER -> ctx.signalDivByZero(z.setInfinite(quotientSign))
        INF_ZER,
        INF_FNZ -> z.setInfinite(quotientSign)
        INF_INF -> ctx.setNanSignalInvalid(z, InvalidOperationReason.DIV_INF_BY_INF)
        else -> z.setNaNOperand(x, y, ctx)
    }
    return z
}

