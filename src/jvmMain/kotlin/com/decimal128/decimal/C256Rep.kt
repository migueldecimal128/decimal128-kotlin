package com.decimal128.decimal

actual open class C256Rep actual constructor(dw3: Long,
                                             dw2: Long,
                                             dw1: Long,
                                             dw0: Long) {
    @JvmField
    internal actual var dw3:Long = dw3
    @JvmField
    internal actual var dw2:Long = dw2
    @JvmField
    internal actual var dw1:Long = dw1
    @JvmField
    internal actual var dw0:Long = dw0
    @JvmField
    internal actual var bitLen: Int = calcBitLen256(dw3, dw2, dw1, dw0)
    @JvmField
    internal actual var digitLen: Int = calcDigitLen256(bitLen, dw3, dw2, dw1, dw0)
}