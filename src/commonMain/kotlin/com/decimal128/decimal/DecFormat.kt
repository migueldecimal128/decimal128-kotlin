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
    val dw0Maxx = getPow10Dw0(precision)
    val dw1Maxx = getPow10Dw1(precision)
    val dw0AfterRollover = getPow10Dw0(precision - 1)
    val dw1AfterRollover = getPow10Dw1(precision - 1)
    val packedLengthsAfterOverflow = pow10PackedLengths(precision - 1)

    fun isC128AddSafe(xBitLen: Int, yBitLen: Int): Boolean {
        val sumBitLen = max(xBitLen, yBitLen) + 1
        return sumBitLen < maxBitLen
    }
}
