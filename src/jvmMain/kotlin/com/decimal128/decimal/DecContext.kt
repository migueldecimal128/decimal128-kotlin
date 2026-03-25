// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

actual class DecContext actual constructor(decFormat: DecFormat,
                                           decRounding: DecRounding,
                                           decPrefs: DecPrefs,
                                           decTrapHandlers: DecTrapHandlers?,
                                           decFlags: DecFlags,
                                           decTmps: DecTmps) {
    @JvmField
    internal actual val decFormat: DecFormat = decFormat
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
    internal actual val precision: Int = decFormat.precision

    actual companion object {
        actual fun decimal128Kotlin(): DecContext = decContextDecimal128Kotlin()

        actual fun decimal128IEEE(): DecContext = decContextDecimal128IEEE()

        actual fun decimal128Extended38(): DecContext = decContextDecimal128Extended38()

        private val threadLocal = ThreadLocal.withInitial { decimal128Kotlin() }

        actual fun current(): DecContext = threadLocal.get()
        actual fun setCurrent(newDecContext: DecContext) = threadLocal.set(newDecContext)

    }

    actual fun <T> context(block: DecContext.() -> T): T = block()

    override fun toString(): String =
        "DecContext(decFormat=$decFormat, decRounding=$decRounding, decPrefs=$decPrefs, decTrapHandlers=$decTrapHandlers, decFlags=$decFlags)"
}
