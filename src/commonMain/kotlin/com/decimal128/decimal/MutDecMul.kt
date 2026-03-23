package com.decimal128.decimal

internal fun mutDecMulImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val qX = x.qExp
    val qY = y.qExp
    val productQExp = qX + qY
    val productSign = x.sign xor y.sign
    val binopSignature = binopSignatureOf(x.type, y.type)
    if (binopSignature == FNZ_FNZ) {
        c256SetMul(z, x, y, ctx.tmps.pentad1)
        z.finalizeFnz(productSign, productQExp, ctx)
    } else when (binopSignature) {
        FNZ_ZER,
        ZER_FNZ,
        ZER_ZER -> z.setZero(productSign, productQExp)
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
    if (type == STEAL_TYP_FNZ) {
        c256SetSqr(z, x, ctx.tmps.pentad1)
        return z.finalizeFnz(false, qExp, ctx)
    }
    if (type == STEAL_TYP_ZER)
        return z.setZero(qExp = qExp)
    if (type == STEAL_TYP_INF)
        return z.setInfinite(false)
    return z.setNaN(x, ctx)
}

internal fun mutDecSqrtImpl(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
    val qX = x.qExp
    when (stealTyp(x.type)) {
        STEAL_TYP_FNZ -> {
            if (! x.sign) {
                return mutDecSqrtPosFnz(z, x, ctx)
            } else {
                ctx.setNanSignalInvalid(z, InvalidOperationReason.SQUARE_ROOT_OF_NEG_FINITE_NON_ZERO)
            }
        }
        STEAL_TYP_ZER -> {
            // IEEE754-2019 6.3 p.50
            // Except that squareRoot(−0) shall be −0,
            // every numeric squareRoot result shall have a positive sign.
            z.setZero(false, qX shr 1)
        }
        STEAL_TYP_INF -> {
            if (! x.sign) {
                z.setInfinite(false)
            } else {
                ctx.setNanSignalInvalid(z, InvalidOperationReason.SQUARE_ROOT_OF_NEG_INFINITY)
            }
        }
        else -> z.setNaNOperand(x, ctx)
    }
    return z

}