package com.decimal128

import java.math.MathContext
import java.math.RoundingMode

class DecimalContext(val decimalConfig: DecimalConfig) {
    constructor() : this(DecimalConfig.DECIMAL_128_CONFIG)
    constructor(roundingDirection: RoundingDirection) : this(DecimalConfig(roundingDirection))

    companion object {

        fun newDecimal64Context() = DecimalContext(DecimalConfig.DECIMAL_64_CONFIG)
        fun newDecimal128Context() = DecimalContext(DecimalConfig.DECIMAL_128_CONFIG)
        fun newDecimal128ExtendedContext() = DecimalContext(DecimalConfig.DECIMAL_128_EXTENDED_CONFIG)
    }
    val precision = decimalConfig.precision
    val roundingDirection = decimalConfig.roundingDirection
    val eMax = decimalConfig.eMax
    val eMin = decimalConfig.eMin
    val qMax = decimalConfig.qMax
    val qTiny = decimalConfig.qTiny

    fun getMathContext() : MathContext {
        return MathContext(precision, roundingDirection.mapToRoundingMode())
    }

    var invalid = false
        private set
    var divByZero = false
        private set
    var overflow = false
        private set
    var underflow = false
        private set
    var inexact = false
        private set

    var trapInvalid = false
        private set

    fun trapInvalid(t: Boolean) {
        trapInvalid = t
    }

    fun setInexact() { inexact = true }
    fun setInexact(inexact:Boolean) { this.inexact = this.inexact or inexact}
    fun setUnderflow() { underflow = true }
    fun setOverflow() { overflow = true }
    fun setDivByZero() { divByZero = true }
    fun setInvalid() { invalid = true }

    fun operandIsSignalingNaN(decimal: Decimal) {
        if (trapInvalid)
            throw RuntimeException("invalid sNaN seen")
    }

    fun getFptestExceptionsString(): String {
        val bytes = ByteArray(5)
        var ib = 0
        if (inexact)
            bytes[ib++] = 'x'.code.toByte()
        if (underflow)
            bytes[ib++] = 'u'.code.toByte()
        if (overflow)
            bytes[ib++] = 'o'.code.toByte()
        if (divByZero)
            bytes[ib++] = 'z'.code.toByte()
        if (invalid)
            bytes[ib++] = 'i'.code.toByte()
        return String(bytes, 0, ib)
    }
}
