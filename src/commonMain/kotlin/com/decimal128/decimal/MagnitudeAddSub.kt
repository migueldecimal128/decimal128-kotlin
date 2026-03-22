package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

object MagnitudeAddSub {

    fun magScaledAdd(z: MutDec, sign: Boolean, x: MutDec, y: MutDec, ctx: DecContext): Residue {
        verify { x.qExp != y.qExp } // the unscaled case should have been caught earlier
        //if (x.qExp == y.qExp) {
        //    z.qExp = x.qExp
        //    u256AddUnscaled(z, x, y)
        //    return Residue.EXACT
        //}
        val flipFlop = x.qExp > y.qExp
        val m = if (flipFlop) x else y
        val n = if (flipFlop) y else x
        val qDelta = m.qExp - n.qExp
        verify { qDelta > 0 }
        val pentad = ctx.tmps.pentad1
        val headroom = ctx.precision - m.digitLen
        val shiftLeft = min(max(headroom, 0), qDelta)
        val qAlign = m.qExp - shiftLeft
        when {
            (m.bitLen > 0 && n.bitLen > 0) -> {
                val shiftRight = qAlign - n.qExp
                val residue = when {
                    shiftRight == 0 -> {
                        verify { shiftLeft > 0 }
                        c256SetAddScaled(z, m, shiftLeft, n, pentad)
                        EXACT
                    }

                    shiftRight >= n.digitLen -> {
                        // perform in this order to avoid aliasing z === n issue
                        val residueT = if (shiftRight > n.digitLen)
                            Residue.LT_HALF
                        else
                            Residue.fromValueDecade(n)
                        c256SetScaleUpPow10(z, m, shiftLeft, pentad)
                        residueT
                    }

                    else -> {
                        // shift right required ... shift left maybe
                        // shift right first into our destination
                        // then do a fused scaling, allowing us to
                        // perform this op without allocating of temp variables
                        val t = if (m === z) MutDec() else z
                        val residue = c256SetScaleDownPow10(t, n, shiftRight, pentad)
                        if (shiftLeft > 0)
                            c256SetAddScaled(z, m, shiftLeft, t)
                        else
                            c256SetAddUnscaled(z, m, t, pentad)
                        residue
                    }
                }
                z.type = STEAL_TYP_FNZ
                z.qExp = qAlign
                z.sign = sign
                return residue
            }
            // one of the two is zero
            // return the value of the non-zero (if any), scaled to the smaller exponent
            (m.bitLen > 0) -> {
                z.type = STEAL_TYP_FNZ
                z.qExp = qAlign
                c256SetScaleUpPow10(z, m, shiftLeft, ctx.tmps.pentad1)
                z.sign = sign
                return EXACT
            }

            else -> {
                // if m == 0 then return n ... n != 0 and n == 0
                z.c256Set(n)
                z.type = n.type
                z.qExp = n.qExp
                z.sign = sign
                return EXACT
            }
        }
    }

    // uses Guard digit
    // decrements when non-exact so that standard round and finalize routine can be called
    // m == minuend  s == subtrahend
    fun magScaledSub(z: MutDec, mSign: Boolean, m: MutDec, s: MutDec, ctx: DecContext): Residue {
        verify { !m.isZero() }
        verify { !s.isZero() }
        val pentad = ctx.tmps.pentad1
        verify { m.compareNumericMagnitudeTo(s, pentad) > 0 }
        verify { m.qExp != s.qExp }
        if (m.qExp > s.qExp) {
            val gap = m.qExp - s.qExp
            val headroomWithGuard =
                if (s.digitLen > ctx.precision) {
                    // It is possible for y.digitLen > precision because
                    // of intermediate result of a FMA operation.
                    // In this case we might have to scale x.coeff up to
                    // x.digitLen == y.digitLen
                    // This will not exceed our 256-bit ALU capacity
                    s.digitLen - m.digitLen
                } else {
                    1 + ctx.precision - m.digitLen  // Standard with guard
                }
            val shiftMLeft = min(gap, max(0, headroomWithGuard))

            val qAlign = m.qExp - shiftMLeft
            val shiftSRight = qAlign - s.qExp
            verify { shiftSRight >= 0 }

            val residue = when {
                shiftSRight == 0 -> {
                    verify { shiftMLeft > 0 }
                    c256SetSubScaled(z, m, shiftMLeft, s, pentad) // z = (x * 10^shiftXLeft) - y
                    EXACT
                }

                shiftSRight >= s.digitLen -> {
                    val residueT = if (shiftSRight > s.digitLen)
                        Residue.GT_HALF // actually Residue.LT_HALF.subtractionInverse()
                    else
                        Residue.fromValueDecade(s).subtractionInverse()
                    if (shiftMLeft > 0) {
                        c256SetScaleUpPow10(z, m, shiftMLeft, ctx.tmps.pentad1)
                    } else {
                        z.c256Set(m)
                    }
                    verify { residueT != EXACT }
                    // decrement and let the residue possibly round it back up
                    z.c256MutateDecrement()
                    residueT
                }

                else -> { // shiftYRight > 0
                    // There is overlap and there will be residue.
                    // align x by shiftLeftX
                    //
                    val tmpY = MutDec()
                    val residue = c256SetScaleDownPow10(tmpY, s, shiftSRight, pentad)
                    if (shiftMLeft > 0)
                        c256SetSubScaled(z, m, shiftMLeft, tmpY, pentad)
                    else
                        c256SetSubUnscaled(z, m, tmpY)
                    if (residue != EXACT)
                        z.c256MutateDecrement()
                    residue.subtractionInverse()
                }
            }
            z.type = STEAL_TYP_FNZ
            z.qExp = qAlign
            z.sign = mSign
            return residue

        } else {
            // this branch is relatively simple
            // abs(x) > abs(y)
            // x.qExp < y.qExp
            // this can only happen if x's coefficient
            // is larger than y
            // 99999e0 - 1e1
            // just adjust y's coefficient and subtract
            val gap = s.qExp - m.qExp
            c256FusedSubMulPow10(z, m, s, gap, pentad)
            z.type = STEAL_TYP_FNZ
            z.qExp = m.qExp
            z.sign = mSign
            return EXACT
        }
    }
}
