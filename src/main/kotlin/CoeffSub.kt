package com.decimal128

import com.decimal128.CoeffScalePow10.coeffScaleFusedMulAbsDiffPow10
import com.decimal128.Residue.Companion.EXACT
import com.decimal128.Residue.Companion.EXACT_NEGATED
import java.lang.Long.compareUnsigned
import kotlin.math.max

object CoeffSub {

    fun coeffSubUnscaled(z: Coeff, x: Coeff, y: Coeff) { // minuend - subtrahend
        assert(z.hasValidLengths())
        assert(x.hasValidLengths())
        assert(y.hasValidLengths())
        assert(x.unscaledCompareTo(y) >= 0)
        val xBitLen = x.bitLen
        assert(xBitLen >= y.bitLen)

        val d0 = x.dw0 - y.dw0
        if (xBitLen <= 64) {
            z.coeffSet64(d0)
            return
        }
        val carry0 = if (compareUnsigned(d0, x.dw0) > 0) 1L else 0L

        if (xBitLen <= 128) {
            val d1 = x.dw1 - y.dw1 - carry0
            z.coeffSet128(d1, d0)
            return
        }
        val d1a = x.dw1 - y.dw1
        val carry1a = if (compareUnsigned(d1a, x.dw1) > 0) 1L else 0L
        val d1 = d1a - carry0
        val carry1 = if (compareUnsigned(d1, d1a) > 0) 1L else carry1a

        if (xBitLen <= 192) {
            val d2 = x.dw2 - y.dw2 - carry1
            z.coeffSet192(d2, d1, d0)
            return
        }
        val d2a = x.dw2 - y.dw2
        val carry2a = if (compareUnsigned(d2a, x.dw2) > 0) 1L else 0L
        val d2 = d2a - carry1
        val carry2 = if (compareUnsigned(d2, d2a) > 0) 1L else carry2a

        val d3a = x.dw3 - y.dw3
        val carry3a = if (compareUnsigned(d3a, x.dw3) > 0) 1L else 0L
        val d3 = d3a - carry2
        val carry3 = if (compareUnsigned(d3, d3a) > 0) 1L else carry3a
        assert(carry3 == 0L)

        z.coeffSet256(d3, d2, d1, d0)
    }

    fun coeffSubScaled(z: Coeff, x: Coeff, scaleDelta: Int, y: Coeff) {
        assert(scaleDelta > 0)
        assert(scaleDelta < 34)

        assert((x.dw3 or x.dw2) == 0L)
        assert((y.dw3 or y.dw2) == 0L)
        assert(x.hasValidLengths())
        assert(y.hasValidLengths())
        assert(z.hasValidLengths())

        assert(y.scaledCompareTo(x, scaleDelta) <= 0)

        CoeffFms.coeffFmsPow10(z, x, scaleDelta, y)
    }

    fun coeffSubScaled(z: Coeff, x: Coeff, y: Coeff, scaleDelta: Int) {
        assert(scaleDelta > 0)
        assert(scaleDelta < 34)

        assert((x.dw3 or x.dw2) == 0L)
        assert((y.dw3 or y.dw2) == 0L)
        assert(x.hasValidLengths())
        assert(y.hasValidLengths())
        assert(z.hasValidLengths())

        assert(x.scaledCompareTo(y, scaleDelta) >= 0)

        CoeffFms.coeffFmsPow10(z, x, y, scaleDelta)
    }

}