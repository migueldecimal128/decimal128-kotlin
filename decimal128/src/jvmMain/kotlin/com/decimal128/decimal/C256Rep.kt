package com.decimal128.decimal

actual abstract class C256Rep {
    @JvmField
    internal actual var steal: Int = STEAL_TYP_ZER
    @JvmField
    internal actual var dw0:Long = 0L
    @JvmField
    internal actual var dw1:Long = 0L
    @JvmField
    internal actual var dw2:Long = 0L
    @JvmField
    internal actual var dw3:Long = 0L
}