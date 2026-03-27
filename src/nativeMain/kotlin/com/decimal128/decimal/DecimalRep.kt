package com.decimal128.decimal

// jvmMain
actual open class DecimalRep actual constructor(steal: Int, dw1: Long, dw0: Long) {
        internal actual val steal: Int = steal
        internal actual val dw1: Long = dw1
        internal actual val dw0: Long = dw0
}
