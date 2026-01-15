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
                        val t = if (m === z) MutDec() else z
                        val residue = u256ScaleDownPow10(t, n, shiftRight)
                        if (shiftLeft > 0)
                            u256AddScaled(z, m, shiftLeft, t)
                        else
                            u256AddUnscaled(z, m, t)
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
                z.c256Set(n)
                z.qExp = n.qExp
                return Residue.EXACT
            }
        }
    }

    // uses Guard digit
    // decrements when non-exact so that standard round and finalize routine can be called
    fun magScaledSub(z: MutDec, x: MutDec, y: MutDec, env: DecEnv): Residue {
        check(!x.isZero())
        check(!y.isZero())
        check(x.magnitudeCompareTo(y) > 0)
        check(x.qExp != y.qExp)

        // DEBUG OUTPUT
        println("=== magScaledSub DEBUG ===")
        println("x.digitLen = ${x.digitLen}, x.qExp = ${x.qExp}")
        println("y.digitLen = ${y.digitLen}, y.qExp = ${y.qExp}")
        println("x.qExp > y.qExp: ${x.qExp > y.qExp}")

        if (x.qExp > y.qExp) {
            val gap = x.qExp - y.qExp
            val headroomWithGuard = 1 + env.precision - x.digitLen
            val shiftLeft = if (headroomWithGuard > 0) {
                min(gap, headroomWithGuard)
            } else {
                0
            }
            val qAlign = x.qExp - shiftLeft
            val shiftRight = qAlign - y.qExp

            println("BRANCH: x.qExp > y.qExp")
            println("gap = $gap")
            println("headroomWithGuard = $headroomWithGuard")
            println("shiftLeft = $shiftLeft")
            println("qAlign = $qAlign")
            println("shiftRight = $shiftRight")
            println("y.digitLen = ${y.digitLen}")
            println("shiftRight >= y.digitLen: ${shiftRight >= y.digitLen}")
            println("=========================")

            val residue = when {
                shiftRight == 0 -> {
                    check(shiftLeft > 0)
                    u256SubScaled(z, x, shiftLeft, y)
                    Residue.EXACT
                }

                shiftRight >= y.digitLen -> {
                    if (shiftLeft > 0) {
                        u256ScaleUpPow10(z, x, shiftLeft)
                    } else {
                        z.c256Set(x)
                    }
                    if (shiftRight > y.digitLen)
                        Residue.LT_HALF
                    else
                        Residue.residueFrom(y).subtractionInverse()
                }

                else -> {
                    val tempY = MutDec()
                    val residue = u256ScaleDownPow10(tempY, y, shiftRight)
                    if (shiftLeft > 0) {
                        u256SubScaled(z, x, shiftLeft, tempY)
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
            val gap = y.qExp - x.qExp

            println("BRANCH: x.qExp < y.qExp")
            println("gap = $gap")
            println("=========================")

            val qDeltaY = y.qExp - x.qExp
            check(qDeltaY < env.precision)
            U256Sub.u256SubScaled(z, x, y, qDeltaY)
            z.qExp = x.qExp
            return EXACT
        }
    }

}
