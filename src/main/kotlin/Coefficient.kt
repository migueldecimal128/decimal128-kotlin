package com.decimal128

import java.math.BigInteger
import java.lang.Long.compareUnsigned
import java.lang.Math.unsignedMultiplyHigh
import com.decimal128.MulCoefficient.Companion.mulCoeff

private const val SIGNBIT = Long.MIN_VALUE


class Coefficient(var dw3: Long, var dw2: Long, var dw1: Long, var dw0: Long) {

    constructor(dw2: Long, dw1: Long, dw0: Long) : this(0L, dw2, dw1, dw0)
    constructor(dw1: Long, dw0: Long) : this(0L, 0L, dw1, dw0)
    constructor(dw0: Long) : this(0L, 0L, 0L, dw0)
    constructor(w0: Int) : this(0L, 0L, 0L, w0.toLong() and 0xFFFFFFFFL)
    constructor() : this(0L, 0L, 0L, 0L)
    constructor(bi: BigInteger) : this(bi.shiftRight(192).toLong(), bi.shiftRight(128).toLong(), bi.shiftRight(64).toLong(), bi.toLong()) {
        require(bi.bitLength() <= 256)
    }
    constructor(str: String) : this(BigInteger(str))
    constructor(c: Coefficient): this(c.dw3, c.dw2, c.dw1, c.dw0)

    var digitCount = 0

    init { recalcDigitCount256orLess() }

    fun setZero() {
        dw3 = 0L; dw2 = 0L; dw1 = 0L; dw0 = 0L; digitCount = 0
    }

    fun isZero() = digitCount == 0

    private fun recalcDigitCountOnly64() = recalcDigitCountOnly64(this)
    private fun recalcDigitCountOnly128() = recalcDigitCountOnly128(this)
    private fun recalcDigitCountOnly192() = recalcDigitCountOnly192(this)
    private fun recalcDigitCountOnly256() = recalcDigitCountOnly256(this)

    private fun tweakDigitCountOnly64() = tweakDigitCountOnly64(this)
    private fun tweakDigitCountOnly128() = tweakDigitCountOnly128(this)
    private fun tweakDigitCountOnly192() = tweakDigitCountOnly192(this)
    private fun tweakDigitCountOnly256() = tweakDigitCountOnly256(this)

    // these are used by subtraction where the number of digits can drop dramatically
    fun recalcDigitCount128orLess() = if (dw1 == 0L) recalcDigitCountOnly64() else recalcDigitCountOnly128()

    fun recalcDigitCount192orLess() {
        when {
            ((dw1 or dw2) == 0L) -> recalcDigitCountOnly64()
            (dw2 == 0L) -> recalcDigitCountOnly128()
            else -> recalcDigitCountOnly192()
        }
    }

    fun recalcDigitCount256orLess() {
        if ((dw3 or dw2) == 0L)
            if (dw1 == 0L) recalcDigitCountOnly64() else recalcDigitCountOnly128()
        else
            if (dw3 == 0L) recalcDigitCountOnly192() else recalcDigitCountOnly256()
    }

    fun isValidDigitCount() : Boolean {
        val prevDigitCount = digitCount
        recalcDigitCount256orLess()
        val t = digitCount
        digitCount = prevDigitCount
        return t == prevDigitCount
    }

