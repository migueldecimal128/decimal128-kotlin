package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.Residue.Companion.GT_HALF
import com.decimal128.decimal.Residue.Companion.HALF
import com.decimal128.decimal.Residue.Companion.LT_HALF

@Suppress("NOTHING_TO_INLINE")
private inline fun getShiftedLeft(v: IntArray, i: Int, shift: Int): Long {
    return ((v[i] shl shift) or ((v[i - (-i ushr 31)] ushr -shift) and (-shift shr 31))).toLong() and MASK32L
    //if (i == 0) {
    //    v[0] shl s
    //} else {
    //    (v[i] shl s) or if (s != 0) (v[i - 1] ushr (32 - s)) else 0
}

internal fun c256SetDivX64(z: C256, x: C256, y0: Long, knuthD: IntArray): Residue {
    val rem = c256SetDivRemX64(z, x, y0, knuthD)
    val residue = Residue.fromRemainderDivisor(rem, y0)
    return residue
}

internal fun c256SetDivRemX64(z: C256?, x: C256, y0: Long, knuthD: IntArray): Long {
    if ((y0 shr 1) == 0L) {
        if (y0 == 0L)
            throw RuntimeException("div by zero")
        z?.c256Set(x)
        return 0
    }
    val x0 = x.dw0
    if (x.bitLen <= 64) {
        val quot = unsignedDiv(x0, y0)
        val rem = unsignedMod(x0, y0)
        z?.c256Set64(quot)
        return rem
    }
    if ((y0 and (y0 - 1)) == 0L) {
        // y0 is an exact power of 2 ... just shift right.
        // 0 and 1 cases handled above, so if we are here then ntz >= 1
        val ntz = y0.countTrailingZeroBits()
        val mask = (1L shl ntz) - 1L
        val rem = x0 and mask
        z?.c256SetShiftRight(x, ntz)
        return rem
    }
    //TODO at this point I know that x.bitLen >= y.bitLen and x > y
    // if (bitLenDelta < some-small-number) then I should use repeated subtraction
    if ((y0 ushr 32) == 0L) {
        return if ((y0 ushr 16) == 0L) DivDirect.divModX16(z, x, y0) else DivDirect.divModX32(z, x, y0)
    }
    return divKnuthDivModX64(z, x, y0, knuthD)
}

internal fun c256SetDiv(z: C256, x: C256, y: C256, knuthD: IntArray) = c256SetDivRem(z, null, x, y, knuthD)

internal fun c256SetRem(z: C256, x: C256, y: C256, knuthD: IntArray) = c256SetDivRem(null, z, x, y, knuthD)

internal fun c256SetDivRem(quot: C256?, rem: C256?, x: C256, y: C256, knuthD: IntArray): Residue {
    if (y.bitLen <= 64) {
        val y0 = y.dw0
        val r0 = c256SetDivRemX64(quot, x, y0, knuthD)
        if (rem != null) {
            rem.c256Set64(r0)
            return EXACT
        }
        val residue = Residue.fromRemainderDivisor(r0, y0)
        return residue
    }
    val bitLenDelta = x.bitLen - y.bitLen
    if (bitLenDelta < 0) {
        rem?.c256Set(x)
        val residue = when {
            rem != null || x.bitLen == 0 -> EXACT
            bitLenDelta <= -2 -> LT_HALF
            else -> Residue.fromRemainderDivisor(x, y)
        }
        quot?.c256SetZero()
        return residue
    }
    if (bitLenDelta == 0) {
        val cmp = c256UnscaledCompare(x, y)
        if (cmp < 0) {
            rem?.c256Set(x)
            val residue = if (rem != null) EXACT else GT_HALF
            quot?.c256SetZero()
            return residue
        } else if (cmp > 0) {
            val t = when {
                rem != null && rem !== y -> rem
                quot != null && quot !== y -> quot
                else -> C256()
            }
            c256SetSubUnscaled(t, x, y)
            val residue = Residue.fromRemainderDivisor(t, y)
            rem?.c256Set(t)
            quot?.c256SetOne()
            return residue
        } else {
            rem?.c256SetZero()
            quot?.c256SetOne()
            return EXACT
        }
    }
    verify { bitLenDelta >= 0 }
    //TODO at this point I know that x.bitLen >= y.bitLen and x > y
    // if (bitLenDelta < some-small-number) then I should use repeated subtraction
    return divKnuth(quot, rem, x, y, knuthD)
}

internal fun c256DivNearestX64(z: C256, x: C256, y: Long, knuthD: IntArray) {
    val residue = c256SetDivX64(z, x, y, knuthD)
    when (residue) {
        GT_HALF -> z.c256MutateIncrement()
        HALF -> if (z.dw0 and 1L == 1L) z.c256MutateIncrement()
        EXACT, LT_HALF -> {}
    }
}

internal fun c256DivNearest(z: C256, x: C256, y: C256, knuthD: IntArray) {
    val residue = c256SetDivRem(z, null, x, y, knuthD)
    when (residue) {
        GT_HALF -> z.c256MutateIncrement()
        HALF -> if (z.dw0 and 1L == 1L) z.c256MutateIncrement()
        EXACT, LT_HALF -> {}
    }
}
