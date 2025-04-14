package com.decimal128

@JvmInline
value class ResidualOld private constructor(val value: Int) {
    companion object {
        val EXACT = ResidualOld(0)
        val LT_HALF = ResidualOld(1)
        val HALF = ResidualOld(2)
        val GT_HALF = ResidualOld(3)


        // contains pairs of bits
        // lo bit of the two is whether or not to add one to the coefficient for rounding
        // hi bit is whether or not the rounding is inexact ... for INEXACT status flag
        // mapping of RoundingDirection to support negative Coefficients is handled under RoundingDirection
        private const val ROUNDING_MAP = 0b0_10101000_11111100_10101000_11111000_11101000L

        fun inexactAndRoundupFrom(roundingDirection: RoundingDirection, residual: ResidualOld, lsbIsOdd: Long) : Long {
            val exactOr3Mask = ((-residual.value shr 1) ushr 30).toLong() // exactOr3Mask = if (EXACT) 0L else 03L
            val roundingMapEvenOdd = ROUNDING_MAP or (lsbIsOdd shl (ResidualOld.HALF.value * 2))
            val bitIndex = ((roundingDirection.value * 4) + residual.value) * 2
            val roundingMapShifted = roundingMapEvenOdd shr bitIndex
            val inexactAndRoundup = exactOr3Mask and roundingMapShifted
            return inexactAndRoundup
        }


    }

    override fun toString(): String = when (this) {
        EXACT -> "EXACT"
        LT_HALF -> "LT_HALF"
        HALF -> "HALF"
        GT_HALF -> "GT_HALF"
        else -> "invalid remainder status"
    }
}
