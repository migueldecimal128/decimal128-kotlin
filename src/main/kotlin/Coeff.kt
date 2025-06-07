package com.decimal128

import java.math.BigInteger
import com.decimal128.CoeffMul.coeffMul
import com.decimal128.CoeffFma.coeffFma
import com.decimal128.CoeffAdd.coeffAddUnscaled
import com.decimal128.CoeffAdd.coeffAdd
import com.decimal128.CoeffCompare.coeffScaledCompare
import com.decimal128.CoeffCompare.coeffScaledEQ
import com.decimal128.CoeffSet.coeffSetShiftRight
import com.decimal128.CoeffCompare.coeffUnscaledCompare
import com.decimal128.CoeffCompare.coeffUnscaledEQ
import com.decimal128.CoeffDivide.coeffDiv
import com.decimal128.CoeffDivide.coeffDivx64
import com.decimal128.CoeffDivide.coeffMod
import com.decimal128.CoeffFma.coeffFmaPow10
import com.decimal128.CoeffPow10.calcDigitLen256
import com.decimal128.CoeffScalePow10.coeffScaleDownPow10
import com.decimal128.CoeffScalePow10.coeffScaleUpPow10
import com.decimal128.CoeffSet.coeffSet

const val PRECISION_34 = 34

private const val SIGNBIT = Long.MIN_VALUE


open class Coeff(d3: Long, d2: Long, d1: Long, d0: Long) {

    constructor(dw2: Long, dw1: Long, dw0: Long) : this(0L, dw2, dw1, dw0)
    constructor(dw1: Long, dw0: Long) : this(0L, 0L, dw1, dw0)
    constructor(dw0: Long) : this(0L, 0L, 0L, dw0)
    constructor(w0: Int) : this(0L, 0L, 0L, w0.toLong() and 0xFFFFFFFFL)
    constructor() : this(0L, 0L, 0L, 0L)
    constructor(bi: BigInteger) : this(
        bi.shiftRight(192).toLong(), bi.shiftRight(128).toLong(), bi.shiftRight(64).toLong(), bi.toLong()
    ) {
        require(bi.bitLength() <= 256)
    }

    constructor(str: String) : this(BigInteger(str))
    constructor(c: Coeff) : this(c.dw3, c.dw2, c.dw1, c.dw0)

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

