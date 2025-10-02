package com.decimal128.decimal

import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_NEGATIVE

data class DecEnv(
    val decFormat: DecFormat = DecFormat.DECIMAL_128,
    val decRounding: DecRounding = DecRounding.ROUND_TIES_TO_EVEN,
    val decPrefs: DecPrefs = DecPrefs.DEFAULT,
    val decTraps: DecTraps? = null,
    val decFlags: DecFlags = DecFlags(),
    val decTemps: DecTemps = DecTemps()
) {
    fun with(newDecFormat: DecFormat) =
        DecEnv(newDecFormat, decRounding, decPrefs, decTraps, decFlags, decTemps)

    fun with(newDecRounding: DecRounding) =
        DecEnv(decFormat, newDecRounding, decPrefs, decTraps, decFlags, decTemps)

    fun with(newDecPrefs: DecPrefs) =
        DecEnv(decFormat, decRounding, newDecPrefs, decTraps, decFlags, decTemps)

    fun deepCopy() = DecEnv(decFormat, decRounding, decPrefs, decTraps)

    inline fun <T> compute(block: () -> T ): T = block()

    inline fun <T> computeDelayedTrap(block: () -> T): T {
        val blockEnv = DecEnv(decFormat, decRounding, decPrefs, null, DecFlags(), decTemps)
        val blockVal = blockEnv.compute(block)
        decTraps?.delayedTrap(blockEnv)
        return blockVal
    }

    fun isRoundTowardNegative() = decRounding == ROUND_TOWARD_NEGATIVE

    fun hasTrapHandler(decException: DecException) =
        decTraps?.hasTrapHandler(decException) ?: false

    fun signal(trapContext: DecExceptionContext): Decimal {
        require(decTraps != null)
        return decTraps.signal(trapContext)
    }
}