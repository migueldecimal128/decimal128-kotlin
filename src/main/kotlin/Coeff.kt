package com.decimal128

import com.decimal128.CoeffFma.Companion.fmaCoeff
import java.math.BigInteger
import java.lang.Long.compareUnsigned
import java.lang.Math.unsignedMultiplyHigh
import com.decimal128.CoeffMul.Companion.mulCoeff

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

    var digitCount = run { calcDigitCount256(dw3, dw2, dw1, dw0) }

    fun setZero() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 0L; digitCount = 0
    }

    fun isZero() = digitCount == 0

    private fun setDigitCount64() = setDigitCount64(this)
    private fun setDigitCount128() = setDigitCount128(this)
    private fun setDigitCount192() = setDigitCount192(this)
    private fun setDigitCount256() = setDigitCount256(this)
    private fun setDigitCount() = setDigitCount(this)

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

    fun EQ(other: Coeff) : Boolean {
        assert(isValidDigitCount())
        assert(other.isValidDigitCount())
        return (digitCount == other.digitCount) && (dw0 == other.dw0) && (dw1 == other.dw1) && (dw2 == other.dw2) && (dw3 == other.dw3)
    }

    fun NE(other: Coeff) : Boolean = !EQ(other)

    fun GE(other: Coeff) : Boolean = !LT(other)

    fun GT(other: Coeff) : Boolean {
        assert(isValidDigitCount())
        assert(other.isValidDigitCount())
        return when {
            (digitCount != other.digitCount) -> digitCount > other.digitCount
            (dw3 !=other.dw3) -> compareUnsigned(dw3, other.dw3) > 0
            (dw2 !=other.dw2) -> compareUnsigned(dw2, other.dw2) > 0
            (dw1 !=other.dw1) -> compareUnsigned(dw1, other.dw1) > 0
            else -> compareUnsigned(dw0, other.dw0) > 0
        }
    }

    fun LE(other: Coeff) : Boolean = !GT(other)

    fun LT(other: Coeff) : Boolean {
        assert(isValidDigitCount())
        assert(other.isValidDigitCount())
        return when {
            (digitCount != other.digitCount) -> digitCount < other.digitCount
            (dw3 !=other.dw3) -> compareUnsigned(dw3, other.dw3) < 0
            (dw2 !=other.dw2) -> compareUnsigned(dw2, other.dw2) < 0
            (dw1 !=other.dw1) -> compareUnsigned(dw1, other.dw1) < 0
            else -> compareUnsigned(dw0, other.dw0) < 0
        }
    }

    fun add(x: Coeff, y: Coeff) {
        val maxDigitCount = Math.max(x.digitCount, y.digitCount)

        val x0 = x.dw0
        val y0 = y.dw0
        val p0 = x0 + y0
        dw0 = p0
        val carry0 = if (compareUnsigned(p0, x0) < 0) 1L else 0L
        // 64 bit boundary is a special case because 19 digits * 2 might generate carry into the next word
        // this is not the case for the 128 and 192 bit boundaries
        //
        // perhaps this test should be:
        // maxDigitCount <= 20 && (x.dw1 or y.dw1 or carry0) == 0
        if (maxDigitCount <= POW10_128_OFFSET && (x.dw1 or y.dw1 or carry0) == 0L) {
            dw3 = 0L; dw2 = 0L; dw1 = 0L
            setDigitCount64()
            return
        }

        val x1 = x.dw1
        val y1 = y.dw1
        val p1a = x1 + y1
        val carry1a = if (compareUnsigned(p1a, x1) < 0) 1L else 0L
        val p1 = p1a + carry0
        dw1 = p1
        val carry1 = if (compareUnsigned(p1, carry0) < 0) 1L else carry1a
        if (maxDigitCount <= POW10_192_OFFSET && (x.dw2 or y.dw2 or carry1) == 0L) {
            dw3 = 0L; dw2 = 0L;
            setDigitCount128()
            return
        }

        val x2 = x.dw2
        val y2 = y.dw2
        val p2a = x2 + y2
        val carry2a = if (compareUnsigned(p2a, x2) < 0) 1L else 0L
        val p2 = p2a + carry1
        dw2 = p2
        val carry2 = if (compareUnsigned(p2, carry1) < 0) 1L else carry2a
        if (maxDigitCount <= POW10_256_OFFSET && (x.dw3 or y.dw3 or carry2) == 0L) {
            dw3 = 0L;
            setDigitCount192()
            return
        }

        val x3 = x.dw3
        val y3 = y.dw3
        val p3a = x3 + y3
        val carry3a = if (compareUnsigned(p3a, x3) < 0) 1L else 0L
        val p3 = p3a + carry2
        dw3 = p3
        val carry3 = if (compareUnsigned(p3, carry2) < 0) 1L else carry3a
        if (carry3 != 0L)
            throw RuntimeException("coefficient add overflow x:$x y:$y")
        setDigitCount256()
    }

    // absolute difference
    // if minuend < subtrahend then negate to return positive result
    fun absDiff(x: Coeff, y: Coeff) : Long { // minuend - subtrahend
        assert(isValidDigitCount())
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        val maxDigitCount = Math.max(x.digitCount, y.digitCount)
        val loDigitCount = maxDigitCount - 1
        val minDigitCount = Math.min(x.digitCount, y.digitCount)
        val digitCountDiff = maxDigitCount - minDigitCount

        val d0 = x.dw0 - y.dw0
        val carry0 = if (compareUnsigned(d0, x.dw0) > 0) 1L else 0L
        // 64 bit boundary is a special case because 19 digits * 2 might generate carry into the next word
        // this is not the case for the 128 and 192 bit boundaries
        //
        // perhaps this test should be:
        // maxDigitCount <= 20 && (x.dw1 or y.dw1 or carry0) == 0
        if (maxDigitCount < POW10_128_OFFSET) {
            // if carry == 1 then complement-and-increment else NOOP
            val negCarry0 = -carry0
            dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = (d0 xor negCarry0) - negCarry0
            setDigitCount64()
            return negCarry0
        }

        val d1a = x.dw1 - y.dw1
        val carry1a = if (compareUnsigned(d1a, x.dw1) > 0) 1L else 0L
        val d1 = d1a - carry0
        val carry1 = if (compareUnsigned(d1, d1a) > 0) 1L else carry1a
        if (maxDigitCount < POW10_192_OFFSET) {
            // if carry == 1 then complement-and-increment else NOOP
            val negCarry1 = -carry1
            dw0 = (d0 xor negCarry1) - negCarry1
            dw1 = (d1 xor negCarry1) - (negCarry1 and ((dw0 or -dw0) shr 63).inv())
            dw2 = 0L
            dw3 = 0L
            setDigitCount128()
            return negCarry1
        }

        val d2a = x.dw2 - y.dw2
        val carry2a = if (compareUnsigned(d2a, x.dw2) > 0) 1L else 0L
        val d2 = d2a - carry1
        val carry2 = if (compareUnsigned(d2, d2a) > 0) 1L else carry2a
        if (maxDigitCount < POW10_256_OFFSET) {
            // if carry == 1 then complement-and-increment else NOOP
            val negCarry2 = -carry2
            dw0 = (d0 xor negCarry2) - negCarry2
            dw1 = (d1 xor negCarry2) - (negCarry2 and ((dw0 or -dw0) shr 63).inv())
            dw2 = (d2 xor negCarry2) - (negCarry2 and ((dw1 or -dw1) shr 63).inv())
            dw3 = 0L
            setDigitCount192()
            return negCarry2
        }

        val d3a = x.dw3 - y.dw3
        val carry3a = if (compareUnsigned(d3a, x.dw3) > 0) 1L else 0L
        val d3 = d3a - carry2
        val carry3 = if (compareUnsigned(d3, d3a) > 0) 1L else carry3a

        // if carry == 1 then complement-and-increment else NOOP
        val negCarry3 = -carry3
        dw0 = (d0 xor negCarry3) - negCarry3
        dw1 = (d1 xor negCarry3) - (negCarry3 and ((dw0 or -dw0) shr 63).inv())
        dw2 = (d2 xor negCarry3) - (negCarry3 and ((dw1 or -dw1) shr 63).inv())
        dw3 = (d3 xor negCarry3) - (negCarry3 and ((dw2 or -dw2) shr 63).inv())
        setDigitCount256()
        return negCarry3
    }

    fun mul(x:Coeff, y:Coeff) {
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        if (x.digitCount <= 1) {
            if (x.digitCount == 0) {
                setZero()
                return
            }
            if (x.dw0 == 1L) {
                set(y)
                return
            }
        }
        when {
            ((y.dw3 or y.dw2) == 0L) -> {
                if (y.dw1 == 0L) {
                    if ((y.dw0 ushr 1) == 0L) {
                        if (y.dw0 == 1L) set(x) else setZero()
                        return
                    }
                    mulCoeff(this, x, y.digitCount, y.dw0)
                } else {
                    mulCoeff(this, x, y.digitCount, y.dw1, y.dw0)
                }
            }
            (y.dw3 == 0L) -> mulCoeff(this, x, y.digitCount, y.dw2, y.dw1, y.dw0)
            else -> mulCoeff(this, x, y.digitCount, y.dw3, y.dw2, y.dw1, y.dw0)
        }
    }

    fun fma(x:Coeff, y:Coeff, a:Coeff) {
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        assert(a.isValidDigitCount())
        if (x.digitCount <= 1) {
            if (x.digitCount == 0) {
                set(a)
                return
            }
            if (x.dw0 == 1L) {
                add(y, a)
                return
            }
        }
        if (y.digitCount <= 1) {
            if (y.digitCount == 0) {
                set(a)
                return
            }
            if (y.dw0 == 1L) {
                add(x, a)
                return
            }
        }
        when {
            ((y.dw3 or y.dw2) == 0L) -> {
                if (y.dw1 == 0L) {
                    if ((y.dw0 ushr 1) == 0L) {
                        if (y.dw0 == 1L) add(x, a) else set(a)
                        return
                    }
                    fmaCoeff(this, x, y.digitCount, y.dw0, a)
                } else {
                    fmaCoeff(this, x, y.digitCount, y.dw1, y.dw0, a)
                }
            }
            (y.dw3 == 0L) -> {
                fmaCoeff(this, x, y.digitCount, y.dw2, y.dw1, y.dw0, a)
            }
            else -> {
                fmaCoeff(this, x, y.digitCount, y.dw3, y.dw2, y.dw1, y.dw0, a)
            }
        }
    }

    fun scalePow10(x:Coeff, pow10:Int, sign:Boolean, ctx:Decimal128Context) {
        CoeffScalePow10.scalePow10Coeff(this, x, pow10, sign, ctx)
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