package com.decimal128.decimal

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

    override fun toString() = toDebugString()

}
