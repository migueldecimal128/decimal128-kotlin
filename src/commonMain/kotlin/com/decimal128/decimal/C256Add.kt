package com.decimal128.decimal

import kotlin.math.max

internal fun c256SetAdd(z: C256, x: C256, scaleDelta: Int, y: C256, pentad: Pentad) {
    verify { x.bitLen <= 127 }
    verify { y.bitLen <= 127 }
    when {
        scaleDelta == 0 -> c256SetAddUnscaled(z, x, y, pentad)
        scaleDelta > 0 -> c256SetAddScaled(z, x, scaleDelta, y)
        else -> c256SetAddScaled(z, y, -scaleDelta, x)
    }
}

/**
 * Adds 2 coefficients together without scaling.
 *
 * This code is called from FMA, so it x might have 76 digits == 253 bits.
 * y will have been normalized and will have up thru 38 digits == 117 bits
 */
internal fun c256SetAddUnscaled(z: C256, x: C256, y: C256, pentad: Pentad) {
    verify { x.bitLen <= 253 }
    verify { y.bitLen <= 127 }
    val maxBitLen = max(x.bitLen, y.bitLen) + 1
    val x0 = x.dw0
    val z0 = x.dw0 + y.dw0
    if (maxBitLen <= 64) {
        z.c256Set64(z0)
        return
    }
    val carry0 = if (unsignedLT(z0, x0)) 1L else 0L
    if (maxBitLen <= 128) {
        val z1 = x.dw1 + y.dw1 + carry0
        z.c256Set128(z1, z0)
        return
    }
    sumU64(pentad, carry0, x.dw1, y.dw1)
    val carry1 = pentad.dw1
    val z1 = pentad.dw0
    val z2 = carry1 + x.dw2
    val carry2 = if (z2 == 0L) carry1 else 0L
    val z3 = carry2 + x.dw3
    z.c256Set256(z3, z2, z1, z0)
}

internal fun c256SetAddScaled(z: C256, x: C256, scaleDelta: Int, y: C256) =
    c256SetAddScaled(z, x, scaleDelta, y, Pentad())

internal fun c256SetAddScaled(z: C256, x: C256, scaleDelta: Int, y: C256, pentad: Pentad) {
    verify { scaleDelta > 0 }
    verify { scaleDelta < PRECISION_34 }

//        check((x.dw3 or x.dw2) == 0L)
//        check((y.dw3 or y.dw2) == 0L)
    verify { x.c256HasValidLengths() }
    verify { y.c256HasValidLengths() }
    verify { z.c256HasValidLengths() }

    c256SetFmaPow10(z, x, scaleDelta, y)
}

internal fun c256MutateIncrement(z: C256) {
    ++z.dw0
    if (z.dw0 == 0L) {
        ++z.dw1
        if (z.dw1 == 0L) {
            ++z.dw2
            if (z.dw2 == 0L) {
                ++z.dw3
                if (z.dw3 == 0L)
                    throw RuntimeException("overflow")
            }
        }
    }
    // flag for roundup which occurs during multiplies while enableIndexSet is active
    if (z.digitLen >= 0)
        z.updateDigitLenBitLen()
}

internal fun c256MutateDecrement(z: C256) {
    --z.dw0
    if (z.dw0 == -1L) {
        --z.dw1
        if (z.dw1 == -1L) {
            --z.dw2
            if (z.dw2 == -1L) {
                --z.dw3
                if (z.dw3 == -1L)
                    throw RuntimeException("decrement underflow")
            }
        }
    }
    z.updateDigitLenBitLen()
}

