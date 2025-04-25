package com.decimal128

import java.lang.Long.compareUnsigned
import com.decimal128.DigitCount.tweakDigitCountAfterRoundUp
import com.decimal128.DigitCount.setDigitCount64
import com.decimal128.DigitCount.setDigitCount128
import com.decimal128.DigitCount.setDigitCount192
import com.decimal128.DigitCount.setDigitCount256

object CoeffAdd {

    fun roundUp(c: Coeff, ctx: Decimal128Context) {
        c.dw0 += 1
        if (c.dw0 == 0L) {
            c.dw1 += 1
            if (c.dw1 == 0L) {
                c.dw2 += 1
                if (c.dw2 == 0L) {
                    c.dw3 += 1
                    if (c.dw3 == 0L)
                        throw RuntimeException("overflow")
                }
            }
        }
        tweakDigitCountAfterRoundUp(c)
    }

    fun coeffAdd(
        sum: Coeff, x: Coeff, y: Coeff,
        scaleDelta: Int, sign: Boolean, ctx: Decimal128Context
    ) {
        if (x.digitCount == 0) {
            sum.set(y)
            return
        }
        if (y.digitCount == 0) {
            sum.set(x)
            return
        }
        if (scaleDelta == 0) {
            coeffAddUnscaled(sum, x, y)
            return
        }
        if (scaleDelta > 0) {
            coeffAddScaled(sum, x, y, scaleDelta, sign, ctx)
        } else {
            coeffAddScaled(sum, y, x, -scaleDelta, sign, ctx)
        }

    }

    fun coeffAddUnscaled(sum: Coeff, x: Coeff, y: Coeff) {
        val maxDigitCount = Math.max(x.digitCount, y.digitCount)

        val x0 = x.dw0
        val y0 = y.dw0
        val p0 = x0 + y0
        sum.dw0 = p0
        val carry0 = if (compareUnsigned(p0, x0) < 0) 1L else 0L
        // 64 bit boundary is a special case because 19 digits * 2 might generate carry into the next word
        // this is not the case for the 128 and 192 bit boundaries
        //
        // perhaps this test should be:
        // maxDigitCount <= 20 && (x.dw1 or y.dw1 or carry0) == 0
        if (maxDigitCount <= POW10_128_OFFSET && (x.dw1 or y.dw1 or carry0) == 0L) {
            sum.dw3 = 0L; sum.dw2 = 0L; sum.dw1 = 0L
            setDigitCount64(sum)
            return
        }

        val x1 = x.dw1
        val y1 = y.dw1
        val p1a = x1 + y1
        val carry1a = if (compareUnsigned(p1a, x1) < 0) 1L else 0L
        val p1 = p1a + carry0
        sum.dw1 = p1
        val carry1 = if (compareUnsigned(p1, carry0) < 0) 1L else carry1a
        if (maxDigitCount <= POW10_192_OFFSET && (x.dw2 or y.dw2 or carry1) == 0L) {
            sum.dw3 = 0L; sum.dw2 = 0L;
            setDigitCount128(sum)
            return
        }

        val x2 = x.dw2
        val y2 = y.dw2
        val p2a = x2 + y2
        val carry2a = if (compareUnsigned(p2a, x2) < 0) 1L else 0L
        val p2 = p2a + carry1
        sum.dw2 = p2
        val carry2 = if (compareUnsigned(p2, carry1) < 0) 1L else carry2a
        if (maxDigitCount <= POW10_256_OFFSET && (x.dw3 or y.dw3 or carry2) == 0L) {
            sum.dw3 = 0L;
            setDigitCount192(sum)
            return
        }

        val x3 = x.dw3
        val y3 = y.dw3
        val p3a = x3 + y3
        val carry3a = if (compareUnsigned(p3a, x3) < 0) 1L else 0L
        val p3 = p3a + carry2
        sum.dw3 = p3
        val carry3 = if (compareUnsigned(p3, carry2) < 0) 1L else carry3a
        if (carry3 != 0L)
            throw RuntimeException("coefficient add overflow x:$x y:$y")
        setDigitCount256(sum)
    }