    fun coeffSetZero() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 0L; bitLen = 0; digitLen = 0
    }

    fun coeffIsZero() = digitLen == 0

    fun coeffIsNotZero() = digitLen > 0

    fun coeffSetOne() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 1L; bitLen = 1; digitLen = 1
    }

    fun coeffSetZeroOrOneMasked(d0: Long) {
        val asInt = (d0 and 1).toInt()
        dw3 = 0; dw2 = 0; dw1 = 0; dw0 = d0 and 1; bitLen = asInt; digitLen = asInt
    }

    fun coeffIsOne() = bitLen == 1

    fun coeffIsLEOne() = bitLen <= 1

    fun coeffIsGTOne() = bitLen > 1

    fun coeffIsMultipleOf5() = CoeffBits.coeffIsMultipleOf5(this)

    fun coeffIsMultipleOf10(): Boolean {
        if (bitLen < 4 || (dw0 and 1) != 0L)
            return false
        return coeffIsMultipleOf5()
    }

    fun coeffIsPow10() = CoeffPow10.coeffIsPow10(this)

    fun coeffIs33Nines() : Boolean  {
        return bitLen == BITLEN_33_NINES && dw1 == DW1_33_NINES && dw0 == DW0_33_NINES
    }

    private fun calcBitLen(): Int {
        return calcBitLen256(dw3, dw2, dw1, dw0)
    }

    private fun calcDigitLen(): Int {
        return when {
            (bitLen <= 64) -> CoeffPow10.calcDigitLen64(bitLen, dw0)
            (bitLen <= 128) -> CoeffPow10.calcDigitLen128(bitLen, dw1, dw0)
            (bitLen <= 192) -> CoeffPow10.calcDigitLen192(bitLen, dw2, dw1, dw0)
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

    fun coeffHasValidLengths(): Boolean {
        if (digitLen != calcDigitLen256(bitLen, dw3, dw2, dw1, dw0))
            return false;
        return true
    }

    fun coeffUnscaledCompareTo(other: Coeff) = coeffUnscaledCompare(this, other)

    fun coeffUnscaledEQ(other: Coeff) = coeffUnscaledEQ(this, other)

    fun coeffScaledCompareTo(other: Coeff, scaleDelta: Int)  = coeffScaledCompare(this, other, scaleDelta)

    fun coeffScaledEQ(other: Coeff, scaleDelta: Int) = coeffScaledEQ(this, other, scaleDelta)

    fun coeffSetAdd(x: Coeff, scaleDelta: Int, y: Coeff) = coeffAdd(this, x, scaleDelta, y)

    fun coeffSetAdd(x: Coeff, y: Coeff) = coeffAddUnscaled(this, x, y)

    fun coeffSetMul(x: Coeff, y: Coeff) = coeffMul(this, x, y)

    fun coeffSetFma(x: Coeff, y: Coeff, a: Coeff) = coeffFma(this, x, y, a)

    fun coeffSetFmaPow10(x: Coeff, pow10: Int, a: Coeff) = coeffFmaPow10(this, x, pow10, a)

    fun coeffSetFmaPow10(x: Coeff, pow10: Int, a0: Long) = coeffFmaPow10(this, x, pow10, a0)

    fun coeffSetFmaPow10(x: Coeff, pow10: Int, a1: Long, a0: Long) = coeffFmaPow10(this, x, pow10, a1, a0)

    fun coeffMutateFmaPow10(pow10: Int, a: Long) = coeffFmaPow10(this, this, pow10, a)

    fun coeffSetFms(x: Coeff, y: Coeff, subtrahend: Coeff) = CoeffFms.coeffFms(this, x, y, subtrahend)

    fun coeffSetDiv(x: Coeff, y: Coeff) = coeffDiv(this, x, y)

    fun coeffSetDivx64(x: Coeff, y0: Long) = coeffDivx64(this, x, y0)

    fun coeffSetMod(x: Coeff, y: Coeff) = coeffMod(this, x, y)

    fun coeffSetScaleUpPow10(x: Coeff, pow10: Int) = coeffScaleUpPow10(this, x, pow10)

    fun coeffSetScaleDownPow10(x: Coeff, pow10: Int) = coeffScaleDownPow10(this, x, pow10)

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

    fun coeffEnableIndexSetAndZeroOut() {
        digitLen = -1
        dw0 = 0L; dw1 = 0L; dw2 = 0L; dw3 = 0L
    }

    fun coeffDisableIndexSetAndUpdateLengths() {
        assert(digitLen == -1)
        updateLengths()
    }

    fun coeffSet64(d0: Long) {
        dw3 = 0L; dw2 = 0L; dw1 = 0L
        dw0 = d0
        bitLen = calcBitLen64(d0)
        digitLen = CoeffPow10.calcDigitLen64(bitLen, d0)
    }

    fun coeffSet128(d1: Long, d0: Long) {
        dw3 = 0L; dw2 = 0L
        dw1 = d1; dw0 = d0
        bitLen = calcBitLen128(d1, d0)
        digitLen = CoeffPow10.calcDigitLen128(bitLen, d1, d0)
    }

    fun coeffSet192(d2: Long, d1: Long, d0: Long) {
        dw3 = 0L
        dw2 = d2; dw1 = d1; dw0 = d0
        bitLen = calcBitLen192(d2, d1, d0)
        digitLen = CoeffPow10.calcDigitLen192(bitLen, d2, d1, d0)
    }


    fun coeffSet256(d3: Long, d2: Long, d1: Long, d0: Long){
        dw3 = d3; dw2 = d2; dw1 = d1; dw0 = d0
        bitLen = calcBitLen256(d3, d2, d1, d0)
        digitLen = calcDigitLen256(bitLen, d3, d2, d1, d0)
    }

    fun coeffSet(bi: BigInteger) {
        require(bi.bitLength() <= 256)
        coeffSet256(bi.shiftRight(192).toLong(), bi.shiftRight(128).toLong(), bi.shiftRight(64).toLong(), bi.toLong())
    }

    fun coeffSet(x: Coeff) {
            bitLen = x.bitLen; digitLen = x.digitLen; dw3 = x.dw3; dw2 = x.dw2; dw1 = x.dw1; dw0 = x.dw0
    }

    fun coeffSet(str: String) = coeffSet(BigInteger(str))

    fun coeffSetShiftRight(x: Coeff, bitShift: Int) = CoeffSet.coeffSetShiftRight(this, x, bitShift)

    fun coeffSetShiftLeft(x: Coeff, bitShift: Int) = CoeffSet.coeffSetShiftLeft(this, x, bitShift)

    fun coeffSet(x: LongArray, xOff: Int, xLen: Int) = coeffSet(this, x, xOff, xLen)

    fun coeffSetShiftRight(x: LongArray, xOff: Int, xLen: Int, bitCount: Int) =
        coeffSetShiftRight(this, x, xOff, xLen, bitCount)

    open fun coeffToBigInteger(): BigInteger {
//        assert(validateDigitCount())
        var bi = BigInteger.ZERO
        val dw0Lo = dw0 and 0xFFFFFFFFL
        val dw0Hi = dw0 ushr 32
        val dw1Lo = dw1 and 0xFFFFFFFFL
        val dw1Hi = dw1 ushr 32
        val dw2Lo = dw2 and 0xFFFFFFFFL
        val dw2Hi = dw2 ushr 32
        val dw3Lo = dw3 and 0xFFFFFFFFL
        val dw3Hi = dw3 ushr 32
        bi = bi or BigInteger(dw0Lo.toString()).shiftLeft(0)
        bi = bi or BigInteger(dw0Hi.toString()).shiftLeft(32)
        bi = bi or BigInteger(dw1Lo.toString()).shiftLeft(64)
        bi = bi or BigInteger(dw1Hi.toString()).shiftLeft(96)
        bi = bi or BigInteger(dw2Lo.toString()).shiftLeft(128)
        bi = bi or BigInteger(dw2Hi.toString()).shiftLeft(160)
        bi = bi or BigInteger(dw3Lo.toString()).shiftLeft(192)
        bi = bi or BigInteger(dw3Hi.toString()).shiftLeft(224)
        return bi
    }

    fun coeffMutateIncrement(doRoundUp: Boolean) {
        if (doRoundUp)
            coeffMutateIncrement()
    }

    fun coeffMutateIncrement() {
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

    fun coeffMutateDecrement() {
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

    fun coeffNumberOfTrailingZeros() = CoeffBits.numberOfTrailingZeros(this)

    fun coeffDwordAtBitIndex(bitIndex: Int) = CoeffBits.getDwordAtBitIndex(this, bitIndex)

    override fun toString() = coeffToBigInteger().toString()

    fun coeffToNaNDiagnosticString() = if (coeffIsZero()) "" else toString()

}