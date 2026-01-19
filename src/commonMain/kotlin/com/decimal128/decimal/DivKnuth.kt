package com.decimal128.decimal

import com.decimal128.bigint.Magia
import com.decimal128.decimal.U256Compare.u256UnscaledCompare
import com.decimal128.decimal.U256Compare.u256GTOne
import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.Residue.Companion.LT_HALF
import com.decimal128.decimal.Residue.Companion.HALF
import com.decimal128.decimal.Residue.Companion.GT_HALF

object DivKnuth {

    //FIXME - allocate temp space in a way that this thread-safe
    private val q = IntArray(8)
    private val vn = IntArray(9)
    private val un = IntArray(9)

    fun knuthDivideWrapper(quot: C256?, rem: C256?, x: C256, y: C256): Residue {
        verify { u256GTOne(y) }
        verify { x.c256UnscaledCompareTo(y) > 0 }

        un[0] = x.dw0.toInt()
        un[1] = (x.dw0 ushr 32).toInt()
        un[2] = x.dw1.toInt()
        un[3] = (x.dw1 ushr 32).toInt()
        un[4] = x.dw2.toInt()
        un[5] = (x.dw2 ushr 32).toInt()
        un[6] = x.dw3.toInt()
        un[7] = (x.dw3 ushr 32).toInt()
        un[8] = 0

        val m = ((x.bitLen - 1) ushr 5) + 1

        vn[0] = y.dw0.toInt()
        vn[1] = (y.dw0 ushr 32).toInt()
        vn[2] = y.dw1.toInt()
        vn[3] = (y.dw1 ushr 32).toInt()
        vn[4] = y.dw2.toInt()
        vn[5] = (y.dw2 ushr 32).toInt()
        vn[6] = y.dw3.toInt()
        vn[7] = (y.dw3 ushr 32).toInt()

        val vnNonZeroIndex = ((y.bitLen - 1) ushr 5)
        val vnNonZeroVal = vn[vnNonZeroIndex]
        val n = vnNonZeroIndex + 1
        val s = vnNonZeroVal.countLeadingZeroBits()

        if (s != 0) {
            Magia.mutateShiftLeft(un, m + 1, s)
            Magia.mutateShiftLeft(vn, n, s)
        }

        q.fill(0)

        knuthDivideCore(m, n)

        if (rem != null) {
            Magia.mutateShiftRight(un, n, s)
            rem.c256Set(un, n)
        }

        // shifting right by s will denormalize to normal remainder
        // I will shift right by s-1
        // this will give me 2*remainder that I can compare with y
        val residue =
            if (rem != null) {
                EXACT
            } else if (un[n - 1] < 0) {
                // msb of remainder is set ... doubling it will make it bigger than the divisor
                GT_HALF
            } else {
                var isZero = 0
                if (s == 0) {
                    // note that this is shifting LEFT to double the remainder
                    for (i in n - 1 downTo 1) {
                        un[i] = (un[i] shl 1) or (un[i - 1] ushr -1)
                        isZero = isZero or un[i]
                    }
                    un[0] = un[0] shl 1
                    isZero = isZero or un[0]
                } else {
                    val s1 = s - 1
                    if (s1 > 0) {
                        for (i in 0 until n - 1) {
                            un[i] = (un[i + 1] shl -s1) or (un[i] ushr s1)
                            isZero = isZero or un[i]
                        }
                        un[n - 1] = un[n - 1] ushr s1
                        isZero = isZero or un[n - 1]
                    } else {
                        // still need to test for isZero
                        var i = 0
                        do {
                            isZero = isZero or un[i]
                        } while (isZero == 0 && ++i < n)
                    }
                }
                if (isZero == 0) {
                    EXACT
                } else {
                    // note that this compare is reversed ... y compare 2*remainder
                    val cmp = u256UnscaledCompare(y, un)
                    if (cmp > 0)
                        LT_HALF
                    else if (cmp < 0)
                        GT_HALF
                    else
                        HALF
                }
            }
        quot?.c256Set(q, m)
        return residue
    }

    fun knuthDivModX64(quot: C256?, x: C256, y0: Long): Long {
        verify { (y0 ushr 32) != 0L }
        verify { x.bitLen > 64 }

        un[0] = x.dw0.toInt()
        un[1] = (x.dw0 ushr 32).toInt()
        un[2] = x.dw1.toInt()
        un[3] = (x.dw1 ushr 32).toInt()
        un[4] = x.dw2.toInt()
        un[5] = (x.dw2 ushr 32).toInt()
        un[6] = x.dw3.toInt()
        un[7] = (x.dw3 ushr 32).toInt()
        un[8] = 0

        val m = ((x.bitLen - 1) ushr 5) + 1

        val s = y0.countLeadingZeroBits()
        val y0Normalized = y0 shl s

        vn[0] = y0Normalized.toInt()
        vn[1] = (y0Normalized ushr 32).toInt()
        //val n = 2

        if (s != 0) {
            Magia.mutateShiftLeft(un, m + 1, s)
        }

        q.fill(0)

        knuthDivideCore(m, 2)

        val remainderNormalized = (un[1].toLong() shl 32) or (un[0].toLong() and MASK32)
        val remainder = remainderNormalized ushr s
        quot?.c256Set(q, m)
        return remainder
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
            var qhat = unsignedDiv(num, vn_1)
            var rhat = unsignedMod(num, vn_1)

            // correct estimate
            while ((qhat ushr 32) != 0L ||
                unsignedCmp(
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