@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

const val PRECISION_34 = 34

private const val SIGNBIT = Long.MIN_VALUE

open class C256(dw3: Long, dw2: Long, dw1: Long, dw0: Long) {

    //constructor(dw2: Long, dw1: Long, dw0: Long) : this(0L, dw2, dw1, dw0)
    //constructor(dw1: Long, dw0: Long) : this(0L, 0L, dw1, dw0)
    constructor(dw0: Long) : this(0L, 0L, 0L, dw0)
    //constructor(w0: Int) : this(0L, 0L, 0L, w0.toLong() and 0xFFFFFFFFL)
    constructor() : this(0L, 0L, 0L, 0L)
    constructor(str: String) : this() {
        IntegerParsePrint.u256FromString(this, false, str)
    }
    constructor(c: C256) : this(c.dw3, c.dw2, c.dw1, c.dw0)

    @JvmField
    internal var dw3: Long
    @JvmField
    internal var dw2: Long
    @JvmField
    internal var dw1: Long
    @JvmField
    internal var dw0: Long
    internal var bitLen: Int
    @JvmField
    internal var digitLen: Int

    //internal var packedLengths: Short
    //internal val bitLen: Int
    //    get() = packedLengths.toInt() and 0x1FF
    //internal val digitLen: Int
    //    get() = (packedLengths.toInt() shr 9) and 0x07F
    init {
        this.dw3 = dw3
        this.dw2 = dw2
        this.dw1 = dw1
        this.dw0 = dw0
        this.bitLen = calcBitLen256(dw3, dw2, dw1, dw0)
        this.digitLen = calcDigitLen256(bitLen, dw3, dw2, dw1, dw0)
        //this.packedLengths = packLengths(digitLen, bitLen)
    }

