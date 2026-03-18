package com.decimal128.decimal

import kotlin.math.max

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
        ZER_INF -> {
            z.setNaN()
            ctx.signalInvalid(z, InvalidOperationReason.MUL_ZERO_BY_INFINITY)
        }
        INF_FNZ,
        FNZ_INF,
        INF_INF -> z.setInfinite(productSign)
        else -> z.setNaNOperand(x, y, ctx)
    }
    return z
}
