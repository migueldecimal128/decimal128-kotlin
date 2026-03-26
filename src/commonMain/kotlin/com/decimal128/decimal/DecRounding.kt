package com.decimal128.decimal

expect value class DecRounding private constructor(val value:Int) {
    companion object {
        val ROUND_TIES_TO_EVEN: DecRounding
        val ROUND_TIES_TO_AWAY: DecRounding
        val ROUND_TOWARD_ZERO: DecRounding
        val ROUND_TOWARD_POSITIVE: DecRounding
        val ROUND_TOWARD_NEGATIVE: DecRounding

        fun fromValue(value: Int): DecRounding
    }

    // roundTiesToEve, roundTiesToAway, and roundTiesTowardZero are the same independent of sign
    // roundTowardPositive and roundTowardNegative are complementary
    fun negate(): DecRounding

    fun negate(sign: Boolean): DecRounding

    fun overflowsToInfinity(sign: Boolean): Boolean

    fun underflowsToZero(sign: Boolean): Boolean

}
