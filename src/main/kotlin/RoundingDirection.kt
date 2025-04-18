package com.decimal128

import java.math.RoundingMode

private val ROUNDING_MODE_MAP = arrayOf(RoundingMode.HALF_EVEN, RoundingMode.HALF_UP,
    RoundingMode.DOWN, RoundingMode.CEILING, RoundingMode.FLOOR)
private val TO_STRING_MAP = arrayOf("ROUND_TIES_TO_EVEN", "ROUND_TIES_TO_AWAY",
    "ROUND_TOWARD_ZERO", "ROUND_TOWARD_POSITIVE", "ROUND_TOWARD_NEGATIVE")


@JvmInline
value class RoundingDirection private constructor(val value:Int) {
    companion object {
        val ROUND_TIES_TO_EVEN = RoundingDirection(0)
        val ROUND_TIES_TO_AWAY = RoundingDirection(1)
        val ROUND_TOWARD_ZERO = RoundingDirection(2)
        val ROUND_TOWARD_POSITIVE = RoundingDirection(3)
        val ROUND_TOWARD_NEGATIVE = RoundingDirection(4)
    }

    // roundTiesToEve, roundTiesToAway, and roundTiesTowardZero are the same independent of sign
    // roundTowardPositive and roundTowardNegative are complementary
    fun negate() :RoundingDirection {
        return if (value < 3) this else if (value == 3) ROUND_TOWARD_NEGATIVE else ROUND_TOWARD_POSITIVE
    }

    fun negate(signBit:Boolean) = if (signBit) negate() else this

    fun mapToRoundingMode() = ROUNDING_MODE_MAP[value]

    override fun toString() = TO_STRING_MAP[value]

}
