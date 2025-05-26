package com.decimal128

import com.decimal128.CoeffAdd.coeffAddScaled
import com.decimal128.CoeffAdd.coeffAddUnscaled
import com.decimal128.CoeffScalePow10.coeffScaleDownPow10
import com.decimal128.CoeffScalePow10.coeffScaleUpPow10
import com.decimal128.CoeffSub.coeffSubScaled
import com.decimal128.Residue.Companion.EXACT

object MagAddSub {

    fun magAdd(z: Mag, x: Mag, y: Mag): Residue {
        if (x.qExp == y.qExp) {
            z.qExp = x.qExp
            coeffAddUnscaled(z, x, y)
            return Residue.EXACT
        }
        val flipFlop = x.qExp > y.qExp
        val m = if (flipFlop) x else y
        val n = if (flipFlop) y else x
        assert(m.qExp > n.qExp)
        val qDelta = m.qExp - n.qExp
        val headroom = PRECISION_34 - m.digitLen
        val shiftLeft = Math.min(qDelta, headroom)
        val qAlign = m.qExp - shiftLeft
        when {
            (m.bitLen > 0 && n.bitLen > 0) -> {
                val shiftRight = qAlign - n.qExp
                val residue = when {
                    shiftRight == 0 -> {
                        assert(shiftLeft > 0)
                        coeffAddScaled(z, m, shiftLeft, n)
                        Residue.EXACT
                    }

                    shiftRight >= n.digitLen -> {
                        coeffScaleUpPow10(z, m, shiftLeft)
                        if (shiftRight > n.digitLen)
                            Residue.LT_HALF
                        else
                            Residue.residueFrom(n)
                    }

                    else -> {
                        // shift right required ... shift left maybe
                        // shift right first into our destination
                        // then do a fused scaling, allowing us to
                        // perform this op without allocating of temp variables
                        val residue = coeffScaleDownPow10(z, n, shiftRight)
                        if (shiftLeft > 0)
                            coeffAddScaled(z, m, shiftLeft, z)
                        else
                            coeffAddUnscaled(z, m, z)
                        residue
                    }
                }
                z.qExp = qAlign
                return residue
            }
            // one of the two is zero
            // return the value of the non-zero (if any), scaled to the smaller exponent
            (m.bitLen > 0) -> {
                z.qExp = qAlign
                coeffScaleUpPow10(z, m, shiftLeft)
                return Residue.EXACT
            }

            else -> {
                // if m == 0 then return n ... n != 0 and n == 0
                z.magSet(n)
                return Residue.EXACT
            }
        }
    }

    // uses Guard and Round digits
    // decrements when non-exact so that standard round and finalize routine can be called
    fun magSub(z: Mag, x: Mag, y: Mag): Residue {
        assert(x.magCompareTo(y) >= 0)
        if (x.qExp == y.qExp) {
            z.qExp = x.qExp
            CoeffSub.coeffSubUnscaled(z, x, y)
            return Residue.EXACT
        }
        val qDelta = Math.abs(x.qExp - y.qExp)
        val headroomWithGuardRound = 2 + PRECISION_34 - x.digitLen
        val shiftLeft = Math.min(qDelta, headroomWithGuardRound)
        val qAlign = x.qExp - shiftLeft
        if (x.qExp > y.qExp) {
            when {
                (x.bitLen > 0 && y.bitLen > 0) -> {
                    val shiftRight = qAlign - y.qExp
                    val residue = when {
                        shiftRight == 0 -> {
                            assert(shiftLeft > 0)
                            coeffSubScaled(z, x, shiftLeft, y)
                            Residue.EXACT
                        }

                        shiftRight >= y.digitLen -> {
                            // swamp cases
                            coeffScaleUpPow10(z, x, shiftLeft)
                            // we always decrement in this case because y is never zero
                            // so Residue.EXACT cannot occur because ZERO would have taken
                            // another path
                            z.coeffMutateDecrement()
                            if (shiftRight > y.digitLen)
                                Residue.GT_HALF
                            else
                                Residue.residueFrom(y).subtractionInverse()
                        }

                        else -> {
                            // shift right required ... shift left maybe
                            // shift right first into our destination
                            // then do a fused scaling, allowing us to
                            // perform this op without allocating of temp variables
                            val residue = coeffScaleDownPow10(z, y, shiftRight)
                            if (shiftLeft > 0)
                                coeffSubScaled(z, x, shiftLeft, z)
                            else
                                coeffAddUnscaled(z, x, z)
                            // if ! EXACT then decrement,
                            // take the inverse of the residue,
                            // and the normal roundAndFinalize() will take care of it
                            if (residue != EXACT)
                                z.coeffMutateDecrement()
                            residue.subtractionInverse()
                        }
                    }
                    z.qExp = qAlign
                    return residue
                }
                // one of the two is zero
                // return the value of the non-zero (if any), scaled to the smaller exponent
                (x.bitLen > 0) -> {
                    z.qExp = qAlign
                    coeffScaleUpPow10(z, x, shiftLeft)
                    return Residue.EXACT
                }

                else -> {
                    // if x == 0 then return y ... y != 0 and y == 0
                    z.magSet(y)
                    return Residue.EXACT
                }
            }
        } else {
            // TC("22E1", "2E2"),
            // x has a smaller q, but y needs to be scaled
            if (y.coeffIsZero()) {
                // subtracting zero with a larger exponent from x
                // simply return x
                z.magSet(x)
                return EXACT
            }
            val qDeltaY = y.qExp - x.qExp
            assert(qDeltaY < PRECISION_34)
            CoeffSub.coeffSubScaled(z, x, y, qDeltaY)
            z.qExp = x.qExp
            val zDigitLen = z.digitLen
            val zExp = z.qExp
            return EXACT
        }
    }

}
