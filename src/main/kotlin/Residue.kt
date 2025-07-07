package com.decimal128

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.U256Pow10.compareWithHalfPow10_128
import com.decimal128.U256Pow10.compareWithHalfPow10_192
import com.decimal128.U256Pow10.compareWithHalfPow10_256
import com.decimal128.U256Pow10.compareWithHalfPow10_64

import java.lang.Long.compareUnsigned

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

        // FIXME - this method is broken and must pass in the pow10 ... or something
        fun residueFrom(c:U256) :Residue {
            val bitLen = c.bitLen
            return when {
                bitLen == 0 -> EXACT
                bitLen <= 64 -> RESIDUE_MAP[(compareWithHalfPow10_64(bitLen, c.dw0) + 2) and 0x03]
                bitLen <= 128 -> RESIDUE_MAP[(compareWithHalfPow10_128(bitLen, c.dw1, c.dw0) + 2) and 0x03]
                bitLen <= 192 -> RESIDUE_MAP[(compareWithHalfPow10_192(bitLen, c.dw2, c.dw1, c.dw0) + 2) and 0x03]
                else -> RESIDUE_MAP[(compareWithHalfPow10_256(bitLen, c.dw3, c.dw2, c.dw1, c.dw0) + 2) and 0x03]
            }
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
            assert(residueX == residueY)
            return residueX
        }

        fun residueFrom(roundBit: Int, stickyBit: Int, stickyBitPow2: Int) =
            residueFrom(roundBit, stickyBit or stickyBitPow2)

        fun residueFrom(roundBit: Int, stickyBit: Int) : Residue {
            assert(roundBit in 0..1)
            assert(stickyBit in 0..1)
            val residueValue = (roundBit shl 1) or stickyBit
            val residueX = RESIDUE_MAP[residueValue and 0x03]
            return residueX
        }

        fun residueFromRemainderDivisor(r: U256, d: U256): Residue {
            if (r.dw3 < 0L) {
                // high bit of residue is set
                // doubling is certainly larger
                return GT_HALF
            }
            val s3 = (r.dw3 shl 1) or (r.dw2 ushr -1)
            if (s3 != d.dw3) {
                val cmp = compareUnsigned(s3, d.dw3)
                return if (cmp < 0) LT_HALF else GT_HALF
            }
            val s2 = (r.dw2 shl 1) or (r.dw1 ushr -1)
            if (s2 != d.dw2) {
                val cmp = compareUnsigned(s2, d.dw2)
                return if (cmp < 0) LT_HALF else GT_HALF
            }
            val s1 = (r.dw1 shl 1) or (r.dw0 ushr -1)
            if (s1 != d.dw1) {
                val cmp = compareUnsigned(s1, d.dw1)
                return if (cmp < 0) LT_HALF else GT_HALF
            }
            val s0 = (r.dw0 shl 1)
            if (s0 != d.dw0) {
                val cmp = compareUnsigned(s0, d.dw0)
                return if (cmp < 0) LT_HALF else GT_HALF
            }
            return EXACT
        }

        fun residueFromRemainderDivisor(remainder: IntArray, divisor: IntArray): Residue {
            if (Car.isZero(remainder))
                return EXACT
            val remainderDoubled = Car.newShiftLeft(remainder, 1)
            val cmp = Car.compare(remainderDoubled, divisor)
            return when {
                cmp < 0 -> LT_HALF
                cmp == 0 -> HALF
                else -> GT_HALF
            }

        }

        fun residueFromRemainderPow10(remainder: Long, pow10: Int): Residue {
            val nonZeroMask = ((remainder or -remainder) shr 63).toInt()
            val pow10div2 = POW10[pow10] ushr 1
            val cmp = compareUnsigned(remainder, pow10div2)
            val index = ((cmp + 2) and nonZeroMask) and 0x03
            //val residue = RESIDUE_MAP[index and 0x03]
            val residue = if (RESIDUE_IS_VALUE_CLASS) Residue(index) else RESIDUE_MAP[index]
            return residue
        }

    }

    fun ulpRoundUp(roundingDirection: RoundingDirection, lsdwIsOdd: Long) : Boolean =
        ulpBias(roundingDirection, lsdwIsOdd) != 0L

    fun ulpBias(roundingDirection: RoundingDirection, lsdwIsOdd: Long) = ulpBiasY(roundingDirection, lsdwIsOdd)

    fun ulpBiasY(roundingDirection: RoundingDirection, lsdwIsOdd: Long) : Long {
        val ULP_BIAS_MAP = 0b0_00000000_00001110_00000000_00001100_00001000L

        val biasMapEvenOdd = ULP_BIAS_MAP or ((lsdwIsOdd and 1) shl 2)
        val bitIndex = (roundingDirection.value * 8) + (value and 0x03) // mask off isNegated bit
        val roundingMapShifted = biasMapEvenOdd shr bitIndex
        val bias = roundingMapShifted and 1
        return bias
    }

    // used in add case when there is no overlap
    fun ulpBiasX(roundingDirection: RoundingDirection, lsdwIsOdd: Long) : Long {
        return when (roundingDirection) {
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
