package com.decimal128.decimal

import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

internal fun mutDecPownImpl(z: MutDec, x: MutDec, n: Int, ctx: DecContext = DecContext.current()): MutDec {
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> return mutDecPownImplFNZ(z, x, n.toLong(), ctx)
        STEAL_TYP_ZER -> return mutDecPownImplZER(z, x, n, ctx)
        STEAL_TYP_INF -> return mutDecPownImplINF(z, x, n, ctx)
        else -> { // STEAL_TYP_NAN
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

internal fun mutDecPownImplFNZ(z: MutDec, x: MutDec, lpow: Long, ctx: DecContext): MutDec {
    val estimatedQExpLong = lpow * stealQExp(x.steal).toLong()
    val negative = (stealSignBit(x.steal) and lpow.toInt()) != 0
    return when {
        lpow == 0L -> z.setOne()
        lpow == 1L -> z.set(x)
        lpow == 2L -> z.setSquare(x, ctx)
        mutDecCompareNumericMagnitude(x, MutDec.ONE) == 0 -> {
            val resultQExp = max(min(estimatedQExpLong, 0L), -33L).toInt()
            z.setOne(negative, resultQExp)
        }
        mutDecCompareNumericMagnitude(x, MutDec.TEN) == 0 -> {
            val minPreferredQExp = min(max(lpow - 33L, -32L), 0L)
            val preferredQExp = max(estimatedQExpLong, minPreferredQExp).toInt()
            val ret = when {
                lpow > 6144L -> ctx.setInfinitySignalInexactOverflow(z, negative)
                lpow < -6176L -> ctx.setZeroSignalInexactUnderflow(z)
                else -> setTenPowN(z, negative, lpow.toInt(), preferredQExp)
            }
            return ret
        }
        lpow < 0 -> {
            val absPow = -lpow
            val estimatedResultExp = stealSciExp(x.steal).toLong() * -lpow
            if (estimatedResultExp < 6100L) {
                // raise first, then reciprocal - avoids subnormal underflow
                mutDecPownImplFNZ(z, x, absPow, ctx)
                z.setReciprocal(z, ctx)
            } else {
                // reciprocal first, then raise
                val reciprocal = ctx.tmps.mdecTrans1.setReciprocal(x, ctx)
                mutDecPownImplFNZ(z, reciprocal, absPow, ctx)
            }
        }
        else -> mutDecPownImplFNZ_pow_GE_3(z, x, lpow, ctx)
    }
}

private fun mutDecPownImplFNZ_pow_GE_3(z:MutDec, x: MutDec, lpow: Long, ctx: DecContext): MutDec {
    verify { lpow >= 3L }
    val m = ctx.tmps.mdecDivRemPowCtzd.set(x)
    // Left-to-right binary exponentiation
    // Find the highest bit below the MSB
    val resultSign = (x.signBit and lpow.toInt()) != 0
    val estimatedResultExp = stealSciExp(x.steal).toLong() * lpow
    when {
        estimatedResultExp > 6200L ->
            return ctx.signalInexactOverflow(z.setInfinite(resultSign))
        estimatedResultExp < -6250L ->
            return ctx.signalInexactUnderflow(z.setZero(resultSign, Q_TINY))
    }
    var singleBitMask = 1L shl (62 - lpow.countLeadingZeroBits())
    z.set(m)
    while (singleBitMask != 0L) {
        z.setSquare(z, ctx)
        if (lpow and singleBitMask != 0L)
            z.setMul(z, m, ctx)
        singleBitMask = singleBitMask shr 1
    }
    return z
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

internal fun mutDecPowImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal;
    val ySteal = y.steal
    val ctx = DecContext.current()
    val yIsIntegral: Boolean = y.isExactIntegral()
    if (yIsIntegral) {
        val n = y.toLongOrMinValue()
        if (n > Int.MIN_VALUE && n <= Int.MAX_VALUE)
            return mutDecPownImpl(z, x, n.toInt(), ctx)
        // large integer y - handle magnitude 1 base
        if (mutDecCompareNumericMagnitude(x, MutDec.ONE) == 0)
            return z.setOne(stealSignFlag(xSteal) && y.isOddIntegral())
    }
    val signature: Int = binopSignatureOf(xSteal, ySteal)
    if (signature == FNZ_FNZ) {
        return mutDecPowFnzFnz(z, x, y, ctx)
    } else when (signature) {
        ZER_ZER,
        INF_ZER,
        FNZ_ZER -> return z.setOne()
        ZER_INF -> return if (stealSignFlag(ySteal)) z.setInfinite() else z.setZero()
        INF_INF,
        FNZ_INF -> return mutDecPowFnzInf(z, x, y, ctx)
        ZER_FNZ -> return mutDecPowZerFnz(z, x, y, yIsIntegral, ctx)
        INF_FNZ -> return mutDecPowInfFnz(z, x, y, yIsIntegral, ctx)
        else -> {
            if (stealIsPositiveFNZ(xSteal) && stealIsQNAN(ySteal) &&
                mutDecCompareNumericMagnitude(x, MutDec.ONE) == 0) {
                return z.setOne()
            } else {
                return z.setNanOperandFound(x, y, ctx)
            }
        }
    }
}

private fun mutDecPowFnzInf(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val yNegative = stealSignFlag(y.steal)

    // IEEE754-2019 p.
    // pow (−1, ±∞) is 1 with no exception
    // pow (+1, y) is 1 for any y (even a quiet NaN)
    // pow(±1, ±∞) = 1
    if (mutDecCompareNumericMagnitude(x, MutDec.ONE) == 0)
        return z.setOne()

    // |x| < 1 when sciExp < 0, |x| > 1 when sciExp >= 0
    // (|x| == 1 already handled via pown delegation)
    val xLessThanOne = stealSciExp(xSteal) < 0

    return if (xLessThanOne xor yNegative) z.setZero() else z.setInfinite()
}

private fun mutDecPowZerFnz(z: MutDec, x: MutDec, y: MutDec, yIsIntegral: Boolean, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    val yNegative = stealSignFlag(ySteal)
    val xNegative = stealSignFlag(xSteal)

    return if (yNegative) {
        // pow(±0, y < 0) → ±∞, signal divideByZero
        // sign of result: negative only if x is -0 and y is odd integer
        val resultNegative = xNegative && yIsIntegral && y.isOddIntegral()
        ctx.signalDivByZero(z.setInfinite(resultNegative))
    } else {
        // pow(±0, y > 0) → ±0
        // sign of result: negative only if x is -0 and y is odd integer
        val resultNegative = xNegative && yIsIntegral && y.isOddIntegral()
        z.setZero(resultNegative)
    }
}

private fun mutDecPowInfFnz(z: MutDec, x: MutDec, y: MutDec, yIsIntegral: Boolean, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    val xNegative = stealSignFlag(xSteal)
    val yNegative = stealSignFlag(ySteal)
    // pow(+∞, y)
    // pow(-∞, y)
    // sign of result: negative only if x negative && y is finite odd integer

    val resultNegative = xNegative && yIsIntegral && y.isOddIntegral()
    return if (yNegative)
        z.setZero(resultNegative)
    else
        z.setInfinite(resultNegative)
}

private fun mutDecPowFnzFnz(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal

    // negative base with non-integer exponent → invalid
    if (stealSignFlag(xSteal))
        return ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.POWER_OF_NEG_BASE_NON_INTEGER_EXPONENT)

    // x^y = exp(y * ln(x))
    // FIXME ... figure out where tmps come from
    val mutX = MutDec().set(x)
    val mutY = MutDec().set(y)
    val log10X = MutDec()
    val yLog10X = MutDec()
    val result = MutDec()

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
    return z.set(result)
}

internal fun mutDecCompoundImpl(z: MutDec, x: MutDec, n: Int, ctx: DecContext): MutDec {
    val xSteal = x.steal
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> {
            val t = ctx.tmps.mdecTrans1.setAdd(MutDec.ONE, x, ctx)
            when {
                t.isNegative() ->
                    return ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.COMPOUND_X_LT_NEG_ONE)
                n == 0 -> return z.setOne()
                t.isZero() -> {
                    return if (n < 0) {
                        z.setInfinite()
                        ctx.signalDivByZero(z)
                    } else {
                        //  Q(compound(x, n)) is floor(n × min(0, Q(x)))
                        val nCapped = min(n, 10_000) // ensure following multiply does not overflow
                        val qPreferred = nCapped * min(0, stealQExp(xSteal))
                        z.setZero(false, qPreferred)
                    }
                }

            }
            mutDecPownImplFNZ(z, t, n.toLong(), ctx)
            if (z.isZero()) {
                val absLong = n.toLong().absoluteValue
                val qPreferredLong = absLong * min(0L, stealQExp(xSteal).toLong())
                val qPreferred = max(min(qPreferredLong, 6111L), -6176L).toInt()
                //val qPreferred = qPreferredLong.coerceIn(-6176L, 6111L).toInt()
                z.setZero(false, qPreferred)
            }
            return z
        }
        STEAL_TYP_ZER -> return z.setOne()
        STEAL_TYP_INF -> return when {
            stealSignFlag(xSteal) ->
                ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.COMPOUND_X_LT_NEG_ONE)
            n > 0 -> z.setInfinite()
            n == 0 -> z.setOne() // compound (x, 0) is 1 for x ≥ −1 or quiet NaN
            else -> // n < 0
                z.setZero()
        }
        else -> { // STEAL_TYP_NAN
            // IEEE754-2019 p. 62
            // compound (x, 0) is 1 for x ≥ −1 or quiet NaN
            if (n == 0 && stealIsQNAN(xSteal))
                return z.setOne()
            else
                return z.setNanOperandFound(x, ctx)
        }
    }
}

