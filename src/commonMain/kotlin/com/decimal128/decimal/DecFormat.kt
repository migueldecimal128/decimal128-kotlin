package com.decimal128.decimal

import com.decimal128.decimal.U256Pow10.pow10BitLen
import kotlin.math.max

data class DecFormat(val precision: Int,
                     val eMax: Int,
                     val eMin: Int,
                     val decRounding: DecRounding
    ) {
    constructor(precision: Int,
                eMax: Int,
                decRounding: DecRounding,
        ) : this(precision, eMax, -(eMax - 1), decRounding)

    constructor(decRounding: DecRounding) : this(34, 6144, decRounding)

    companion object {
        val DECIMAL_64 = DecFormat(16, 384, DecRounding.ROUND_TIES_TO_EVEN)
        init { check(DECIMAL_64.maxBitLen == 54)}
        val DECIMAL_128 = DecFormat(34, 6144, DecRounding.ROUND_TIES_TO_EVEN)
        init { check(DECIMAL_128.maxBitLen == 113)}
        val DECIMAL_128_EXTENDED = DecFormat(38, 6144, DecRounding.ROUND_TIES_TO_EVEN)
        init { check(DECIMAL_128_EXTENDED.maxBitLen == 127)}
    }

    val qMax = eMax - (precision - 1)
    val qTiny = eMin - (precision - 1)
    val maxBitLen = pow10BitLen(precision)

    fun withRoundingDirection(decRounding: DecRounding): DecFormat =
        DecFormat(this.precision, this.eMax, decRounding)

    fun isC128AddSafe(xBitLen: Int, yBitLen: Int): Boolean {
        val sumBitLen = max(xBitLen, yBitLen) + 1
        return sumBitLen < maxBitLen
    }
}
