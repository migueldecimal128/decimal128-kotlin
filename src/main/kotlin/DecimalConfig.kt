package com.decimal128

class DecimalConfig(val precision: Int, val eMax: Int, val roundingDirection: RoundingDirection) {
    constructor(roundingDirection: RoundingDirection) : this(34, 6144, roundingDirection)

    companion object {
        val DECIMAL_64_CONFIG = DecimalConfig(16, 384, RoundingDirection.ROUND_TIES_TO_EVEN)
        val DECIMAL_128_CONFIG = DecimalConfig(34, 6144, RoundingDirection.ROUND_TIES_TO_EVEN)
        val DECIMAL_128_EXTENDED_CONFIG = DecimalConfig(38, 9999, RoundingDirection.ROUND_TIES_TO_EVEN)
    }

    val eMin = -(eMax - 1)
    val qMax = eMax - (precision - 1)
    val qTiny = eMin - (precision - 1)
}
