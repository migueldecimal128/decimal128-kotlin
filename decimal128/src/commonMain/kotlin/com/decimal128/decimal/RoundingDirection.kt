package com.decimal128.decimal

expect value class RoundingDirection private constructor(val value:Int) {
    companion object {
        val TIES_TO_EVEN: RoundingDirection
        val TIES_TO_AWAY: RoundingDirection
        val TOWARD_ZERO: RoundingDirection
        val TOWARD_POSITIVE: RoundingDirection
        val TOWARD_NEGATIVE: RoundingDirection

        fun fromValue(value: Int): RoundingDirection
    }

    // roundTiesToEve, roundTiesToAway, and roundTiesTowardZero are the same independent of sign
    // roundTowardPositive and roundTowardNegative are complementary

    fun negated(): RoundingDirection

    fun negated(sign: Boolean): RoundingDirection

    fun overflowsToInfinity(sign: Boolean): Boolean

    fun underflowsToZero(sign: Boolean): Boolean

}