// For the rootn operation:
//n = 0: invalid operation;
//x < 0 and n even: invalid operation;
//n = −1: overflow, underflow
//x = 0 and n < 0: divideByZero
//rootn (±0, n) is ±∞ and signals the divideByZero exception for odd n < 0
//rootn (±0, n) is +∞ and signals the divideByZero exception for even n < 0
//rootn (±0, n) is +0 for even n > 0
//rootn (±0, n) is ±0 for odd n > 0
//rootn (+∞, n) is +∞ for n > 0
//rootn (−∞, n) is −∞ for odd n > 0
//rootn (−∞, n) is qNaN and signals the invalid operation exception for even n > 0
//rootn (+∞, n) is +0 for n < 0
//rootn (−∞, n) is −0 for odd n < 0
//rootn (−∞, n) is qNaN and signals the invalid operation exception for even n < 0.
//NOTE — rootn (−0, 2) differs from squareRoot(−0) because they have different consistency
//considerations.
internal fun mutDecRootnImpl(z: MutDec, x: MutDec, n: Int, ctx: DecContext): MutDec {
    val ctx = DecContext.current()
    val xSteal = x.steal
    if (n == 0)
        return ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.ROOTN_BAD_ARGS)
    val xSignFlag = stealSignFlag(xSteal)
    val nIsOdd = (n and 1) != 0
    return when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> {
            if (xSignFlag && !nIsOdd)
                ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.ROOTN_BAD_ARGS)
            else
                mutDecRootnImplFNZ(z, x, n, ctx)
        }
        STEAL_TYP_ZER -> {
            //rootn (±0, n) is ±∞ and signals the divideByZero exception for odd n < 0
            //rootn (±0, n) is +∞ and signals the divideByZero exception for even n < 0
            //rootn (±0, n) is +0 for even n > 0
            //rootn (±0, n) is ±0 for odd n > 0
            if (n < 0)
                ctx.signalDivByZero(z.setInfinite(nIsOdd and xSignFlag))
            else
                z.setZero(nIsOdd and xSignFlag)
        }
        STEAL_TYP_INF -> when {
            //rootn (−∞, n) is −0 for odd n < 0
            xSignFlag && nIsOdd && n < 0 ->
                z.setZero(true)
            //rootn (−∞, n) is −∞ for odd n > 0
            xSignFlag && nIsOdd -> z.setInfinite(true)
            //rootn (−∞, n) is qNaN and signals the invalid operation exception for even n < 0.
            //rootn (−∞, n) is qNaN and signals the invalid operation exception for even n > 0
            xSignFlag ->
                ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.ROOTN_BAD_ARGS)
            //rootn (+∞, n) is +0 for n < 0
            n < 0 -> z.setZero()
            //rootn (+∞, n) is +∞ for n > 0
            else -> z.setInfinite(false)
        }
        else -> // STEAL_TYP_NAN
            z.setNanOperandFound(x, ctx)
    }
}

private fun mutDecRootnImplFNZ(z: MutDec, x: MutDec, n: Int, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val nAbs = n.toLong().absoluteValue
    if (nAbs.countOneBits() == 1) {
        // n is a power of 2, apply sqrt repeatedly
        val shifts = nAbs.countTrailingZeroBits()
        z.set(x)
        if (shifts > 0) {
            if (shifts > 1) {
                val ctx38 = DecContext.internal38()
                repeat(shifts-1) {
                    z.setSqrt(z, ctx38)
                }
            }
            z.setSqrt(z, ctx)
        }
        if (n < 0) z.setReciprocal(z, ctx)
        return z
    }
    val absX = if (stealSignFlag(xSteal)) ctx.tmps.mdecTrans2.set(x).mutateNegate() else x
    val nReciprocal = ctx.tmps.mdecTrans1.set(n)
    nReciprocal.setReciprocal(nReciprocal, ctx)
    z.setPow(absX, nReciprocal, ctx)
    if (stealSignFlag(xSteal)) z.mutateNegate()
    return z
}