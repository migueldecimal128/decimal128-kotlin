package com.decimal128.decimal

import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO

internal fun mutDecDivImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
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

private fun setDivIntFnzFnz(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
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

