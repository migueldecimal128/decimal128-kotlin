@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.FNZ_FNZ
import com.decimal128.decimal.BinopSignature.FNZ_INF
import com.decimal128.decimal.BinopSignature.FNZ_ZER
import com.decimal128.decimal.BinopSignature.INF_FNZ
import com.decimal128.decimal.BinopSignature.INF_INF
import com.decimal128.decimal.BinopSignature.INF_ZER
import com.decimal128.decimal.BinopSignature.NAN_FOUND
import com.decimal128.decimal.BinopSignature.ZER_FNZ
import com.decimal128.decimal.BinopSignature.ZER_INF
import com.decimal128.decimal.BinopSignature.ZER_ZER
import com.decimal128.decimal.Dec2.Companion.bothFnz

object Dec2Compare {


    fun cmpTotalOrder(x: Dec2, y: Dec2): Int {
        if (bothFnz(x, y))
            return cmpTotalOrderFnzFnz(x, y)
        val cmp = when (BinopSignature.of(x, y)) {
            ZER_ZER -> cmp32(x.qExp, y.qExp)
            ZER_FNZ -> -1
            ZER_INF -> -1

            FNZ_ZER -> 1
            FNZ_FNZ -> throw IllegalStateException()
            FNZ_INF -> -1

            INF_ZER -> 1
            INF_FNZ -> 1
            INF_INF -> 0
            NAN_FOUND -> cmpTotalOrderMagnitudeNanFound(x, y)
        }
        return negateForSign(cmp, x.sign0Neg1)
    }

    private fun cmpTotalOrderFnzFnz(x: Dec2, y: Dec2): Int {
        val xSign = x.sign0Neg1 // 0 or -1 (0xFFFF_FFFF)
        if ((xSign xor y.sign0Neg1) != 0)
            return (xSign shl 1) + 1 // return -1 or 1
        val cmpMag = when {
            x.eExp > y.eExp -> 1
            x.eExp < y.eExp -> -1
            x.bExpMin > y.bExpMax -> 1
            x.bExpMax < y.bExpMin -> -1
            x.qExp == y.qExp -> ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
            x.qExp > y.qExp -> -ucmp128ScalePow10(y.dw1, y.dw0, x.dw1, x.dw0, x.qExp - y.qExp)
            // x.qExp < y.qExp
            else -> ucmp128ScalePow10(x.dw1, x.dw0, y.dw1, y.dw0, y.qExp - x.qExp)
        }

        // If x and y represent the same floating-point datum:
        //  i) If x and y have negative sign,
        //    totalOrder(x, y) is true if and only if the exponent of x ≥ the exponent of y
        //  ii) otherwise,
        //    totalOrder(x, y) is true if and only if the exponent of x ≤ the exponent of y.
        val cmpExp = cmp32(x.qExp, y.qExp)

        val cmpMagIsZeroMask = (cmpMag or -cmpMag).inv()
        val cmp = cmpMag or (cmpExp and cmpMagIsZeroMask)
        return negateForSign(cmp, xSign)
    }

    private fun cmpTotalOrderMagnitudeNanFound(x: Dec2, y: Dec2): Int {
        return when {
            x.qExp < NON_FINITE_QNAN -> -1
            y.qExp < NON_FINITE_QNAN -> 1
            // if both are the same NaN, then compare payloads
            x.qExp == y.qExp -> ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
            // strange ... qNaN sorts higher than sNaN
            x.qExp == NON_FINITE_QNAN -> 1
            else -> -1
        }
    }



    /**
     * Returns `cmp` when `sign0Neg1 == 0` and `-cmp` when `sign0Neg1 == -1`,
     * using the branch-free transform `(cmp xor sign0Neg1) - sign0Neg1`.
     *
     * Used in comparison routines to reverse magnitude comparison results
     * for negative values.
     *
     * @param cmp the magnitude comparison result (−1, 0, or +1)
     * @param sign0Neg1 0 for non-negative values, or −1 for negative values
     */
    private inline fun negateForSign(cmp: Int, sign0Neg1: Int) =
        (cmp xor sign0Neg1) - sign0Neg1

}