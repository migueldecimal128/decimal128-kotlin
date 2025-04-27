package com.decimal128

import com.decimal128.CoeffDigitCount.setDigitCount128
import com.decimal128.CoeffDigitCount.setDigitCount192
import com.decimal128.CoeffDigitCount.setDigitCount256
import com.decimal128.CoeffDigitCount.setDigitCount64
import com.decimal128.Residue.Companion.EXACT
import com.decimal128.Residue.Companion.EXACT_NEGATED
import java.lang.Long.compareUnsigned
import kotlin.math.max

object CoeffAbsDiff {

    // if y exceeds x then return a _NEGATED residue
    // because it would have gone negative
    @Suppress("unused")
    fun coeffAbsDiff(sum: Coeff, x: Coeff, y: Coeff, scaleDelta: Int): Residue {
        if (x.digitCount == 0) {
            sum.set(y)
            return if (y.digitCount > 0) EXACT_NEGATED else EXACT
        }
        if (y.digitCount == 0) {
            sum.set(x)
            return EXACT
        }
        if (scaleDelta == 0) {
            return coeffAbsDiffUnscaled(sum, x, y)
        }
        return (
                if (scaleDelta > 0)
                    coeffAbsDiffScaled(sum, x, y, scaleDelta)
                else
                    coeffAbsDiffScaled(sum, y, x, -scaleDelta).toggleNegate()
                )

    }


    fun coeffAbsDiffUnscaled(z: Coeff, x: Coeff, y: Coeff): Residue { // minuend - subtrahend
        assert(z.isValidDigitCount())
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        val maxDigitCount = max(x.digitCount, y.digitCount)

        val d0 = x.dw0 - y.dw0
        val carry0 = if (compareUnsigned(d0, x.dw0) > 0) 1L else 0L
        // 64 bit boundary is a special case because 19 digits * 2 might generate carry into the next word
        // this is not the case for the 128 and 192 bit boundaries
        //
        // perhaps this test should be:
        // maxDigitCount <= 20 && (x.dw1 or y.dw1 or carry0) == 0
        if (maxDigitCount < POW10_128_OFFSET) {
            // if carry == 1 then complement-and-increment else NOOP
            val negCarry0 = -carry0
            z.dw3 = 0L; z.dw2 = 0L; z.dw1 = 0L; z.dw0 = (d0 xor negCarry0) - negCarry0
            setDigitCount64(z)
            return if (negCarry0 < 0) EXACT_NEGATED else EXACT
        }

        val d1a = x.dw1 - y.dw1
        val carry1a = if (compareUnsigned(d1a, x.dw1) > 0) 1L else 0L
        val d1 = d1a - carry0
        val carry1 = if (compareUnsigned(d1, d1a) > 0) 1L else carry1a
        if (maxDigitCount < POW10_192_OFFSET) {
            // if carry == 1 then complement-and-increment else NOOP
            val negCarry1 = -carry1
            z.dw0 = (d0 xor negCarry1) - negCarry1
            z.dw1 = (d1 xor negCarry1) - (negCarry1 and ((z.dw0 or -z.dw0) shr 63).inv())
            z.dw2 = 0L
            z.dw3 = 0L
            setDigitCount128(z)
            return if (negCarry1 < 0) EXACT_NEGATED else EXACT
        }

        val d2a = x.dw2 - y.dw2
        val carry2a = if (compareUnsigned(d2a, x.dw2) > 0) 1L else 0L
        val d2 = d2a - carry1
        val carry2 = if (compareUnsigned(d2, d2a) > 0) 1L else carry2a
        if (maxDigitCount < POW10_256_OFFSET) {
            // if carry == 1 then complement-and-increment else NOOP
            val negCarry2 = -carry2
            z.dw0 = (d0 xor negCarry2) - negCarry2
            z.dw1 = (d1 xor negCarry2) - (negCarry2 and ((z.dw0 or -z.dw0) shr 63).inv())
            z.dw2 = (d2 xor negCarry2) - (negCarry2 and ((z.dw1 or -z.dw1) shr 63).inv())
            z.dw3 = 0L
            setDigitCount192(z)
            return if (negCarry2 < 0) EXACT_NEGATED else EXACT
        }

        val d3a = x.dw3 - y.dw3
        val carry3a = if (compareUnsigned(d3a, x.dw3) > 0) 1L else 0L
        val d3 = d3a - carry2
        val carry3 = if (compareUnsigned(d3, d3a) > 0) 1L else carry3a

        // if carry == 1 then complement-and-increment else NOOP
        val negCarry3 = -carry3
        z.dw0 = (d0 xor negCarry3) - negCarry3
        z.dw1 = (d1 xor negCarry3) - (negCarry3 and ((z.dw0 or -z.dw0) shr 63).inv())
        z.dw2 = (d2 xor negCarry3) - (negCarry3 and ((z.dw1 or -z.dw1) shr 63).inv())
        z.dw3 = (d3 xor negCarry3) - (negCarry3 and ((z.dw2 or -z.dw2) shr 63).inv())
        setDigitCount256(z)
        return if (negCarry3 < 0) EXACT_NEGATED else EXACT
    }

    private fun coeffAbsDiffScaled(sum: Coeff, x: Coeff, y: Coeff, scaleDelta: Int): Residue {
        assert(x.digitCount > 0)
        assert(y.digitCount > 0)
        assert(scaleDelta == x.digitCount - y.digitCount)
        assert(scaleDelta > 0)
        if (scaleDelta >= y.digitCount) {
            val residue = (
                    if (scaleDelta == y.digitCount)
                        Residue.residueFrom(y)
                    else
                        Residue.LT_HALF
                    )
            sum.set(x)
            return residue
        }
        return absDiffScaledOverlap(sum, x, y, scaleDelta)
    }

    private fun absDiffScaledOverlap(
        z: Coeff, x: Coeff, y: Coeff,
        scaleDelta: Int
    ): Residue {
        assert(scaleDelta > 0)
        assert(scaleDelta < y.digitCount)
        assert((x.dw3 or x.dw2) == 0L)
        assert((y.dw3 or y.dw2) == 0L)
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        assert(z.isValidDigitCount())

        throw RuntimeException("not impl")
        //coeffScaleFmaPow10(z, x, scaleDelta, y, sign, ctx)
    }

}