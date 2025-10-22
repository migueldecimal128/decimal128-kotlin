package com.decimal128.decimal

import com.decimal128.decimal.U256ScalePow10.u256ScaleFmaPow10
import kotlin.math.max

object U256Add {

    fun u256Add(z: C256, x: C256, scaleDelta: Int, y: C256) {
        check (x.bitLen < 128)
        check (y.bitLen < 128)
        when {
            scaleDelta == 0 -> u256AddUnscaled(z, x, y)
            scaleDelta > 0 -> u256AddScaled(z, x, scaleDelta, y)
            else -> u256AddScaled(z, y, -scaleDelta, x)
        }
    }

    fun u256AddUnscaled(z: C256, x: C256, y: C256) {
        val maxBitLen = max(x.bitLen, y.bitLen) + 1
        check (maxBitLen <= 128)

        val p0 = x.dw0 + y.dw0
        if (maxBitLen < 64) {
            z.c256Set64(p0)
            return
        }
        val carry0 = if (unsignedLT(p0, x.dw0)) 1L else 0L
        val p1 = x.dw1 + y.dw1 + carry0
        z.c256Set128(p1, p0)
    }

    fun u256AddScaled(z: C256, x: C256, scaleDelta: Int, y: C256) {
        check(scaleDelta > 0)
        check(scaleDelta < PRECISION_34)

//        check((x.dw3 or x.dw2) == 0L)
//        check((y.dw3 or y.dw2) == 0L)
        check(x.c256HasValidLengths())
        check(y.c256HasValidLengths())
        check(z.c256HasValidLengths())

        u256ScaleFmaPow10(z, x, scaleDelta, y)
    }

    fun u256MutateIncrement(z: C256) {
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

    fun u256MutateDecrement(z: C256) {
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
