package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

object MagnitudeAddSub {

    fun magScaledAdd(z: MutDec, x: MutDec, y: MutDec, env: DecContext): Residue {
        verify { x.qExp != y.qExp } // the unscaled case should have been caught earlier
        //if (x.qExp == y.qExp) {
        //    z.qExp = x.qExp
        //    u256AddUnscaled(z, x, y)
        //    return Residue.EXACT
        //}
        val flipFlop = x.qExp > y.qExp
        val m = if (flipFlop) x else y
        val n = if (flipFlop) y else x
        verify { m.qExp > n.qExp }
        val qDelta = m.qExp - n.qExp
        val headroom = env.precision - m.digitLen
        val shiftLeft = min(max(headroom, 0), qDelta)
        val qAlign = m.qExp - shiftLeft
        when {
            (m.bitLen > 0 && n.bitLen > 0) -> {
                val shiftRight = qAlign - n.qExp
                val residue = when {
                    shiftRight == 0 -> {
                        verify { shiftLeft > 0 }
                        c256SetAddScaled(z, m, shiftLeft, n)
                        EXACT
                    }

                    shiftRight >= n.digitLen -> {
                        // perform in this order to avoid aliasing z === n issue
                        val residueT = if (shiftRight > n.digitLen)
                            Residue.LT_HALF
                        else
                            Residue.residueFrom(n)
                        c256SetScaleUpPow10(z, m, shiftLeft)
                        residueT
                    }

                    else -> {
                        // shift right required ... shift left maybe
                        // shift right first into our destination
                        // then do a fused scaling, allowing us to
                        // perform this op without allocating of temp variables
                        val t = if (m === z) MutDec() else z
                        val residue = c256SetScaleDownPow10(t, n, shiftRight)
                        if (shiftLeft > 0)
                            c256SetAddScaled(z, m, shiftLeft, t)
                        else
                            c256SetAddUnscaled(z, m, t)
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
                c256SetScaleUpPow10(z, m, shiftLeft)
                return EXACT
            }

            else -> {
                // if m == 0 then return n ... n != 0 and n == 0
                z.c256Set(n)
                z.qExp = n.qExp
                return EXACT
            }
        }
    }

    // uses Guard digit
    // decrements when non-exact so that standard round and finalize routine can be called
    fun magScaledSub(z: MutDec, x: MutDec, y: MutDec, env: DecContext): Residue {
        verify { !x.isZero() }
        verify { !y.isZero() }
        verify { x.magnitudeCompareTo(y) > 0 }
        verify { x.qExp != y.qExp }

        if (x.qExp > y.qExp) {
            val gap = x.qExp - y.qExp
            val headroomWithGuard = if (y.digitLen > env.precision) {
                y.digitLen - x.digitLen  // Just make room for all of y
            } else {
                1 + env.precision - x.digitLen  // Standard with guard
            }
            val shiftLeft = if (headroomWithGuard > 0) {
                min(gap, headroomWithGuard)
            } else {
                0
            }
            val qAlign = x.qExp - shiftLeft
            val shiftRight = qAlign - y.qExp

            val residue = when {
                shiftRight == 0 -> {
                    verify { shiftLeft > 0 }
                    c256SetSubScaled(z, x, shiftLeft, y)
                    EXACT
                }

                shiftRight >= y.digitLen -> {
                    val residueT = if (shiftRight > y.digitLen)
                        Residue.GT_HALF // actually Residue.LT_HALF.subtractionInverse()
                    else
                        Residue.residueFrom(y).subtractionInverse()
                    if (shiftLeft > 0) {
                        c256SetScaleUpPow10(z, x, shiftLeft)
                    } else {
                        z.c256Set(x)
                    }
                    verify { residueT != EXACT }
                    // decrement and let the residue possibly round it back up
                    z.c256MutateDecrement()
                    residueT
                }

                else -> {
                    val tempY = MutDec()
                    val residue = c256SetScaleDownPow10(tempY, y, shiftRight)
                    if (shiftLeft > 0) {
                        c256SetSubScaled(z, x, shiftLeft, tempY)
                        if (residue != EXACT)
                            z.c256MutateDecrement()
                    } else {
                        z.c256SetSub(x, tempY)  // Now uses tempY instead of z
                        if (residue != EXACT && z.bitLen > 0)
                            z.c256MutateDecrement()
                    }
                    residue.subtractionInverse()
                }
            }
            z.qExp = qAlign
            return residue

        } else {
            // x.qExp < y.qExp
            val gap = y.qExp - x.qExp

            val headroom = 77 - y.digitLen
            val shiftLeft = min(gap, headroom)
            val qAlign = y.qExp - shiftLeft
            val shiftRight = x.qExp - qAlign   // >= 0

            val residue = when {
                shiftRight == 0 -> {
                    verify { shiftLeft > 0 } // because x.qExp != y.qExp
                    c256SetSubScaled(z, x, y, shiftLeft)   // (x * 10^shiftLeft) - y
                    EXACT
                }

                shiftRight >= y.digitLen -> {
                    // y becomes "all dropped" at this alignment
                    val residueT =
                        if (shiftRight > y.digitLen) Residue.GT_HALF
                        else Residue.residueFrom(y)

                    if (shiftLeft > 0) c256SetScaleUpPow10(z, x, shiftLeft) else z.c256Set(x)

                    // bias down by 1; residue will decide if it rounds back up
                    z.c256MutateDecrement()

                    // because the dropped part belonged to the SUBTRAHEND, keep your convention:
                    residueT.subtractionInverse()
                }

                else -> {
                    val tempY = MutDec().set(y)
                    c256SetScaleUpPow10(tempY, y, shiftLeft) // y -> y * 10^shiftLeft (exact)
                    tempY.qExp -= shiftLeft

                    if (shiftRight > 0) {
                        val r = c256SetScaleDownPow10(z, x, shiftRight)  // x -> trunc(x / 10^shiftRight), residue r
                        z.c256SetSub(z, tempY)  // z = z - tempY
                        if (r != EXACT && z.bitLen > 0) z.c256MutateDecrement()
                        r.subtractionInverse()
                    } else {
                        z.c256SetSub(x, tempY)  // z = x - tempY
                        EXACT
                    }
                }
            }
            z.qExp = qAlign
            return residue
        }
    }

    fun magScaledSub_2(z: MutDec, x: MutDec, y: MutDec, env: DecContext): Residue {
        verify { !x.isZero() }
        verify { !y.isZero() }
        verify { x.magnitudeCompareTo(y) > 0 }
        verify { x.qExp != y.qExp }

        val qMax = max(x.qExp, y.qExp)
        val qMin = min(x.qExp, y.qExp)
        val gap = qMax - qMin

        // Determine which operand is at which exponent
        val (hiExpOperand, loExpOperand) = if (x.qExp > y.qExp) {
            Pair(x, y)
        } else {
            Pair(y, x)
        }

        // Calculate headroom for scaling UP the high-exp operand
        val headroomWithGuard = if (loExpOperand.digitLen > env.precision) {
            loExpOperand.digitLen - hiExpOperand.digitLen  // FMA case
        } else {
            1 + env.precision - hiExpOperand.digitLen  // Normal case
        }

        val shiftUp = if (headroomWithGuard > 0) {
            min(gap, headroomWithGuard)
        } else {
            0
        }

        val shiftDown = gap - shiftUp

        // Align to: qMax + shiftUp = qMin + gap + shiftUp = qMin + shiftDown + shiftUp
        val qAlign = qMax + shiftUp

        // Scale high-exp operand UP by shiftUp
        // Scale low-exp operand DOWN by shiftDown (or keep if shiftDown == 0)

        val residue: Residue

        if (x.qExp > y.qExp) {
            // x is high-exp, y is low-exp
            // Compute: (x scaled up) - (y scaled down)
            residue = if (shiftDown == 0) {
                // y stays, x scales up
                c256SetSubScaled(z, x, shiftUp, y)
                EXACT
            } else {
                // y scales down, x may scale up
                val tempY = MutDec()
                val res = c256SetScaleDownPow10(tempY, y, shiftDown)

                if (shiftUp > 0) {
                    c256SetSubScaled(z, x, shiftUp, tempY)
                    if (res != EXACT)
                        z.c256MutateDecrement()
                } else {
                    z.c256SetSub(x, tempY)
                    if (res != EXACT && z.bitLen > 0)
                        z.c256MutateDecrement()
                }
                res.subtractionInverse()
            }
        } else {
            // y is high-exp, x is low-exp
            // Compute: (y scaled up) - (x scaled down)
            residue = if (shiftDown == 0) {
                // x stays, y scales up
                val tempY = MutDec()
                tempY.c256SetScaleUpPow10(y, shiftUp)

                // Check coefficient order and subtract correctly
                if (tempY.c256UnscaledCompareTo(x) >= 0) {
                    z.c256SetSub(tempY, x)
                } else {
                    z.c256SetSub(x, tempY)
                }
                EXACT
            } else {
                // x scales down, y may scale up
                val tempX = MutDec()
                val res = c256SetScaleDownPow10(tempX, x, shiftDown)

                if (shiftUp > 0) {
                    val tempY = MutDec()
                    tempY.c256SetScaleUpPow10(y, shiftUp)

                    // Check coefficient order
                    if (tempY.c256UnscaledCompareTo(tempX) >= 0) {
                        z.c256SetSub(tempY, tempX)
                    } else {
                        z.c256SetSub(tempX, tempY)
                        // Shouldn't happen!
                        throw IllegalStateException("Coefficient order wrong")
                    }
                    if (res != EXACT)
                        z.c256MutateDecrement()
                } else {
                    // Check coefficient order
                    if (y.c256UnscaledCompareTo(tempX) >= 0) {
                        z.c256SetSub(y, tempX)
                    } else {
                        z.c256SetSub(tempX, y)
                        // Shouldn't happen!
                        throw IllegalStateException("Coefficient order wrong")
                    }
                    if (res != EXACT && z.bitLen > 0)
                        z.c256MutateDecrement()
                }
                res.subtractionInverse()
            }
        }

        //z.qExp = qAlign
        return residue
    }

}
