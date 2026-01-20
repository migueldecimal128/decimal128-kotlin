package com.decimal128.decimal

import kotlin.math.max

object C256Add {

    fun c256SetAdd(z: C256, x: C256, scaleDelta: Int, y: C256) {
        verify { x.bitLen < 128 }
        verify { y.bitLen < 128 }
        when {
            scaleDelta == 0 -> c256SetAddUnscaled(z, x, y)
            scaleDelta > 0 -> c256SetAddScaled(z, x, scaleDelta, y)
            else -> c256SetAddScaled(z, y, -scaleDelta, x)
        }
    }

    fun c256SetAddUnscaled(z: C256, x: C256, y: C256) {
        val maxBitLen = max(x.bitLen, y.bitLen) + 1

        if (maxBitLen < 64) {
            z.c256Set64(x.dw0 + y.dw0)
            return
        }
        val (carry0, p0) = sumU64(x.dw0, y.dw0)
        if (maxBitLen < 128) {
            val p1 = x.dw1 + y.dw1 + carry0
            z.c256Set128(p1, p0)
            return
        }
        val (carry1, p1) = sumU64(x.dw1, y.dw1, carry0)
        val (carry2, p2) = sumU64(x.dw2, y.dw2, carry1)
        val (carry3, p3) = sumU64(x.dw3, y.dw3, carry2)
        verify { carry3 == 0L }
        z.c256Set256(p3, p2, p1, p0)
    }

    fun c256SetAddScaled(z: C256, x: C256, scaleDelta: Int, y: C256) {
        verify { scaleDelta > 0 }
        verify { scaleDelta < PRECISION_34 }

//        check((x.dw3 or x.dw2) == 0L)
//        check((y.dw3 or y.dw2) == 0L)
        verify { x.c256HasValidLengths() }
        verify { y.c256HasValidLengths() }
        verify { z.c256HasValidLengths() }

        C256Fma.c256SetFmaPow10(z, x, scaleDelta, y)
    }

    fun c256MutateIncrement(z: C256) {
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

    fun c256MutateDecrement(z: C256) {
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



}
