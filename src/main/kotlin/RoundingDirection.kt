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

        fun fromValue(value:Int) : RoundingDirection {
            return when (value) {
                0 -> ROUND_TIES_TO_EVEN
                1 -> ROUND_TIES_TO_AWAY
                2 -> ROUND_TOWARD_ZERO
                3 -> ROUND_TOWARD_POSITIVE
                4 -> ROUND_TOWARD_NEGATIVE
                else -> throw RuntimeException("value:$value out of range for RoundingDirection")
            }
        }

    }

    // roundTiesToEve, roundTiesToAway, and roundTiesTowardZero are the same independent of sign
    // roundTowardPositive and roundTowardNegative are complementary
    fun negate() :RoundingDirection {
        return if (value < 3) this else if (value == 3) ROUND_TOWARD_NEGATIVE else ROUND_TOWARD_POSITIVE
    }

    fun negate(signBit: Int) = if (signBit != 0) negate() else this

    fun mapToRoundingMode() = ROUNDING_MODE_MAP[value]

    fun overflowsToInfinity(sign: Int): Boolean {
        val toInfinity = when (this.value) {
            ROUND_TIES_TO_EVEN.value -> true
            ROUND_TIES_TO_AWAY.value -> true
            ROUND_TOWARD_ZERO.value -> false
            ROUND_TOWARD_POSITIVE.value -> sign == 0
            ROUND_TOWARD_NEGATIVE.value -> sign != 0
            else -> throw RuntimeException("unrecognized RoundingDirection value:$value")
        }
        return toInfinity
    }

    fun underflowsToZero(sign: Int): Boolean {
        val toZero = when (this.value) {
            ROUND_TIES_TO_EVEN.value -> true
            ROUND_TIES_TO_AWAY.value -> true
            ROUND_TOWARD_ZERO.value -> true
            ROUND_TOWARD_POSITIVE.value -> sign != 0
            ROUND_TOWARD_NEGATIVE.value -> sign == 0
            else -> throw RuntimeException("unrecognized RoundingDirection value:$value")
        }
        return toZero
    }

    override fun toString() = TO_STRING_MAP[value]

}
