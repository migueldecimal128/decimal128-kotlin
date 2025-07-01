package com.decimal128

import java.math.MathContext

class DecimalContext(val decimalFormat: DecimalFormat) {
    constructor() : this(DecimalFormat.DECIMAL_128)
    constructor(decimalFormat: DecimalFormat, roundingDirection:RoundingDirection) :
            this(decimalFormat.withRoundingDirection(roundingDirection))
    constructor(roundingDirection: RoundingDirection) :
            this(DecimalFormat.DECIMAL_128.withRoundingDirection(roundingDirection))

    companion object {

        fun newDecimal64Context() = DecimalContext(DecimalFormat.DECIMAL_64)
        fun newDecimal128Context() = DecimalContext(DecimalFormat.DECIMAL_128)
        fun newDecimal128ExtendedContext() = DecimalContext(DecimalFormat.DECIMAL_128_EXTENDED)
    }
    val precision = decimalFormat.precision
    val roundingDirection = decimalFormat.roundingDirection
    val eMax = decimalFormat.eMax
    val eMin = decimalFormat.eMin
    val qMax = decimalFormat.qMax
    val qTiny = decimalFormat.qTiny

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