    fun compareTo(other: Coefficient) : Int {
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

    fun EQ(other: Coefficient) : Boolean {
        assert(isValidDigitCount())
        assert(other.isValidDigitCount())
        return (digitCount == other.digitCount) && (dw0 == other.dw0) && (dw1 == other.dw1) && (dw2 == other.dw2) && (dw3 == other.dw3)
    }

    fun NE(other: Coefficient) : Boolean = !EQ(other)

    fun GE(other: Coefficient) : Boolean = !LT(other)

    fun GT(other: Coefficient) : Boolean {
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

    fun LE(other: Coefficient) : Boolean = !GT(other)

    fun LT(other: Coefficient) : Boolean {
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

    fun add(x: Coefficient, y: Coefficient) {
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
            digitCount = maxDigitCount
            tweakDigitCountOnly64()
            assert(isValidDigitCount())
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
            digitCount = maxDigitCount
            tweakDigitCountOnly128()
            assert(isValidDigitCount())
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
            digitCount = maxDigitCount
            tweakDigitCountOnly192()
            assert(isValidDigitCount())
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
        digitCount = maxDigitCount
        tweakDigitCountOnly256()
        assert(isValidDigitCount())
    }

    // absolute difference
    // if minuend < subtrahend then negate to return positive result
    fun absDiff(x: Coefficient, y: Coefficient) : Long { // minuend - subtrahend
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
            digitCount = loDigitCount
            when {
                (dw0 != 0L) -> if (digitCountDiff >= 2) tweakDigitCountOnly64() else recalcDigitCountOnly64()
                else        -> digitCount = 0
            }
            assert(isValidDigitCount())
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
            digitCount = loDigitCount
            when {
                (dw1 != 0L) -> if (digitCountDiff >= 2) tweakDigitCountOnly128() else recalcDigitCountOnly128()
                (dw0 != 0L) -> recalcDigitCountOnly64()
                else        -> digitCount = 0
            }
            assert(isValidDigitCount())
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
            digitCount = loDigitCount
            when {
                (dw2 != 0L) -> if (digitCountDiff >= 2) tweakDigitCountOnly192() else recalcDigitCountOnly192()
                (dw1 != 0L) -> recalcDigitCountOnly128()
                (dw0 != 0L) -> recalcDigitCountOnly64()
                else        -> digitCount = 0
            }
            assert(isValidDigitCount())
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
        digitCount = loDigitCount
        when {
            (dw3 != 0L) -> if (digitCountDiff >= 2) tweakDigitCountOnly256() else recalcDigitCountOnly256()
            (dw2 != 0L) -> recalcDigitCountOnly192()
            (dw1 != 0L) -> recalcDigitCountOnly128()
            (dw0 != 0L) -> recalcDigitCountOnly64()
            else        -> digitCount = 0
        }
        assert(isValidDigitCount())
        return negCarry3
    }

    fun mul(x: Coefficient, y:Coefficient) {
        mul(x, y.digitCount, y.dw3, y.dw2, y.dw1, y.dw0)
    }

    fun mul(x: Coefficient, yDigitCount: Int, yDw3: Long, yDw2: Long, yDw1: Long, yDw0: Long) {
        assert(isValidDigitCount())
        assert(x.isValidDigitCount())
        assert(calcDigitCount(yDw3, yDw2, yDw1, yDw0) == yDigitCount)
        if ((x.digitCount or yDigitCount) == 0) {
            setZero();
            return
        }
        when {
            (x.dw3 != 0L) ->
                if ((yDw3 or yDw2 or yDw1) == 0L) {
                    mulCoeff(this, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0, yDigitCount, yDw0)
                    return
                }
            (x.dw2 != 0L) ->
                if ((yDw3 or yDw2) == 0L) {
                    if (yDw1 != 0L)
                        mulCoeff(this, x.digitCount, x.dw2, x.dw1, x.dw0, yDigitCount, yDw1, yDw0)
                    else
                        mulCoeff(this, x.digitCount, x.dw2, x.dw1, x.dw0, yDigitCount, yDw0)
                    return
                }
            (x.dw1 != 0L) -> {
                if (yDw3 == 0L) {
                    if (yDw2 != 0L)
                        mulCoeff(this, yDigitCount, yDw2, yDw1, yDw0, x.digitCount, x.dw1, x.dw0)
                    else if (yDw1 != 0L)
                        mulCoeff(this, x.digitCount, x.dw1, x.dw0, yDigitCount, yDw1, yDw0)
                    else
                        mulCoeff(this, x.digitCount, x.dw1, x.dw0, yDigitCount, yDw0)
                    return
                }
            }
            else -> {
                when {
                    (yDw3 != 0L) -> mulCoeff(this, yDigitCount, yDw3, yDw2, yDw1, yDw0, x.digitCount, x.dw0)
                    (yDw2 != 0L) -> mulCoeff(this, yDigitCount, yDw2, yDw1, yDw0, x.digitCount, x.dw0)
                    (yDw1 != 0L) -> mulCoeff(this, yDigitCount, yDw1, yDw0, x.digitCount, x.dw0)
                    else -> mulCoeff(this, yDigitCount, yDw0, x.digitCount, x.dw0)
                }
                return
            }
        }
        throw RuntimeException("coefficient multiply overflow")

    }

    fun mul_x(x: Coefficient, y:Coefficient) {
        assert(isValidDigitCount())
        assert(x.isValidDigitCount())
        assert(y.isValidDigitCount())
        if (x.digitCount == 0 || y.digitCount == 0) {
            setZero()
            return
        }
        val maxMulDigitCount = x.digitCount + y.digitCount
        val loDigitCount = maxMulDigitCount - 1
        val pp00Lo = x.dw0 * y.dw0
        val dw0T = pp00Lo
        if (maxMulDigitCount < POW10_128_OFFSET) {
            dw0 = dw0T
            dw1 = 0L; dw2 = 0L; dw3 = 0L
            digitCount = loDigitCount
            tweakDigitCountOnly64()
            assert(isValidDigitCount())
            return
        }
        val pp00Hi = unsignedMultiplyHigh(x.dw0, y.dw0)
        val pp01Lo = x.dw0 * y.dw1
        val pp10Lo = x.dw1 * y.dw0
        if (maxMulDigitCount < POW10_192_OFFSET) {
            dw0 = dw0T
            dw1 = pp00Hi + pp01Lo + pp10Lo // no carry possible because of maxMulDigitCount
            dw2 = 0L; dw3 = 0L;
            digitCount = loDigitCount
            if (dw1 == 0L)
                tweakDigitCountOnly64()
            else
                tweakDigitCountOnly128()
            assert(isValidDigitCount())
            return
        }
        val pp01Hi = unsignedMultiplyHigh(x.dw0, y.dw1)
        val pp10Hi = unsignedMultiplyHigh(x.dw1, y.dw0)
        val pp11Lo = x.dw1 * y.dw1
        val pp02Lo = x.dw0 * y.dw2
        val pp20Lo = x.dw2 * y.dw0
        val (carry1, dw1T) = sumU64(pp00Hi, pp01Lo, pp10Lo)
        if (maxMulDigitCount < POW10_256_OFFSET) {
            dw0 = dw0T
            dw1 = dw1T
            dw2 = carry1 + pp01Hi + pp10Hi + pp11Lo + pp02Lo + pp20Lo
            dw3 = 0L;
            digitCount = loDigitCount
            if (dw2 == 0L)
                tweakDigitCountOnly128()
            else
                tweakDigitCountOnly192()
            assert(isValidDigitCount())
            return
        }
        val pp11Hi = unsignedMultiplyHigh(x.dw1, y.dw1)
        val pp02Hi = unsignedMultiplyHigh(x.dw0, y.dw2)
        val pp20Hi = unsignedMultiplyHigh(x.dw2, y.dw0)
        val pp12Lo = x.dw1 * y.dw2
        val pp21Lo = x.dw2 * y.dw1
        val pp03Lo = x.dw0 * y.dw3
        val pp30Lo = x.dw3 * y.dw0
        val (carry2, dw2T) = sumU64(carry1, pp01Hi, pp10Hi, pp11Lo, pp02Lo, pp20Lo)

        if (maxMulDigitCount < POW10_MAX_OFFSET) {
            dw0 = dw0T
            dw1 = dw1T
            dw2 = dw2T
            dw3 = carry2 + pp11Hi + pp02Hi + pp20Hi + pp12Lo + pp21Lo + pp03Lo + pp30Lo
            digitCount = loDigitCount
            if (dw3 == 0L)
                tweakDigitCountOnly192()
            else
                tweakDigitCountOnly256()
            assert(isValidDigitCount())
            return
        }
        val pp12Hi = unsignedMultiplyHigh(x.dw1, y.dw2)
        val pp21Hi = unsignedMultiplyHigh(x.dw2, y.dw1)
        val pp03Hi = unsignedMultiplyHigh(x.dw0, y.dw3)
        val pp30Hi = unsignedMultiplyHigh(x.dw3, y.dw0)
        val pp22Lo = x.dw2 * y.dw2
        val (carry3, dw3T) = sumU64(carry2, pp11Hi, pp02Hi, pp20Hi, pp12Lo, pp21Lo, pp03Lo, pp30Lo)
        val dw4 = carry3 + pp12Hi + pp21Hi + pp03Hi + pp30Hi + pp22Lo
        if (dw4 == 0L) {
            // when you multiply (10**256-1 * 1) you have 78+1 = 79, but result is 78
            assert (maxMulDigitCount == 78 || maxMulDigitCount == 79)
            dw0 = dw0T
            dw1 = dw1T
            dw2 = dw2T
            dw3 = dw3T
            digitCount = loDigitCount
            tweakDigitCountOnly256()
            assert(isValidDigitCount())
            return
        }
        throw RuntimeException("coefficient multiply overflow")
    }

    fun mutateScalePow10(pow10: Int) {
        if (digitCount == 0 || pow10 == 0)
            return
        if (pow10 >= 0) {
            //println("mutateScalePow10 $this $pow10")
            // note that this is a lie
            // digitCount is actually pow10 + 1
            // but this works OK because multiplying by a power of 10 will not cause the product digitCount to increase by more than pow10
            val pow10DigitCount = pow10
            val finalDigitCount = digitCount + pow10DigitCount
            if (finalDigitCount > POW10_MAX_OFFSET)
                throw RuntimeException("mutateScalePow10 overflow digitCount:$digitCount + pow10:$pow10 = finalDigitCount:$finalDigitCount")
            when {
                (pow10 < POW10_128_OFFSET) ->
                { val index = pow10; mulCoeff(this, this, pow10DigitCount, POW10[index + 0]) }
                (pow10 < POW10_192_OFFSET) ->
                { val index = POW10_128_DWORD_INDEX + 2*(pow10 - POW10_128_OFFSET);
                    mulCoeff(this, this, pow10DigitCount, POW10[index + 1], POW10[index + 0]) }
                (pow10 < POW10_256_OFFSET) ->
                { val index = POW10_192_DWORD_INDEX + 3*(pow10 - POW10_192_OFFSET);
                    mulCoeff(this, this, pow10DigitCount, POW10[index + 2], POW10[index + 1], POW10[index + 0]) }
                (pow10 < POW10_MAX_OFFSET) ->
                { val index = POW10_256_DWORD_INDEX + 4*(pow10 - POW10_256_OFFSET);
                    mulCoeff(this, this, pow10DigitCount, POW10[index + 3], POW10[index + 2], POW10[index + 1], POW10[index + 0]) }
                else -> throw RuntimeException("?que?")
            }
            digitCount = finalDigitCount
            if (! isValidDigitCount()) {
                println("mutateScalePow10 $this $pow10")
                println("foo!")
            }
            assert(isValidDigitCount())
        }
    }

    fun mutateBitShiftRightX(shift: Int) {
        val dwordShift = shift and 0x3F.inv()
        val bitShiftRight = shift and 0x3F
        if (bitShiftRight == 0) {
            if (dwordShift >= 1) {
                if (dwordShift == 1) {
                    dw0 = dw1; dw1 = dw2; dw2 = dw3
                } else {
                    if (dwordShift == 2) {
                        dw0 = dw2; dw1 = dw3
                    } else {
                        if (dwordShift == 3) {
                            dw0 = dw3
                        } else {
                            dw0 = 0L
                        }
                        dw1 = 0L
                    }
                    dw2 = 0L;
                }
                dw3 = 0L
            }
        } else {
            val bitShiftLeft = 64 - bitShiftRight
            if (dwordShift == 0) {
                dw0 = (dw1 shl bitShiftLeft) or (dw0 ushr bitShiftRight)
                dw1 = (dw2 shl bitShiftLeft) or (dw1 ushr bitShiftRight)
                dw2 = (dw3 shl bitShiftLeft) or (dw2 ushr bitShiftRight)
                dw3 = dw3 ushr bitShiftRight
            } else {
                if (dwordShift == 1) {
                    dw0 = (dw2 shl bitShiftLeft) or (dw1 ushr bitShiftRight)
                    dw1 = (dw3 shl bitShiftLeft) or (dw2 ushr bitShiftRight)
                    dw2 = dw3 ushr bitShiftRight
                } else {
                    if (dwordShift == 2) {
                        dw0 = (dw3 shl bitShiftLeft) or (dw2 ushr bitShiftRight)
                        dw1 = dw3 ushr bitShiftRight
                    } else {
                        if (dwordShift == 3) {
                            dw0 = dw3 ushr bitShiftRight
                        } else {
                            dw0 = 0L
                        }
                        dw1 = 0L
                    }
                    dw2 = 0L;
                }
                dw3 = 0L
            }
        }
    }

    fun set(dw0: Long) {
        this.dw3 = 0L; this.dw2 = 0L; this.dw1 = 0L
        this.dw0 = dw0;
        recalcDigitCountOnly64()
        assert(isValidDigitCount())
    }

    fun set(dw1: Long, dw0: Long) {
        this.dw3 = 0L; this.dw2 = 0L;
        this.dw1 = dw1;this.dw0 = dw0;
        recalcDigitCount128orLess()
        assert(isValidDigitCount())
    }

    fun set(dw2: Long, dw1: Long, dw0: Long) {
        this.dw3 = 0L;
        this.dw2 = dw2; this.dw1 = dw1;this.dw0 = dw0;
        recalcDigitCount192orLess()
        assert(isValidDigitCount())
    }

    fun set(dw3: Long, dw2: Long, dw1: Long, dw0: Long) {
        this.dw3 = dw3; this.dw2 = dw2; this.dw1 = dw1;this.dw0 = dw0;
        recalcDigitCount256orLess()
    }

    fun set(digitCount: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) {
        this.digitCount = digitCount; this.dw3 = dw3; this.dw2 = dw2; this.dw1 = dw1;this.dw0 = dw0;
        assert(isValidDigitCount())
    }

    fun set(bi: BigInteger) {
        require (bi.bitLength() <= 256)
        set(bi.shiftRight(192).toLong(), bi.shiftRight(128).toLong(), bi.shiftRight(64).toLong(), bi.toLong())
        recalcDigitCount256orLess();
    }

    fun set(c: Coefficient) {
        digitCount = c.digitCount; dw3 = c.dw3; dw2 = c.dw2; dw1 = c.dw1; dw0 = c.dw0
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