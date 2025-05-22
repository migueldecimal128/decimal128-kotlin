package com.decimal128

import com.decimal128.CoeffAdd.coeffAddScaled
import com.decimal128.CoeffAdd.coeffAddUnscaled
import com.decimal128.CoeffScalePow10.coeffScaleDownPow10
import com.decimal128.CoeffScalePow10.coeffScaleUpPow10
import com.decimal128.CoeffSet.coeffSet

object MagAdd {

    fun magAdd(z: Mag, x: Mag, y: Mag): Residue {
        if (x.qExp == y.qExp) {
            coeffAddUnscaled(z.c, x.c, y.c)
            z.qExp = x.qExp
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
                coeffScaleUpPow10(z.c, m.c, shiftLeft)
                z.qExp = qAlign
                return Residue.EXACT
            }
            else -> {
                // if m == 0 then return n ... n != 0 and n == 0
                z.magSet(n)
                return Residue.EXACT
            }
        }


    }

    fun magSub(z: Mag, x: Mag, y: Mag): Pair<Boolean, Residue> {
        throw RuntimeException("not impl")
    }
}
