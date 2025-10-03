package com.decimal128.decimal

import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_EVEN

class DecimalContext(val decFormat: DecFormat, val roundingDirection: DecRounding) {
    constructor() : this(DecFormat.DECIMAL_128, ROUND_TIES_TO_EVEN)
    constructor(decFormat: DecFormat) :
            this(decFormat, ROUND_TIES_TO_EVEN)
    constructor(decRounding: DecRounding) :
            this(DecFormat.DECIMAL_128, decRounding)

    companion object {

        fun newDecimal64Context() = DecimalContext(DecFormat.DECIMAL_64)
        fun newDecimal128Context() = DecimalContext(DecFormat.DECIMAL_128)
        fun newDecimal128ExtendedContext() = DecimalContext(DecFormat.DECIMAL_128_EXTENDED)
    }
    val precision = decFormat.precision
    val eMax = decFormat.eMax
    val eMin = decFormat.eMin
    val qMax = decFormat.qMax
    val qTiny = decFormat.qTiny
    val isRoundTowardNegative = roundingDirection == DecRounding.ROUND_TOWARD_NEGATIVE

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

    //
    fun signalInexact(z: MutDec): MutDec { inexact = true; return z }
    fun signalUnderflow(z: MutDec): MutDec { underflow = true; return z }
    fun signalInexactUnderflow(z: MutDec): MutDec { inexact = true; underflow = true; return z }
    fun signalInexactOverflow(z: MutDec): MutDec { inexact = true; overflow = true; return z}
    fun signalDivByZero(z: MutDec): MutDec { divByZero = true; return z }
    fun signalInvalid(z: MutDec): MutDec { invalid = true; return z }

    fun operandIsSignalingNaN(mutDec: MutDec) {
        if (trapInvalid)
            throw RuntimeException("invalid sNaN seen")
    }

    // x == inexact
    // u == underflow
    // o == overflow
    // z == divByZero
    // i == invalid
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

    private fun add(x: MutDec, y: MutDec) = MutDec.newAdd(x, y, this)

    private fun subtract(x: MutDec, y: MutDec) = MutDec.newSub(x, y, this)

    private fun multiply(x: MutDec, y: MutDec) = MutDec.newMul(x, y, this)

    private fun divide(x: MutDec, y: MutDec) = MutDec.newDiv(x, y, this)

    operator fun MutDec.plus(other: MutDec): MutDec = this@DecimalContext.add(this, other)
    operator fun MutDec.minus(other: MutDec): MutDec = this@DecimalContext.subtract(this, other)
    operator fun MutDec.times(other: MutDec): MutDec = this@DecimalContext.multiply(this, other)
    operator fun MutDec.div(other: MutDec): MutDec = this@DecimalContext.divide(this, other)

    private fun mutateAdd(x: MutDec, y: MutDec) = x.mutateAdd(y, this)
    private fun mutateSub(x: MutDec, y: MutDec) = x.mutateSub(y, this)
    private fun mutateMul(x: MutDec, y: MutDec) = x.mutateMul(y, this)
    private fun mutateDiv(x: MutDec, y: MutDec) = x.mutateDiv(y, this)

    operator fun MutDec.plusAssign(other: MutDec) = this@DecimalContext.mutateAdd(this, other)
    operator fun MutDec.minusAssign(other: MutDec) = this@DecimalContext.mutateSub(this, other)
    operator fun MutDec.timesAssign(other: MutDec) = this@DecimalContext.mutateMul(this, other)
    operator fun MutDec.divAssign(other: MutDec) = this@DecimalContext.mutateDiv(this, other)

}
