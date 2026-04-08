// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.DecException.*
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE

expect class DecContext(decFormat: DecFormat,
                        decRounding: DecRounding,
                        decPrefs: DecPrefs,
                        decTrapHandlers: DecTrapHandlers?,
                        decFlags: DecFlags,
                        decTmps: DecTmps) {
    internal val decFormat: DecFormat
    internal val decRounding: DecRounding
    internal val decPrefs: DecPrefs
    internal val decTrapHandlers: DecTrapHandlers?
    internal val decFlags: DecFlags
    internal val tmps: DecTmps

    internal val precision: Int

    internal val eMax: Int
    internal val eMin: Int

    internal val dw0MaxxCoeff: Long
    internal val dw1MaxxCoeff: Long
    internal val dw0MinFullPrecisionCoeff:Long
    internal val dw1MinFullPrecisionCoeff:Long

    internal fun coeffFits(dw1: Long, dw0: Long): Boolean

    internal fun coeffQexpFit(dw1: Long, dw0: Long, qExp: Int): Boolean

    internal inline fun coeffIsMaxx(dw1: Long, dw0: Long): Boolean


    companion object {
        fun decimal128Kotlin(): DecContext
        fun decimal128IEEE(): DecContext
        fun decimal128Extended38(): DecContext

        fun current(): DecContext
        fun setCurrent(newDecContext: DecContext)
    }
}
