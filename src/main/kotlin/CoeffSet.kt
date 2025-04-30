package com.decimal128

import com.decimal128.CoeffDigitCount.setDigitCount64
import com.decimal128.CoeffDigitCount.setDigitCount128
import com.decimal128.CoeffDigitCount.setDigitCount192
import com.decimal128.CoeffDigitCount.setDigitCount256
import com.decimal128.CoeffDigitCount.setDigitCount
import com.decimal128.CoeffDigitCount.isValidDigitCount
import java.math.BigInteger

private const val MASK32 = 0xFFFFFFFFL

object CoeffSet {

    fun coeffSetZero(c: Coeff) {
        c.dw3 = 0L; c.dw2 = 0L; c.dw1 = 0L; c.dw0 = 0L; c.digitCount = 0
    }

    fun coeffSetOne(c: Coeff) {
        c.dw3 = 0L; c.dw2 = 0L; c.dw1 = 0L; c.dw0 = 1L; c.digitCount = 1
    }

    fun coeffSet(c: Coeff, dw0: Long) {
        c.dw3 = 0L; c.dw2 = 0L; c.dw1 = 0L
        c.dw0 = dw0
        setDigitCount64(c)
    }

    fun coeffSet(c: Coeff, dw1: Long, dw0: Long) {
        c.dw3 = 0L; c.dw2 = 0L
        c.dw1 = dw1;c.dw0 = dw0
        setDigitCount128(c)
    }

    fun coeffSet(c: Coeff, dw2: Long, dw1: Long, dw0: Long) {
        c.dw3 = 0L
        c.dw2 = dw2; c.dw1 = dw1; c.dw0 = dw0
        setDigitCount192(c)
    }

    fun coeffSet(c: Coeff, dw3: Long, dw2: Long, dw1: Long, dw0: Long) {
        c.dw3 = dw3; c.dw2 = dw2; c.dw1 = dw1;c.dw0 = dw0
        setDigitCount(c)
    }

    fun coeffSet(c: Coeff, digitCount: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) {
        c.digitCount = digitCount; c.dw3 = dw3; c.dw2 = dw2; c.dw1 = dw1;c.dw0 = dw0
        require(isValidDigitCount(c))
    }

    fun coeffSet(c: Coeff, bi: BigInteger) {
        require(bi.bitLength() <= 256)
        coeffSet(c, bi.shiftRight(192).toLong(), bi.shiftRight(128).toLong(), bi.shiftRight(64).toLong(), bi.toLong())
        setDigitCount(c)
    }

    fun coeffSet(c: Coeff, x:Coeff) {
        if (c != x) {
            c.digitCount = x.digitCount; c.dw3 = x.dw3; c.dw2 = x.dw2; c.dw1 = x.dw1; c.dw0 = x.dw0
        }
        assert(isValidDigitCount(c))
    }

    fun coeffSet(c: Coeff, str: String) = coeffSet(c, BigInteger(str))

    fun coeffSet(c: Coeff, x: LongArray, xOff: Int, xLen: Int) {
        coeffSetZero(c)
        if (xLen == 0)
            return
        var nonZeroIndex = xLen
        var nonZeroVal = 0L
        while (nonZeroVal == 0L && --nonZeroIndex >= 0) {
            nonZeroVal = x[xOff + nonZeroIndex]
        }
        when (nonZeroIndex) {
            -1 -> {}
            0 -> {
                c.dw0 = nonZeroVal; setDigitCount64(c)
            }

            1 -> {
                c.dw0 = x[xOff + 0]; c.dw1 = nonZeroVal; setDigitCount128(c)
            }

            2 -> {
                c.dw0 = x[xOff + 0]; c.dw1 = x[xOff + 1]; c.dw2 = nonZeroVal; setDigitCount192(c)
            }

            3 -> {
                c.dw0 = x[xOff + 0]; c.dw1 = x[xOff + 1];
                c.dw2 = x[xOff + 2]; c.dw3 = nonZeroVal; setDigitCount256(c)
            }

            else -> throw RuntimeException("overflow")
        }
    }

