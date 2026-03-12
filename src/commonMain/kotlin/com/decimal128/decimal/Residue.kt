// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.bigint.BigInt
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO

@JvmInline
value class Residue internal constructor(val value:Int) {

    companion object {
        val EXACT = Residue(0)
        val LT_HALF = Residue(1)
        val HALF = Residue(2)
        val GT_HALF = Residue(3)

        operator fun invoke(res: Int) = Residue(res and 0x03)

        internal val RESIDUE_MAP = arrayOf(EXACT, LT_HALF, HALF, GT_HALF)

        internal val STRING_NAMES = arrayOf("EXACT", "LT_HALF", "HALF", "GT_HALF")

        internal const val DIGIT_MAP = 0b11_11_11_11_10_01_01_01_01_00

        internal inline fun fromDecimalDigit(digit: Int): Residue = Residue((DIGIT_MAP shr (digit shl 1)) and 0x03)

        // FIXME - this method is fine, but it needs a better name
        //  ... and perhaps a better implementation
        fun fromValueDecade(c:C256): Residue {
            val digitLen = c.digitLen
            if (digitLen == 0)
                return EXACT
            val c0 = c.dw0
            val c1 = c.dw1
            val cmp = when {
                digitLen < MIN_POW10_DIGIT_LEN_128 -> compareWithHalfPow10_1(c0, digitLen)
                digitLen < MIN_POW10_DIGIT_LEN_192 -> compareWithHalfPow10_2(c1, c0, digitLen)
                digitLen < MIN_POW10_DIGIT_LEN_256 -> compareWithHalfPow10_3(c.dw2, c1, c0, digitLen)
                else -> compareWithHalfPow10_4(c.dw3, c.dw2, c1, c0, digitLen)
            }
            val residueValue = (cmp + 2) and 0x03
            val residue = Residue(residueValue)
            return residue
        }

        fun fromValueDecade(x: Decimal): Residue {
            val digitLen = stealDigitLen(x.steal)
            if (digitLen == 0)
                return EXACT
            val x0 = x.dw0
            val x1 = x.dw1
            val cmp = when {
                digitLen < MIN_POW10_DIGIT_LEN_128 -> compareWithHalfPow10_1(x0, digitLen)
                else -> compareWithHalfPow10_2(x1, x0, digitLen)
            }
            val residueValue = (cmp + 2) and 0x03
            val residue = Residue(residueValue)
            return residue
        }

        fun fromValuePow10(dw1: Long, dw0: Long, pow10: Int): Residue {
            val (dw1P, dw0P) = pow10_128(pow10)
            val dw1H = dw1P ushr 1
            val dw0H = (dw1P shl 63) or (dw0P ushr 1)
            val cmp = ucmp128(dw1, dw0, dw1H, dw0H)
            return Residue(cmp + 2)
        }


        fun fromRoundBitStickyBitsStickyBits(isolatedRoundBit: Long, stickyBitsFracCompare: Int, stickyBitsPow2: Long) : Residue {
            val stickyBit = if (stickyBitsFracCompare >= 0 || stickyBitsPow2 != 0L) 1 else 0
            val roundBit = if (isolatedRoundBit == 0L) 0 else 1
            val residueValue = ((roundBit shl 1) or stickyBit) and 0x03
            val residueX = Residue(residueValue)
            val residueY =
                if (stickyBitsPow2 == 0L) {
                    if (stickyBitsFracCompare < 0) {
                        if (isolatedRoundBit == 0L) EXACT else HALF
                    } else {
                        if (isolatedRoundBit == 0L) LT_HALF else GT_HALF
                    }
                } else {
                    if (isolatedRoundBit == 0L) LT_HALF else GT_HALF
                }
            if (residueX != residueY)
                println("residueX:$residueX residueY:$residueY")
            verify { residueX == residueY }
            return residueX
        }

        fun fromRoundBitStickBit(roundBit: Int, stickyBit: Int) : Residue {
            verify { roundBit in 0..1 }
            verify { stickyBit in 0..1 }
            val residueValue = (roundBit shl 1) or stickyBit
            val residueX = Residue(residueValue)
            return residueX
        }

        fun fromRoundBitStickyBits(isolatedRoundBit: Long, stickyBits: Long) : Residue {
            val roundBit = ((isolatedRoundBit or -isolatedRoundBit) ushr 63).toInt()
            val stickyBit = ((stickyBits or -stickyBits) ushr 63).toInt()
            val residueValue = (roundBit shl 1) or stickyBit
            val residueX = Residue(residueValue)
            return residueX
        }

        fun fromRemainderDivisor(r: C256, d: C256): Residue {
            if (r.dw3 < 0L) {
                // high bit of residue is set
                // doubling is certainly larger
                return GT_HALF
            }
            val s3 = (r.dw3 shl 1) or (r.dw2 ushr -1)
            if (s3 != d.dw3) {
                val cmp = unsignedCmp(s3, d.dw3)
                return if (cmp < 0) LT_HALF else GT_HALF
            }
            val s2 = (r.dw2 shl 1) or (r.dw1 ushr -1)
            if (s2 != d.dw2) {
                val cmp = unsignedCmp(s2, d.dw2)
                return if (cmp < 0) LT_HALF else GT_HALF
            }
            val s1 = (r.dw1 shl 1) or (r.dw0 ushr -1)
            if (s1 != d.dw1) {
                val cmp = unsignedCmp(s1, d.dw1)
                return if (cmp < 0) LT_HALF else GT_HALF
            }
            val s0 = (r.dw0 shl 1)
            if (s0 != d.dw0) {
                val cmp = unsignedCmp(s0, d.dw0)
                return if (cmp < 0) LT_HALF else GT_HALF
            }
            return EXACT
        }

        fun fromRemainderDivisor(remainder: Long, divisor: Long): Residue {
            val residue = when {
                remainder == 0L -> EXACT
                remainder < 0L -> GT_HALF // hi bit set .. so doubling would be 65 bits ... GT y0
                unsignedLT(2 * remainder, divisor) -> LT_HALF // we are doubling the remainder here
                unsignedCmp(2 * remainder, divisor) > 0 -> GT_HALF
                else -> HALF
            }
            return residue
        }

        fun residueFromRemainderPow10(remainder: Long, pow10: Int): Residue {
            val nonZeroMask = ((remainder or -remainder) shr 63).toInt()
            val pow10div2 = pow10_64(pow10) ushr 1
            val cmp = unsignedCmp(remainder, pow10div2)
            val index = ((cmp + 2) and nonZeroMask) and 0x03
            //val residue = RESIDUE_MAP[index and 0x03]
            val residue = Residue(index)
            return residue
        }

    }

    fun ulpRoundUp(decRounding: DecRounding, lsdwIsOdd: Long) : Boolean =
        ulpBias(decRounding, lsdwIsOdd) != 0L

    fun ulpRoundUp01L(decRounding: DecRounding, lsdwIsOdd: Long) : Long =
        -ulpBias(decRounding, lsdwIsOdd) ushr 63

    fun ulpBias(decRounding: DecRounding, lsdwIsOdd: Long) = ulpBiasY(decRounding, lsdwIsOdd)

    fun ulpBiasY(decRounding: DecRounding, lsdwIsOdd: Long) : Long {
        val ULP_BIAS_MAP = 0b0_00000000_00001110_00000000_00001100_00001000L

        val biasMapEvenOdd = ULP_BIAS_MAP or ((lsdwIsOdd and 1) shl 2)
        val bitIndex = (decRounding.value shl 3) + (value and 0x03) // mask off isNegated bit
        val roundingMapShifted = biasMapEvenOdd shr bitIndex
        val bias = roundingMapShifted and 1
        return bias
    }

    // used in add case when there is no overlap
    fun ulpBiasX(decRounding: DecRounding, lsdwIsOdd: Long) : Long {
        return when (decRounding) {
            ROUND_TIES_TO_EVEN -> when (value) {
                LT_HALF.value -> 0L
                HALF.value -> lsdwIsOdd and 1L
                GT_HALF.value -> 1L
                // EXACT
                else -> 0L
            }
            ROUND_TIES_TO_AWAY -> when (value) {
                LT_HALF.value -> 0L
                HALF.value -> 1L
                GT_HALF.value -> 1L
                // EXACT
                else -> 0L
            }
            ROUND_TOWARD_ZERO -> when (value) {
                LT_HALF.value -> 0L
                HALF.value -> 0L
                GT_HALF.value -> 0L
                // EXACT
                else -> 0L
            }
            ROUND_TOWARD_POSITIVE -> when (value) {
                LT_HALF.value -> 1L
                HALF.value -> 1L
                GT_HALF.value -> 1L
                // EXACT
                else -> 0L
            }
            // ROUND_TOWARD_NEGATIVE
            else -> when (value) {
                LT_HALF.value -> 0L
                HALF.value -> 0L
                GT_HALF.value -> 0L
                // EXACT
                else -> 0L
            }
        }
    }

    /*
                previous
                exact lt_half half    gt_half
    exact       exact lt_half lt_half lt_half
    lt_half     lt_half lt_half lt_half lt_half
    half        half, gt_half, gt_half, gtHalf
    gt_half     gt_half gt_half gt_half gt_half

     */

    /**
     * Merges previous residue with new stickResidue in left-to-right fashion ...
     * as though parsing digits left to right.
     */
    fun merge(stickyResidue: Residue): Residue {
        /*
        val mergedResidue = when (this.value) {
            EXACT.value -> if (stickyResidue.value == EXACT.value) EXACT else LT_HALF
            LT_HALF.value -> LT_HALF
            HALF.value -> if (stickyResidue.value == EXACT.value) HALF else GT_HALF
            GT_HALF.value -> GT_HALF

            else -> throw RuntimeException("unrecognized Residue.value")
        }
         */
        val s = (stickyResidue.value and 1) or (stickyResidue.value ushr 1)
        val r = (this.value or s) and 0x03
        val mergedResidue = Residue(r)
        return mergedResidue
    }

    fun subtractionInverse() : Residue {
        val inverse = when (this.value) {
            EXACT.value -> EXACT
            LT_HALF.value -> GT_HALF
            HALF.value -> HALF
            GT_HALF.value -> LT_HALF
            else -> throw RuntimeException("unrecognized Residue.value")
        }
        return inverse
    }

    override fun toString() : String {
        return if (this.value in STRING_NAMES.indices) STRING_NAMES[this.value] else "invalid Residue:$value"
    }
}
