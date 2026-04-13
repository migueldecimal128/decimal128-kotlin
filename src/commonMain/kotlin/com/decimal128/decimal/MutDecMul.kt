package com.decimal128.decimal

import kotlin.math.absoluteValue
import kotlin.math.min

internal fun mutDecMulImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    val xQ = stealQExp(xSteal)
    val yQ = stealQExp(ySteal)
    val productQExp = xQ + yQ
    val productSign = stealSignFlag(xSteal) xor stealSignFlag(ySteal)
    val binopSignature = binopSignatureOf(x.steal, y.steal)
    if (binopSignature == FNZ_FNZ) {
        c256SetMul(z, x, y, ctx.tmps.pentad)
        z.finalizeFnz(productSign, productQExp, ctx)
    } else when (binopSignature) {
        FNZ_ZER,
        ZER_FNZ,
        ZER_ZER -> z.setZero(productSign, productQExp)
        INF_ZER,
        ZER_INF -> ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.MUL_ZERO_BY_INFINITY)
        INF_FNZ,
        FNZ_INF,
        INF_INF -> z.setInfinite(productSign)
        else -> z.setNanOperandFound(x, y, ctx)
    }
    return z
}

internal fun mutDecMulImpl(z: MutDec, x: MutDec, l: Long, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val prodQ = stealQExp(xSteal)
    val prodSign = stealSignFlag(xSteal) xor (l < 0L)
    return when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> {
            val dw = l.absoluteValue
            val bitLen = 64 - dw.countLeadingZeroBits()
            c256SetMul(z, x, bitLen, dw, ctx.tmps.pentad)
            z.finalizeFnz(prodSign, prodQ, ctx)
        }
        STEAL_TYP_ZER -> z.setZero(prodSign, prodQ)
        STEAL_TYP_INF -> z.setInfinite(prodSign)
        else -> z.setNanOperandFound(x, ctx)
    }
}


internal fun mutDecSqrImpl(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val typ = stealTyp(xSteal)
    val sqrQExp = stealQExp(xSteal) shl 1
    if (typ == STEAL_TYP_FNZ) {
        c256SetSqr(z, x, ctx.tmps.pentad)
        return z.finalizeFnz(false, sqrQExp, ctx)
    }
    if (typ == STEAL_TYP_ZER)
        return z.setZero(false, sqrQExp)
    if (typ == STEAL_TYP_INF)
        return z.setInfinite(false)
    return z.setNanOperandFound(x, ctx)
}

internal fun mutDecSqrtImpl(z: MutDec, x: MutDec, ctx: DecContext, reduceToPreferredQExp: Boolean): MutDec {
    val xSteal = x.steal
    val xQ = stealQExp(xSteal)
    val xSign = stealSignFlag(xSteal)
    when (stealTyp(x.steal)) {
        STEAL_TYP_FNZ -> {
            if (! xSign) {
                return mutDecSqrtPosFnz(z, x, ctx, reduceToPreferredQExp)
            } else {
                ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.SQUARE_ROOT_OF_NEG_FINITE_NON_ZERO)
            }
        }
        STEAL_TYP_ZER -> {
            // IEEE754-2019 6.3 p.50
            // Except that squareRoot(−0) shall be −0,
            // every numeric squareRoot result shall have a positive sign.
            z.setZero(false, xQ shr 1)
        }
        STEAL_TYP_INF -> {
            if (! xSign) {
                z.setInfinite(false)
            } else {
                ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.SQUARE_ROOT_OF_NEG_INFINITY)
            }
        }
        else -> z.setNanOperandFound(x, ctx)
    }
    return z
}

