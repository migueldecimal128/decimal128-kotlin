// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

actual abstract class DecContextRep actual constructor(
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

    internal actual val dw0MaxxCoeff: Long = pow10_128_dw0(precision)
    internal actual val dw1MaxxCoeff: Long = pow10_128_dw1(precision)
    internal actual val dw0MinFullPrecisionCoeff:Long = pow10_128_dw0(precision - 1)
    internal actual val dw1MinFullPrecisionCoeff:Long = pow10_128_dw1(precision - 1)

    actual companion object {
        actual fun decimal128Kotlin(): DecContext = decContextDecimal128Kotlin()

        actual fun decimal128IEEE(): DecContext = decContextDecimal128IEEE()

        actual fun decimal128Extended38(): DecContext = decContextDecimal128Extended38()

        actual fun current(): DecContext = DecContextThreadLocal.current()
        actual fun setCurrent(newDecContext: DecContext) {
            DecContextThreadLocal.setCurrent(newDecContext)
        }

        actual fun internal38(): DecContext = DecContextThreadLocal.internal38()
        actual fun setInternal38(newDecContext: DecContext) {
            DecContextThreadLocal.setInternal38(newDecContext)
        }

    }

    override fun toString(): String =
        "DecContext(isExtendedPrecision38:$isExtendedPrecision38, decRounding=$decRounding, decPrefs=$decPrefs, decTrapHandlers=$decTrapHandlers, decFlags=$decFlags)"
}

actual object DecContextThreadLocal {
    private val threadLocalCurrent = ThreadLocal.withInitial { DecContext.decimal128Kotlin() }
    private val threadLocalInternal: ThreadLocal<DecContext?> = ThreadLocal.withInitial { null }

    actual fun current(): DecContext = threadLocalCurrent.get()
    actual fun setCurrent(newDecContext: DecContext) = threadLocalCurrent.set(newDecContext)

    actual fun internal38(): DecContext {
        var ctx = threadLocalInternal.get()
        if (ctx == null) {
            ctx = DecContext.decimal128Extended38()
            threadLocalInternal.set(ctx)
        }
        return ctx
    }
    actual fun setInternal38(newDecContext: DecContext) = threadLocalInternal.set(newDecContext)

}
