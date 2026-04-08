// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

@kotlin.native.concurrent.ThreadLocal
private var ctxCurrent: DecContext = decContextDecimal128Kotlin()

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
    internal actual val decPrefs: DecPrefs = decPrefs
    internal actual val decTrapHandlers: DecTrapHandlers? = decTrapHandlers
    internal actual val decFlags: DecFlags = decFlags
    internal actual val tmps: DecTmps = decTmps
    internal actual val isExtendedPrecision38: Boolean = isExtendedPrecision38

    internal actual val precision: Int = if (isExtendedPrecision38) 38 else 34
    internal actual val eMax:Int = Q_MAX + precision - 1
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

        actual fun current(): DecContext = ctxCurrent
        actual fun setCurrent(newDecContext: DecContext) {
            ctxCurrent = newDecContext
        }

    }

    override fun toString(): String =
        "DecContext(decFormat=$decFormat, decRounding=$decRounding, decPrefs=$decPrefs, decTrapHandlers=$decTrapHandlers, decFlags=$decFlags)"
}
