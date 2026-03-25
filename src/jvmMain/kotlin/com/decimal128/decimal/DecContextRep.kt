package com.decimal128.decimal

actual open class DecContextRep actual constructor(decFormat: DecFormat,
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
}