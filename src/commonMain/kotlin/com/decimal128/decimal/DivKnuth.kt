// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.Residue.Companion.GT_HALF
import com.decimal128.decimal.Residue.Companion.HALF
import com.decimal128.decimal.Residue.Companion.LT_HALF

/**
 * All 3 of the multi-limb values are stored in the same knuthD array,
 * but at different offsets.
 * Offset bases use conventional Knuth-D names.
 * Note that entry 0 in knuthD is not used because this simplifies
 * the normalization loop by eliminating the boundary condition.
 */
private const val UN = 1
private const val VN = 10
private const val Q = 18
private const val BCE = 0x1F

internal fun divKnuth(quot: C256?, rem: C256?, x: C256, y: C256, knuthD: IntArray): Residue {
    verify { c256GTOne(y) }
    verify { c256UnscaledCompare(x, y) > 0 }
    check(knuthD.size == 32)

    verify { knuthD[0] == 0 }
    knuthD[UN    ] = x.dw0.toInt(); knuthD[UN + 1] = (x.dw0 ushr 32).toInt()
    knuthD[UN + 2] = x.dw1.toInt(); knuthD[UN + 3] = (x.dw1 ushr 32).toInt()
    knuthD[UN + 4] = x.dw2.toInt(); knuthD[UN + 5] = (x.dw2 ushr 32).toInt()
    knuthD[UN + 6] = x.dw3.toInt(); knuthD[UN + 7] = (x.dw3 ushr 32).toInt()
    verify { knuthD[UN + 8] == 0 }

    val m = ((x.bitLen - 1) ushr 5) + 1

    knuthD[VN    ] = y.dw0.toInt(); knuthD[VN + 1] = (y.dw0 ushr 32).toInt()
    knuthD[VN + 2] = y.dw1.toInt(); knuthD[VN + 3] = (y.dw1 ushr 32).toInt()
    knuthD[VN + 4] = y.dw2.toInt(); knuthD[VN + 5] = (y.dw2 ushr 32).toInt()
    knuthD[VN + 6] = y.dw3.toInt(); knuthD[VN + 7] = (y.dw3 ushr 32).toInt()

    val vnNonZeroIndex = ((y.bitLen - 1) ushr 5)
    val vnNonZeroVal = knuthD[VN + vnNonZeroIndex]
    val n = vnNonZeroIndex + 1
    val s = vnNonZeroVal.countLeadingZeroBits()

    if (s != 0) {
        // note that this is shifting both dividend (starting at entry[1])
        // and divisor (starting at entry 10)
        normalizeShiftLeft(knuthD, VN + n, s)
    }

    for (i in 0 until ((m - n + 1) and BCE))
        knuthD[Q + i] = 0

    divKnuthCore(knuthD, m, n)

    if (rem != null) {
        // looks funny but this is correct
        // remainder has at most n limbs
        denormalizeRemainderShiftRight(knuthD, n, s)
        val r0 = (knuthD[UN + 1].toLong() shl 32) or (knuthD[UN    ].toLong() and MASK32L)
        val r1 = (knuthD[UN + 3].toLong() shl 32) or (knuthD[UN + 2].toLong() and MASK32L)
        val r2 = (knuthD[UN + 5].toLong() shl 32) or (knuthD[UN + 4].toLong() and MASK32L)
        val r3 = (knuthD[UN + 7].toLong() shl 32) or (knuthD[UN + 6].toLong() and MASK32L)
        rem.c256Set256(r3, r2, r1, r0)
    }

    // shifting right by s will denormalize to normal remainder
    // I will shift right by s-1
    // this will give me 2*remainder that I can compare with y
    val residue =
        if (rem != null) {
            EXACT
        } else if (knuthD[(UN + n - 1) and BCE] < 0) {
            // msb of remainder is set ... doubling it will make it bigger than the divisor
            GT_HALF
        } else {
            var isZero = 0
            if (s == 0) {
                // note that this is shifting LEFT to double the remainder
                // remember that UN == 1 and that we peek into the unused knuthD[0] == 0
                for (i in n - 1 downTo 0) {
                    val UN_i = (UN + i) and BCE
                    val t = (knuthD[UN_i] shl 1) or (knuthD[(UN_i - 1) and BCE] ushr -1)
                    knuthD[UN_i] = t
                    isZero = isZero or t
                }
            } else {
                val s1 = s - 1
                if (s1 > 0) {
                    // this is a shift right by s1 == s - 1
                    // we will peek into the next higher limb
                    // that is guaranteed to be == 0
                    // this eliminates the boundary-condition check
                    for (i in 0 until n) {
                        val UN_i = (UN + i) and BCE
                        val t = (knuthD[(UN_i + 1) and BCE] shl -s1) or (knuthD[UN_i] ushr s1)
                        knuthD[UN_i] = t
                        isZero = isZero or t
                    }
                } else {
                    // still need to test for isZero
                    var i = 0
                    do {
                        isZero = isZero or knuthD[(UN + i) and BCE]
                    } while (isZero == 0 && ++i < n)
                }
            }
            if (isZero == 0) {
                EXACT
            } else {
                // note that this compare is reversed ... y compare 2*remainder
                val cmp = compareRemainderDoubled(y, knuthD)
                if (cmp > 0)
                    LT_HALF
                else if (cmp < 0)
                    GT_HALF
                else
                    HALF
            }
        }
    if (quot != null) {
        val q0 = (knuthD[Q + 1].toLong() shl 32) or (knuthD[Q    ].toLong() and MASK32L)
        val q1 = (knuthD[Q + 3].toLong() shl 32) or (knuthD[Q + 2].toLong() and MASK32L)
        val q2 = (knuthD[Q + 5].toLong() shl 32) or (knuthD[Q + 4].toLong() and MASK32L)
        val q3 = (knuthD[Q + 7].toLong() shl 32) or (knuthD[Q + 6].toLong() and MASK32L)
        quot.c256Set256(q3, q2, q1, q0)
    }
    return residue
}

