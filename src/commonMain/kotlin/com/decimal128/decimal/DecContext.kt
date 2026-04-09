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

    internal val dw0MaxxCoeff: Long
    internal val dw1MaxxCoeff: Long
    internal val dw0MinFullPrecisionCoeff:Long
    internal val dw1MinFullPrecisionCoeff:Long

    companion object {
        fun decimal128Kotlin(): DecContext
        fun decimal128IEEE(): DecContext
        fun decimal128Extended38(): DecContext

        fun current(): DecContext
        fun setCurrent(newDecContext: DecContext)

        fun internal38(): DecContext
        fun setInternal38(newDecContext: DecContext)
    }
}
