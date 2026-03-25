// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.DecException.*
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE

expect open class DecContextRep(decFormat: DecFormat,
                                decRounding: DecRounding,
                                decPrefs: DecPrefs,
                                decTrapHandlers: DecTrapHandlers?,
                                decFlags: DecFlags,
                                decTmps: DecTmps) {
    internal val decFormat: DecFormat
    internal val decRounding: DecRounding
    internal val decPrefs: DecPrefs
    internal val decTrapHandlers: DecTrapHandlers?
    internal val decFlags: DecFlags
    internal val tmps: DecTmps

    internal val precision: Int
}

class DecContext internal constructor(
    decFormat: DecFormat = DecFormat.DECIMAL_128,
    decRounding: DecRounding = DecRounding.ROUND_TIES_TO_EVEN,
    decPrefs: DecPrefs = DecPrefs.KOTLIN_DEFAULT,
    decTrapHandlers: DecTrapHandlers?,
    decFlags: DecFlags,
    decTmps: DecTmps
): DecContextRep(decFormat, decRounding, decPrefs, decTrapHandlers, decFlags, decTmps) {

    companion object {

        fun decimal128Kotlin(): DecContext = DecContext(
            decFormat = DecFormat.DECIMAL_128,
            decRounding = DecRounding.ROUND_TIES_TO_EVEN,
            decPrefs = DecPrefs.KOTLIN_DEFAULT,  // parseMalformedSignalsInvalidOperation = false
            decTrapHandlers = null,
            decFlags = DecFlags(),
            decTmps = DecTmps()
        )

        fun decimal128IEEE(): DecContext = DecContext(
            decFormat = DecFormat.DECIMAL_128,
            decRounding = DecRounding.ROUND_TIES_TO_EVEN,
            decPrefs = DecPrefs.KOTLIN_DEFAULT,  // parseMalformedSignalsInvalidOperation = false
            decTrapHandlers = null,
            decFlags = DecFlags(),
            decTmps = DecTmps()
        )

        fun decimal128Extended(): DecContext = DecContext(
            decFormat = DecFormat.DECIMAL_128_EXTENDED,
            decRounding = DecRounding.ROUND_TIES_TO_EVEN,
            decPrefs = DecPrefs.KOTLIN_DEFAULT,  // parseMalformedSignalsInvalidOperation = false
            decTrapHandlers = null,
            decFlags = DecFlags(),
            decTmps = DecTmps()
        )

        val threadLocal = ThreadLocal.withInitial { decimal128Kotlin() }

        fun current(): DecContext = threadLocal.get()
        fun setCurrent(newDecContext: DecContext) = threadLocal.set(newDecContext)


    }

    fun <T> context(block: DecContext.() -> T): T {
        val result = this.block()
        return result
    }

    override fun toString(): String =
        "DecContext(decFormat=$decFormat, decRounding=$decRounding, decPrefs=$decPrefs, decTrapHandlers=$decTrapHandlers, decFlags=$decFlags)"
}
