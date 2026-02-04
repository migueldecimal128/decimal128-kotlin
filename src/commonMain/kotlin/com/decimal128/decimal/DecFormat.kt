package com.decimal128.decimal

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
        init {
            verify { DECIMAL_64.maxBitLen == 54 }
        }
        val DECIMAL_128 = DecFormat(34, 6144)
        init {
            verify { DECIMAL_128.maxBitLen == 113 }
        }
        val DECIMAL_128_EXTENDED = DecFormat(38, 6144)
        init {
            verify { DECIMAL_128_EXTENDED.maxBitLen == 127 }
        }
    }

    val qMax: Int = eMax - (precision - 1)
    val qTiny: Int = eMin - (precision - 1)
    val maxBitLen: Int = pow10BitLen(precision)
    val maxxPair = pow10_128(precision)
    val dw0Maxx: Long
        get() = maxxPair.second
    val dw1Maxx: Long
        get() = maxxPair.first
    val afterRolloverPair = pow10_128(precision - 1)
    val dw0AfterRollover:Long
        get() = afterRolloverPair.second
    val dw1AfterRollover:Long
        get() = afterRolloverPair.first
    val packedLengthsAfterOverflow = pow10PackedLengths(precision - 1)

    fun isC128AddSafe(xBitLen: Int, yBitLen: Int): Boolean {
        val sumBitLen = max(xBitLen, yBitLen) + 1
        return sumBitLen < maxBitLen
    }
}
