// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

actual class DecContext actual constructor(
    decRounding: DecRounding,
    decPrefs: DecPrefs,
    decTrapHandlers: DecTrapHandlers?,
    decFlags: DecFlags,
    decTmps: DecTmps,
    isExtendedPrecision38: Boolean
) {
    //@JvmField DecRounding is a value class :(
    internal actual val decRounding: DecRounding = decRounding
    @JvmField
    internal actual val decPrefs: DecPrefs = decPrefs
    @JvmField
    internal actual val decTrapHandlers: DecTrapHandlers? = decTrapHandlers
    @JvmField
    internal actual val decFlags: DecFlags = decFlags
    @JvmField
    internal actual val tmps: DecTmps = decTmps
    @JvmField
    internal actual val isExtendedPrecision38: Boolean = isExtendedPrecision38

    @JvmField
    internal actual val precision: Int = if (isExtendedPrecision38) 38 else 34
    @JvmField
    internal actual val eMax:Int = Q_MAX + precision - 1
    @JvmField
    internal actual val eMin:Int = Q_TINY + 34 - precision

    internal actual val dw0MaxxCoeff: Long = pow10_128_dw0(precision)
    internal actual val dw1MaxxCoeff: Long = pow10_128_dw1(precision)
    internal actual val dw0MinFullPrecisionCoeff:Long = pow10_128_dw0(precision - 1)
    internal actual val dw1MinFullPrecisionCoeff:Long = pow10_128_dw1(precision - 1)

    internal actual fun coeffFits(dw1: Long, dw0: Long): Boolean =
        unsignedLT(dw1, dw1MaxxCoeff) || dw1 == dw1MaxxCoeff && unsignedLT(dw0, dw0MaxxCoeff)

    internal actual fun coeffQexpFit(dw1: Long, dw0: Long, qExp: Int): Boolean =
        (unsignedLT(dw1, dw1MaxxCoeff) || dw1 == dw1MaxxCoeff && unsignedLT(dw0, dw0MaxxCoeff)) &&
                (qExp >= Q_TINY && qExp <= Q_MAX)

    internal actual inline fun coeffIsMaxx(dw1: Long, dw0: Long): Boolean =
        dw1 == this.dw1MaxxCoeff && dw0 == this.dw0MaxxCoeff



    actual companion object {
        actual fun decimal128Kotlin(): DecContext = decContextDecimal128Kotlin()

        actual fun decimal128IEEE(): DecContext = decContextDecimal128IEEE()

        actual fun decimal128Extended38(): DecContext = decContextDecimal128Extended38()

        private val threadLocal = ThreadLocal.withInitial { decimal128Kotlin() }

        actual fun current(): DecContext = threadLocal.get()
        actual fun setCurrent(newDecContext: DecContext) = threadLocal.set(newDecContext)

    }

    override fun toString(): String =
        "DecContext(isExtendedPrecision38:$isExtendedPrecision38, decRounding=$decRounding, decPrefs=$decPrefs, decTrapHandlers=$decTrapHandlers, decFlags=$decFlags)"
}
