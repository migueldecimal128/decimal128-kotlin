// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.InvalidCause.POWER_OF_NEG_BASE_NON_INTEGER_EXPONENT
import kotlin.math.min
import kotlin.math.max


internal fun d128PownImpl(x: Decimal, n: Int, ctx: DecContext = DecContext.current()): Decimal {
    val xSteal = x.steal
    return when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> pownImplFNZ(x, n, ctx)
        STEAL_TYP_ZER -> pownImplZER(x, n, ctx)
        STEAL_TYP_INF -> pownImplINF(x, n)
        else -> {
            // IEEE754-2019 p.63
            // pown (x, 0) is 1 if x is not a signaling NaN
            if (n == 0 && stealIsQNAN(xSteal))
                Decimal.ONE
            else
                nanOperandFound(x, ctx)
        }
    }
}

private inline fun pownImplINF(x: Decimal, n: Int): Decimal {
    val resultSign = x.signBit and (n and 1) != 0
    return when {
        n > 0 -> Decimal.infinity(resultSign)
        n < 0 -> Decimal.zero(resultSign)
        else -> Decimal.ONE
    }
}


private inline fun pownImplZER(x: Decimal, n: Int, ctx: DecContext): Decimal {
    val resultSign = x.signBit and (n and 1) != 0
    return when {
        n > 0 -> {
            // clamp power to prevent overflow with multiplication
            val pClamp = min(n, 9999)
            val zQ = x.qExp * pClamp
            Decimal.zero(resultSign, zQ)
        }
        n < 0 -> ctx.signalDivByZero(Decimal.infinity(resultSign))
        else -> Decimal.ONE
    }
}

private inline fun pownImplFNZ(x: Decimal, n: Int, ctx: DecContext): Decimal {
    when {
        n > 0 -> when {
            n == 1 -> return x
            n == 2 -> return x.square()
        }
        n == 0 -> return Decimal.ONE
    }
    when {
        d128CompareNumericMagnitude(x, Decimal.ONE) == 0 -> {
            val resultQExpLong = n.toLong() * x.qExp.toLong()
            val resultQExp = max(min(resultQExpLong, 0L), -33L).toInt()
            val negative = stealSignFlag(x.steal) && (n and 1) != 0
            return Decimal.one(negative, resultQExp)
        }
        d128CompareNumericMagnitude(x, Decimal.TEN) == 0 -> {
            val negative = stealSignFlag(x.steal) && (n and 1) != 0
            val preferredQExpLong = n.toLong() * stealQExp(x.steal).toLong()
            val minPreferredQExp = min(max((n - 33).toLong(), -32L), 0L)
            val preferredQExp = max(preferredQExpLong, minPreferredQExp).toInt()
            //val preferredQExp = max(min(preferredQExpLong, 1L), minPreferredQExp).toInt()
            //val preferredQExp = max(min(preferredQExpLong, 0L), minPreferredQExp).toInt()
            val ret = when {
                n > 6144 -> ctx.signalInexactOverflow(Decimal.infinity(negative))
                n < -6176 -> ctx.signalInexactUnderflow(Decimal.zero(negative, -6176))
                else -> tenPowN(negative, n, preferredQExp)
            }
            return ret
        }
    }

    val tmps = ctx.tmps
    val tmp1 = tmps.mdecBridge1.set(x)
    val tmp2 = mutDecPownImplFNZ(tmps.mdecBridgeResult, tmp1, n.toLong(), ctx)
    return Decimal.from(tmp2)
}

private fun tenPowN(sign: Boolean, n: Int, preferredQExp: Int): Decimal {
    val coeffExp = n - preferredQExp
    return when {
        coeffExp <= 0 || coeffExp > 33 -> {
            // coefficient is 1, qExp = n
            decFinalizeFinite(sign, n, 0L, 1L)
        }

        else -> {
            // coefficient is 10^coeffExp, qExp = preferredQExp
            val pow10Offset = (coeffExp shl 1) and POW10_BCE
            val dw1 = POW10[pow10Offset + 1]
            val dw0 = POW10[pow10Offset]
            decFinalizeFinite(sign, preferredQExp, dw1, dw0)
        }
    }
}

internal fun d128PowImpl(x: Decimal, y: Decimal): Decimal {
    val xSteal = x.steal; val ySteal = y.steal
    val ctx = DecContext.current()
    val yIsIntegral = d128IsExactIntegral(y)
    if (yIsIntegral) {
        val n = y.toLongTowardZeroNoFlags()
        if (n > Int.MIN_VALUE && n <= Int.MAX_VALUE)
            return d128PownImpl(x, n.toInt(), ctx)
        // large integer y - handle magnitude 1 base
        if (d128CompareNumericMagnitude(x, Decimal.ONE) == 0)
            return if (stealSignFlag(xSteal) && y.isOddIntegral())
                Decimal.NEG_ONEe0  // -1 ^ large integer - need parity check
            else
                Decimal.POS_ONEe0  // 1 ^ anything = 1
    }
    val signature = binopSignatureOf(xSteal, ySteal)
    return if (signature == FNZ_FNZ) {
        powFnzFnz(x, y, ctx)
    } else when (signature) {
        ZER_ZER,
        INF_ZER,
        FNZ_ZER -> Decimal.ONE
        ZER_INF -> if (stealSignFlag(ySteal)) Decimal.POS_INFINITY else Decimal.ZERO
        INF_INF,
        FNZ_INF -> powFnzInf(x, y, ctx)
        ZER_FNZ -> powZerFnz(x, y, yIsIntegral, ctx)
        INF_FNZ -> powInfFnz(x, y, yIsIntegral, ctx)
        else -> {
            if (stealIsPositiveFNZ(xSteal) && stealIsQNAN(ySteal) &&
                d128CompareNumericMagnitude(x, Decimal.ONE) == 0) {
                Decimal.ONE
            } else {
                nanOperandFound(x, y, ctx)
            }
        }
    }
}

