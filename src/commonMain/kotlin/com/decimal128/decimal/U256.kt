@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

const val PRECISION_34 = 34

private const val SIGNBIT = Long.MIN_VALUE

open class U256() {

    @JvmField
    internal val reg = LongArray(4)

    internal var bitLen = 0
        private set
    internal var digitLen = 0
        private set

    constructor(dw0P: Long) : this() {
        reg[0] = dw0P
        bitLen = calcBitLen64(dw0P)
        digitLen = U256Pow10.calcDigitLen64(bitLen, dw0P)
    }
    constructor(dw3P: Long, dw2P: Long, dw1P: Long, dw0P: Long) : this() {
        reg[3] = dw3P
        reg[2] = dw2P
        reg[1] = dw1P
        reg[0] = dw0P
        bitLen = calcBitLen256(dw3P, dw2P, dw1P, dw0P)
        digitLen = U256Pow10.calcDigitLen256(bitLen, dw3P, dw2P, dw1P, dw0P)
    }
    constructor(str: String) : this() {
        Int256ParsePrint.u256FromString(this, false, str)
    }
    constructor(c: U256) : this() {
        c.reg.copyInto(reg)
        bitLen = c.bitLen
        digitLen = c.digitLen
    }

    internal var dw3: Long
        get() = reg[3]
        set(v) { reg[3] = v }
    internal var dw2: Long
        get() = reg[2]
        set(v) { reg[2] = v }
    internal var dw1: Long
        get() = reg[1]
        set(v) { reg[1] = v }
    internal var dw0: Long
        get() = reg[0]
        set(v) { reg[0] = v }

