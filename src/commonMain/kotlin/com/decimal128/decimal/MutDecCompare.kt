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

private inline fun negateForSign(cmp: Int, signMask: Int) =
    (cmp xor signMask) - signMask


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
    val x0 = x.dw0
    val x1 = x.dw1
    val y0 = y.dw0
    val y1 = y.dw1
    if (xQ == yQ)
        return ucmp128(x1, x0, y1, y0)
    val pentad = DecContext.current().tmps.pentad1
    if (xQ > yQ)
        return -ucmp128ScalePow10(y1, y0, x1, x0, xQ - yQ, pentad)
    return ucmp128ScalePow10(x1, x0, y1, y0, yQ - xQ, pentad)
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

