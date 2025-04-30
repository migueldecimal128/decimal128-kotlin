package com.decimal128

import com.decimal128.CoeffAbsDiff.coeffAbsDiffScaled
import java.math.BigInteger
import com.decimal128.CoeffMul.coeffMul
import com.decimal128.CoeffFma.coeffFma
import com.decimal128.CoeffFusedMulAbsDiff.coeffFusedMulAbsDiff
import com.decimal128.CoeffAdd.coeffAddUnscaled
import com.decimal128.CoeffAbsDiff.coeffAbsDiffUnscaled
import com.decimal128.CoeffAdd.coeffAdd
import com.decimal128.CoeffBits.setLoBit
import com.decimal128.CoeffBits.coeffSetShiftRight
import com.decimal128.CoeffBits.coeffShiftRight
import com.decimal128.CoeffCompare.coeffCompare
import com.decimal128.CoeffCompare.coeffEQ
import com.decimal128.CoeffCompare.coeffGT
import com.decimal128.CoeffCompare.coeffLT
import com.decimal128.CoeffScalePow10.coeffScaleDownPow10
import com.decimal128.CoeffScalePow10.coeffScaleUpPow10
import com.decimal128.CoeffSet.coeffSet
import com.decimal128.CoeffSet.coeffSetZero

const val PRECISION_34 = 34

private const val SIGNBIT = Long.MIN_VALUE


class Coeff(var dw3: Long, var dw2: Long, var dw1: Long, var dw0: Long) {

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

    var digitCount = run { CoeffDigitCount.calcDigitCount256(dw3, dw2, dw1, dw0) }

    fun setZero() = coeffSetZero(this)

    fun isZero() = digitCount == 0

    fun isOne() = digitCount == 1 && dw0 == 1L

    fun isLEOne() = digitCount <= 1 && (dw0 and 0x0F) <= 1

    fun isGTOne() = digitCount > 1 || (dw0 and 0x0F) > 1

    private fun setDigitCount64() = CoeffDigitCount.setDigitCount64(this)
    private fun setDigitCount128() = CoeffDigitCount.setDigitCount128(this)
    private fun setDigitCount192() = CoeffDigitCount.setDigitCount192(this)
    private fun setDigitCount256() = CoeffDigitCount.setDigitCount256(this)
    private fun setDigitCount() = CoeffDigitCount.setDigitCount(this)

    fun isValidDigitCount(): Boolean {
        val prevDigitCount = digitCount
        setDigitCount()
        val t = digitCount
        digitCount = prevDigitCount
        return t == prevDigitCount
    }

    fun compareTo(other: Coeff) = coeffCompare(this, other)

    fun EQ(other: Coeff) = coeffEQ(this, other)

    fun NE(other: Coeff): Boolean = !EQ(other)

    fun GE(other: Coeff): Boolean = !LT(other)

    fun GT(other: Coeff) = coeffGT(this, other)

    fun LE(other: Coeff): Boolean = !GT(other)

    fun LT(other: Coeff) = coeffLT(this, other)

    fun add(x: Coeff, scaleDelta: Int, y: Coeff) = coeffAdd(this, x, scaleDelta, y)

    fun add(x: Coeff, y: Coeff) = coeffAddUnscaled(this, x, y)

    // absolute difference
    // if minuend < subtrahend then negate to return positive result
    // and return a _NEGATED residue
    // because it would have gone negative
    fun absDiff(x: Coeff, scaleDelta: Int, y: Coeff) = CoeffAbsDiff.absDiff(this, x, scaleDelta, y)

    fun absDiff(x: Coeff, y: Coeff) = coeffAbsDiffUnscaled(this, x, y)


    fun mul(x: Coeff, y: Coeff) = coeffMul(this, x, y)

    fun fma(x: Coeff, y: Coeff, a: Coeff) = coeffFma(this, x, y, a)

    fun fusedMulAbsDiff(x: Coeff, y: Coeff, a: Coeff) = coeffFusedMulAbsDiff(this, x, y, a)

    fun div(x: Coeff, y: Coeff) = CoeffDivide.coeffDiv(this, x, y)

    fun mod(x: Coeff, y: Coeff) = CoeffDivide.coeffMod(this, x, y)

    fun shiftRight(bitShift: Int) = coeffShiftRight(this, bitShift)

    fun scaleUpPow10(x: Coeff, pow10: Int) = coeffScaleUpPow10(this, x, pow10)

    fun scaleDownPow10(x: Coeff, pow10: Int) = coeffScaleDownPow10(this, x, pow10)

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
        when (index) {
            0 -> dw0 = value
            1 -> dw1 = value
            2 -> dw2 = value
            3 -> dw3 = value
            else -> throw RuntimeException("indexOutOfBounds")
        }
    }

    fun setLoBit(dw0: Long) = setLoBit(this, dw0)

    fun set(dw0: Long) = coeffSet(this, dw0)

    fun set(dw1: Long, dw0: Long) = coeffSet(this, dw1, dw0)

    fun set(dw2: Long, dw1: Long, dw0: Long) = coeffSet(this, dw2, dw1, dw0)


    fun set(dw3: Long, dw2: Long, dw1: Long, dw0: Long) = coeffSet(this, dw3, dw2, dw1, dw0)

    fun set(digitCount: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) =
        coeffSet(this, digitCount, dw3, dw2, dw1, dw0)

    fun set(bi: BigInteger) = coeffSet(this, bi)

    fun set(c: Coeff) = coeffSet(this, c)

    fun set(str: String) = coeffSet(this, str)

    fun set(x: LongArray, xOff: Int, xLen: Int) = coeffSet(this, x, xOff, xLen)

    fun setShiftRight(x: LongArray, xOff: Int, xLen: Int, bitCount: Int) =
        coeffSetShiftRight(this, x, xOff, xLen, bitCount)

    fun toBigInteger(): BigInteger {
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

    override fun toString() = toBigInteger().toString()

}