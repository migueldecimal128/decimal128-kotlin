package com.decimal128.decimal

import com.decimal128.bigint.BigInt
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.decimal.U256Pow10.compareWithHalfPow10_1
import com.decimal128.decimal.U256Pow10.compareWithHalfPow10_2
import com.decimal128.decimal.U256Pow10.compareWithHalfPow10_3
import com.decimal128.decimal.U256Pow10.compareWithHalfPow10_4
import com.decimal128.decimal.U256Pow10.POW10

private const val RESIDUE_IS_VALUE_CLASS = true
@JvmInline
value class Residue private constructor(val value:Int) {

    companion object {
        val EXACT = Residue(0)
        val LT_HALF = Residue(1)
        val HALF = Residue(2)
        val GT_HALF = Residue(3)

        val RESIDUE_MAP = arrayOf(EXACT, LT_HALF, HALF, GT_HALF)

        val STRING_NAMES = arrayOf("EXACT", "LT_HALF", "HALF", "GT_HALF")

        // FIXME - this method is fine, but it needs a better name
        //  ... and perhaps a better implementation
        fun residueFrom(c:C256) :Residue {
            val digitLen = c.digitLen
            if (digitLen == 0)
                return EXACT
            val cmp = when {
                digitLen < MIN_POW10_DIGIT_LEN_128 -> compareWithHalfPow10_1(c.dw0, digitLen)
                digitLen < MIN_POW10_DIGIT_LEN_192 -> compareWithHalfPow10_2(c.dw1, c.dw0, digitLen)
                digitLen < MIN_POW10_DIGIT_LEN_256 -> compareWithHalfPow10_3(c.dw2, c.dw1, c.dw0, digitLen)
                else -> compareWithHalfPow10_4(c.dw3, c.dw2, c.dw1, c.dw0, digitLen)
            }
            val residueValue = (cmp + 2) and 0x03
            val residue = if (RESIDUE_IS_VALUE_CLASS) Residue(residueValue) else RESIDUE_MAP[residueValue]
            return residue
        }


        fun residueFrom(isolatedRoundBit: Long, stickyBitsFracCompare: Int, stickyBitsPow2: Long) : Residue {
            val stickyBit = if (stickyBitsFracCompare >= 0 || stickyBitsPow2 != 0L) 1 else 0
            val roundBit = if (isolatedRoundBit == 0L) 0 else 1
            val residueValue = ((roundBit shl 1) or stickyBit) and 0x03
            val residueX = if (RESIDUE_IS_VALUE_CLASS) Residue(residueValue) else RESIDUE_MAP[residueValue]
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

        fun residueFrom(roundBit: Int, stickyBit: Int, stickyBitPow2: Int) =
            residueFrom(roundBit, stickyBit or stickyBitPow2)

        fun residueFrom(roundBit: Int, stickyBit: Int) : Residue {
            verify { roundBit in 0..1 }
            verify { stickyBit in 0..1 }
            val residueValue = (roundBit shl 1) or stickyBit
            val residueX = RESIDUE_MAP[residueValue and 0x03]
            return residueX
        }

        fun residueFromRemainderDivisor(r: C256, d: C256): Residue {
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

        fun residueFromRemainderDivisor(remainder: Long, divisor: Long): Residue {
            val residue = when {
                remainder == 0L -> EXACT
                remainder < 0L -> GT_HALF // hi bit set .. so doubling would be 65 bits ... GT y0
                unsignedLT(2 * remainder, divisor) -> LT_HALF // we are doubling the remainder here
                unsignedCmp(2 * remainder, divisor) > 0 -> GT_HALF
                else -> HALF
            }
            return residue
        }

        fun residueFromRemainderDivisor(remainder: BigInt, divisor: BigInt): Residue {
            if (remainder.isZero())
                return EXACT
            val remainderDoubled = remainder shl 1
            val cmp = remainderDoubled.compareTo(divisor)
            return when {
                cmp < 0 -> LT_HALF
                cmp == 0 -> HALF
                else -> GT_HALF
            }

        }

        fun residueFromRemainderPow10(remainder: Long, pow10: Int): Residue {
            val nonZeroMask = ((remainder or -remainder) shr 63).toInt()
            val pow10div2 = POW10[pow10] ushr 1
            val cmp = unsignedCmp(remainder, pow10div2)
            val index = ((cmp + 2) and nonZeroMask) and 0x03
            //val residue = RESIDUE_MAP[index and 0x03]
            val residue = if (RESIDUE_IS_VALUE_CLASS) Residue(index) else RESIDUE_MAP[index]
            return residue
        }

    }

    fun ulpRoundUp(decRounding: DecRounding, lsdwIsOdd: Long) : Boolean =
        ulpBias(decRounding, lsdwIsOdd) != 0L

    fun ulpBias(decRounding: DecRounding, lsdwIsOdd: Long) = ulpBiasY(decRounding, lsdwIsOdd)

    fun ulpBiasY(decRounding: DecRounding, lsdwIsOdd: Long) : Long {
        val ULP_BIAS_MAP = 0b0_00000000_00001110_00000000_00001100_00001000L

        val biasMapEvenOdd = ULP_BIAS_MAP or ((lsdwIsOdd and 1) shl 2)
        val bitIndex = (decRounding.value * 8) + (value and 0x03) // mask off isNegated bit
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
        val mergedResidue = if (RESIDUE_IS_VALUE_CLASS) Residue(r) else RESIDUE_MAP[r]
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
