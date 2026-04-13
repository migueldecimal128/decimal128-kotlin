// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.InvalidOperationReason.POWER_OF_NEG_BASE_NON_INTEGER_EXPONENT
import kotlin.math.min
import kotlin.math.max

internal fun d128MulImpl(x: Decimal, y: Decimal): Decimal =
    d128MulImpl(x, y, DecContext.current())

internal fun d128MulImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal; val ySteal = y.steal
    val prodSignFlag = stealSignFlag(xSteal) xor stealSignFlag(ySteal)
    val prodExp = stealQExp(xSteal) + stealQExp(ySteal)
    val signature = binopSignatureOf(xSteal, ySteal)
    return if (signature == FNZ_FNZ) {
        val prodBitLen = stealBitLen(xSteal) + stealBitLen(ySteal)
        mulFnzFnz(prodSignFlag, prodExp, prodBitLen, x, y, ctx)
    } else when (signature) {
        ZER_ZER,
        ZER_FNZ,
        FNZ_ZER -> Decimal.zero(prodSignFlag, prodExp)

        ZER_INF,
        INF_ZER -> ctx.signalInvalidOperation(InvalidOperationReason.MAGNITUDE_SUBTRACTION_OF_INFINITIES)

        FNZ_INF,
        INF_FNZ,
        INF_INF -> Decimal.infinity(prodSignFlag)

        else -> nanOperandFound(x, y, ctx)
    }
}

// fast-path iff ...
//  product bitLen strictly less than decFormat.maxBitLen
//  (equal bitLen could overflow coefficient decimal limit)
//
//  exponent on the upper end is easy, must be < qMax
//  exponent on the low end must be >= eMin, not qTiny
//  anything in the range [qTiny, eMin) is subnormal
//  and must be scaled, so not on the fast-path
private inline fun mulFnzFnz(prodSignFlag: Boolean, prodExp: Int, prodBitLen: Int, x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    if (prodBitLen <= 128) {
        val p0 = x.dw0 * y.dw0
        val p1 = unsignedMulHi(x.dw0, y.dw0) + (x.dw1 * y.dw0) + (y.dw1 * x.dw0)
        return decFinalizeFinite(prodSignFlag, p1, p0, prodExp, ctx)
    }
    return mulFnzFnz256(x, y, ctx)
}

private fun mulFnzFnz256(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal; val ySteal = y.steal
    val tmps = ctx.tmps
    val m = tmps.mdecBridge1.set(x)
    val n = tmps.mdecBridge2.set(y)
    val p = tmps.mdecBridgeResult
    c256SetMul(p, m, n, tmps.pentad)
    p.finalizeFnz(
        stealSignFlag(xSteal) xor stealSignFlag(ySteal),
        stealQExp(xSteal) + stealQExp(ySteal), ctx)
    val d = Decimal.from(p)
    return d
}

internal fun d128SqrImpl(x: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> {
            if (stealBitLen(xSteal) <= 64) {
                val xDw0 = x.dw0
                val p0 = xDw0 * xDw0
                val p1 = unsignedMulHi(xDw0, xDw0) + ((x.dw1 * xDw0) shl 1)
                return decFinalizeFinite(false, p1, p0, stealQExp(xSteal) shl 1, ctx)
            } else {
                val tmps = ctx.tmps
                val m = tmps.mdecBridge1.set(x)
                val z = tmps.mdecBridge2
                z.setSquare(m, ctx)
                return Decimal.from(z)
            }
        }
        STEAL_TYP_ZER -> return Decimal.zero(false, stealQExp(xSteal) shl 1)
        STEAL_TYP_INF -> return Decimal.POS_INFINITY
        else -> return nanOperandFound(x, ctx)
    }
}

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
    val tmp2 = mutDecPowNImplFNZ(tmps.mdecBridgeResult, tmp1, n, ctx)
    return Decimal.from(tmp2)
}

private fun tenPowN(sign: Boolean, n: Int, preferredQExp: Int): Decimal {
    val coeffExp = n - preferredQExp
    return when {
        coeffExp <= 0 || coeffExp > 33 -> {
            // coefficient is 1, qExp = n
            decFinalizeFinite(sign, 0L, 1L, n)
        }

        else -> {
            // coefficient is 10^coeffExp, qExp = preferredQExp
            val pow10Offset = (coeffExp shl 1) and POW10_BCE
            val dw1 = POW10[pow10Offset + 1]
            val dw0 = POW10[pow10Offset]
            decFinalizeFinite(sign, dw1, dw0, preferredQExp)
        }
    }
}

internal fun d128PowImpl(x: Decimal, y: Decimal): Decimal {
    val ctx = DecContext.current()
    val yIsIntegral = d128IsExactInteger(y, ctx)
    if (yIsIntegral) {
        val n = y.toLongOrMinValue()
        if (n > Int.MIN_VALUE && n <= Int.MAX_VALUE)
            return d128PownImpl(x, n.toInt(), ctx)
    }
    val xSteal = x.steal; val ySteal = y.steal
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
        else -> nanOperandFound(x, y, ctx)
    }
}

private fun powFnzInf(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val xSteal = x.steal
    val yNegative = stealSignFlag(y.steal)

    // |x| < 1 when sciExp < 0, |x| > 1 when sciExp >= 0
    // (|x| == 1 already handled via pown delegation)
    val xLessThanOne = stealSciExp(xSteal) < 0

    return if (xLessThanOne)
        if (yNegative) Decimal.POS_INFINITY else Decimal.ZERO
    else
        if (yNegative) Decimal.ZERO else Decimal.POS_INFINITY
}

private fun powZerFnz(x: Decimal, y: Decimal, yIsIntegral: Boolean, ctx: DecContext): Decimal {
    val xSteal = x.steal
    val ySteal = y.steal
    val yNegative = stealSignFlag(ySteal)
    val xNegative = stealSignFlag(xSteal)

    return if (yNegative) {
        // pow(±0, y < 0) → ±∞, signal divideByZero
        // sign of result: negative only if x is -0 and y is odd integer
        val resultNegative = xNegative && yIsIntegral && y.isOddInteger()
        ctx.signalDivByZero(Decimal.infinity(resultNegative))
    } else {
        // pow(±0, y > 0) → ±0
        // sign of result: negative only if x is -0 and y is odd integer
        val resultNegative = xNegative && yIsIntegral && y.isOddInteger()
        Decimal.zero(resultNegative)
    }
}

private fun powInfFnz(x: Decimal, y: Decimal, yIsIntegral: Boolean, ctx: DecContext): Decimal {
    val xSteal = x.steal
    val ySteal = y.steal
    val xNegative = stealSignFlag(xSteal)
    val yNegative = stealSignFlag(ySteal)

    return if (!xNegative) {
        // pow(+∞, y)
        if (yNegative) Decimal.ZERO else Decimal.POS_INFINITY
    } else {
        // pow(-∞, y)
        // sign of result: negative only if y is finite odd integer
        val resultNegative = yIsIntegral && y.isOddInteger()
        if (yNegative)
            Decimal.zero(resultNegative)
        else
            Decimal.infinity(resultNegative)
    }
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

    return Decimal.from(result)
}