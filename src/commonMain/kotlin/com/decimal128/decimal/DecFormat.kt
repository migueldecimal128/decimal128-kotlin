// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

class DecFormat private constructor (val precision: Int,
//                                     val eMax: Int,
//                                     val eMin: Int,
    ) {

    companion object {

        val DECIMAL_128 = DecFormat(
            precision = 34,
            //eMax = 6144,
            //eMin = -6176,
        )

        val DECIMAL_128_EXTENDED = DecFormat(
            precision = 38,
            //eMax = 6148,
            //eMin = -6180,
        )
    }

}
