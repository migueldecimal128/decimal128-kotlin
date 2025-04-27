package com.decimal128

import java.math.BigInteger
import java.lang.Long.compareUnsigned
import com.decimal128.CoeffMul.coeffMul
import com.decimal128.CoeffFma.coeffFma
import com.decimal128.CoeffAdd.coeffAdd
import com.decimal128.CoeffAdd.coeffAddUnscaled
import com.decimal128.CoeffAbsDiff.coeffAbsDiffUnscaled
import com.decimal128.CoeffCompare.coeffEQ
import com.decimal128.CoeffCompare.coeffGT
import com.decimal128.CoeffCompare.coeffLT
import java.lang.Long.numberOfLeadingZeros

const val PRECISION_34 = 34

private const val SIGNBIT = Long.MIN_VALUE


class Coeff(var dw3:Long, var dw2:Long, var dw1:Long, var dw0:Long) {

    constructor(dw2:Long, dw1:Long, dw0: Long) : this(0L, dw2, dw1, dw0)
    constructor(dw1: Long, dw0: Long) : this(0L, 0L, dw1, dw0)
    constructor(dw0: Long) : this(0L, 0L, 0L, dw0)
    constructor(w0: Int) : this(0L, 0L, 0L, w0.toLong() and 0xFFFFFFFFL)
    constructor() : this(0L, 0L, 0L, 0L)
    constructor(bi: BigInteger) : this(bi.shiftRight(192).toLong(), bi.shiftRight(128).toLong(), bi.shiftRight(64).toLong(), bi.toLong()) {
        require(bi.bitLength() <= 256)
    }
    constructor(str: String) : this(BigInteger(str))
    constructor(c: Coeff) : this(c.dw3, c.dw2, c.dw1, c.dw0)

    var digitCount = run { DigitCount.calcDigitCount256(dw3, dw2, dw1, dw0) }

