// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

actual abstract class DecContextRep actual constructor(
    roundingDirection: RoundingDirection,
    parsePrefs: ParsePrefs,
    printPrefs: PrintPrefs,
    arithmeticPrefs: ArithmeticPrefs,
    decTrapHandlers: DecTrapHandlers?,
    decFlags: DecFlags,
    decTmps: DecTmps,
    isExtendedPrecision38: Boolean
) {
    //@JvmField DecRounding is a value class :(
    internal actual val roundingDirection: RoundingDirection = roundingDirection
    @JvmField
    internal actual val parsePrefs: ParsePrefs = parsePrefs
    @JvmField
    internal actual val printPrefs: PrintPrefs = printPrefs
    @JvmField
    internal actual val arithmeticPrefs: ArithmeticPrefs = arithmeticPrefs
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

    override fun toString(): String =
        "DecContext(isExtendedPrecision38:$isExtendedPrecision38, decRounding=$roundingDirection, parsePrefs=$parsePrefs, printPrefs=$printPrefs, arithmeticPrefs=$arithmeticPrefs, decTrapHandlers=$decTrapHandlers, decFlags=$decFlags)"
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
