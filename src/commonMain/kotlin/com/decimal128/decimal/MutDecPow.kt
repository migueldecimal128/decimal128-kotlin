package com.decimal128.decimal

import kotlin.math.min

internal fun mutDecPowNImpl(z: MutDec, x: MutDec, n: Int, ctx: DecContext): MutDec {
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> return mutDecPowNImplFNZ(z, x, n, ctx)
        STEAL_TYP_ZER -> return mutDecPowNImplZER(z, x, n, ctx)
        STEAL_TYP_INF -> return mutDecPowNImplINF(z, x, n, ctx)
        else -> return z.setNanOperandFound(x, ctx)
    }
}

private fun mutDecPowNImplINF(z: MutDec, x: MutDec, n: Int, ctx: DecContext): MutDec {
    val resultSign = x.signBit and (n and 1) != 0
    return when {
        n > 0 -> z.setInfinite(resultSign)
        n < 0 -> z.setZero(resultSign)
        else -> z.setOne()
    }
}


private fun mutDecPowNImplZER(z: MutDec, x: MutDec, n: Int, ctx: DecContext): MutDec {
    val resultSign = x.signBit and (n and 1) != 0
    when {
        n > 0 -> {
            // clamp power to prevent overflow with multiplication
            val pClamp = min(n, 9999)
            val zQ = x.qExp * pClamp
            return z.setZero(resultSign, zQ)
        }
        n < 0 -> return ctx.signalDivByZero(z.setInfinite(resultSign))
        else -> return z.setOne()
    }
}

private fun mutDecPowNImplFNZ(z: MutDec, x: MutDec, n: Int, ctx: DecContext): MutDec {
    return when {
        n > 0 -> when {
            n == 1 -> z.set(x)
            n == 2 -> z.setSquare(x, ctx)
            else -> mutDecPowNImplFNZ_pow_GE_3(z, x, n, ctx)
        }
        n < 0 -> z.setReciprocal(mutDecPowNImplFNZ(z, x, -n, ctx), ctx)
        else -> z.setOne()
    }
}

private fun mutDecPowNImplFNZ_pow_GE_3(z:MutDec, x: MutDec, pow: Int, ctx: DecContext): MutDec {
    verify { pow >= 3 }
    val m = ctx.tmps.mdecDivRemPowCtzd.set(x)
    // Left-to-right binary exponentiation
    // Find the highest bit below the MSB
    val resultSign = x.signBit and (pow and 1) != 0
    val estimatedResultExp = x.qExp.toLong() * pow.toLong()
    when {
        estimatedResultExp > 6200 ->
            return ctx.signalInexactOverflow(z.setInfinite(resultSign))
        estimatedResultExp < -6250 ->
            return ctx.signalInexactUnderflow(z.setZero(resultSign, Q_TINY))
    }
    var singleBitMask = 1 shl (30 - pow.countLeadingZeroBits())
    z.set(m)
    while (singleBitMask != 0) {
        z.setSquare(z, ctx)
        if (pow and singleBitMask != 0)
            z.setMul(z, m, ctx)
        singleBitMask = singleBitMask shr 1
    }
    return z
}

internal fun mutDecPowImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    TODO()
    /*
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> return mutDecPowNImplFNZ(z, x, pow, ctx)
        STEAL_TYP_ZER -> return mutDecPowNImplZER(z, x, pow, ctx)
        STEAL_TYP_INF -> return mutDecPowNImplINF(z, x, pow, ctx)
        else -> return z.setNanOperandFound(x, ctx)
    }

     */
}

