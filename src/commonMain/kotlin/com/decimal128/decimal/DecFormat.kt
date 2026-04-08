// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

class DecFormat private constructor (val precision: Int,
                                     val eMax: Int,
                                     val eMin: Int,
    ) {

    companion object {

        val DECIMAL_128 = DecFormat(
            precision = 34,
            eMax = 6144,
            eMin = -6176,
        )

        val DECIMAL_128_EXTENDED = DecFormat(
            precision = 38,
            eMax = 6148,
            eMin = -6180,
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
                (qExp >= Q_TINY && qExp <= Q_MAX)

    internal inline fun coeffIsMaxx(dw1: Long, dw0: Long): Boolean =
        dw1 == this.dw1MaxxCoeff && dw0 == this.dw0MaxxCoeff


    override fun toString(): String = when (precision) {
        34 -> "DECIMAL_128"
        38 -> "DECIMAL_128_EXTENDED"
        else -> "DecFormat(precision=$precision)"
    }
}
