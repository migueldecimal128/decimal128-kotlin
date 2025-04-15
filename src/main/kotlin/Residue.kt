package com.decimal128

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import java.lang.Long.compareUnsigned
import java.math.BigInteger


@JvmInline
value class Residue private constructor(val value: Int) {
    companion object {
        val HALF = Residue(0)
        val BIAS_TRUNC = Residue(1)
        val LT_HALF = Residue(2)
        val GT_HALF = Residue(3)
        val EXACT = Residue(4)

        fun residueFrom(c: Coefficient) : Residue {
            assert (c.digitCount > 0)
            return (
                    if (( c.dw3 or c.dw2) == 0L) {
                        if (c.dw1 == 0L)
                            residueFrom(c.digitCount, c.dw0)
                        else
                            residueFrom(c.digitCount, c.dw1, c.dw0)
                    } else {
                        if (c.dw3 == 0L)
                            residueFrom(c.digitCount, c.dw2, c.dw1, c.dw0)
                        else
                            residueFrom(c.digitCount, c.dw3, c.dw2, c.dw1, c.dw0)
                    }
                    )

        }

        fun residueFrom(digitCount: Int, dw0: Long) : Residue {
            if (dw0 >= 0) {
                val dw0x2 = dw0 shl 1
                val ten0 = POW10[digitCount]
                val cmp0 = compareUnsigned(dw0x2, ten0)
                val residue = if (cmp0 < 0) LT_HALF else if (cmp0 == 0) HALF else GT_HALF
                return residue
            } else {
                val residue = if (digitCount < POW10_128_OFFSET) GT_HALF else LT_HALF
                return residue
            }
        }

        fun residueFrom(digitCount: Int, dw1: Long, dw0: Long) : Residue {
            if (dw1 >= 0) {
                val index = 2 * (digitCount - POW10_128_OFFSET) + POW10_128_DWORD_INDEX

                val dw1x2 = (dw1 shl 1) or (dw0 ushr 63)
                val ten1 = POW10[index + 1]
                val cmp1 = compareUnsigned(dw1x2, ten1)
                if (cmp1 < 0)
                    return LT_HALF
                if (cmp1 > 0)
                    return GT_HALF

                val dw0x2 = (dw0 shl 1)
                val ten0 = POW10[index + 0]
                val cmp0 = compareUnsigned(dw0x2, ten0)
                if (cmp0 < 0)
                    return LT_HALF
                if (cmp0 > 0)
                    return GT_HALF

                return HALF
            } else {
                return if (digitCount < POW10_192_OFFSET) GT_HALF else LT_HALF
            }
        }

        private val fiveE57 = BigInteger.TEN.pow(58).shiftRight(1)
        val bitLengthVerify = run { require(fiveE57.bitLength() == 192); 192}
        val fiveE57dw0 = fiveE57.toLong()
        val fiveE57dw1 = fiveE57.shiftRight(64).toLong()
        val fiveE57dw2 = fiveE57.shiftRight(128).toLong()


        fun residueFrom(digitCount: Int, dw2: Long, dw1: Long, dw0: Long) : Residue {
            if (dw2 >= 0) {
                val index = 3 * (digitCount - POW10_192_OFFSET) + POW10_192_DWORD_INDEX

                val dw2x2 = (dw2 shl 1) or (dw1 ushr 63)
                val ten2 = POW10[index + 2]
                val cmp2 = compareUnsigned(dw2x2, ten2)
                if (cmp2 < 0)
                    return LT_HALF
                if (cmp2 > 0)
                    return GT_HALF

                val dw1x2 = (dw1 shl 1) or (dw0 ushr 63)
                val ten1 = POW10[index + 1]
                val cmp1 = compareUnsigned(dw1x2, ten1)
                if (cmp1 < 0)
                    return LT_HALF
                if (cmp1 > 0)
                    return GT_HALF

                val dw0x2 = (dw0 shl 1)
                val ten0 = POW10[index + 0]
                val cmp0 = compareUnsigned(dw0x2, ten0)
                if (cmp0 < 0)
                    return LT_HALF
                if (cmp0 > 0)
                    return GT_HALF

                return HALF
            } else {
                assert(digitCount == 58)
                // this case is a problem child because (10**58)/2 has 192 bits == 3 dwords

                val cmp2 = compareUnsigned(dw2, fiveE57dw2)
                if (cmp2 < 0)
                    return LT_HALF
                if (cmp2 > 0)
                    return GT_HALF

                val cmp1 = compareUnsigned(dw1, fiveE57dw1)
                if (cmp1 < 0)
                    return LT_HALF
                if (cmp1 > 0)
                    return GT_HALF

                val cmp0 = compareUnsigned(dw0, fiveE57dw0)
                if (cmp0 < 0)
                    return LT_HALF
                if (cmp0 > 0)
                    return GT_HALF

                return HALF
            }
        }

        fun residueFrom(digitCount: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) : Residue {
            if (dw3 >= 0) {
                if (digitCount == 58)
                    return GT_HALF
                val index = 4 * (digitCount - POW10_256_OFFSET) + POW10_256_DWORD_INDEX

                val dw3x2 = (dw3 shl 1) or (dw2 ushr 63)
                val ten3 = POW10[index + 3]
                val cmp3 = compareUnsigned(dw3x2, ten3)
                if (cmp3 < 0)
                    return LT_HALF
                if (cmp3 > 0)
                    return GT_HALF

                val dw2x2 = (dw2 shl 1) or (dw1 ushr 63)
                val ten2 = POW10[index + 2]
                val cmp2 = compareUnsigned(dw2x2, ten2)
                if (cmp2 < 0)
                    return LT_HALF
                if (cmp2 > 0)
                    return GT_HALF

                val dw1x2 = (dw1 shl 1) or (dw0 ushr 63)
                val ten1 = POW10[index + 1]
                val cmp1 = compareUnsigned(dw1x2, ten1)
                if (cmp1 < 0)
                    return LT_HALF
                if (cmp1 > 0)
                    return GT_HALF

                val dw0x2 = (dw0 shl 1)
                val ten0 = POW10[index + 0]
                val cmp0 = compareUnsigned(dw0x2, ten0)
                if (cmp0 < 0)
                    return LT_HALF
                if (cmp0 > 0)
                    return GT_HALF

                return HALF
            } else {
                if (digitCount >= POW10_MAX_OFFSET)
                    throw RuntimeException("decimal digit overflow 74396")
                // all 77 digit numbers that have the hi bit set are GT_HALF
                return GT_HALF
            }
        }

    }

    // used by scaling operations with RecipMul
    fun halfUlpBias(roundingDirection: RoundingDirection, lsdwIsOdd: Long) : Long {
        // with RoundingDirection ROUND_TOWARDS_POSITIVE and ROUND_TOWARDS_NEGATIVE
        // the bias is 1 ULP, not 1/2 ULP
        // this table stores 5 RoundingDirections, starting from the right
        // each entry is 8 bits long, although only 3x2=6 bits are actually used
        assert(value != LT_HALF.value && value != GT_HALF.value)
        val HALF_ULP_BIAS_MAP = 0b0_00000000_00001010_00000000_00000101_00000100L

        val exactOr3Mask = (((value - EXACT.value) shr 1) ushr 30).toLong() // exactOr3Mask = if (EXACT) 0L else 03L
        val biasMapEvenOdd = HALF_ULP_BIAS_MAP or (lsdwIsOdd and 1)
        val bitIndex = ((roundingDirection.value * 4) + value) * 2
        val roundingMapShifted = biasMapEvenOdd shr bitIndex
        val bias = exactOr3Mask and roundingMapShifted
        return bias
    }

    fun ulpBias(roundingDirection: RoundingDirection, lsdwIsOdd: Long) = ulpBiasY(roundingDirection, lsdwIsOdd)

    // for add operation when there is no overlap ...
    // in this case, we are not going to add halfULP and trunc
    // rather, we are going to bias the ULP by a whole amount
    // used in add case when there is no overlapg
    fun ulpBiasY(roundingDirection: RoundingDirection, lsdwIsOdd: Long) : Long {
        assert(value != BIAS_TRUNC.value)
        // this becomes one bit per entry, with the special treament of ROUND_TIES_TO_EVEN and lsbg
        val ULP_BIAS_MAP = 0b0_00000000_00001101_00000000_00001001_00001000L

        val biasMapEvenOdd = ULP_BIAS_MAP or (lsdwIsOdd and 1)
        val bitIndex = (roundingDirection.value * 8) + value
        val roundingMapShifted = biasMapEvenOdd shr bitIndex
        val bias = roundingMapShifted and 1
        return bias
    }

    // used in add case when there is no overlap
    fun ulpBiasX(roundingDirection: RoundingDirection, lsdwIsOdd: Long) : Long {
        assert(value != BIAS_TRUNC.value)
        return when (roundingDirection) {
            ROUND_TIES_TO_EVEN -> when (value) {
                HALF.value -> lsdwIsOdd and 1L
                BIAS_TRUNC.value -> 0L
                LT_HALF.value -> 0L
                GT_HALF.value -> 1L
                // EXACT
                else -> 0L
            }
            ROUND_TIES_TO_AWAY -> when (value) {
                HALF.value -> 1L
                BIAS_TRUNC.value -> 0L
                LT_HALF.value -> 0L
                GT_HALF.value -> 1L
                // EXACT
                else -> 0L
            }
            ROUND_TOWARD_ZERO -> when (value) {
                HALF.value -> 0L
                BIAS_TRUNC.value -> 0L
                LT_HALF.value -> 0L
                GT_HALF.value -> 0L
                // EXACT
                else -> 0L
            }
            ROUND_TOWARD_POSITIVE -> when (value) {
                HALF.value -> 1L
                BIAS_TRUNC.value -> 0L
                LT_HALF.value -> 1L
                GT_HALF.value -> 1L
                // EXACT
                else -> 0L
            }
            // ROUND_TOWARD_NEGATIVE
            else -> when (value) {
                HALF.value -> 0L
                BIAS_TRUNC.value -> 0L
                LT_HALF.value -> 0L
                GT_HALF.value -> 0L
                // EXACT
                else -> 0L
            }
        }
    }

    override fun toString(): String = when (this) {
        EXACT -> "EXACT"
        HALF -> "HALF"
        BIAS_TRUNC -> "BIAS_TRUNC"
        LT_HALF -> "LT_HALF"
        GT_HALF -> "GT_HALF"
        else -> "invalid Residue value"
    }
}
