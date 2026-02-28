package com.decimal128.decimal

// jvmMain
actual open class DecimalRep actual constructor(seal: Int, dw1: Long, dw0: Long) {
    @JvmField
    internal actual val seal: Int = seal
    @JvmField
    internal actual val dw1: Long = dw1
    @JvmField
    internal actual val dw0: Long = dw0
}
