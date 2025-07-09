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

    private fun add(x: Decimal, y: Decimal) = Decimal.newAdd(x, y, this)

    private fun subtract(x: Decimal, y: Decimal) = Decimal.newSub(x, y, this)

    private fun multiply(x: Decimal, y: Decimal) = Decimal.newMul(x, y, this)

    private fun divide(x: Decimal, y: Decimal) = Decimal.newDiv(x, y, this)

    operator fun Decimal.plus(other: Decimal): Decimal = this@DecimalContext.add(this, other)
    operator fun Decimal.minus(other: Decimal): Decimal = this@DecimalContext.subtract(this, other)
    operator fun Decimal.times(other: Decimal): Decimal = this@DecimalContext.multiply(this, other)
    operator fun Decimal.div(other: Decimal): Decimal = this@DecimalContext.divide(this, other)

    private fun mutateAdd(x: Decimal, y: Decimal) = x.mutateAdd(y, this)
    private fun mutateSub(x: Decimal, y: Decimal) = x.mutateSub(y, this)
    private fun mutateMul(x: Decimal, y: Decimal) = x.mutateMul(y, this)
    private fun mutateDiv(x: Decimal, y: Decimal) = x.mutateDiv(y, this)

    operator fun Decimal.plusAssign(other: Decimal) = this@DecimalContext.mutateAdd(this, other)
    operator fun Decimal.minusAssign(other: Decimal) = this@DecimalContext.mutateSub(this, other)
    operator fun Decimal.timesAssign(other: Decimal) = this@DecimalContext.mutateMul(this, other)
    operator fun Decimal.divAssign(other: Decimal) = this@DecimalContext.mutateDiv(this, other)

}
