// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

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
    val dw0MaxxCoeff: Long = pow10_128_dw0(precision)
    val dw1MaxxCoeff: Long = pow10_128_dw1(precision)
    val dw0MinFullPrecisionCoeff:Long = pow10_128_dw0(precision - 1)
    val dw1MinFullPrecisionCoeff:Long = pow10_128_dw1(precision - 1)
    val packedLengthsAfterOverflow = pow10PackedLengths(precision - 1)
    val nanPayloadPrecision = if (precision == 34) 33 else 38

    fun isC128AddSafe(xBitLen: Int, yBitLen: Int): Boolean {
        val sumBitLen = max(xBitLen, yBitLen) + 1
        return sumBitLen < maxBitLen
    }

    internal inline fun coeffFits(dw1: Long, dw0: Long): Boolean =
        unsignedLT(dw1, dw1MaxxCoeff) || dw1 == dw1MaxxCoeff && unsignedLT(dw0, dw0MaxxCoeff)

    internal inline fun coeffQexpFit(dw1: Long, dw0: Long, qExp: Int): Boolean =
        (unsignedLT(dw1, dw1MaxxCoeff) || dw1 == dw1MaxxCoeff && unsignedLT(dw0, dw0MaxxCoeff)) &&
                (qExp >= qTiny && qExp <= qMax)

    internal inline fun coeffIsMaxx(dw1: Long, dw0: Long): Boolean =
        dw1 == this.dw1MaxxCoeff && dw0 == this.dw0MaxxCoeff


}
