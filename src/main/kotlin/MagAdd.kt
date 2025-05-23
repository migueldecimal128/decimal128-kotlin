package com.decimal128

import com.decimal128.CoeffAbsDiff.coeffAbsDiffScaled
import com.decimal128.CoeffAbsDiff.coeffAbsDiffUnscaled
import com.decimal128.CoeffAdd.coeffAddScaled
import com.decimal128.CoeffAdd.coeffAddUnscaled
import com.decimal128.CoeffScalePow10.coeffScaleDownPow10
import com.decimal128.CoeffScalePow10.coeffScaleUpPow10
import com.decimal128.Residue.Companion.EXACT
import com.decimal128.Residue.Companion.GT_HALF

object MagAdd {

    fun magAdd(z: Mag, x: Mag, y: Mag): Residue {
        if (x.qExp == y.qExp) {
            z.qExp = x.qExp
            coeffAddUnscaled(z.c, x.c, y.c)
            return Residue.EXACT
        }
        val flipFlop = x.qExp > y.qExp
        val m = if (flipFlop) x else y
        val n = if (flipFlop) y else x
        assert(m.qExp > n.qExp)
        val qDelta = m.qExp - n.qExp
        val headroom = PRECISION_34 - m.c.digitLen
        val shiftLeft = Math.min(qDelta, headroom)
        val qAlign = m.qExp - shiftLeft
        when {
            (m.c.bitLen > 0 && n.c.bitLen > 0) -> {
                val shiftRight = qAlign - n.qExp
                val residue = when {
                    shiftRight == 0 -> {
                        assert(shiftLeft > 0)
                        coeffAddScaled(z.c, m.c, shiftLeft, n.c)
                        Residue.EXACT
                    }

                    shiftRight >= n.c.digitLen -> {
                        coeffScaleUpPow10(z.c, m.c, shiftLeft)
                        if (shiftRight > n.c.digitLen)
                            Residue.LT_HALF
                        else
                            Residue.residueFrom(n.c)
                    }

                    else -> {
                        // shift right required ... shift left maybe
                        // shift right first into our destination
                        // then do a fused scaling, allowing us to
                        // perform this op without allocating of temp variables
                        val residue = coeffScaleDownPow10(z.c, n.c, shiftRight)
                        if (shiftLeft > 0)
                            coeffAddScaled(z.c, m.c, shiftLeft, z.c)
                        else
                            coeffAddUnscaled(z.c, m.c, z.c)
                        residue
                    }
                }
                z.qExp = qAlign
                return residue
            }
            // one of the two is zero
            // return the value of the non-zero (if any), scaled to the smaller exponent
            (m.c.bitLen > 0) -> {
                z.qExp = qAlign
                coeffScaleUpPow10(z.c, m.c, shiftLeft)
                return Residue.EXACT
            }
            else -> {
                // if m == 0 then return n ... n != 0 and n == 0
                z.magSet(n)
                return Residue.EXACT
            }
        }


    }

    fun magSub(z: Mag, x: Mag, y: Mag): Residue {
        assert(x.magCompareTo(y) >= 0)
        if (x.qExp == y.qExp) {
            assert(x.c.unscaledCompareTo(y.c) >= 0)
            z.qExp = x.qExp
            CoeffSub.coeffSubUnscaled(z.c, x.c, y.c)
            return EXACT
        }
        val qAlign = Math.min(x.qExp, y.qExp)
        val qDeltaX = x.qExp - qAlign
        if (qDeltaX > 0) {
            if ((x.c.digitLen + qDeltaX - y.c.digitLen) > PRECISION_34) {
                // x completely swamps y
                val guardDigitHeadroom = 1 + PRECISION_34 - x.c.digitLen
                coeffScaleUpPow10(z.c, x.c, guardDigitHeadroom)
                z.qExp = x.qExp - guardDigitHeadroom
                if (y.c.isZero()) {
                    if (z.c.isZero())
                        z.qExp = qAlign
                    return EXACT
                }
                z.c.coeffDecrement()
                return GT_HALF
            }
            CoeffSub.coeffSubScaled(z.c, x.c, qDeltaX, y.c)
            z.qExp = qAlign
            return EXACT
        } else {
            if (y.c.isZero()) {
                // subtracting zero with a larger exponent from x
                // simply return x
                z.magSet(x)
                return EXACT
            }
            // subtracting y from x, but the coefficient of y needs to be scaled
            val qDeltaY = y.qExp - qAlign
            assert(qDeltaY < PRECISION_34)
            CoeffSub.coeffSubScaled(z.c, x.c, y.c, qDeltaY)
            z.qExp = qAlign
            return EXACT
        }
    }
}
