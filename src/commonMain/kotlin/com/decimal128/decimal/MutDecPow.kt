package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min

internal fun mutDecPownImpl(z: MutDec, x: MutDec, n: Int, ctx: DecContext = DecContext.current()): MutDec {
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> return mutDecPownImplFNZ(z, x, n, ctx)
        STEAL_TYP_ZER -> return mutDecPownImplZER(z, x, n, ctx)
        STEAL_TYP_INF -> return mutDecPownImplINF(z, x, n, ctx)
        else -> {
            // IEEE754-2019 p.63
            // pown (x, 0) is 1 if x is not a signaling NaN
            if (n == 0 && stealIsQNAN(xSteal))
                return z.setOne()
            else
                return z.setNanOperandFound(x, ctx)
        }
    }
}

private fun mutDecPownImplINF(z: MutDec, x: MutDec, n: Int, ctx: DecContext): MutDec {
    val resultSign = x.signBit and (n and 1) != 0
    return when {
        n > 0 -> z.setInfinite(resultSign)
        n < 0 -> z.setZero(resultSign)
        else -> z.setOne()
    }
}


private fun mutDecPownImplZER(z: MutDec, x: MutDec, n: Int, ctx: DecContext): MutDec {
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

internal fun mutDecPownImplFNZ(z: MutDec, x: MutDec, n: Int, ctx: DecContext): MutDec {
    return when {
        n == 0 -> z.setOne()
        n == 1 -> z.set(x)
        n == 2 -> z.setSquare(x, ctx)
        mutDecCompareNumericMagnitude(x, MutDec.ONE) == 0 -> {
            val resultQExpLong = n.toLong() * x.qExp.toLong()
            val resultQExp = max(min(resultQExpLong, 0L), -33L).toInt()
            val negative = stealSignFlag(x.steal) && (n and 1) != 0
            z.setOne(negative, resultQExp)
        }
        mutDecCompareNumericMagnitude(x, MutDec.TEN) == 0 -> {
            val negative = stealSignFlag(x.steal) && (n and 1) != 0
            val preferredQExpLong = n.toLong() * stealQExp(x.steal).toLong()
            val minPreferredQExp = min(max((n - 33).toLong(), -32L), 0L)
            val preferredQExp = max(preferredQExpLong, minPreferredQExp).toInt()
            //val preferredQExp = max(min(preferredQExpLong, 1L), minPreferredQExp).toInt()
            //val preferredQExp = max(min(preferredQExpLong, 0L), minPreferredQExp).toInt()
            val ret = when {
                n > 6144 -> ctx.setInfinitySignalInexactOverflow(z, negative)
                n < -6176 -> ctx.setZeroSignalInexactUnderflow(z)
                else -> setTenPowN(z, negative, n, preferredQExp)
            }
            return ret
        }
        n < 0 -> {
            val reciprocal = MutDec().setReciprocal(x, ctx)
            mutDecPownImplFNZ(z, reciprocal, -n, ctx)
        }
        else -> mutDecPownImplFNZ_pow_GE_3(z, x, n, ctx)
    }
}

private fun setTenPowN(z: MutDec, sign: Boolean, n: Int, preferredQExp: Int): MutDec {
    verify { n >= Q_TINY && n <= E_MAX } // yes, E_MAX
    val coeffExp = n - preferredQExp
    return when {
        coeffExp <= 0 || coeffExp > 33 -> {
            // coefficient is 1, qExp = n
            z.setOne(sign)
            z.finalizeFnz(sign, n)
        }
        else -> {
            // coefficient is 10^coeffExp, qExp = preferredQExp
            val pow10Offset = (coeffExp shl 1) and POW10_BCE
            val dw1 = POW10[pow10Offset + 1]
            val dw0 = POW10[pow10Offset]
            z.c256Set128(dw1, dw0)
            z.finalizeFnz(sign, preferredQExp)
        }
    }
}

private fun mutDecPownImplFNZ_pow_GE_3(z:MutDec, x: MutDec, pow: Int, ctx: DecContext): MutDec {
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

