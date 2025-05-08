package com.decimal128

import java.lang.Long.compareUnsigned
import com.decimal128.CoeffScalePow10.coeffScaleFmaPow10
import kotlin.Long.Companion.MIN_VALUE

object CoeffAdd {

    fun coeffAdd(z: Coeff, x: Coeff, scaleDelta: Int, y: Coeff) {
        when {
            scaleDelta == 0 -> coeffAddUnscaled(z, x, y)
            scaleDelta > 0 -> coeffAddScaled(z, x, scaleDelta, y)
            else -> coeffAddScaled(z, y, -scaleDelta, x)
        }
    }

    fun coeffAddUnscaled(z: Coeff, x: Coeff, y: Coeff) {
        /*
        if (x.bitLen == 0) {
            z.coeffSet(y)
            return
        }
        if (y.bitLen == 0) {
            z.coeffSet(x)
            return
        }
        */

        val maxBitLen = Math.max(x.bitLen, y.bitLen)

        val x0 = x.dw0
        val y0 = y.dw0
        val p0 = x0 + y0
        if (maxBitLen < 64) {
            z.coeffSet64(p0)
            return
        }
        val carry0 = if (compareUnsigned(p0, x0) < 0) 1L else 0L
        val x1 = x.dw1
        val y1 = y.dw1
        if (maxBitLen < 128) {
            val p1 = x1 + y1 + carry0
            z.coeffSet128(p1, p0)
            return
        }
        val p1a = x1 + y1
        val carry1a = if (compareUnsigned(p1a, x1) < 0) 1L else 0L
        val p1 = p1a + carry0
        val carry1 = if (compareUnsigned(p1, carry0) < 0) 1L else carry1a

        val x2 = x.dw2
        val y2 = y.dw2
        if (maxBitLen < 192) {
            val p2 = x2 + y2 + carry1
            z.coeffSet192(p2, p1, p0)
            return
        }
        val p2a = x2 + y2
        val carry2a = if (compareUnsigned(p2a, x2) < 0) 1L else 0L
        val p2 = p2a + carry1
        val carry2 = if (compareUnsigned(p2, carry1) < 0) 1L else carry2a

        val x3 = x.dw3
        val y3 = y.dw3
        if (maxBitLen < 256) {
            val p3 = x3 + y3 + carry2
            z.coeffSet256(p3, p2, p1, p0)
        }
        val p3a = x3 + y3
        val carry3a = if (compareUnsigned(p3a, x3) < 0) 1L else 0L
        val p3 = p3a + carry2
        val carry3 = if (compareUnsigned(p3, carry2) < 0) 1L else carry3a
        if (carry3 != 0L)
            throw RuntimeException("coefficient add overflow x:$x y:$y")
        z.coeffSet256(p3, p2, p1, p0)
    }

    fun coeffAddScaled(z: Coeff, x: Coeff, scaleDelta: Int, y: Coeff) {
        assert(scaleDelta > 0)
        assert(scaleDelta < PRECISION_34)

        assert((x.dw3 or x.dw2) == 0L)
        assert((y.dw3 or y.dw2) == 0L)
        assert(x.hasValidLengths())
        assert(y.hasValidLengths())
        assert(z.hasValidLengths())

        coeffScaleFmaPow10(z, x, scaleDelta, y)
    }
}
