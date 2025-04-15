package com.decimal128
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE
import kotlin.math.round

@JvmInline
value class Residual private constructor(val value: Int) {
    companion object {
        val HALF = Residual(0)
        val BIAS_TRUNC = Residual(1)
        val LT_HALF = Residual(2)
        val GT_HALF = Residual(3)
        val EXACT = Residual(4)


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
        else -> "invalid remainder status"
    }
}
