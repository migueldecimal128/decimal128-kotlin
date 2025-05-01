package com.decimal128

import java.lang.Long.compareUnsigned
import com.decimal128.CoeffDigitLen.tweakDigitLenAfterRoundUp
import com.decimal128.CoeffScalePow10.coeffScaleFmaPow10
import com.decimal128.FiniteSet.finiteSet
import com.decimal128.Residue.Companion.EXACT

object FiniteAdd {

    fun roundUp(f: Finite, ctx: Decimal128Context) {
        val c = f.c
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
        tweakDigitLenAfterRoundUp(c)
    }

    fun finiteAdd(z: Finite, x: Finite, y: Finite, ctx: Decimal128Context) {
        val zSign = x.sign xor y.sign
        if (x.exp >= y.exp) {
            if (!zSign) magAdd(z, x.sign, x, y, ctx) else magSub(z, x.sign, x, y, ctx)
        } else {
            if (!zSign) magAdd(z, y.sign, y, x, ctx) else magSub(z, y.sign, y, x, ctx)
        }
        z.finalize(ctx)
    }

    fun magAdd(z: Finite, sign: Boolean, x: Finite, y: Finite, ctx: Decimal128Context) {
        assert(x.exp >= y.exp)
        val exp = y.exp
        val expDelta = x.exp - y.exp
        if (expDelta >= PRECISION_34) {
            val residue = (
                    if (expDelta == PRECISION_34)
                        Residue.residueFrom(y.c)
                    else
                        Residue.LT_HALF
                    )
            val roundUp = residue.ulpRoundUp(ctx.roundingDirection.negate(sign), x.c.dw0)
            finiteSet(z, x)
            if (roundUp)
                roundUp(z, ctx)
            z.sign = sign
            if (residue.withoutNegate() != EXACT)
                ctx.setInexact()
            return
        }
        when {
            expDelta == 0 -> coeffAddUnscaled(z.c, x.c, y.c)
            expDelta > 0 -> coeffAddScaled(z.c, x.c, expDelta, y.c)
            else -> coeffAddScaled(z.c, y.c, -expDelta, x.c)
        }
        z.exp = exp
        z.sign = sign
    }

    fun coeffAddUnscaled(z: Coeff, x: Coeff, y: Coeff) {
        if (x.digitLen == 0) {
            z.set(y)
            return
        }
        if (y.digitLen == 0) {
            z.set(x)
            return
        }
        val maxDigitCount = Math.max(x.digitLen, y.digitLen)

        val x0 = x.dw0
        val y0 = y.dw0
        val p0 = x0 + y0
        z.dw0 = p0
        val carry0 = if (compareUnsigned(p0, x0) < 0) 1L else 0L
        // 64 bit boundary is a special case because 19 digits * 2 might generate carry into the next word
        // this is not the case for the 128 and 192 bit boundaries
        //
        // perhaps this test should be:
        // maxDigitCount <= 20 && (x.dw1 or y.dw1 or carry0) == 0
        if (maxDigitCount <= POW10_128_OFFSET && (x.dw1 or y.dw1 or carry0) == 0L) {
            z.dw3 = 0L; z.dw2 = 0L; z.dw1 = 0L
            z.updateLengths64()
            return
        }

        val x1 = x.dw1
        val y1 = y.dw1
        val p1a = x1 + y1
        val carry1a = if (compareUnsigned(p1a, x1) < 0) 1L else 0L
        val p1 = p1a + carry0
        z.dw1 = p1
        val carry1 = if (compareUnsigned(p1, carry0) < 0) 1L else carry1a
        if (maxDigitCount <= POW10_192_OFFSET && (x.dw2 or y.dw2 or carry1) == 0L) {
            z.dw3 = 0L; z.dw2 = 0L;
            z.updateLengths128()
            return
        }

        val x2 = x.dw2
        val y2 = y.dw2
        val p2a = x2 + y2
        val carry2a = if (compareUnsigned(p2a, x2) < 0) 1L else 0L
        val p2 = p2a + carry1
        z.dw2 = p2
        val carry2 = if (compareUnsigned(p2, carry1) < 0) 1L else carry2a
        if (maxDigitCount <= POW10_256_OFFSET && (x.dw3 or y.dw3 or carry2) == 0L) {
            z.dw3 = 0L;
            z.updateLengths192()
            return
        }

        val x3 = x.dw3
        val y3 = y.dw3
        val p3a = x3 + y3
        val carry3a = if (compareUnsigned(p3a, x3) < 0) 1L else 0L
        val p3 = p3a + carry2
        z.dw3 = p3
        val carry3 = if (compareUnsigned(p3, carry2) < 0) 1L else carry3a
        if (carry3 != 0L)
            throw RuntimeException("coefficient add overflow x:$x y:$y")
        z.updateLengths256()
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

    fun magSub(z: Finite, sign: Boolean, x: Finite, y: Finite, ctx: Decimal128Context) {
        throw RuntimeException("not impl")
    }
}
