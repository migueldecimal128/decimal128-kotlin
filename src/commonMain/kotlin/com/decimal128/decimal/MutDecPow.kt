package com.decimal128.decimal

import kotlin.math.min

internal fun mutDecPowImpl(z: MutDec, x: MutDec, pow: Int, ctx: DecContext): MutDec {
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> return mutDecPowImplFNZ(z, x, pow, ctx)
        STEAL_TYP_ZER -> return mutDecPowImplZER(z, x, pow, ctx)
        STEAL_TYP_INF -> return mutDecPowImplINF(z, x, pow, ctx)
        else -> return z.setNanOperandFound(x, ctx)
    }
}

internal fun mutDecPowImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    TODO()
    //val xSteal = x.steal
    //when (stealTyp(xSteal)) {
//        STEAL_TYP_FNZ -> return mutDecPowImplFNZ(z, x, pow, ctx)
//        STEAL_TYP_ZER -> return mutDecPowImplZER(z, x, pow, ctx)
//        STEAL_TYP_INF -> return mutDecPowImplINF(z, x, pow, ctx)
//        else -> return z.setNanOperandFound(x, ctx)
//    }
}

private fun mutDecPowImplINF(z: MutDec, x: MutDec, pow: Int, ctx: DecContext): MutDec {
    val resultSign = x.signBit and (pow and 1) != 0
    return when {
        pow > 0 -> z.setInfinite(resultSign)
        pow < 0 -> z.setZero(resultSign)
        else -> z.setOne()
    }
}


private fun mutDecPowImplZER(z: MutDec, x: MutDec, pow: Int, ctx: DecContext): MutDec {
    val resultSign = x.signBit and (pow and 1) != 0
    when {
        pow > 0 -> {
            // clamp power to prevent overflow with multiplication
            val pClamp = min(pow, 9999)
            val zQ = x.qExp * pClamp
            return z.setZero(resultSign, zQ)
        }
        pow < 0 -> return ctx.signalDivByZero(z.setInfinite(resultSign))
        else -> return z.setOne()
    }
}

private fun mutDecPowImplFNZ(z: MutDec, x: MutDec, pow: Int, ctx: DecContext): MutDec {
    return when {
        pow > 0 -> when {
            pow == 1 -> z.set(x)
            pow == 2 -> z.setSquare(x, ctx)
            else -> mutDecPowImplFNZ_pow_GE_3(z, x, pow, ctx)
        }
        pow < 0 -> z.setReciprocal(mutDecPowImplFNZ(z, x, -pow, ctx), ctx)
        else -> z.setOne()
    }
}

private fun mutDecPowImplFNZ_pow_GE_3(z:MutDec, x: MutDec, pow: Int, ctx: DecContext): MutDec {
    verify { pow >= 3 }
    val m = if (z !== x) x else ctx.tmps.mdecArg1.set(x)
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

