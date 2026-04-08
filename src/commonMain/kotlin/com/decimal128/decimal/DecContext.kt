// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

expect class DecContext(
    decRounding: DecRounding,
    decPrefs: DecPrefs,
    decTrapHandlers: DecTrapHandlers?,
    decFlags: DecFlags,
    decTmps: DecTmps,
    isExtendedPrecision38: Boolean = false
) {
    internal val decRounding: DecRounding
    internal val decPrefs: DecPrefs
    internal val decTrapHandlers: DecTrapHandlers?
    internal val decFlags: DecFlags
    internal val tmps: DecTmps
    internal val isExtendedPrecision38: Boolean

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