/**
 * This is not a _normal_ shift in that x[0] is not
 * shifted at all
 */
private inline fun normalizeShiftLeft(x: IntArray, xLen: Int, bitCount: Int) {
    val right = 32 - bitCount
    for (i in xLen - 1 downTo 1)
        x[i] = (x[i] shl bitCount) or (x[i - 1] ushr right)
}

/**
 * This is not a _normal_ shift right in that it peeks
 * to the next higher limb, which for the remainder is
 * guaranteed to be zero
 */
private inline fun denormalizeRemainderShiftRight(x: IntArray, xLen: Int, bitCount: Int) {
    val left = 32 - bitCount
    for (i in 0..<xLen)
        x[i] = (x[i + 1] shl left) or (x[i] ushr bitCount)
}

private inline fun compareRemainderDoubled(x: C256, knuthD: IntArray): Int {
    val y3 = (knuthD[UN + 7].toLong() shl 32) or (knuthD[UN + 6].toLong() and MASK32L)
    if (x.dw3 != y3)
        return unsignedCmp(x.dw3, y3)
    val y2 = (knuthD[UN + 5].toLong() shl 32) or (knuthD[UN + 4].toLong() and MASK32L)
    if (x.dw2 != y2)
        return unsignedCmp(x.dw2, y2)
    val y1 = (knuthD[UN + 3].toLong() shl 32) or (knuthD[UN + 2].toLong() and MASK32L)
    if (x.dw1 != y1)
        return unsignedCmp(x.dw1, y1)
    val y0 = (knuthD[UN + 1].toLong() shl 32) or (knuthD[UN    ].toLong() and MASK32L)
    return unsignedCmp(x.dw0, y0)
}


/**
 * specialized knuth D wrapper when the divisor is 64 bits.
 *
 * returns the remainder, not the Residue
 */
