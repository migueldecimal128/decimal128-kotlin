// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

expect abstract class DecContextRep(
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

}

expect object DecContextThreadLocal {

    fun current(): DecContext
    fun setCurrent(newDecContext: DecContext)

    fun internal38(): DecContext
    fun setInternal38(newDecContext: DecContext)

}

class DecContext(
    decRounding: DecRounding,
    decPrefs: DecPrefs,
    decTrapHandlers: DecTrapHandlers?,
    decFlags: DecFlags,
    decTmps: DecTmps,
    isExtendedPrecision38: Boolean = false
) : DecContextRep(
    decRounding, decPrefs, decTrapHandlers, decFlags, decTmps, isExtendedPrecision38) {

    companion object {
        fun decimal128Kotlin(): DecContext = decContextDecimal128Kotlin()

        fun decimal128IEEE(): DecContext = decContextDecimal128IEEE()

        fun decimal128Extended38(): DecContext = decContextDecimal128Extended38()

        fun current(): DecContext = DecContextThreadLocal.current()
        fun setCurrent(newDecContext: DecContext) {
            DecContextThreadLocal.setCurrent(newDecContext)
        }

        fun internal38(): DecContext = DecContextThreadLocal.internal38()
        fun setInternal38(newDecContext: DecContext) {
            DecContextThreadLocal.setInternal38(newDecContext)
        }

    }

}