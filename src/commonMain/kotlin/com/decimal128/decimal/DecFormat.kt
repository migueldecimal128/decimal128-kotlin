// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

data class DecFormat(val precision: Int,
                     val qMax: Int,
                     val qTiny: Int,
                     val eMax: Int,
                     val eMin: Int,
                     val maxBitLen: Int,
    ) {

    companion object {

        internal const val Q_MAX = 6111
        internal const val Q_TINY = -6176
        internal const val NAN_PAYLOAD_PRECISION = 33

        val DECIMAL_128 = DecFormat(
            precision = 34,
            qMax = Q_MAX,
            qTiny = Q_TINY,
            eMax = 6144,
            eMin = -6176,
            maxBitLen = 113,
        )

        val DECIMAL_128_EXTENDED = DecFormat(
            precision = 38,
            qMax = Q_MAX,
            qTiny = Q_TINY,
            eMax = 6148,
            eMin = -6180,
            maxBitLen = 127,
        )
    }

    val dw0MaxxCoeff: Long = pow10_128_dw0(precision)
    val dw1MaxxCoeff: Long = pow10_128_dw1(precision)
    val dw0MinFullPrecisionCoeff:Long = pow10_128_dw0(precision - 1)
    val dw1MinFullPrecisionCoeff:Long = pow10_128_dw1(precision - 1)

    internal inline fun coeffFits(dw1: Long, dw0: Long): Boolean =
        unsignedLT(dw1, dw1MaxxCoeff) || dw1 == dw1MaxxCoeff && unsignedLT(dw0, dw0MaxxCoeff)

    internal /* inline */ fun coeffQexpFit(dw1: Long, dw0: Long, qExp: Int): Boolean =
        (unsignedLT(dw1, dw1MaxxCoeff) || dw1 == dw1MaxxCoeff && unsignedLT(dw0, dw0MaxxCoeff)) &&
                (qExp >= qTiny && qExp <= qMax)

    internal inline fun coeffIsMaxx(dw1: Long, dw0: Long): Boolean =
        dw1 == this.dw1MaxxCoeff && dw0 == this.dw0MaxxCoeff


}