internal fun divKnuthDivModX64(quot: C256?, x: C256, y0: Long, knuthD: IntArray): Long {
    verify { (y0 ushr 32) != 0L }
    verify { x.bitLen > 64 }
    check(knuthD.size == 32)

    verify { knuthD[0] == 0 }
    knuthD[UN    ] = x.dw0.toInt(); knuthD[UN + 1] = (x.dw0 ushr 32).toInt()
    knuthD[UN + 2] = x.dw1.toInt(); knuthD[UN + 3] = (x.dw1 ushr 32).toInt()
    knuthD[UN + 4] = x.dw2.toInt(); knuthD[UN + 5] = (x.dw2 ushr 32).toInt()
    knuthD[UN + 6] = x.dw3.toInt(); knuthD[UN + 7] = (x.dw3 ushr 32).toInt()
    verify { knuthD[UN + 8] == 0 }

    val m = ((x.bitLen - 1) ushr 5) + 1

    val s = y0.countLeadingZeroBits()

    knuthD[VN    ] = y0.toInt()
    knuthD[VN + 1] = (y0 ushr 32).toInt()
    //val n = 2

    if (s != 0) {
        // note that this is shifting both dividend (starting at entry[1])
        // and divisor (starting at entry 10)
        normalizeShiftLeft(knuthD, VN + 2, s)
    }

    for (i in 0 until ((m - 2 + 1) and BCE))
        knuthD[Q + i] = 0

    divKnuthCore(knuthD, m, 2)

    val remainderNormalized = (knuthD[UN + 1].toLong() shl 32) or (knuthD[UN    ].toLong() and MASK32L)
    val remainder = remainderNormalized ushr s
    if (quot != null) {
        val q0 = (knuthD[Q + 1].toLong() shl 32) or (knuthD[Q    ].toLong() and MASK32L)
        val q1 = (knuthD[Q + 3].toLong() shl 32) or (knuthD[Q + 2].toLong() and MASK32L)
        val q2 = (knuthD[Q + 5].toLong() shl 32) or (knuthD[Q + 4].toLong() and MASK32L)
        val q3 = (knuthD[Q + 7].toLong() shl 32) or (knuthD[Q + 6].toLong() and MASK32L)
        quot.c256Set256(q3, q2, q1, q0)
    }
    return remainder
}


/**
 * Multi‐word division (Knuth’s Algorithm D) in base 2^32.
 *
 * A specialized variant that uses a single array with dedicated
 * offsets to support division with limb counts up to 8.
 *
 * knuthD.size == 32 must be true
 *
 * QN: quotient offset (length ≥ m – n + 1)
 * remainder is left in UN and must be de-normalized
 * UN: normalized dividend offset (length = m), little‐endian
 * VN: normalized divisor array (length = n ≥ 2), little‐endian, v[n − 1] ≠ 0
 * m: number of words in u (≥ n), but the normalized UN consumes 1 more hi word
 * n: number of words in v (≥ 2) ... or VN
 *
 */
private fun divKnuthCore(
    knuthD: IntArray,
    m: Int,
    n: Int
) {
    // invalid parameters?
    if (m > 8 || m < 3 || n > 8 || n < 2 || m < n || knuthD.size != 32)
        throw IllegalArgumentException()

    val vn_1 = knuthD[VN + n - 1].toLong() and MASK32L
    val vn_2 = knuthD[VN + n - 2].toLong() and MASK32L

    // -- main loop --
    for (j in m - n downTo 0) {

        val UN_j_n = (UN + j + n) and BCE

        // estimate q̂ = (un[j+n]*B + un[j+n-1]) / vn[n-1]
        val hi = knuthD[UN_j_n].toLong() and MASK32L
        val lo = knuthD[(UN_j_n - 1) and BCE].toLong() and MASK32L
        val num = (hi shl 32) or lo
        var qhat = unsignedDiv(num, vn_1)
        var rhat = num - (qhat * vn_1)

        // correct estimate
        while ((qhat ushr 32) != 0L ||
            unsignedCmp(
                qhat * vn_2, (rhat shl 32) + (knuthD[(UN_j_n - 2) and BCE].toLong() and MASK32L)
            ) > 0
        ) {
            qhat--
            rhat += vn_1
            if ((rhat ushr 32) != 0L)
                break
        }


        // multiply & subtract
        var carry = 0L
        for (i in 0 until n) {
            val UN_j_i = (UN + j + i) and BCE
            val prod = qhat * (knuthD[(VN + i) and BCE].toLong() and MASK32L)
            val prodHi = prod ushr 32
            val prodLo = prod and MASK32L
            val unIJ = (knuthD[UN_j_i].toLong() and MASK32L)
            val t = unIJ - prodLo - carry
            knuthD[UN_j_i] = t.toInt()
            carry = prodHi - (t shr 32) // t is signed, so this should *indeed* be signed shr
        }
        val t = (knuthD[UN_j_n].toLong() and MASK32L) - carry
        knuthD[UN_j_n] = t.toInt()
        knuthD[(Q + j) and BCE] = (qhat - (t ushr 63)).toInt()
        if (t < 0) {
            var c2 = 0L
            for (i in 0 until n) {
                val UN_j_i = (UN + j + i) and BCE
                val sum = (knuthD[UN_j_i].toLong() and MASK32L) +
                        (knuthD[(VN + i) and BCE].toLong() and MASK32L) + c2
                knuthD[UN_j_i] = sum.toInt()
                c2 = sum ushr 32
            }
            knuthD[UN_j_n] += c2.toInt()
        }
    }
}
