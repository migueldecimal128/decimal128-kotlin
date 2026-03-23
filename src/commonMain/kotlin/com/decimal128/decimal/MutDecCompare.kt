// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import kotlin.math.max

internal fun mutDecCompareTotalOrder(x: MutDec, y: MutDec): Int {
    val xSignMask = x.signMask // 0 or -1 (0xFFFF_FFFF)
    if (xSignMask != y.signMask)
        return (xSignMask shl 1) + 1 // return -1 or 1
    val cmpMag = mutDecCompareTotalOrderMag(x, y)
    return negateForSign(cmpMag, xSignMask)
}

internal fun mutDecCompareTotalOrderMag(x: MutDec, y: MutDec): Int {
    val signature = binopSignatureOf(x.type, y.type)
    val cmp =
        if (signature == FNZ_FNZ) {
            cmpTotalOrderMagFnzFnz(x, y)
        } else when (signature) {
            ZER_ZER -> cmp32(x.qExp, y.qExp)
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

internal fun mutDecCompareNumericMagnitude(x: MutDec, y: MutDec, pentad: Pentad): Int {
    val signature = binopSignatureOf(x.type, y.type)
    return if (signature == FNZ_FNZ) {
        cmpMagFnzFnz(x, y)
    } else when (signature) {
        FNZ_INF,
        ZER_FNZ,
        ZER_INF -> -1

        FNZ_ZER,
        INF_FNZ,
        INF_ZER -> 1
        INF_INF,
        ZER_ZER -> 0
        else -> throw IllegalArgumentException("NaN found")
    }
}

internal fun mutDecCompareJavaStyle(x: MutDec, y: MutDec): Int {
    val stealX = x.type
    val stealY = y.type
    when {
        !stealHasNAN(stealX, stealY) -> {
            val xSignMask = x.signMask // 0 or -1 (0xFFFF_FFFF)
            if (xSignMask != y.signMask)
                return (xSignMask shl 1) + 1 // return -1 or 1
            val cmpMag = cmpNumericMagnitude(x, y)
            return negateForSign(cmpMag, xSignMask)
        }

        !stealIsNAN(stealX) -> return -1
        !stealIsNAN(stealY) -> return 1
        else -> return 0
    }
}

internal fun mutDecEqJavaStyle(x: MutDec, y: MutDec): Boolean {
    val signature = binopSignatureOf(x.type, y.type)
    return if (signature == FNZ_FNZ) {
        x.sign == y.sign && cmpMagFnzFnz(x, y) == 0
    } else when (signature) {
        // FIXME ... come up with a branchless way to do this
        FNZ_ZER,
        FNZ_INF,
        INF_ZER,
        ZER_FNZ,
        ZER_INF,
        INF_FNZ -> false
        ZER_ZER -> true
        INF_INF -> x.sign == y.sign
        else -> x.isNaN() && y.isNaN()
    }
}

private inline fun negateForSign(cmp: Int, signMask: Int) =
    (cmp xor signMask) - signMask



private inline fun cmpTotalOrderMagFnzFnz(x: MutDec, y: MutDec): Int {
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
    val xQ = x.qExp
    val yQ = y.qExp
    val xE = x.eExp
    val yE = y.eExp
    if (xE != yE)
        return ((xE - yE) shr 31) or 1
    if (x.bExpMin() > y.bExpMax())
        return 1
    if (x.bExpMax() < y.bExpMin())
        return -1
    val qDelta = xQ - yQ
    if (qDelta == 0)
        return c256UnscaledCompare(x, y)
    val pentad = DecContext.current().tmps.pentad1
    return if (qDelta > 0)
        -c256ScaledCompare(y, x, qDelta, pentad)
    else
        c256ScaledCompare(x, y, -qDelta, pentad)
}

private fun cmpTotalOrderMagnitudeNanFound(x: MutDec, y: MutDec): Int {
    verify { x.isNaN() || y.isNaN() }
    return when {
        !x.isNaN() -> -1
        !y.isNaN() -> 1
        // if both are the same NaN, then compare payloads

        (x.type and STEAL_NAN_MASK) == (y.type and STEAL_NAN_MASK) ->
            ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
        // qNaN sorts higher than sNaN
        stealIsQNAN(x.type) -> 1
        else -> -1
    }
}

private fun cmpNumericMagnitude(x: MutDec, y: MutDec): Int {
    val signature = binopSignatureOf(x.type, y.type)
    val cmpMag =
        if (signature == FNZ_FNZ) {
            cmpMagFnzFnz(x, y)
        } else when (signature) {
            // FIXME ... come up with a branchless way to do this
            //  ... after eliminating NAN_FOUND
            ZER_ZER -> 0
            ZER_FNZ,
            ZER_INF,
            FNZ_INF -> -1

            FNZ_ZER,
            INF_ZER,
            INF_FNZ -> 1
            INF_INF -> 0
            else -> throw IllegalStateException()
        }
    return cmpMag
}

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
    if (!x.isNaN() && !y.isNaN()) {
        var cmp = if ((op and MAG_MASK) != 0)
            x.compareNumericMagnitudeTo(y, ctx.tmps.pentad1)
        else
            x.compareTo(y)
        if (cmp == 0)
            cmp = x.compareTotalOrderTo(y)
        return z.set(if ((cmp >= 0) xor ((op and MAX_MASK) == 0)) x else y)

    }
    if ((op and NUM_MASK) != 0) {
        if (!x.isNaN()) {
            z.set(x)
            if (y.isSignaling())
                ctx.signalInvalid(z)
            return z
        }
        if (!y.isNaN()) {
            z.set(y)
            if (x.isSignaling())
                ctx.signalInvalid(z)
            return z
        }
    }
    return z.setNaNOperand(x, y, ctx)
}

