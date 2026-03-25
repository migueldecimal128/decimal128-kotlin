// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal fun mutDecCompareTotalOrder(x: MutDec, y: MutDec): Int {
    val xSignMask = x.signMask // 0 or -1 (0xFFFF_FFFF)
    if (xSignMask != y.signMask)
        return (xSignMask shl 1) + 1 // return -1 or 1
    val cmpMag = mutDecCompareTotalOrderMag(x, y)
    return negateForSign(cmpMag, xSignMask)
}

internal fun mutDecCompareTotalOrderMag(x: MutDec, y: MutDec): Int {
    val xSteal = x.steal
    val ySteal = y.steal
    val signature = binopSignatureOf(xSteal, ySteal)
    val cmp =
        if (signature == FNZ_FNZ) {
            cmpTotalOrderMagFnzFnz(x, y)
        } else when (signature) {
            ZER_ZER -> cmp32(stealQExp(xSteal), stealQExp(ySteal))
            ZER_FNZ,
            ZER_INF,
            FNZ_INF -> -1

            FNZ_ZER,
            INF_ZER,
            INF_FNZ -> 1
            INF_INF -> 0
            else -> cmpTotalOrderMagnitudeNanFound(x, y)
        }
    return cmp
}

internal fun mutDecCompareJavaStyle(x: MutDec, y: MutDec): Int {
    val xSteal = x.steal
    val ySteal = y.steal
    when {
        !stealHasNAN(xSteal, ySteal) -> {
            val xSignMask = stealSignMask(xSteal) // 0 or -1 (0xFFFF_FFFF)
            if (xSignMask != stealSignMask(ySteal))
                return (xSignMask shl 1) + 1 // return -1 or 1
            val cmpMag = mutDecCompareNumericMagnitude(x, y)
            return negateForSign(cmpMag, xSignMask)
        }

        !stealIsNAN(xSteal) -> return -1
        !stealIsNAN(ySteal) -> return 1
        else -> return 0
    }
}

internal fun mutDecEqJavaStyle(x: MutDec, y: MutDec): Boolean {
    val xSteal = x.steal
    val ySteal = y.steal
    val signature = binopSignatureOf(xSteal, ySteal)
    return if (signature == FNZ_FNZ) {
        stealSignBit(xSteal) == stealSignBit(ySteal) && cmpMagFnzFnz(x, y) == 0
    } else when (signature) {
        FNZ_ZER,
        FNZ_INF,
        INF_ZER,
        ZER_FNZ,
        ZER_INF,
        INF_FNZ -> false
        ZER_ZER -> true
        INF_INF -> stealSignBit(xSteal) == stealSignBit(ySteal)
        else -> stealBothNAN(xSteal, ySteal)
    }
}

private inline fun negateForSign(cmp: Int, signMask: Int) =
    (cmp xor signMask) - signMask



private inline fun cmpTotalOrderMagFnzFnz(x: MutDec, y: MutDec): Int {
    verify { x.isFiniteNonZero() && y.isFiniteNonZero() }
    val cmpMag = cmpMagFnzFnz(x, y)

    // If x and y represent the same floating-point datum:
    //  i) If x and y have negative sign,
    //    totalOrder(x, y) is true if and only if the exponent of x ≥ the exponent of y
    //  ii) otherwise,
    //    totalOrder(x, y) is true if and only if the exponent of x ≤ the exponent of y.

    // of course, we are comparing magnitude so assume signs are equal
    // and that return value may be negated because of sign.
    val cmpExp = cmp32(x.qExp, y.qExp)
    val cmpMagIsZeroMask = (cmpMag or -cmpMag).inv()
    val cmp = cmpMag or (cmpExp and cmpMagIsZeroMask)
    return cmp
}

