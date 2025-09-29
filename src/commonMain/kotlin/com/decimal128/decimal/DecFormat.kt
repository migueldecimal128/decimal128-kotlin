package com.decimal128.decimal

class DecFormat(val precision: Int, val eMax: Int, val eMin: Int, val decRounding: DecRounding) {
    constructor(precision: Int, eMax: Int, decRounding: DecRounding) : this(precision, eMax, -(eMax - 1), decRounding)
    constructor(decRounding: DecRounding) : this(34, 6144, decRounding)

    companion object {
        val DECIMAL_64 = DecFormat(16, 384, DecRounding.ROUND_TIES_TO_EVEN)
        val DECIMAL_128 = DecFormat(34, 6144, DecRounding.ROUND_TIES_TO_EVEN)
        val DECIMAL_128_EXTENDED = DecFormat(38, 9999, DecRounding.ROUND_TIES_TO_EVEN)
    }

    val qMax = eMax - (precision - 1)
    val qTiny = eMin - (precision - 1)

    fun withRoundingDirection(decRounding: DecRounding): DecFormat =
        DecFormat(this.precision, this.eMax, decRounding)
}