    fun c256SetZero() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 0L;
        updateDigitLenBitLen(0, 0)
    }

    fun c256SetOne() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 1L;
        updateDigitLenBitLen(1, 1)
    }

    internal inline fun c256IsZero() = bitLen == 0

    internal inline fun c256IsMultipleOf5() = c256IsMultipleOf5(this)

    internal inline fun c256IsMultipleOf10(): Boolean {
        if (bitLen < 4 || (dw0 and 1) != 0L)
            return false
        return c256IsMultipleOf5()
    }

    internal inline fun c256IsPowerOf10() = coeffIsPow10(this)

    internal inline fun c256IsAllNines(nineCount: Int) : Boolean  {
        val pow10BitLen = pow10BitLen(nineCount)
        if (bitLen != pow10BitLen)
            return false
        val offset = pow10Offset(nineCount)
        if (dw0 != POW10[offset] - 1)
            return false
        if (nineCount < MIN_POW10_DIGIT_LEN_128 || dw1 != POW10[offset])
            return false
        return true
    }

    fun updateDigitLenBitLen() {
        val bitLen = calcBitLen256(dw3, dw2, dw1, dw0)
        val digitLen = calcDigitLen256(bitLen, dw3, dw2, dw1, dw0)
        updateDigitLenBitLen(digitLen, bitLen)
    }

    fun updateDigitLenBitLen(digitLen: Int, bitLen: Int) {
        //this.packedLengths = packLengths(digitLen, bitLen)
        this.digitLen = digitLen
        this.bitLen = bitLen
    }

    //FIXME this case can probably be accelerated because
    // of bitLen delta <= 1 and digitLen delta <= 1
    private fun updateLengthsAfterRoundUp() = updateDigitLenBitLen()

    internal fun c256HasValidLengths(): Boolean {
        if (bitLen != calcBitLen256(dw3, dw2, dw1, dw0))
            return false
        if (digitLen != calcDigitLen256(bitLen, dw3, dw2, dw1, dw0))
            return false;
        return true
    }

    internal inline fun c256UnscaledCompareTo(other: C256) = c256UnscaledCompare(this, other)

    internal inline fun c256UnscaledEQ(other: C256) = u256UnscaledEQ(this, other)

    internal inline fun c256ScaledCompareTo(other: C256, scaleDelta: Int)  = c256ScaledCompare(this, other, scaleDelta)

    internal inline fun c256ScaledEQ(other: C256, scaleDelta: Int) = c256ScaledEQ(this, other, scaleDelta)

    internal inline fun c256SetPow2(pow2: Int) = c256SetPow2(this, pow2)

    internal inline fun c256SetPow10(pow10: Int) = coeffSetPow10(this, pow10)

    internal inline fun c256SetAdd(x: C256, scaleDelta: Int, y: C256) = c256SetAdd(this, x, scaleDelta, y)

    internal inline fun c256SetAdd(x: C256, y: C256) = c256SetAddUnscaled(this, x, y)

    internal inline fun c256SetSub(x: C256, y: C256) = c256SetSubUnscaled(this, x, y)

    internal inline fun c256SetMul(x: C256, y: C256) = c256SetMul(this, x, y)

    internal inline fun c256SetSqr(x: C256) = c256SetSqr(this, x)

    internal inline fun c256SetFma(x: C256, y: C256, a: C256) = c256SetFma(this, x, y, a)

    internal inline fun c256SetFmaPow10(x: C256, pow10: Int, a: C256) = c256SetFmaPow10(this, x, pow10, a)

    internal inline fun c256SetFmaPow10(x: C256, pow10: Int, a0: Long) = c256SetFmaPow10(this, x, pow10, a0)

    internal inline fun c256SetFmaPow10(x: C256, pow10: Int, a1: Long, a0: Long) = c256SetFmaPow10(this, x, pow10, a1, a0)

    internal inline fun u256MutateFmaPow10(pow10: Int, a: Long) = c256SetFmaPow10(this, this, pow10, a)

    internal inline fun c256SetFms(x: C256, y: C256, subtrahend: C256) = c256SetFms(this, x, y, subtrahend)

    internal inline fun c256SetDiv(x: C256, y: C256) = c256SetDiv(this, x, y)

    internal inline fun c256SetDivX64(x: C256, y0: Long) = c256SetDivX64(this, x, y0)

    internal inline fun c256SetDivModX64(x: C256, y0: Long) = c256SetDivRemX64(this, x, y0)

    internal inline fun c256SetRem(x: C256, y: C256) = c256SetRem(this, x, y)

    internal inline fun c256SetDivRem(rem: C256, x: C256, y: C256) = c256SetDivRem(this, rem, x, y)

    internal inline fun c256SetScaleUpPow10(x: C256, pow10: Int) = c256SetScaleUpPow10(this, x, pow10)

    internal inline fun c256SetScaleDownPow10(x: C256, pow10: Int) = c256SetScaleDownPow10(this, x, pow10)

    internal inline operator fun get(index: Int): Long {
        return when (index) {
            0 -> dw0
            1 -> dw1
            2 -> dw2
            3 -> dw3
            else -> throw RuntimeException("indexOutOfBounds")
        }
    }

    internal inline operator fun set(index: Int, value: Long) {
        //check(packedLengths.toInt() == -1)
        verify { bitLen == -1 }
        when (index) {
            0 -> dw0 = value
            1 -> dw1 = value
            2 -> dw2 = value
            3 -> dw3 = value
            else -> throw RuntimeException("indexOutOfBounds")
        }
    }

    internal inline fun c256EnableIndexSetAndZeroOut() {
        //packedLengths = -1
        bitLen = -1
        dw0 = 0L; dw1 = 0L; dw2 = 0L; dw3 = 0L
    }

    internal inline fun c256DisableIndexSetAndUpdateLengths() {
        //check(packedLengths.toInt() == -1)
        verify { bitLen == -1 }
        updateDigitLenBitLen()
    }

    internal inline fun c256Set64(d0: Long) {
        dw3 = 0L; dw2 = 0L; dw1 = 0L
        dw0 = d0
        bitLen = calcBitLen64(d0)
        digitLen = calcDigitLen64(bitLen, d0)
    }

    internal inline fun c256Set128(d1: Long, d0: Long) {
        dw3 = 0L; dw2 = 0L
        dw1 = d1; dw0 = d0
        bitLen = calcBitLen128(d1, d0)
        digitLen = calcDigitLen128(bitLen, d1, d0)
    }

    internal inline fun c256Set192(d2: Long, d1: Long, d0: Long) {
        dw3 = 0L
        dw2 = d2; dw1 = d1; dw0 = d0
        bitLen = calcBitLen192(d2, d1, d0)
        digitLen = calcDigitLen192(bitLen, d2, d1, d0)
    }

    internal inline fun c256Set256(d3: Long, d2: Long, d1: Long, d0: Long) {
        dw3 = d3; dw2 = d2; dw1 = d1; dw0 = d0
        bitLen = calcBitLen256(d3, d2, d1, d0)
        digitLen = calcDigitLen256(bitLen, d3, d2, d1, d0)
    }

    fun c256Set(x: C256): C256 {
        dw3 = x.dw3; dw2 = x.dw2; dw1 = x.dw1; dw0 = x.dw0
        //packedLengths = x.packedLengths
        bitLen = x.bitLen
        digitLen = x.digitLen
        return this
    }


    internal inline fun c256Set(str: String) = IntegerParsePrint.u256FromString(this, false, str)

    internal inline fun c256SetShiftRight(x: C256, bitShift: Int) = c256SetShiftRight(this, x, bitShift)

    internal inline fun c256SetShiftLeft(x: C256, bitShift: Int) = c256SetShiftLeftOr(this, x, bitShift, 0L)

    internal inline fun c256MutateShiftLeft(bitShift: Int) = c256SetShiftLeftOr(this, this, bitShift, 0L)

    internal inline fun c256MutateShiftLeftOr(bitShift: Int, d0: Long) = c256SetShiftLeftOr(this, this, bitShift, d0)

    internal inline fun c256Set(x: LongArray, xOff: Int, xLen: Int) = c256Set(this, x, xOff, xLen)

    internal inline fun c256Set(x: IntArray, xLen: Int) = c256Set(this, x, xLen)

    internal inline fun c256SetShiftRight(x: LongArray, xOff: Int, xLen: Int, bitCount: Int) =
        c256SetShiftRight(this, x, xOff, xLen, bitCount)

    internal inline fun getDwordAtBitIndex(bitIndex: Int): Long = getDwordAtBitIndex(this, bitIndex)

    internal inline fun c256ToFloorDouble() = c256ToFloorDouble(this)

    internal inline fun c256Set(d: Double) = c256Set(this, d)

    internal inline fun c256ToNewDoubleDouble() = c256ToNewDoubleDouble(this)

    internal inline fun c256Set(dd: DoubleDouble) = c256Set(this, dd)

    internal inline fun c256MutateIncrement(doRoundUp: Boolean) {
        if (doRoundUp)
            c256MutateIncrement(this)
    }

    internal inline fun c256MutateIncrement() = c256MutateIncrement(this)

    internal inline fun c256MutateDecrement() = c256MutateDecrement(this)

    internal inline fun c256NumberTrailingZeros() = c256Ctz(this)

    internal inline fun c256DwordAtBitIndex(bitIndex: Int) = getDwordAtBitIndex(this, bitIndex)

    open fun toHexString() = IntegerParsePrint.int256ToHexString(false, this)
    //override fun toString() = coeffToBigInteger().toString()
    override fun toString() =
        if (bitLen == -1)
            "MutDec \uD83D\uDEA7 under construction \uD83D\uDEA7"
        else
            IntegerParsePrint.int256ToString(false, this)
    fun u256ToUtf8(bytes: ByteArray, off: Int) = IntegerParsePrint.int256ToUtf8(false, this, bytes, off)

    override fun equals(other: Any?) = other is C256 && u256UnscaledEQ(this, other)

}