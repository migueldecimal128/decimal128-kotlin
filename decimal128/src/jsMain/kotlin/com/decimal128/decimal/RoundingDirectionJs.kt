package com.decimal128.decimal

private val TO_STRING_MAP = arrayOf("ROUND_TIES_TO_EVEN", "ROUND_TIES_TO_AWAY",
    "ROUND_TOWARD_ZERO", "ROUND_TOWARD_POSITIVE", "ROUND_TOWARD_NEGATIVE")


actual value class RoundingDirection private constructor(val value:Int) {
    actual companion object {
        actual val TIES_TO_EVEN = RoundingDirection(0)
        actual val TIES_TO_AWAY = RoundingDirection(1)
        actual val TOWARD_ZERO = RoundingDirection(2)
        actual val TOWARD_POSITIVE = RoundingDirection(3)
        actual val TOWARD_NEGATIVE = RoundingDirection(4)

        actual fun fromValue(value:Int) : RoundingDirection {
            return when (value) {
                0 -> TIES_TO_EVEN
                1 -> TIES_TO_AWAY
                2 -> TOWARD_ZERO
                3 -> TOWARD_POSITIVE
                4 -> TOWARD_NEGATIVE
                else -> throw RuntimeException("value:$value out of range for RoundingDirection")
            }
        }

    }

    override fun toString() = TO_STRING_MAP[value]

}