    fun setZero() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 0L; digitCount = 0
    }

    fun isZero() = digitCount == 0

    private fun setDigitCount64() = DigitCount.setDigitCount64(this)
    private fun setDigitCount128() = DigitCount.setDigitCount128(this)
    private fun setDigitCount192() = DigitCount.setDigitCount192(this)
    private fun setDigitCount256() = DigitCount.setDigitCount256(this)
    private fun setDigitCount() = DigitCount.setDigitCount(this)

    fun isValidDigitCount() : Boolean {
        val prevDigitCount = digitCount
        setDigitCount()
        val t = digitCount
        digitCount = prevDigitCount
        return t == prevDigitCount
    }

    fun compareTo(other: Coeff) : Int {
        assert(isValidDigitCount())
        assert(other.isValidDigitCount())
        if (digitCount != other.digitCount)
            return digitCount.compareTo(other.digitCount)
        if (dw3 != other.dw3)
            return compareUnsigned(dw3, other.dw3)
        if (dw2 != other.dw2)
            return compareUnsigned(dw2, other.dw2)
        if (dw1 != other.dw1)
            return compareUnsigned(dw1, other.dw1)
        return compareUnsigned(dw0, other.dw0)
    }

    fun EQ(other: Coeff) = coeffEQ(this, other)

    fun NE(other: Coeff) : Boolean = !EQ(other)

    fun GE(other: Coeff) : Boolean = !LT(other)

    fun GT(other: Coeff) = coeffGT(this, other)

    fun LE(other: Coeff) : Boolean = !GT(other)

    fun LT(other: Coeff) = coeffLT(this, other)

    fun add(x: Coeff, scaleDelta: Int, y: Coeff) = coeffAdd(this, x, scaleDelta, y)

    fun add(x: Coeff, y: Coeff) = coeffAddUnscaled(this, x, y)

    // absolute difference
    // if minuend < subtrahend then negate to return positive result
    fun absDiff(x: Coeff, y: Coeff) = coeffAbsDiffUnscaled(this, x, y)

    fun mul(x:Coeff, y:Coeff) = coeffMul(this, x, y)

    fun fma(x:Coeff, y:Coeff, a:Coeff) = coeffFma(this, x, y, a)

    fun shiftRight(bitShift:Int) {
        val wholeDwordCount = bitShift ushr 6
        val innerShift = bitShift and 0x3F
        if (innerShift == 0) {
            when (wholeDwordCount) {
                0 -> return
                1 -> { dw0 = dw1; dw1 = dw2; dw2 = dw3; dw3 = 0L; setDigitCount192() }
                2 -> { dw0 = dw2; dw1 = dw3; dw2 = 0L; dw3 = 0L; setDigitCount128()}
                3 -> { dw0 = dw3; dw1 = 0L; dw2 = 0L; dw3 = 0L; setDigitCount64() }
                else -> setZero()
            }
        } else {
            val leftShift = 64 - innerShift
            when (wholeDwordCount ) {
                0 -> {
                    dw0 = (dw1 shl leftShift) or (dw0 ushr innerShift)
                    dw1 = (dw2 shl leftShift) or (dw1 ushr innerShift)
                    dw2 = (dw3 shl leftShift) or (dw2 ushr innerShift)
                    dw3 = dw3 ushr innerShift
                    setDigitCount256()
                }
                1 -> {
                    dw0 = (dw2 shl leftShift) or (dw1 ushr innerShift)
                    dw1 = (dw3 shl leftShift) or (dw2 ushr innerShift)
                    dw2 = dw3 ushr innerShift
                    dw3 = 0L
                    setDigitCount192()
                }
                2 -> {
                    dw0 = (dw3 shl leftShift) or (dw2 ushr innerShift)
                    dw1 = dw3 ushr innerShift
                    dw2 = 0L; dw3 = 0L
                    setDigitCount128()
                }
                3 -> {
                    dw0 = dw3 ushr innerShift
                    dw1 = 0L; dw2 = 0L; dw3 = 0L
                    setDigitCount64()
                }
                else -> setZero()
            }
        }
    }

    fun scalePow10(x:Coeff, pow10:Int, sign:Boolean, ctx:Decimal128Context) {
        CoeffScalePow10.scalePow10Coeff(this, x, pow10, sign, ctx)
    }

    operator fun get(index:Int) : Long {
        return when (index) {
            0 -> dw0
            1 -> dw1
            2 -> dw2
            3 -> dw3
            else -> throw RuntimeException("indexOutOfBounds")
        }
    }

    operator fun set(index:Int, value:Long) {
        when (index) {
            0 -> dw0 = value
            1 -> dw1 = value
            2 -> dw2 = value
            3 -> dw3 = value
            else -> throw RuntimeException("indexOutOfBounds")
        }
    }

    fun setLoBit(dw0:Long) {
        val masked = dw0 and 1
        this.dw3 = 0L; this.dw2 = 0L; this.dw1 = 0L
        this.dw0 = masked
        this.digitCount = masked.toInt()
    }

    fun set(dw0: Long) {
        this.dw3 = 0L; this.dw2 = 0L; this.dw1 = 0L
        this.dw0 = dw0;
        setDigitCount64()
    }

    fun set(dw1: Long, dw0: Long) {
        this.dw3 = 0L; this.dw2 = 0L;
        this.dw1 = dw1;this.dw0 = dw0;
        setDigitCount128()
    }

    fun set(dw2: Long, dw1: Long, dw0: Long) {
        this.dw3 = 0L;
        this.dw2 = dw2; this.dw1 = dw1; this.dw0 = dw0;
        setDigitCount192()
    }

    fun set(dw3: Long, dw2: Long, dw1: Long, dw0: Long) {
        this.dw3 = dw3; this.dw2 = dw2; this.dw1 = dw1;this.dw0 = dw0;
        setDigitCount()
    }

    fun set(digitCount: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) {
        this.digitCount = digitCount; this.dw3 = dw3; this.dw2 = dw2; this.dw1 = dw1;this.dw0 = dw0;
        require(isValidDigitCount())
    }

    fun set(bi: BigInteger) {
        require (bi.bitLength() <= 256)
        set(bi.shiftRight(192).toLong(), bi.shiftRight(128).toLong(), bi.shiftRight(64).toLong(), bi.toLong())
        setDigitCount()
    }

    fun set(c: Coeff) {
        if (this != c) {
            digitCount = c.digitCount; dw3 = c.dw3; dw2 = c.dw2; dw1 = c.dw1; dw0 = c.dw0
        }
        assert(isValidDigitCount())
    }

    fun set(str: String) = set(BigInteger(str))

    fun set(x:LongArray, xOff:Int, xLen:Int) {
        setZero()
        if (xLen == 0)
            return
        var nonZeroIndex = xLen - 1
        var nonZeroVal = 0L
        while (nonZeroVal == 0L && --nonZeroIndex >= 0) {
            nonZeroVal = x[xOff + nonZeroIndex]
        }
        when (nonZeroIndex) {
            -1 -> {}
            0 -> { dw0 = nonZeroVal; setDigitCount64() }
            1 -> { dw0 = x[xOff+0]; dw1 = nonZeroVal; setDigitCount128() }
            2 -> { dw0 = x[xOff+0]; dw1 = x[xOff+1]; dw2 = nonZeroVal; setDigitCount192() }
            3 -> { dw0 = x[xOff+0]; dw1 = x[xOff+1]; dw2 = x[xOff+2]; dw3 = nonZeroVal; setDigitCount256() }
            4 -> throw RuntimeException("overflow")
        }
    }

    fun bitLength() : Int {
        when {
            (dw3 != 0L) -> return 192 + 64 - numberOfLeadingZeros(dw3)
            (dw2 != 0L) -> return 128 + 64 - numberOfLeadingZeros(dw2)
            (dw1 != 0L) -> return 64 + 64 - numberOfLeadingZeros(dw1)
            (dw0 != 0L) -> return 64 - numberOfLeadingZeros(dw0)
            else -> return 0
        }
    }

    fun setShiftRight(x:LongArray, xOff:Int, xLen:Int, bitCount:Int) {
        setZero()
        // strip leading zeros from x
        var nonZeroIndex = xLen - 1
        var nonZeroVal = 0L
        while (nonZeroVal == 0L && --nonZeroIndex >= 0) {
            nonZeroVal = x[xOff + nonZeroIndex]
        }
        val nonZeroLen = nonZeroIndex + 1

        val dwordShift = bitCount ushr 6
        val innerShift = bitCount and ((1 shl 6) - 1)
        val newLen = xLen - dwordShift
        val shiftOff = xOff + dwordShift
        if (innerShift == 0) {
            when (newLen) {
                0 -> {}
                1 -> { dw0 = x[shiftOff + 0]; setDigitCount64() }
                2 -> { dw0 = x[shiftOff + 0]; dw1 = x[shiftOff + 1]; setDigitCount128() }
                3 -> { dw0 = x[shiftOff + 0]; dw1 = x[shiftOff + 1]; dw2 = x[shiftOff + 2]; setDigitCount192() }
                4 -> { dw0 = x[shiftOff + 0]; dw1 = x[shiftOff + 1]; dw2 = x[shiftOff + 2];
                    dw3 = x[shiftOff + 3]; setDigitCount256() }
                else -> {
                    for (i in 5..<newLen)
                        if (x[shiftOff + i] != 0L)
                            throw RuntimeException("overflow")
                }
            }
            return
        }
        val leftShift = 64 - innerShift
        when (newLen) {
            0 -> {}
            1 -> {
                dw0 = x[shiftOff + 0] ushr innerShift; setDigitCount64()
            }

            2 -> {
                dw0 = (x[shiftOff + 1] shl leftShift) or (x[shiftOff + 0] ushr innerShift)
                dw1 = x[shiftOff + 1] ushr innerShift
                setDigitCount128()
            }

            3 -> {
                dw0 = (x[shiftOff + 1] shl leftShift) or (x[shiftOff + 0] ushr innerShift)
                dw1 = (x[shiftOff + 2] shl leftShift) or (x[shiftOff + 1] ushr innerShift)
                dw2 = x[shiftOff + 2] ushr innerShift
                setDigitCount192()
            }

            4 -> {
                dw0 = (x[shiftOff + 1] shl leftShift) or (x[shiftOff + 0] ushr innerShift)
                dw1 = (x[shiftOff + 2] shl leftShift) or (x[shiftOff + 1] ushr innerShift)
                dw2 = (x[shiftOff + 3] shl leftShift) or (x[shiftOff + 2] ushr innerShift)
                dw3 = x[shiftOff + 3] ushr innerShift
                setDigitCount256()
            }

            5 -> {
                dw0 = (x[shiftOff + 1] shl leftShift) or (x[shiftOff + 0] ushr innerShift)
                dw1 = (x[shiftOff + 2] shl leftShift) or (x[shiftOff + 1] ushr innerShift)
                dw2 = (x[shiftOff + 3] shl leftShift) or (x[shiftOff + 2] ushr innerShift)
                dw3 = (x[shiftOff + 4] shl leftShift) or (x[shiftOff + 3] ushr innerShift)
                val dw4 = x[shiftOff + 4] ushr innerShift
                if (dw4 != 0L)
                    throw RuntimeException("overflow")
                setDigitCount256()
            }

            else -> {
                throw RuntimeException("overflow")
            }
        }
    }

    fun toBigInteger() : BigInteger {
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