private fun cmpMagFnzFnz(x: MutDec, y: MutDec): Int {
    val xSteal = x.steal
    val ySteal = y.steal
    val cmpSteal = stealCompareMagnitudeFnzFnz(xSteal, ySteal)
    if (cmpSteal != 0)
        return cmpSteal
    val qDelta = stealQExp(xSteal) - stealQExp(ySteal)
    if (qDelta == 0)
        return c256UnscaledCompare(x, y)
    val pentad = DecContext.current().tmps.pentad1
    return if (qDelta > 0)
        -c256ScaledCompare(y, x, qDelta, pentad)
    else
        c256ScaledCompare(x, y, -qDelta, pentad)
}

private fun cmpTotalOrderMagnitudeNanFound(x: MutDec, y: MutDec): Int {
    val xSteal = x.steal
    val ySteal = y.steal
    verify { x.isNaN() || y.isNaN() }
    return when {
        !stealIsNAN(xSteal) -> -1
        !stealIsNAN(ySteal) -> 1
        stealIsSNAN(xSteal) xor stealIsSNAN(ySteal) -> // exactly one isSignaling()
            if (stealIsSNAN(xSteal)) -1 else 1
        else -> // if both are the same NaN, then compare payloads
            ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
    }
}

internal fun mutDecCompareNumericMagnitude(x: MutDec, y: MutDec): Int {
    if (stealBothFNZ(x.steal, y.steal))
        return cmpMagFnzFnz(x, y)
    val xTyp = stealTyp(x.steal)
    val yTyp = stealTyp(y.steal)
    if ((xTyp != STEAL_TYP_NAN) && (yTyp != STEAL_TYP_NAN)) {
        val xInf = xTyp and 1
        val yInf = yTyp and 1
        val xZer = xTyp shr 1
        val yZer = yTyp shr 1
        val cmpMag = (xInf - yInf) or (yZer - xZer)
        return cmpMag
    }
    throw IllegalStateException()
}

/*
private fun cmpNumericMagnitude(x: MutDec, y: MutDec): Int {
    val signature = binopSignatureOf(x.steal, y.steal)
    val cmpMag =
        if (signature == FNZ_FNZ) {
            cmpMagFnzFnz(x, y)
        } else when (signature) {
            INF_INF,
            ZER_ZER -> 0
            ZER_FNZ,
            ZER_INF,
            FNZ_INF -> -1

            FNZ_ZER,
            INF_ZER,
            INF_FNZ -> 1
            else -> throw IllegalStateException()
        }
    return cmpMag
}
 */

internal const val MAX_MASK = 1
internal const val MAG_MASK = 2
internal const val NUM_MASK = 4

internal const val MIN_OP = 0
internal const val MAX_OP = MAX_MASK
internal const val MIN_MAG_OP = MAG_MASK
internal const val MAX_MAG_OP = MAX_MASK or MAG_MASK
internal const val MIN_NUM_OP = NUM_MASK
internal const val MAX_NUM_OP = MAX_MASK or NUM_MASK
internal const val MIN_MAG_NUM_OP = MAG_MASK or NUM_MASK
internal const val MAX_MAG_NUM_OP = MAX_MASK or MAG_MASK or NUM_MASK

internal fun mutDecSetMinMaxImpl(z: MutDec, x: MutDec, y: MutDec, op: Int, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    if (!stealIsNAN(xSteal) && !stealIsNAN(ySteal)) {
        var cmp = if ((op and MAG_MASK) != 0)
            mutDecCompareNumericMagnitude(x, y)
        else
            mutDecCompareJavaStyle(x, y)
        if (cmp == 0)
            cmp = mutDecCompareTotalOrder(x, y)
        return z.set(if ((cmp >= 0) xor ((op and MAX_MASK) == 0)) x else y)

    }
    if ((op and NUM_MASK) != 0) {
        if (!stealIsNAN(xSteal)) {
            z.set(x)
            if (stealIsSNAN(ySteal))
                ctx.signalInvalid(z)
            return z
        }
        if (!stealIsNAN(ySteal)) {
            z.set(y)
            if (stealIsSNAN(xSteal))
                ctx.signalInvalid(z)
            return z
        }
    }
    return z.setNaNOperand(x, y, ctx)
}

