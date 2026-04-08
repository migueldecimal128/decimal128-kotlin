// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

@kotlin.native.concurrent.ThreadLocal
private var ctxCurrent: DecContext = decContextDecimal128Kotlin()

actual class DecContext actual constructor(decFormat: DecFormat,
                                           decRounding: DecRounding,
                                           decPrefs: DecPrefs,
                                           decTrapHandlers: DecTrapHandlers?,
                                           decFlags: DecFlags,
                                           decTmps: DecTmps) {
        internal actual val decFormat: DecFormat = decFormat
    //@JvmField DecRounding is a value class :(
    internal actual val decRounding: DecRounding = decRounding
    internal actual val decPrefs: DecPrefs = decPrefs
    internal actual val decTrapHandlers: DecTrapHandlers? = decTrapHandlers
    internal actual val decFlags: DecFlags = decFlags
    internal actual val tmps: DecTmps = decTmps

    internal actual val precision: Int = decFormat.precision
    internal actual val eMax:Int = Q_MAX + precision - 1
    internal actual val eMin:Int = Q_TINY + 34 - precision


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
