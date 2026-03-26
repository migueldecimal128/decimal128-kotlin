package com.decimal128.decimal

private val TO_STRING_MAP = arrayOf("ROUND_TIES_TO_EVEN", "ROUND_TIES_TO_AWAY",
    "ROUND_TOWARD_ZERO", "ROUND_TOWARD_POSITIVE", "ROUND_TOWARD_NEGATIVE")


@JvmInline
actual value class DecRounding private constructor(val value:Int) {
    actual companion object {
        actual val ROUND_TIES_TO_EVEN = DecRounding(0)
        actual val ROUND_TIES_TO_AWAY = DecRounding(1)
        actual val ROUND_TOWARD_ZERO = DecRounding(2)
        actual val ROUND_TOWARD_POSITIVE = DecRounding(3)
        actual val ROUND_TOWARD_NEGATIVE = DecRounding(4)

        actual fun fromValue(value:Int) : DecRounding {
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
    actual fun negate() :DecRounding {
        return if (value < 3) this else if (value == 3) ROUND_TOWARD_NEGATIVE else ROUND_TOWARD_POSITIVE
    }

    actual fun negate(sign: Boolean) = if (sign) negate() else this

    actual fun overflowsToInfinity(sign: Boolean): Boolean {
        val toInfinity = when (this.value) {
            ROUND_TIES_TO_EVEN.value -> true
            ROUND_TIES_TO_AWAY.value -> true
            ROUND_TOWARD_ZERO.value -> false
            ROUND_TOWARD_POSITIVE.value -> !sign
            ROUND_TOWARD_NEGATIVE.value -> sign
            else -> throw RuntimeException("unrecognized RoundingDirection value:$value")
        }
        return toInfinity
    }

    actual fun underflowsToZero(sign: Boolean): Boolean {
        val toZero = when (this.value) {
            ROUND_TIES_TO_EVEN.value -> true
            ROUND_TIES_TO_AWAY.value -> true
            ROUND_TOWARD_ZERO.value -> true
            ROUND_TOWARD_POSITIVE.value -> sign
            ROUND_TOWARD_NEGATIVE.value -> !sign
            else -> throw RuntimeException("unrecognized RoundingDirection value:$value")
        }
        return toZero
    }

    override fun toString() = TO_STRING_MAP[value]

}
