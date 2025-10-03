package com.decimal128.decimal

import com.decimal128.decimal.U256Pow10.pow10BitLen
import kotlin.math.max

data class DecFormat(val precision: Int,
                     val eMax: Int,
                     val eMin: Int
    ) {
    constructor(precision: Int,
                eMax: Int
        ) : this(precision, eMax, -(eMax - 1))

    constructor(decRounding: DecRounding) : this(34, 6144)

    companion object {
        val DECIMAL_64 = DecFormat(16, 384)
        init { check(DECIMAL_64.maxBitLen == 54)}
        val DECIMAL_128 = DecFormat(34, 6144)
        init { check(DECIMAL_128.maxBitLen == 113)}
        val DECIMAL_128_EXTENDED = DecFormat(38, 6144)
        init { check(DECIMAL_128_EXTENDED.maxBitLen == 127)}
    }

    val qMax = eMax - (precision - 1)
    val qTiny = eMin - (precision - 1)
    val maxBitLen = pow10BitLen(precision)

    fun isC128AddSafe(xBitLen: Int, yBitLen: Int): Boolean {
        val sumBitLen = max(xBitLen, yBitLen) + 1
        return sumBitLen < maxBitLen
    }
}