    fun coeffSet(c: Coeff, x: IntArray, xLen: Int) {
        coeffSetZero(c)
        if (xLen == 0)
            return
        var nonZeroIndex2 = (xLen + 1) ushr 1
        var nonZeroVal = if ((xLen and 1) != 0) (x[xLen - 1].toLong() and MASK32) else 0L
        while (nonZeroVal == 0L && --nonZeroIndex2 >= 0) {
            nonZeroVal = (x[nonZeroIndex2*2 + 1].toLong() shl 32) or (x[nonZeroIndex2*2].toLong() and MASK32)
        }
        when (nonZeroIndex2) {
            -1 -> {}
            0 -> {
                c.dw0 = nonZeroVal ; setDigitCount64(c)
            }

            1 -> {
                c.dw0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32);
                c.dw1 = nonZeroVal
                setDigitCount128(c)
            }

            2 -> {
                c.dw0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32);
                c.dw1 = (x[3].toLong() shl 32) or (x[2].toLong() and MASK32);
                c.dw2 = nonZeroVal
                setDigitCount192(c)
            }

            3 -> {
                c.dw0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32);
                c.dw1 = (x[3].toLong() shl 32) or (x[2].toLong() and MASK32);
                c.dw2 = (x[5].toLong() shl 32) or (x[4].toLong() and MASK32);
                c.dw3 = nonZeroVal;
                setDigitCount256(c)
            }

            else -> throw RuntimeException("overflow")
        }
    }

    fun coeffSetShiftRight(z: Coeff, x: Coeff, bitShift: Int) {
        if (x.digitCount < POW10_128_OFFSET) {
            val le63Mask = if (bitShift <= 63) -1L else 0L
            val r = (x.dw0 ushr bitShift) and le63Mask
            coeffSet(z, r)
            return
        }
        val wholeDwordCount = bitShift ushr 6
        val innerShift = bitShift and 0x3F
        val nonZeroMask = -innerShift.toLong() shr 63
        val leftShift = -innerShift
        when (wholeDwordCount) {
            0 -> {
                z.dw0 = (nonZeroMask and (x.dw1 shl leftShift)) or (x.dw0 ushr innerShift)
                z.dw1 = (nonZeroMask and (x.dw2 shl leftShift)) or (x.dw1 ushr innerShift)
                z.dw2 = (nonZeroMask and (x.dw3 shl leftShift)) or (x.dw2 ushr innerShift)
                z.dw3 = x.dw3 ushr innerShift
                if (innerShift <= 3)
                    // FIXME
                setDigitCount(z)
            }

            1 -> {
                z.dw0 = (nonZeroMask and (x.dw2 shl leftShift)) or (x.dw1 ushr innerShift)
                z.dw1 = (nonZeroMask and (x.dw3 shl leftShift)) or (x.dw2 ushr innerShift)
                z.dw2 = x.dw3 ushr innerShift
                z.dw3 = 0L
                setDigitCount192(z)
            }

            2 -> {
                z.dw0 = (nonZeroMask and (x.dw3 shl leftShift)) or (x.dw2 ushr innerShift)
                z.dw1 = x.dw3 ushr innerShift
                z.dw2 = 0L; z.dw3 = 0L
                setDigitCount128(z)
            }

            3 -> {
                z.dw0 = x.dw3 ushr innerShift
                z.dw1 = 0L; z.dw2 = 0L; z.dw3 = 0L
                setDigitCount64(z)
            }

            else -> coeffSetZero(z)
        }
    }

    fun coeffSetShiftRight(c: Coeff, x: LongArray, xOff: Int, xLen: Int, bitCount: Int) {

        coeffSetZero(c)
        // strip leading zeros from x
        var nonZeroLen = xLen
        while (nonZeroLen > 0 && x[xOff + nonZeroLen - 1] == 0L)
            --nonZeroLen

        val dwordShift = bitCount ushr 6
        val innerShift = bitCount and 0x3F
        val innerShiftNonZeroMask = -innerShift.toLong() shr 63
        val newLen = nonZeroLen - dwordShift
        val shiftOff = xOff + dwordShift
        val leftShift = -innerShift // only bottom 6 bits are used
        when (newLen) {
            0 -> {}
            1 -> {
                c.dw0 = x[shiftOff + 0] ushr innerShift
                setDigitCount64(c)
            }

            2 -> {
                c.dw0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                c.dw1 = x[shiftOff + 1] ushr innerShift
                setDigitCount128(c)
            }

            3 -> {
                c.dw0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                c.dw1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl leftShift)) or (x[shiftOff + 1] ushr innerShift)
                c.dw2 = x[shiftOff + 2] ushr innerShift
                setDigitCount192(c)
            }

            4 -> {
                c.dw0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                c.dw1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl leftShift)) or (x[shiftOff + 1] ushr innerShift)
                c.dw2 = (innerShiftNonZeroMask and (x[shiftOff + 3] shl leftShift)) or (x[shiftOff + 2] ushr innerShift)
                c.dw3 = x[shiftOff + 3] ushr innerShift
                setDigitCount256(c)
            }

            5 -> {
                c.dw0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                c.dw1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl leftShift)) or (x[shiftOff + 1] ushr innerShift)
                c.dw2 = (innerShiftNonZeroMask and (x[shiftOff + 3] shl leftShift)) or (x[shiftOff + 2] ushr innerShift)
                c.dw3 = (innerShiftNonZeroMask and (x[shiftOff + 4] shl leftShift)) or (x[shiftOff + 3] ushr innerShift)
                val dw4 = x[shiftOff + 4] ushr innerShift
                if (dw4 != 0L)
                    throw RuntimeException("overflow")
                setDigitCount256(c)
            }

            else -> {
                throw RuntimeException("overflow")
            }
        }
    }

    fun coeffSetShiftRight(c: Coeff, x: IntArray, xLen: Int, s: Int) {
        assert(s < 32)
        if (s == 0) {
            coeffSet(c, x, xLen)
            return
        }
        coeffSetZero(c)
        if (xLen == 0)
            return
        var nonZeroIndex2 = (xLen + 1) ushr 1
        var nonZeroVal = if ((xLen and 1) != 0) ((x[xLen - 1].toLong() and MASK32) shr s) else 0L
        while (nonZeroVal == 0L && --nonZeroIndex2 >= 0) {
            nonZeroVal = ((x[nonZeroIndex2*2 + 1].toLong() shl 32) or (x[nonZeroIndex2*2].toLong() and MASK32)) shr s
        }
        when (nonZeroIndex2) {
            -1 -> {}
            0 -> {
                c.dw0 = nonZeroVal ; setDigitCount64(c)
            }

            1 -> {
                c.dw0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                c.dw1 = nonZeroVal
                setDigitCount128(c)
            }

            2 -> {
                c.dw0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                c.dw1 = ((x[4] shl -s).toLong() shl 32) or (((x[3].toLong() shl 32) or (x[2].toLong() and MASK32)) shr s)
                c.dw2 = nonZeroVal
                setDigitCount192(c)
            }

            3 -> {
                c.dw0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                c.dw1 = ((x[4] shl -s).toLong() shl 32) or (((x[3].toLong() shl 32) or (x[2].toLong() and MASK32)) shr s)
                c.dw2 = ((x[6] shl -s).toLong() shl 32) or (((x[5].toLong() shl 32) or (x[4].toLong() and MASK32)) shr s)
                c.dw3 = nonZeroVal;
                setDigitCount256(c)
            }

            else -> throw RuntimeException("overflow")
        }
    }

}
