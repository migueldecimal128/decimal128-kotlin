package com.decimal128

import com.decimal128.CoeffScalePow10.coeffScaleFusedMulAbsDiffPow10
import com.decimal128.Residue.Companion.EXACT
import com.decimal128.Residue.Companion.EXACT_NEGATED
import java.lang.Long.compareUnsigned
import kotlin.math.max

object CoeffAbsDiff {

    fun absDiff(z: Coeff, x: Coeff, scaleDelta: Int, y: Coeff): Residue {
        return when {
            scaleDelta == 0 -> coeffAbsDiffUnscaled(z, x, y)
            scaleDelta > 0 -> coeffAbsDiffScaled(z, x, scaleDelta, y)
            else -> coeffAbsDiffScaled(z, y, -scaleDelta, x)
        }
    }

    fun coeffAbsDiffUnscaled(z: Coeff, x: Coeff, y: Coeff) =
        coeffAbsDiffUnscaled_bit(z, x, y)


    fun coeffAbsDiffUnscaled_bit(z: Coeff, x: Coeff, y: Coeff): Residue { // minuend - subtrahend
        assert(z.hasValidLengths())
        assert(x.hasValidLengths())
        assert(y.hasValidLengths())
        val maxBitLen = max(x.bitLen, y.bitLen)

        val d0 = x.dw0 - y.dw0
        if (maxBitLen < 64) {
            val negCarry0 = d0 shr 63
            val z0 = (d0 xor negCarry0) - negCarry0
            z.coeffSet64(z0)
            return if (negCarry0 < 0) EXACT_NEGATED else EXACT
        }
        val carry0 = if (compareUnsigned(d0, x.dw0) > 0) 1L else 0L

        if (maxBitLen < 128) {
            val d1 = x.dw1 - y.dw1 - carry0
            val negCarry1 = d1 shr 63
            var z1 = d1 xor negCarry1
            val z0 = (d0 xor negCarry1) - negCarry1
            if (negCarry1 < 0L && z0 == 0L) {
                ++z1
            }
            z.coeffSet128(z1, z0)
            return if (negCarry1 < 0) EXACT_NEGATED else EXACT
        }
        val d1a = x.dw1 - y.dw1
        val carry1a = if (compareUnsigned(d1a, x.dw1) > 0) 1L else 0L
        val d1 = d1a - carry0
        val carry1 = if (compareUnsigned(d1, d1a) > 0) 1L else carry1a

        if (maxBitLen < 192) {
            val d2 = x.dw2 - y.dw2 - carry1
            val negCarry2 = d2 shr 63
            var z2 = d2 xor negCarry2
            var z1 = d1 xor negCarry2
            val z0 = (d0 xor negCarry2) - negCarry2 // complement and increment
            if (negCarry2 < 0L && z0 == 0L) {
                ++z1
                if (z1 == 0L) {
                    ++z2
                }
            }
            z.coeffSet192(z2, z1, z0)
            return if (negCarry2 < 0) EXACT_NEGATED else EXACT
        }
        val d2a = x.dw2 - y.dw2
        val carry2a = if (compareUnsigned(d2a, x.dw2) > 0) 1L else 0L
        val d2 = d2a - carry1
        val carry2 = if (compareUnsigned(d2, d2a) > 0) 1L else carry2a

        val d3a = x.dw3 - y.dw3
        val carry3a = if (compareUnsigned(d3a, x.dw3) > 0) 1L else 0L
        val d3 = d3a - carry2
        val carry3 = if (compareUnsigned(d3, d3a) > 0) 1L else carry3a

        // if carry == 1 then complement-and-increment else NOOP
        val negCarry3 = -carry3
        var z3 = d3 xor negCarry3
        var z2 = d2 xor negCarry3
        var z1 = d1 xor negCarry3
        val z0 = (d0 xor negCarry3) - negCarry3 // complement and increment
        if (negCarry3 < 0L && z0 == 0L) {
            ++z1
            if (z1 == 0L) {
                ++z2
                if (z2 == 0L)
                    ++z3
            }
        }
        z.coeffSet256(z3, z2, z1, z0)
        return if (negCarry3 < 0) EXACT_NEGATED else EXACT
    }

    fun coeffAbsDiffScaled(z: Coeff, x: Coeff, scaleDelta: Int, y: Coeff): Residue {
        assert(scaleDelta > 0)
        assert(scaleDelta < 34)

        assert((x.dw3 or x.dw2) == 0L)
        assert((y.dw3 or y.dw2) == 0L)
        assert(x.hasValidLengths())
        assert(y.hasValidLengths())
        assert(z.hasValidLengths())

        return coeffScaleFusedMulAbsDiffPow10(z, x, scaleDelta, y)
    }

}