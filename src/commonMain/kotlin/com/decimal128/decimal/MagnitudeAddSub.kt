package com.decimal128.decimal

import com.decimal128.decimal.U256Add.u256AddScaled
import com.decimal128.decimal.U256Add.u256AddUnscaled
import com.decimal128.decimal.U256ScalePow10.u256ScaleDownPow10
import com.decimal128.decimal.U256ScalePow10.u256ScaleUpPow10
import com.decimal128.decimal.U256Sub.u256SubScaled
import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

object MagnitudeAddSub {

    fun magScaledAdd(z: MutDec, x: MutDec, y: MutDec, env: DecEnv): Residue {
        check(x.qExp != y.qExp) // the unscaled case should have been caught earlier
        //if (x.qExp == y.qExp) {
        //    z.qExp = x.qExp
        //    u256AddUnscaled(z, x, y)
        //    return Residue.EXACT
        //}
        val flipFlop = x.qExp > y.qExp
        val m = if (flipFlop) x else y
        val n = if (flipFlop) y else x
        check(m.qExp > n.qExp)
        val qDelta = m.qExp - n.qExp
        val headroom = env.precision - m.digitLen
        val shiftLeft = min(max(headroom, 0), qDelta)
        val qAlign = m.qExp - shiftLeft
        when {
            (m.bitLen > 0 && n.bitLen > 0) -> {
                val shiftRight = qAlign - n.qExp
                val residue = when {
                    shiftRight == 0 -> {
                        check(shiftLeft > 0)
                        u256AddScaled(z, m, shiftLeft, n)
                        Residue.EXACT
                    }

                    shiftRight >= n.digitLen -> {
                        u256ScaleUpPow10(z, m, shiftLeft)
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
                        val residue = u256ScaleDownPow10(z, n, shiftRight)
                        if (shiftLeft > 0)
                            u256AddScaled(z, m, shiftLeft, z)
                        else
                            u256AddUnscaled(z, m, z)
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
                u256ScaleUpPow10(z, m, shiftLeft)
                return Residue.EXACT
            }

            else -> {
                // if m == 0 then return n ... n != 0 and n == 0
                z.u256Set(n)
                z.qExp = n.qExp
                return Residue.EXACT
            }
        }
    }

    // uses Guard digit
    // decrements when non-exact so that standard round and finalize routine can be called
    fun magSub(z: MutDec, x: MutDec, y: MutDec, env: DecEnv): Residue {
        check(x.magnitudeCompareTo(y) >= 0)
        check(x.qExp != y.qExp) // should be caught earlier
        //if (x.qExp == y.qExp) {
        //    z.qExp = x.qExp
        //    U256Sub.u256SubUnscaled(z, x, y)
        //    return Residue.EXACT
        //}
        if (x.qExp > y.qExp) {
            val qDelta = x.qExp - y.qExp
            // one guard digit is enough ...
            // ... residue provides sufficient info for rounding
            val headroomWithGuard = 1 + env.precision - x.digitLen
            // shiftLeft is always >0 because guard digit provides 1 digit of headroom
            val shiftLeft = min(qDelta, headroomWithGuard)
            check (shiftLeft > 0)
            val qAlign = x.qExp - shiftLeft
            when {
                (x.bitLen > 0 && y.bitLen > 0) -> {
                    val shiftRight = qAlign - y.qExp
                    val residue = when {
                        shiftRight == 0 -> {
                            check(shiftLeft > 0)
                            u256SubScaled(z, x, shiftLeft, y)
                            Residue.EXACT
                        }

                        shiftRight >= y.digitLen -> {
                            // swamp cases
                            u256ScaleUpPow10(z, x, shiftLeft)
                            // we always decrement in this case because y is never zero
                            // so Residue.EXACT cannot occur because ZERO would have taken
                            // another path
                            z.u256MutateDecrement()
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
                            val residue = u256ScaleDownPow10(z, y, shiftRight)
                            u256SubScaled(z, x, shiftLeft, z)
                            // if ! EXACT then decrement,
                            // take the inverse of the residue,
                            // and the normal roundAndFinalize() will take care of it
                            if (residue != EXACT)
                                z.u256MutateDecrement()
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
                    u256ScaleUpPow10(z, x, shiftLeft)
                    return Residue.EXACT
                }

                else -> {
                    // if x == 0 then return y ... y != 0 and y == 0
                    z.u256Set(y)
                    z.qExp = y.qExp
                    return Residue.EXACT
                }
            }
        } else {
            // TC("22E1", "2E2"),
            // x has a smaller q, but y needs to be scaled
            if (y.u256IsZero()) {
                // subtracting zero with a larger exponent from x
                // simply return x
                z.u256Set(x)
                z.qExp = x.qExp
                return EXACT
            }
            val qDeltaY = y.qExp - x.qExp
            // FIXME
            //  PRECISION needs to come from DecEnv
            check(qDeltaY < PRECISION_34)
            U256Sub.u256SubScaled(z, x, y, qDeltaY)
            z.qExp = x.qExp
            val zDigitLen = z.digitLen
            val zExp = z.qExp
            return EXACT
        }
    }

}
