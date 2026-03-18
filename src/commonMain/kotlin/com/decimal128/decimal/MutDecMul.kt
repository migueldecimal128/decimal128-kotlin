package com.decimal128.decimal

internal fun mutDecMulImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val qX = x.qExp
    val qY = y.qExp
    val productQExp = qX + qY
    val productSign = x.sign xor y.sign
    val binopSignature = binopSignatureOf(x.type, y.type)
    if (binopSignature == FNZ_FNZ) {
        c256SetMul(z, x, y, ctx.tmps.pentad1)
        z.type = STEAL_TYPE_FNZ
        z.qExp = productQExp
        z.sign = productSign
        z.finalize(ctx)
    } else when (binopSignature) {
        FNZ_ZER,
        ZER_FNZ,
        ZER_ZER -> z.setZero(productSign, productQExp, ctx)
        INF_ZER,
        ZER_INF -> ctx.setNanSignalInvalid(z, InvalidOperationReason.MUL_ZERO_BY_INFINITY)
        INF_FNZ,
        FNZ_INF,
        INF_INF -> z.setInfinite(productSign)
        else -> z.setNaNOperand(x, y, ctx)
    }
    return z
}

internal fun mutDecSqrImpl(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
    val type = x.type
    val qExp = x.qExp shl 1
    if (type == STEAL_TYPE_FNZ) {
        c256SetSqr(z, x, ctx.tmps.pentad1)
        z.type = STEAL_TYPE_FNZ
        z.qExp = qExp
        z.sign = false
        return z.finalize(ctx)
    }
    if (type == STEAL_TYPE_ZER)
        return z.setZero(false, qExp, ctx)
    if (type == STEAL_TYPE_INF)
        return z.setInfinite(false)
    return z.setNaN(x, ctx)
}

