package com.decimal128

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.CoeffPow10.compareWithHalfPow10_128
import com.decimal128.CoeffPow10.compareWithHalfPow10_192
import com.decimal128.CoeffPow10.compareWithHalfPow10_256
import com.decimal128.CoeffPow10.compareWithHalfPow10_64

import java.lang.Long.compareUnsigned


//@JvmInline
/*value*/ class Residue private constructor(val value:Int) {

    // comment this out when we turn Residue into a value class
    override fun equals(other: Any?): Boolean {
        return other is Residue && this.value == other.value
    }

    fun toggleNegate() = RESIDUE_MAP[value xor 0x04]
    fun withoutNegate() = RESIDUE_MAP[value and 0x03]
    fun isNegated() = (value and 0x04) != 0

    companion object {
        val EXACT = Residue(0)
        val LT_HALF = Residue(1)
        val HALF = Residue(2)
        val GT_HALF = Residue(3)

        val EXACT_NEGATED = Residue(4)
        val LT_HALF_NEGATED = Residue(5)
        val HALF_NEGATED = Residue(6)
        val GT_HALF_NEGATED = Residue(7)


        val RESIDUE_MAP = arrayOf(EXACT, LT_HALF, HALF, GT_HALF, EXACT_NEGATED, LT_HALF_NEGATED, HALF_NEGATED, GT_HALF_NEGATED)

        val STRING_NAMES = arrayOf("EXACT", "LT_HALF", "HALF", "GT_HALF",
            "EXACT_NEGATED", "LT_HALF_NEGATED", "HALF_NEGATED", "GT_HALF_NEGATED")

        fun residueFrom(c:Coeff) :Residue {
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
            val residueValue = (roundBit shl 1) or stickyBit
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
            assert(residueX == residueY)
            return residueX
        }

        fun residueFrom(roundBit: Int, stickyBit: Int, stickyBitPow2: Int) =
            residueFrom(roundBit, stickyBit or stickyBitPow2)

        fun residueFrom(roundBit: Int, stickyBit: Int) : Residue {
            assert(roundBit in 0..1)
            assert(stickyBit in 0..1)
            val residueValue = (roundBit shl 1) or stickyBit
            val residueX = Residue(residueValue)
            return residueX
        }

        fun residueFromRemainderDivisor(r: Coeff, d: Coeff): Residue {
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
            ROUND_TIES_TO_EVEN -> when (value and 0x03) { // mask off isNegated bit
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
    //FIXME turn this into a map
    // it is really just just merging the sticky bit

    //FIXME what to do about _NEGATED residues
    fun merge(stickyResidue: Residue): Residue {
        val mergedResidue = when (this.value) {
            EXACT.value -> if (stickyResidue.value and 3 == EXACT.value) EXACT else LT_HALF
            LT_HALF.value -> LT_HALF
            HALF.value -> if (stickyResidue.value and 3 == EXACT.value) HALF else GT_HALF
            GT_HALF.value -> GT_HALF

            EXACT_NEGATED.value -> if (stickyResidue.value and 3 == EXACT.value) EXACT_NEGATED else LT_HALF_NEGATED
            LT_HALF_NEGATED.value -> LT_HALF_NEGATED
            HALF_NEGATED.value -> if (stickyResidue.value and 3 == EXACT.value) HALF_NEGATED else GT_HALF_NEGATED
            GT_HALF_NEGATED.value -> GT_HALF_NEGATED

            else -> throw RuntimeException("unrecognized Residue.value")
        }
        return mergedResidue
    }

    override fun toString() : String {
        return if (this.value in STRING_NAMES.indices) STRING_NAMES[this.value] else "invalid Residue:$value"
    }
}