    fun u256SetZero() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 0L;
        updateDigitLenBitLen(0, 0)
    }

    fun u256SetOne() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 1L;
        updateDigitLenBitLen(1, 1)
    }

    internal inline fun u256IsZero() = bitLen == 0

    internal inline fun u256IsNotZero() = bitLen != 0

    internal inline fun u256IsOne() = bitLen == 1

    internal inline fun u256IsLEOne() = bitLen <= 1

    internal inline fun u256IsGTOne() = bitLen > 1

    internal inline fun u256IsMultipleOf5() = U256Bits.u256IsMultipleOf5(this)

    internal inline fun u256IsMultipleOf10(): Boolean {
        if (bitLen < 4 || (dw0 and 1) != 0L)
            return false
        return u256IsMultipleOf5()
    }

    internal inline fun u256IsPowerOf10() = U256Pow10.coeffIsPow10(this)

    internal inline fun u256IsAllNines(nineCount: Int) : Boolean  {
        val pow10BitLen = U256Pow10.pow10BitLen(nineCount)
        if (bitLen != pow10BitLen)
            return false
        val offset = U256Pow10.pow10Offset(nineCount)
        if (dw0 != POW10[offset] - 1)
            return false
        if (nineCount < MIN_POW10_DIGIT_LEN_128 || dw1 != POW10[offset])
            return false
        return true
    }

    fun updateDigitLenBitLen() {
        val bitLen = calcBitLen256(dw3, dw2, dw1, dw0)
        val digitLen = U256Pow10.calcDigitLen256(bitLen, dw3, dw2, dw1, dw0)
        updateDigitLenBitLen(digitLen, bitLen)
    }

    fun updateDigitLenBitLen(digitLen: Int, bitLen: Int) {
        this.digitLen = digitLen
        this.bitLen = bitLen
    }

    //FIXME this case can probably be accelerated because
    // of bitLen delta <= 1 and digitLen delta <= 1
    private fun updateLengthsAfterRoundUp() = updateDigitLenBitLen()

    internal fun u256HasValidLengths(): Boolean {
        if (bitLen != calcBitLen256(dw3, dw2, dw1, dw0))
            return false
        if (digitLen != U256Pow10.calcDigitLen256(bitLen, dw3, dw2, dw1, dw0))
            return false;
        return true
    }

    internal inline fun u256UnscaledCompareTo(other: U256) = U256Compare.u256UnscaledCompare(this, other)

    internal inline fun u256UnscaledEQ(other: U256) = U256Compare.u256UnscaledEQ(this, other)

    internal inline fun u256ScaledCompareTo(other: U256, scaleDelta: Int)  = U256Compare.u256ScaledCompare(this, other, scaleDelta)

    internal inline fun u256ScaledEQ(other: U256, scaleDelta: Int) = U256Compare.u256ScaledEQ(this, other, scaleDelta)

    internal inline fun u256SetPow2(pow2: Int) = U256Bits.u256SetPow2(this, pow2)

    internal inline fun u256SetPow10(pow10: Int) = U256Pow10.coeffSetPow10(this, pow10)

    internal inline fun u256SetAdd(x: U256, scaleDelta: Int, y: U256) = U256Add.u256Add(this, x, scaleDelta, y)

    internal inline fun u256SetAdd(x: U256, y: U256) = U256Add.u256AddUnscaled(this, x, y)

    internal inline fun u256SetSub(x: U256, y: U256) = U256Sub.u256SubUnscaled(this, x, y)

    internal inline fun u256SetMul(x: U256, y: U256) = U256Mul.u256Mul(this, x, y)

    internal inline fun u256SetSqr(x: U256) = U256Sqr.u256Sqr(this, x)

    internal inline fun u256SetFma(x: U256, y: U256, a: U256) = U256Fma.u256Fma(this, x, y, a)

    internal inline fun u256SetFmaPow10(x: U256, pow10: Int, a: U256) = U256Fma.u256FmaPow10(this, x, pow10, a)

    internal inline fun u256SetFmaPow10(x: U256, pow10: Int, a0: Long) = U256Fma.u256FmaPow10(this, x, pow10, a0)

    internal inline fun u256SetFmaPow10(x: U256, pow10: Int, a1: Long, a0: Long) = U256Fma.u256FmaPow10(this, x, pow10, a1, a0)

    internal inline fun u256MutateFmaPow10(pow10: Int, a: Long) = U256Fma.u256FmaPow10(this, this, pow10, a)

    internal inline fun u256SetFms(x: U256, y: U256, subtrahend: U256) = U256Fms.u256Fms(this, x, y, subtrahend)

    internal inline fun u256SetDiv(x: U256, y: U256) = U256Div.u256Div(this, x, y)

    internal inline fun u256SetDivX64(x: U256, y0: Long) = U256Div.u256DivX64(this, x, y0)

    internal inline fun u256SetDivModX64(x: U256, y0: Long) = U256Div.u256DivModX64(this, x, y0)

    internal inline fun u256SetMod(x: U256, y: U256) = U256Div.u256Mod(this, x, y)

    internal inline fun u256SetDivMod(rem: U256, x: U256, y: U256) = U256Div.u256DivMod(this, rem, x, y)

    internal inline fun u256SetScaleUpPow10(x: U256, pow10: Int) = U256ScalePow10.u256ScaleUpPow10(this, x, pow10)

    internal inline fun u256SetScaleDownPow10(x: U256, pow10: Int) = U256ScalePow10.u256ScaleDownPow10(this, x, pow10)

    internal operator fun get(index: Int): Long = reg[index]

    internal operator fun set(index: Int, value: Long) {
        check(bitLen == -1)
        when (index) {
            0 -> dw0 = value
            1 -> dw1 = value
            2 -> dw2 = value
            3 -> dw3 = value
            else -> throw RuntimeException("indexOutOfBounds")
        }
    }

    internal inline fun u256EnableIndexSetAndZeroOut() {
        bitLen = -1
        dw0 = 0L; dw1 = 0L; dw2 = 0L; dw3 = 0L
    }

    internal inline fun u256DisableIndexSetAndUpdateLengths() {
        check(bitLen == -1)
        updateDigitLenBitLen()
    }

    internal inline fun u256Set64(d0: Long) = U256Set.u256Set64(this, d0)

    internal inline fun u256Set128(d1: Long, d0: Long) = U256Set.u256Set128(this, d1, d0)

    internal inline fun u256Set192(d2: Long, d1: Long, d0: Long) = U256Set.u256Set192(this, d2, d1, d0)

    internal inline fun u256Set256(d3: Long, d2: Long, d1: Long, d0: Long)  = U256Set.u256Set256(this, d3, d2, d1, d0)

    fun u256Set(x: U256) {
        dw3 = x.dw3; dw2 = x.dw2; dw1 = x.dw1; dw0 = x.dw0
        bitLen = x.bitLen
        digitLen = x.digitLen
    }


    internal inline fun u256Set(str: String) = Int256ParsePrint.u256FromString(this, false, str)

    internal inline fun u256SetShiftRight(x: U256, bitShift: Int) = U256Set.u256SetShiftRight(this, x, bitShift)

    internal inline fun u256SetShiftLeft(x: U256, bitShift: Int) = U256Set.u256SetShiftLeftOr(this, x, bitShift, 0L)

    internal inline fun u256MutateShiftLeft(bitShift: Int) = U256Set.u256SetShiftLeftOr(this, this, bitShift, 0L)

    internal inline fun u256MutateShiftLeftOr(bitShift: Int, d0: Long) = U256Set.u256SetShiftLeftOr(this, this, bitShift, d0)

    internal inline fun u256Set(x: LongArray, xOff: Int, xLen: Int) = U256Set.u256Set(this, x, xOff, xLen)

    internal inline fun u256Set(x: IntArray, xLen: Int) = U256Set.u256Set(this, x, xLen)

    internal inline fun u256SetShiftRight(x: LongArray, xOff: Int, xLen: Int, bitCount: Int) =
        U256Set.u256SetShiftRight(this, x, xOff, xLen, bitCount)

    internal inline fun getDwordAtBitIndex(bitIndex: Int): Long = U256Bits.getDwordAtBitIndex(this, bitIndex)

    internal inline fun u256ToFloorDouble() = U256Bits.u256ToFloorDouble(this)

    internal inline fun u256Set(d: Double) = U256Bits.u256Set(this, d)

    internal inline fun u256ToNewDoubleDouble() = U256Bits.u256ToNewDoubleDouble(this)

    internal inline fun u256Set(dd: DoubleDouble) = U256Bits.u256Set(this, dd)

    internal inline fun u256MutateIncrement(doRoundUp: Boolean) {
        if (doRoundUp)
            U256Add.u256MutateIncrement(this)
    }

    internal inline fun u256MutateIncrement() = U256Add.u256MutateIncrement(this)

    internal inline fun u256MutateDecrement() = U256Add.u256MutateDecrement(this)

    internal inline fun u256Ntz() = U256Bits.ntz(this)

    internal inline fun u256DwordAtBitIndex(bitIndex: Int) = U256Bits.getDwordAtBitIndex(this, bitIndex)

    open fun toHexString() = Int256ParsePrint.int256ToHexString(false, this)
    //override fun toString() = coeffToBigInteger().toString()
    override fun toString() = Int256ParsePrint.int256ToString(false, this)
    fun u256ToUtf8(bytes: ByteArray, off: Int) = Int256ParsePrint.int256ToUtf8(false, this, bytes, off)

    override fun equals(other: Any?) = other is U256 && U256Compare.u256UnscaledEQ(this, other)

}