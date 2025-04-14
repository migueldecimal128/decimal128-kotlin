package com.decimal128

@JvmInline
value class Residual private constructor(val value: Int) {
    companion object {
        val EXACT = Residual(0)
        val HALF = Residual(1)
        val BIAS_TRUNC = Residual(2)


        // with RoundingDirection ROUND_TOWARDS_POSITIVE and ROUND_TOWARDS_NEGATIVE
        // the bias is 1 ULP, not 1/2 ULP
        // this table stores 5 RoundingDirections, starting from the right
        // each entry is 8 bits long, although only 3x2=6 bits are actually used
        private const val BIAS_MAP = 0b0_00000000_00101000_00000000_00010100_00010000L

        fun biasFrom(roundingDirection: RoundingDirection, residual: Residual, lsbIsOdd: Long) : Long {
            val exactOr3Mask = ((-residual.value shr 1) ushr 30).toLong() // exactOr3Mask = if (EXACT) 0L else 03L
            val biasMapEvenOdd = BIAS_MAP or (lsbIsOdd shl 2)
            val bitIndex = ((roundingDirection.value * 4) + residual.value) * 2
            val roundingMapShifted = biasMapEvenOdd shr bitIndex
            val bias = exactOr3Mask and roundingMapShifted
            return bias
        }


    }

    override fun toString(): String = when (this) {
        EXACT -> "EXACT"
        HALF -> "HALF"
        BIAS_TRUNC -> "BIAS_TRUNC"
        else -> "invalid remainder status"
    }
}
