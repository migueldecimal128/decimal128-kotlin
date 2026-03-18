package com.decimal128.decimal

import kotlin.math.max

fun mutDecMulImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val qX = x.qExp
    val qY = y.qExp
    val productSign = x.sign xor y.sign
    val qMaxXY = max(qX, qY)
    when {
        qMaxXY < MIN_SPECIAL_VALUE -> {
            if (x.isZero() || y.isZero()) {
                z.c256SetZero()
                z.type = STEAL_TYPE_ZER
            } else {
                c256SetMul(z, x, y, ctx.tmps.pentad1)
                z.type = STEAL_TYPE_FNZ
            }
            z.qExp = x.qExp + y.qExp
            z.sign = productSign
            return z.finalize(ctx)
        }
        qMaxXY == NON_FINITE_INF -> {
            if (x.isZero() || y.isZero()) {
                z.setNaN()
                return ctx.signalInvalid(z)
            } else {
                z.setInfinite(productSign)
            }
        }
        else -> z.setNaNOperand(x, y, ctx)
    }
    return z
}
