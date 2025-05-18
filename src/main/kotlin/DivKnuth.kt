package com.decimal128

import com.decimal128.CoeffCompare.coeffCompare
import com.decimal128.CoeffCompare.coeffGT
import com.decimal128.CoeffCompare.coeffGTOne
import com.decimal128.CoeffSet.coeffSet
import com.decimal128.CoeffSet.coeffSetShiftRight
import com.decimal128.Residue.Companion.EXACT
import com.decimal128.Residue.Companion.GT_HALF
import java.lang.Long.*

object DivKnuth {

    private val q = IntArray(8)
    private val vn = IntArray(9)
    private val un = IntArray(9)

    fun knuthDivideWrapper(z: Coeff, x: Coeff, y: Coeff, wantRemainder: Boolean): Residue {
        assert(coeffGTOne(y))
        assert(coeffGT(x, y))

        un[0] = x.dw0.toInt()
        un[1] = (x.dw0 ushr 32).toInt()
        un[2] = x.dw1.toInt()
        un[3] = (x.dw1 ushr 32).toInt()
        un[4] = x.dw2.toInt()
        un[5] = (x.dw2 ushr 32).toInt()
        un[6] = x.dw3.toInt()
        un[7] = (x.dw3 ushr 32).toInt()
        un[8] = 0

        var unNonZeroLen = 8
        while (unNonZeroLen > 0 && un[unNonZeroLen - 1] == 0) {
            --unNonZeroLen
        }
        val m = unNonZeroLen

        vn[0] = y.dw0.toInt()
        vn[1] = (y.dw0 ushr 32).toInt()
        vn[2] = y.dw1.toInt()
        vn[3] = (y.dw1 ushr 32).toInt()
        vn[4] = y.dw2.toInt()
        vn[5] = (y.dw2 ushr 32).toInt()
        vn[6] = y.dw3.toInt()
        vn[7] = (y.dw3 ushr 32).toInt()

        var vnNonZeroIndex = 8
        var vnNonZeroVal = 0
        while (vnNonZeroVal == 0 && --vnNonZeroIndex >= 0) {
            vnNonZeroVal = vn[vnNonZeroIndex]
        }
        val n = vnNonZeroIndex + 1
        val s = Integer.numberOfLeadingZeros(vnNonZeroVal)

        if (s != 0) {
            for (i in n - 1 downTo 1) {
                vn[i] = (vn[i] shl s) or (vn[i - 1] ushr -s)
            }
            vn[0] = vn[0] shl s

            un[m] = un[m - 1] ushr -s
            for (i in m - 1 downTo 1) {
                un[i] = (un[i] shl s) or (un[i - 1] ushr -s)
            }
            un[0] = un[0] shl s
        }

        q.fill(0)

        knuthDivideCore(m, n)

        if (wantRemainder) {
            coeffSetShiftRight(z, un, n, s)
            return EXACT
        }

        val residue =
            if (s == 0 && un[n - 1] < 0) {
                // msb of remainder is set ... doubling it will make it bigger than the divisor
                GT_HALF
            } else {
                if (s == 0) {
                    for (i in n - 1 downTo 1) {
                        un[i] = (un[i] shl 1) or (un[i - 1] ushr -1)
                    }
                    un[0] = un[0] shl 1
                } else {
                    val s1 = s - 1
                    if (s1 > 0) {
                        for (i in 0 until n - 1) {
                            un[i] = (un[i + 1] shl -s1) or (un[i] ushr s1)
                        }
                        un[n - 1] = un[n - 1] ushr s1
                    }
                }
                val cmp = coeffCompare(y, un)
                if (cmp < 0)
                    Residue.LT_HALF
                else if (cmp == 0)
                    Residue.HALF
                else
                    GT_HALF
            }
        coeffSet(z, q, m)
        return residue
    }


    /**
     * Multi‐word division (Knuth’s Algorithm D) in base 2^32.
     *
     * q: quotient array (length ≥ m – n + 1)
     * r: remainder array (length ≥ n), or null if you don’t need it
     * u: dividend array (length = m), little‐endian (u[0] = low word)
     * v: divisor array (length = n ≥ 2), little‐endian, v[n − 1] ≠ 0
     * m: number of words in u (≥ n)
     * n: number of words in v (≥ 1)
     *
     * Returns 0 on success, 1 if (m < n || n ≤ 0 || v[n − 1] == 0).
     */

    fun knuthDivideCore(
//        q: IntArray,
//        r: IntArray,
//        u: IntArray,
//        v: IntArray,
        m: Int,
        n: Int
    ) {
        // invalid parameters?
        if (m > 8 || m < 2 || m < n || n < 2) {
            throw RuntimeException("invalid args")
        }

        val vn_1 = vn[n - 1].toLong() and MASK32
        val vn_2 = vn[n - 2].toLong() and MASK32

        // -- main loop --
        for (j in m - n downTo 0) {

            // estimate q̂ = (un[j+n]*B + un[j+n-1]) / vn[n-1]
            val hi = un[j + n].toLong() and MASK32
            val lo = un[j + n - 1].toLong() and MASK32
            //if (hi == 0L && lo < vn_1) // this would short-circuit,
            //    continue               // but probability is astronomically small
            val num = (hi shl 32) or lo
            var qhat = divideUnsigned(num, vn_1)
            var rhat = remainderUnsigned(num, vn_1)

            // correct estimate
            while ((qhat ushr 32) != 0L ||
                compareUnsigned(
                    qhat * vn_2, (rhat shl 32) + (un[j + n - 2].toLong() and MASK32)
                ) > 0
            ) {
                qhat--
                rhat += (vn[n - 1].toLong() and MASK32)
                if ((rhat ushr 32) != 0L)
                    break
            }


            // multiply & subtract
            var carry = 0L
            for (i in 0 until n) {
                val prod = qhat * (vn[i].toLong() and MASK32)
                val prodHi = prod ushr 32
                val prodLo = prod and MASK32
                val unIJ = (un[j + i].toLong() and MASK32)
                val t = unIJ - prodLo - carry
                un[j + i] = t.toInt()
                carry = prodHi - (t shr 32) // t is signed, so this should *indeed* be signed shr
            }
            val t = (un[j + n].toLong() and MASK32) - carry
            un[j + n] = t.toInt()
            q[j] = (qhat - (t ushr 63)).toInt()
            if (t < 0) {
                var c2 = 0L
                for (i in 0 until n) {
                    val sum = (un[j + i].toLong() and MASK32) +
                            (vn[i].toLong() and MASK32) + c2
                    un[j + i] = sum.toInt()
                    c2 = sum ushr 32
                }
                un[j + n] += c2.toInt()
            }
        }
    }

}