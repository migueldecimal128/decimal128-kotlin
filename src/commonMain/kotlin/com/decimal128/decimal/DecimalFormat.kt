package com.decimal128.decimal

class DecimalFormat(val precision: Int, val eMax: Int, val eMin: Int, val roundingDirection: RoundingDirection) {
    constructor(precision: Int, eMax: Int, roundingDirection: RoundingDirection) : this(precision, eMax, -(eMax - 1), roundingDirection)
    constructor(roundingDirection: RoundingDirection) : this(34, 6144, roundingDirection)

    companion object {
        val DECIMAL_64 = DecimalFormat(16, 384, RoundingDirection.ROUND_TIES_TO_EVEN)
        val DECIMAL_128 = DecimalFormat(34, 6144, RoundingDirection.ROUND_TIES_TO_EVEN)
        val DECIMAL_128_EXTENDED = DecimalFormat(38, 9999, RoundingDirection.ROUND_TIES_TO_EVEN)
    }

    val qMax = eMax - (precision - 1)
    val qTiny = eMin - (precision - 1)

    fun withRoundingDirection(roundingDirection: RoundingDirection): DecimalFormat =
        DecimalFormat(this.precision, this.eMax, roundingDirection)
}