    fun coeffAddScaled(
        sum: Coeff, x: Coeff, y: Coeff,
        scaleDelta: Int, sign: Boolean, ctx: Decimal128Context
    ) {
        assert(x.digitCount > 0)
        assert(y.digitCount > 0)
        assert(scaleDelta > 0)
        assert(scaleDelta == x.digitCount - y.digitCount)
        if (scaleDelta >= y.digitCount) {
            val residue = (
                    if (scaleDelta == y.digitCount)
                        Residue.residueFrom(y)
                    else
                        Residue.LT_HALF
                    )
            val roundUp = residue.ulpRoundUp(ctx.roundingDirection.negate(sign), x.dw0)
            sum.set(x)
            if (roundUp)
                roundUp(sum, ctx)
            ctx.setInexact()
            return
        }
        addScaledOverlap(sum, x, y, scaleDelta, sign, ctx)
    }

    fun addScaledOverlap(
        sum: Coeff, x: Coeff, y: Coeff,
        scaleDelta: Int, sign: Boolean, ctx: Decimal128Context
    ) {
        // FIXME above this is correct, but below has not been modified for scaling + rounding
        val maxDigitCount = Math.max(x.digitCount, y.digitCount)

        val x0 = x.dw0
        val y0 = y.dw0
        val p0 = x0 + y0
        sum.dw0 = p0
        val carry0 = if (compareUnsigned(p0, x0) < 0) 1L else 0L
        // 64 bit boundary is a special case because 19 digits * 2 might generate carry into the next word
        // this is not the case for the 128 and 192 bit boundaries
        //
        // perhaps this test should be:
        // maxDigitCount <= 20 && (x.dw1 or y.dw1 or carry0) == 0
        if (maxDigitCount <= POW10_128_OFFSET && (x.dw1 or y.dw1 or carry0) == 0L) {
            sum.dw3 = 0L; sum.dw2 = 0L; sum.dw1 = 0L
            setDigitCount64(sum)
            return
        }

        val x1 = x.dw1
        val y1 = y.dw1
        val p1a = x1 + y1
        val carry1a = if (compareUnsigned(p1a, x1) < 0) 1L else 0L
        val p1 = p1a + carry0
        sum.dw1 = p1
        val carry1 = if (compareUnsigned(p1, carry0) < 0) 1L else carry1a
        if (maxDigitCount <= POW10_192_OFFSET && (x.dw2 or y.dw2 or carry1) == 0L) {
            sum.dw3 = 0L; sum.dw2 = 0L;
            setDigitCount128(sum)
            return
        }

        val x2 = x.dw2
        val y2 = y.dw2
        val p2a = x2 + y2
        val carry2a = if (compareUnsigned(p2a, x2) < 0) 1L else 0L
        val p2 = p2a + carry1
        sum.dw2 = p2
        val carry2 = if (compareUnsigned(p2, carry1) < 0) 1L else carry2a
        if (maxDigitCount <= POW10_256_OFFSET && (x.dw3 or y.dw3 or carry2) == 0L) {
            sum.dw3 = 0L;
            setDigitCount192(sum)
            return
        }

        val x3 = x.dw3
        val y3 = y.dw3
        val p3a = x3 + y3
        val carry3a = if (compareUnsigned(p3a, x3) < 0) 1L else 0L
        val p3 = p3a + carry2
        sum.dw3 = p3
        val carry3 = if (compareUnsigned(p3, carry2) < 0) 1L else carry3a
        if (carry3 != 0L)
            throw RuntimeException("coefficient add overflow x:$x y:$y")
        setDigitCount256(sum)
    }

    fun coeffAbsDiffUnscaled(z:Coeff, x:Coeff, y:Coeff) : Long { // minuend - subtrahend
        assert(z.isValidDigitCount())
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        val maxDigitCount = Math.max(x.digitCount, y.digitCount)
        val loDigitCount = maxDigitCount - 1
        val minDigitCount = Math.min(x.digitCount, y.digitCount)
        val digitCountDiff = maxDigitCount - minDigitCount

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
            return negCarry0
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
            return negCarry1
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
            return negCarry2
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
        return negCarry3
    }


}