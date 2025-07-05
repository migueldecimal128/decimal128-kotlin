package com.decimal128

import com.decimal128.U256Mul.u256Mul
import com.decimal128.U256Fma.u256Fma
import com.decimal128.U256Add.u256AddUnscaled
import com.decimal128.U256Add.u256Add
import com.decimal128.U256Compare.u256ScaledCompare
import com.decimal128.U256Compare.u256ScaledEQ
import com.decimal128.U256Set.u256SetShiftRight
import com.decimal128.U256Compare.u256UnscaledCompare
import com.decimal128.U256Compare.u256UnscaledEQ
import com.decimal128.U256Divide.u256Div
import com.decimal128.U256Divide.u256Divx64
import com.decimal128.U256Divide.u256Mod
import com.decimal128.U256Fma.u256FmaPow10
import com.decimal128.U256Pow10.calcDigitLen256
import com.decimal128.U256Pow10.pow10Offset
import com.decimal128.U256ScalePow10.u256ScaleDownPow10
import com.decimal128.U256ScalePow10.u256ScaleUpPow10
import com.decimal128.U256Set.u256Set
import com.decimal128.U256Sub.u256SubUnscaled
import java.lang.Long.numberOfLeadingZeros
import kotlin.math.max
import kotlin.math.min

const val PRECISION_34 = 34

private const val SIGNBIT = Long.MIN_VALUE


open class U256(d3: Long, d2: Long, d1: Long, d0: Long) {

    constructor(dw2: Long, dw1: Long, dw0: Long) : this(0L, dw2, dw1, dw0)
    constructor(dw1: Long, dw0: Long) : this(0L, 0L, dw1, dw0)
    constructor(dw0: Long) : this(0L, 0L, 0L, dw0)
    constructor(w0: Int) : this(0L, 0L, 0L, w0.toLong() and 0xFFFFFFFFL)
    constructor() : this(0L, 0L, 0L, 0L)
    constructor(str: String) : this() {
        U256ParsePrint.u256FromString(this, str)
    }
    constructor(c: U256) : this(c.dw3, c.dw2, c.dw1, c.dw0)

    var dw3 = d3
        private set
    var dw2 = d2
        private set
    var dw1 = d1
        private set
    var dw0 = d0
        private set
    var bitLen = calcBitLen256(d3, d2, d1, d0)
        private set
    var digitLen = calcDigitLen256(bitLen, d3, d2, d1, d0)
        private set
    var sign = false