private fun powFnzInf(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal
    val yNegative = stealSignFlag(y.steal)

    // IEEE754-2019 p.
    // pow (−1, ±∞) is 1 with no exception
    // pow (+1, y) is 1 for any y (even a quiet NaN)
    // pow(±1, ±∞) = 1
    if (d128CompareNumericMagnitude(x, Decimal.ONE) == 0)
        return Decimal.ONE

    // |x| < 1 when sciExp < 0, |x| > 1 when sciExp >= 0
    // (|x| == 1 already handled via pown delegation)
    val xLessThanOne = stealSciExp(xSteal) < 0

    return if (xLessThanOne xor yNegative) Decimal.ZERO else Decimal.POS_INFINITY
}

private fun powZerFnz(x: Decimal, y: Decimal, yIsIntegral: Boolean, ctx: DecContext): Decimal {
    val xSteal = x.steal
    val ySteal = y.steal
    val yNegative = stealSignFlag(ySteal)
    val xNegative = stealSignFlag(xSteal)

    return if (yNegative) {
        // pow(±0, y < 0) → ±∞, signal divideByZero
        // sign of result: negative only if x is -0 and y is odd integer
        val resultNegative = xNegative && yIsIntegral && y.isOddIntegral()
        ctx.signalDivByZero(Decimal.infinity(resultNegative))
    } else {
        // pow(±0, y > 0) → ±0
        // sign of result: negative only if x is -0 and y is odd integer
        val resultNegative = xNegative && yIsIntegral && y.isOddIntegral()
        Decimal.zero(resultNegative)
    }
}

private fun powInfFnz(x: Decimal, y: Decimal, yIsIntegral: Boolean, ctx: DecContext): Decimal {
    val xSteal = x.steal
    val ySteal = y.steal
    val xNegative = stealSignFlag(xSteal)
    val yNegative = stealSignFlag(ySteal)

    val resultNegative = xNegative && yIsIntegral && y.isOddIntegral()
    // pow(+∞, y)
    // pow(-∞, y)
    // sign of result: negative only if y is finite odd integer
    return if (yNegative) Decimal.zero(resultNegative) else Decimal.infinity(resultNegative)
}

private fun powFnzFnz(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal

    // negative base with non-integer exponent → invalid
    if (stealSignFlag(xSteal))
        return ctx.signalInvalidOperation(POWER_OF_NEG_BASE_NON_INTEGER_EXPONENT)

    // x^y = exp(y * ln(x))
    val mutX = ctx.tmps.mdecBridge1.set(x)
    val mutY = ctx.tmps.mdecBridge2.set(y)
    val log10X = ctx.tmps.mdecTrans1
    val yLog10X = ctx.tmps.mdecTrans2
    val result = ctx.tmps.mdecBridgeResult

    val ctx38 = DecContext.internal38()

    logImplFNZ(log10X, mutX, isLog10 = true, ctx38)
    yLog10X.setMul(mutY, log10X, ctx38)
    exp10ImplFNZ(result, yLog10X, ctx) // final operation is in ctx.precision

    // apply preferred exponent: floor(y × Q(x))
    if (result.isFiniteNonZero()) {
        val ySteal = y.steal
        val qX = stealQExp(xSteal)
        val preferredQExp = when {
            qX == 0 -> 0
            stealSciExp(ySteal) >= 5 ->
                if (stealSignFlag(ySteal)) Q_TINY else Q_MAX

            else -> {
                val tmp = ctx.tmps.mdecTrans1  // reuse, log10X no longer needed
                mutDecMulImpl(tmp, mutY, qX.toLong(), ctx38)
                tmp.setRoundToIntegralTowardNegative(tmp, ctx38)
                val clamped = max(min(tmp.toLongOrMinValue(), Q_MAX.toLong()), Q_TINY.toLong()).toInt()
                clamped
            }
        }

        result.setWithPreferredScale(result, -preferredQExp, ctx)
    }
    return Decimal.from(result)
}

internal fun d128CompoundImpl(x: Decimal, n: Int): Decimal {
    val ctx = DecContext.current()
    val tmps = ctx.tmps
    return Decimal.from(
        mutDecCompoundImpl(tmps.mdecBridgeResult, tmps.mdecBridge1.set(x), n, ctx)
    )
}

internal fun d128RootnImpl(x: Decimal, n: Int): Decimal {
    val ctx = DecContext.current()
    val tmps = ctx.tmps
    return Decimal.from(
        mutDecRootnImpl(tmps.mdecBridgeResult, tmps.mdecBridge1.set(x), n, ctx)
    )
}

