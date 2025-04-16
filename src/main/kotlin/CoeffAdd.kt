package com.decimal128

import java.lang.Long.compareUnsigned

class CoeffAdd private constructor() {
    companion object {

        fun roundUp(c: Coefficient, ctx: Decimal128Context) {
            c.dw0 += 1
            if (c.dw0 != 0L) {
                tweakDigitCountPostRoundup(c, ctx)
                return
            }
            c.dw1 += 1
            if (c.dw1 != 0L) {
                tweakDigitCountPostRoundup(c, ctx)
                return
            }
            c.dw2 += 1
            if (c.dw2 != 0L) {
                tweakDigitCountPostRoundup(c, ctx)
                return
            }
            c.dw3 += 1
            if (c.dw3 != 0L) {
                tweakDigitCountPostRoundup(c, ctx)
                return
            }
        }

        fun addNonZero(sign:  Boolean, sum: Coefficient, x: Coefficient, y: Coefficient,
                       scaleDelta: Int, ctx: Decimal128Context) {
            assert(x.digitCount > 0)
            assert(y.digitCount > 0)
            assert(scaleDelta > 0)
            assert(scaleDelta == x.digitCount - y.digitCount)
            if (scaleDelta >= y.digitCount) {
                val residue = if (scaleDelta > y.digitCount) Residue.LT_HALF else Residue.residueFrom(y)
                val roundUp = residue.ulpBias(ctx.roundingDirection.negate(sign), x.dw0)
                sum.set(x)
                if (roundUp > 0)
                    roundUp(sum, ctx)
                ctx.setInexact()
                return
            }
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
                sum.digitCount = maxDigitCount
                tweakDigitCountOnly64(sum)
                assert(sum.isValidDigitCount())
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
                sum.digitCount = maxDigitCount
                tweakDigitCountOnly128(sum)
                assert(sum.isValidDigitCount())
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
                sum.digitCount = maxDigitCount
                tweakDigitCountOnly192(sum)
                assert(sum.isValidDigitCount())
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
            sum.digitCount = maxDigitCount
            tweakDigitCountOnly256(sum)
            assert(sum.isValidDigitCount())
        }

    }
}