    fun u256SetZero() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 0L; bitLen = 0; digitLen = 0
    }

    fun u256IsZero() = bitLen == 0

    fun u256IsNotZero() = bitLen > 0

    fun u256SetOne() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 1L; bitLen = 1; digitLen = 1
    }

    fun u256SetZeroOrOneMasked(d0: Long) {
        val asInt = (d0 and 1).toInt()
        dw3 = 0; dw2 = 0; dw1 = 0; dw0 = d0 and 1; bitLen = asInt; digitLen = asInt
    }

    fun u256IsOne() = bitLen == 1

    fun u256IsLEOne() = bitLen <= 1

    fun u256IsGTOne() = bitLen > 1

    fun u256IsMultipleOf5() = U256Bits.u256IsMultipleOf5(this)

    fun u256IsMultipleOf10(): Boolean {
        if (bitLen < 4 || (dw0 and 1) != 0L)
            return false
        return u256IsMultipleOf5()
    }

    fun u256IsPowerOf10() = U256Pow10.coeffIsPow10(this)

    fun u256IsAllNines(nineCount: Int) : Boolean  {
        val pow10BitLen = U256Pow10.pow10BitLen(nineCount)
        if (bitLen != pow10BitLen)
            return false
        val offset = pow10Offset(nineCount)
        if (dw0 != POW10[offset] - 1)
            return false
        if (nineCount < MIN_POW10_DIGIT_LEN_128 || dw1 != POW10[offset])
            return false
        return true
    }

    private fun calcBitLen(): Int {
        return calcBitLen256(dw3, dw2, dw1, dw0)
    }

    private fun calcDigitLen(): Int {
        return when {
            (bitLen <= 64) -> U256Pow10.calcDigitLen64(bitLen, dw0)
            (bitLen <= 128) -> U256Pow10.calcDigitLen128(bitLen, dw1, dw0)
            (bitLen <= 192) -> U256Pow10.calcDigitLen192(bitLen, dw2, dw1, dw0)
            else -> calcDigitLen256(bitLen, dw3, dw2, dw1, dw0)
        }
    }

    private fun updateLengths() {
        bitLen = calcBitLen256(dw3, dw2, dw1, dw0)
        digitLen = calcDigitLen256(bitLen, dw3, dw2, dw1, dw0)
    }

    //FIXME this case can probably be accelerated because
    // of bitLen delta <= 1 and digitLen delta <= 1
    private fun updateLengthsAfterRoundUp() = updateLengths()

    fun u256HasValidLengths(): Boolean {
        if (digitLen != calcDigitLen256(bitLen, dw3, dw2, dw1, dw0))
            return false;
        return true
    }

    fun u256UnscaledCompareTo(other: U256) = u256UnscaledCompare(this, other)

    fun u256UnscaledEQ(other: U256) = u256UnscaledEQ(this, other)

    fun u256ScaledCompareTo(other: U256, scaleDelta: Int)  = u256ScaledCompare(this, other, scaleDelta)

    fun u256ScaledEQ(other: U256, scaleDelta: Int) = u256ScaledEQ(this, other, scaleDelta)

    fun u256SetPow2(pow2: Int) {
        if (pow2 !in 0..255)
            throw IllegalArgumentException()
        val shifted = 1L shl pow2
        val i = pow2 ushr 6
        val j = 1L shl (60 + i)
        dw0 = shifted and ((j shl 3) shr 63)
        dw1 = shifted and ((j shl 2) shr 63)
        dw2 = shifted and ((j shl 1) shr 63)
        dw3 = shifted and ((j      ) shr 63)
        bitLen = pow2 + 1
        digitLen = ((pow2 * 1233) ushr 12) + 1
    }

    fun u256SetPow10(pow10: Int) = U256Pow10.coeffSetPow10(this, pow10)

    fun u256SetAdd(x: U256, scaleDelta: Int, y: U256) = u256Add(this, x, scaleDelta, y)

    fun u256SetAdd(x: U256, y: U256) = u256AddUnscaled(this, x, y)

    fun u256SetSub(x: U256, y: U256) = u256SubUnscaled(this, x, y)

    fun u256SetMul(x: U256, y: U256) = u256Mul(this, x, y)

    fun u256SetSqr(x: U256) = U256Sqr.u256Sqr(this, x)

    fun u256SetFma(x: U256, y: U256, a: U256) = u256Fma(this, x, y, a)

    fun u256SetFmaPow10(x: U256, pow10: Int, a: U256) = u256FmaPow10(this, x, pow10, a)

    fun u256SetFmaPow10(x: U256, pow10: Int, a0: Long) = u256FmaPow10(this, x, pow10, a0)

    fun u256SetFmaPow10(x: U256, pow10: Int, a1: Long, a0: Long) = u256FmaPow10(this, x, pow10, a1, a0)

    fun u256MutateFmaPow10(pow10: Int, a: Long) = u256FmaPow10(this, this, pow10, a)

    fun u256SetFms(x: U256, y: U256, subtrahend: U256) = U256Fms.u256Fms(this, x, y, subtrahend)

    fun u256SetDiv(x: U256, y: U256) = u256Div(this, x, y)

    fun u256SetDivx64(x: U256, y0: Long) = u256Divx64(this, x, y0)

    fun u256SetMod(x: U256, y: U256) = u256Mod(this, x, y)

    fun u256SetScaleUpPow10(x: U256, pow10: Int) = u256ScaleUpPow10(this, x, pow10)

    fun u256SetScaleDownPow10(x: U256, pow10: Int) = u256ScaleDownPow10(this, x, pow10)

    operator fun get(index: Int): Long {
        return when (index) {
            0 -> dw0
            1 -> dw1
            2 -> dw2
            3 -> dw3
            else -> throw RuntimeException("indexOutOfBounds")
        }
    }

    operator fun set(index: Int, value: Long) {
        assert(digitLen == -1)
        when (index) {
            0 -> dw0 = value
            1 -> dw1 = value
            2 -> dw2 = value
            3 -> dw3 = value
            else -> throw RuntimeException("indexOutOfBounds")
        }
    }

    fun u256EnableIndexSetAndZeroOut() {
        digitLen = -1
        dw0 = 0L; dw1 = 0L; dw2 = 0L; dw3 = 0L
    }

    fun u256DisableIndexSetAndUpdateLengths() {
        assert(digitLen == -1)
        updateLengths()
    }

    fun u256Set64(d0: Long) {
        dw3 = 0L; dw2 = 0L; dw1 = 0L
        dw0 = d0
        bitLen = calcBitLen64(d0)
        digitLen = U256Pow10.calcDigitLen64(bitLen, d0)
    }

    fun u256Set128(d1: Long, d0: Long) {
        dw3 = 0L; dw2 = 0L
        dw1 = d1; dw0 = d0
        bitLen = calcBitLen128(d1, d0)
        digitLen = U256Pow10.calcDigitLen128(bitLen, d1, d0)
    }

    fun u256Set192(d2: Long, d1: Long, d0: Long) {
        dw3 = 0L
        dw2 = d2; dw1 = d1; dw0 = d0
        bitLen = calcBitLen192(d2, d1, d0)
        digitLen = U256Pow10.calcDigitLen192(bitLen, d2, d1, d0)
    }


    fun u256Set256(d3: Long, d2: Long, d1: Long, d0: Long){
        dw3 = d3; dw2 = d2; dw1 = d1; dw0 = d0
        bitLen = calcBitLen256(d3, d2, d1, d0)
        digitLen = calcDigitLen256(bitLen, d3, d2, d1, d0)
    }

    fun u256Set(x: U256) {
            bitLen = x.bitLen; digitLen = x.digitLen; dw3 = x.dw3; dw2 = x.dw2; dw1 = x.dw1; dw0 = x.dw0
    }

    fun u256Set(str: String) = U256ParsePrint.u256FromString(this, str)

    fun u256SetShiftRight(x: U256, bitShift: Int) = U256Set.u256SetShiftRight(this, x, bitShift)

    fun u256SetShiftLeft(x: U256, bitShift: Int) = U256Set.u256SetShiftLeftOr(this, x, bitShift, 0L)

    fun u256MutateShiftLeft(bitShift: Int) = U256Set.u256SetShiftLeftOr(this, this, bitShift, 0L)

    fun u256MutateShiftLeftOr(bitShift: Int, d0: Long) = U256Set.u256SetShiftLeftOr(this, this, bitShift, d0)

    fun u256Set(x: LongArray, xOff: Int, xLen: Int) = u256Set(this, x, xOff, xLen)

    fun u256SetShiftRight(x: LongArray, xOff: Int, xLen: Int, bitCount: Int) =
        u256SetShiftRight(this, x, xOff, xLen, bitCount)

    fun getDwordAtBitIndex(bitIndex: Int): Long =
        U256Bits.getDwordAtBitIndex(this, bitIndex)

    open fun u256ToFloorDouble(): Double {
        val hiBitLen = min(53, bitLen)
        val hiBitIndex = bitLen - hiBitLen
        val hiBits = getDwordAtBitIndex(hiBitIndex)
        val dHiBits = Math.scalb(hiBits.toDouble(), hiBitIndex)
        return dHiBits
    }

    fun u256Set(d: Double) {
        val dRaw = d.toRawBits()
        val exp = ((dRaw ushr 52).toInt() and 0x7FF) - 1023
        if (exp <= 63) {
            u256Set64(Math.abs(d).toLong())
            return
        }
        if (exp > 255) {
            throw RuntimeException("coefficient overflow")
        }
        val significand = ((dRaw and ((1L shl 52) - 1)) or (1L shl 52))
        u256Set64(significand)
        u256SetShiftLeft(this, exp - 52)
    }

    open fun u256ToNewDoubleDouble(): DoubleDouble {
        val hiBitsLen = min(53, bitLen)
        val hiBitsIndex = bitLen - hiBitsLen
        val hiBits = getDwordAtBitIndex(hiBitsIndex)
        val dHiBits = Math.scalb(hiBits.toDouble(), hiBitsIndex)
        if (hiBitsIndex == 0)
            return DoubleDouble(dHiBits, 0.0)
        var loBits64Index: Int = max(0, hiBitsIndex - 64)
        var loBitsMask = -1L ushr max(0, 64 - hiBitsIndex)
        var loBits: Long
        var nlz: Int
        while (true) {
            loBits = getDwordAtBitIndex(loBits64Index) and loBitsMask
            nlz = numberOfLeadingZeros(loBits)
            if (loBits64Index == 0 || nlz <= 11)
                break
            loBits64Index = max(loBits64Index - nlz, 0)
            loBitsMask = -1
        }
        val extraBits = max(0, 11 - nlz)
        loBits = loBits ushr extraBits
        val loBits53Index = loBits64Index + extraBits
        val dLoBits = Math.scalb(loBits.toDouble(), loBits53Index)
        return DoubleDouble(dHiBits, dLoBits)
    }

    fun u256Set(dd: DoubleDouble) {
        u256Set(dd.hi)
        if (dd.lo == 0.0)
            return
        val coeffLo = U256()
        coeffLo.u256Set(dd.lo)
        if (dd.lo > 0)
            u256SetAdd(this, coeffLo)
        else
            u256SetSub(this, coeffLo)
    }

    fun u256MutateIncrement(doRoundUp: Boolean) {
        if (doRoundUp)
            u256MutateIncrement()
    }

    fun u256MutateIncrement() {
        ++dw0
        if (dw0 == 0L) {
            ++dw1
            if (dw1 == 0L) {
                ++dw2
                if (dw2 == 0L) {
                    ++dw3
                    if (dw3 == 0L)
                        throw RuntimeException("overflow")
                }
            }
        }
        // flag for roundup which occurs during multiplies while enableIndexSet is active
        if (digitLen >= 0)
            updateLengthsAfterRoundUp()
    }

    fun u256MutateDecrement() {
        --dw0
        if (dw0 == -1L) {
            --dw1
            if (dw1 == -1L) {
                --dw2
                if (dw2 == -1L) {
                    --dw3
                    if (dw3 == -1L)
                        throw RuntimeException("decrement underflow")
                }
            }
        }
        updateLengths()
    }

    fun u256NumberOfTrailingZeros() = U256Bits.numberOfTrailingZeros(this)

    fun u256DwordAtBitIndex(bitIndex: Int) = U256Bits.getDwordAtBitIndex(this, bitIndex)

    //override fun toString() = coeffToBigInteger().toString()
    override fun toString() = U256ParsePrint.u256ToString(this)

    override fun equals(other: Any?) = other is U256 && u256UnscaledEQ(this, other)


    fun u256ToNaNDiagnosticString() = if (u256IsZero()) "" else toString()

    ///////////////////////
    // signed operations //
    ///////////////////////

    fun s256Add(z: XInt256, x: XInt256, y: XInt256) = s256AddImpl(z, x, y.sign, y)

    fun s256Sub(z: XInt256, x: XInt256, y: XInt256) = s256AddImpl(z, x, ! y.sign, y)

    private fun s256AddImpl(z: XInt256, x: XInt256, ySign: Boolean, y: XInt256) {
        val xSign = x.sign
        if (xSign == ySign) {
            // FIXME do I need to worry about -0 here?
            z.sign = ySign
            u256AddUnscaled(z, x, y)
        } else {
            val cmp = u256UnscaledCompare(x, y)
            when {
                (cmp > 0) -> {
                    u256SubUnscaled(z, x, y)
                    z.sign = xSign and (z.bitLen > 0)
                }
                (cmp < 0) -> {
                    u256SubUnscaled(z, y, x)
                    z.sign = ySign and (z.bitLen > 0)
                }
                else -> {
                    z.sign = false
                    u256SetZero()
                }
            }
        }
    }

    fun s256Mul(z: XInt256, x: XInt256, y: XInt256) {
        u256Mul(z, x, y)
        z.sign = (x.sign xor y.sign) and (z.bitLen > 0)
    }

    fun s256Divx64(z: XInt256, x: XInt256, y0: Long) {
        u256Divx64(z, x, y0)
        z.sign = x.sign
    }

    fun s256Div(z: XInt256, x: XInt256, y: XInt256) {
        u256Div(z, x, y)
        z.sign = x.sign xor y.sign
    }

    fun s256Mod(z: XInt256, x: XInt256, y: XInt256) {
        u256Mod(z, x, y)
        z.sign = x.sign xor y.sign
    }

    fun s256ScaleUpPow10(z: XInt256, x: XInt256, pow10: Int) {
        u256ScaleUpPow10(z, x, pow10)
        z.sign = x.sign
    }

    fun s256ScaleDownPow10(z: XInt256, x: XInt256, pow10: Int) {
        u256ScaleDownPow10(z, x, pow10)
        z.sign = x.sign
    }

    fun s256FmaPow10(z: XInt256, x: XInt256, pow10: Int, a: XInt256) {
        if (! (x.sign xor a.sign)) {
            z.sign = x.sign
            U256ScalePow10.u256ScaleFmaPow10(z, x, pow10, a)
            return
        }
        val prod = if (z === a) XInt256() else z
        s256ScaleUpPow10(prod, x, pow10)
        s256Add(z, prod, a)
    }


}