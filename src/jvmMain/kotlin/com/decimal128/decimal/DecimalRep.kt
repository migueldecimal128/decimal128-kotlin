package com.decimal128.decimal

// jvmMain
actual open class DecimalRep actual constructor(steal: Int, dw1: Long, dw0: Long) {
    @JvmField
    internal actual val steal: Int = steal
    @JvmField
    internal actual val dw1: Long = dw1
    @JvmField
    internal actual val dw0: Long = dw0
}
