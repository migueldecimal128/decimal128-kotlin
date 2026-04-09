// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

@kotlin.native.concurrent.ThreadLocal
private var ctxCurrent: DecContext = decContextDecimal128Kotlin()
private var ctxInternal38: DecContext? = null

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

    internal actual val dw0MaxxCoeff: Long = pow10_128_dw0(precision)
    internal actual val dw1MaxxCoeff: Long = pow10_128_dw1(precision)
    internal actual val dw0MinFullPrecisionCoeff:Long = pow10_128_dw0(precision - 1)
    internal actual val dw1MinFullPrecisionCoeff:Long = pow10_128_dw1(precision - 1)

    actual companion object {
        actual fun decimal128Kotlin(): DecContext = decContextDecimal128Kotlin()

        actual fun decimal128IEEE(): DecContext = decContextDecimal128IEEE()

        actual fun decimal128Extended38(): DecContext = decContextDecimal128Extended38()

        actual fun current(): DecContext = ctxCurrent
        actual fun setCurrent(newDecContext: DecContext) {
            ctxCurrent = newDecContext
        }

        actual fun internal38(): DecContext {
            var ctx38 = ctxInternal38
            if (ctx38 == null) {
                ctx38 = decContextDecimal128Extended38()
                ctxInternal38 = ctx38
            }
            return ctx38
        }

        actual fun setInternal38(newDecContext: DecContext) {
            ctxInternal38 = newDecContext
        }

    }

    override fun toString(): String =
        "DecContext(decFormat=$decFormat, decRounding=$decRounding, decPrefs=$decPrefs, decTrapHandlers=$decTrapHandlers, decFlags=$decFlags)"
}
