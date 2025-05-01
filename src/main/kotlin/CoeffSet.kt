package com.decimal128

import com.decimal128.CoeffDigitLen.setDigitLen64
import com.decimal128.CoeffDigitLen.setDigitLen128
import com.decimal128.CoeffDigitLen.setDigitLen192
import com.decimal128.CoeffDigitLen.setDigitLen256
import com.decimal128.CoeffDigitLen.setDigitLen
import com.decimal128.CoeffDigitLen.isValidDigitLen
import java.math.BigInteger

private const val MASK32 = 0xFFFFFFFFL

object CoeffSet {

    fun coeffSetZero(c: Coeff) {
        c.dw3 = 0L; c.dw2 = 0L; c.dw1 = 0L; c.dw0 = 0L; c.digitLen = 0
    }

    fun coeffSetOne(c: Coeff) {
        c.dw3 = 0L; c.dw2 = 0L; c.dw1 = 0L; c.dw0 = 1L; c.digitLen = 1
    }

    fun coeffSet(c: Coeff, dw0: Long) {
        c.setCoeff64(dw0)
    }

    fun coeffSet(c: Coeff, dw1: Long, dw0: Long) {
        c.setCoeff128(dw1, dw0)
    }

    fun coeffSet(c: Coeff, dw2: Long, dw1: Long, dw0: Long) {
        c.setCoeff192(dw2, dw1, dw0)
    }

    fun coeffSet(c: Coeff, dw3: Long, dw2: Long, dw1: Long, dw0: Long) {
        c.setCoeff256(dw3,dw2, dw1, dw0)
    }

    fun coeffSet(c: Coeff, bi: BigInteger) {
        require(bi.bitLength() <= 256)
        coeffSet(c, bi.shiftRight(192).toLong(), bi.shiftRight(128).toLong(), bi.shiftRight(64).toLong(), bi.toLong())
        setDigitLen(c)
    }

    fun coeffSet(c: Coeff, x:Coeff) {
        if (c != x) {
            c.digitLen = x.digitLen; c.dw3 = x.dw3; c.dw2 = x.dw2; c.dw1 = x.dw1; c.dw0 = x.dw0
        }
        assert(isValidDigitLen(c))
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
                c.dw0 = nonZeroVal; setDigitLen64(c)
            }

            1 -> {
                c.dw0 = x[xOff + 0]; c.dw1 = nonZeroVal; setDigitLen128(c)
            }

            2 -> {
                c.dw0 = x[xOff + 0]; c.dw1 = x[xOff + 1]; c.dw2 = nonZeroVal; setDigitLen192(c)
            }

            3 -> {
                c.dw0 = x[xOff + 0]; c.dw1 = x[xOff + 1];
                c.dw2 = x[xOff + 2]; c.dw3 = nonZeroVal; setDigitLen256(c)
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
                c.dw0 = nonZeroVal ; setDigitLen64(c)
            }

            1 -> {
                c.dw0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32);
                c.dw1 = nonZeroVal
                setDigitLen128(c)
            }

            2 -> {
                c.dw0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32);
                c.dw1 = (x[3].toLong() shl 32) or (x[2].toLong() and MASK32);
                c.dw2 = nonZeroVal
                setDigitLen192(c)
            }

            3 -> {
                c.dw0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32);
                c.dw1 = (x[3].toLong() shl 32) or (x[2].toLong() and MASK32);
                c.dw2 = (x[5].toLong() shl 32) or (x[4].toLong() and MASK32);
                c.dw3 = nonZeroVal;
                setDigitLen256(c)
            }

            else -> throw RuntimeException("overflow")
        }
    }

    fun coeffSetShiftRight(z: Coeff, x: Coeff, bitShift: Int) {
        if (x.digitLen < POW10_128_OFFSET) {
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
                if (innerShift <= 3) {
                    //FIXME less than one digit change going on here
                    // tweak the digit count instead of recalculating
                }
                setDigitLen(z)
            }

            1 -> {
                z.dw0 = (nonZeroMask and (x.dw2 shl leftShift)) or (x.dw1 ushr innerShift)
                z.dw1 = (nonZeroMask and (x.dw3 shl leftShift)) or (x.dw2 ushr innerShift)
                z.dw2 = x.dw3 ushr innerShift
                z.dw3 = 0L
                setDigitLen192(z)
            }

            2 -> {
                z.dw0 = (nonZeroMask and (x.dw3 shl leftShift)) or (x.dw2 ushr innerShift)
                z.dw1 = x.dw3 ushr innerShift
                z.dw2 = 0L; z.dw3 = 0L
                setDigitLen128(z)
            }

            3 -> {
                z.dw0 = x.dw3 ushr innerShift
                z.dw1 = 0L; z.dw2 = 0L; z.dw3 = 0L
                setDigitLen64(z)
            }

            else -> coeffSetZero(z)
        }
    }

    fun coeffSetShiftLeft(z: Coeff, x: Coeff, s: Int) {
        val wholeDwordCount = s ushr 6
        val innerL = s and 0x3F
        val nonZeroMask = -innerL.toLong() shr 63
        val innerR = -innerL
        val topBitsMask = nonZeroMask shl innerR
        //FIXME need to check for non-zero bit overflow out the top
        when (wholeDwordCount) {
            0 -> {
                if (x.dw3 and topBitsMask != 0L)
                    throw RuntimeException("coefficientOverflow")
                z.dw3 = (x.dw3 shl innerL) or ((x.dw2 ushr innerR) and nonZeroMask)
                z.dw2 = (x.dw2 shl innerL) or ((x.dw1 ushr innerR) and nonZeroMask)
                z.dw1 = (x.dw1 shl innerL) or ((x.dw0 ushr innerR) and nonZeroMask)
                z.dw0 = (x.dw0 shl innerL)
                if (innerL <= 3) {
                    //FIXME less than one digit change going on here
                    // tweak the digit count instead of recalculating
                }
            }

            1 -> {
                if ((x.dw3 or (x.dw2 and topBitsMask)) != 0L)
                    throw RuntimeException("coefficientOverflow")
                z.dw3 = (x.dw2 shl innerL) or ((x.dw1 ushr innerR) and nonZeroMask)
                z.dw2 = (x.dw1 shl innerL) or ((x.dw0 ushr innerR) and nonZeroMask)
                z.dw1 = (x.dw0 shl innerL)
                z.dw0 = 0L
            }

            2 -> {
                if ((x.dw3 or x.dw2 or (x.dw1 and topBitsMask)) != 0L)
                    throw RuntimeException("coefficientOverflow")
                z.dw3 = (x.dw1 shl innerL) or ((x.dw0 ushr innerR) and nonZeroMask)
                z.dw2 = (x.dw0 shl innerL)
                z.dw1 = 0L
                z.dw0 = 0L
            }

            3 -> {
                if ((x.dw3 or x.dw2 or x.dw1 or (x.dw0 and topBitsMask)) != 0L)
                    throw RuntimeException("coefficientOverflow")
                z.dw3 = (x.dw0 shl innerL)
                z.dw2 = 0L
                z.dw1 = 0L
                z.dw0 = 0L
            }

            else -> {
                coeffSetZero(z)
                return
            }
        }
        setDigitLen(z)
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
                setDigitLen64(c)
            }

            2 -> {
                c.dw0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                c.dw1 = x[shiftOff + 1] ushr innerShift
                setDigitLen128(c)
            }

            3 -> {
                c.dw0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                c.dw1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl leftShift)) or (x[shiftOff + 1] ushr innerShift)
                c.dw2 = x[shiftOff + 2] ushr innerShift
                setDigitLen192(c)
            }

            4 -> {
                c.dw0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                c.dw1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl leftShift)) or (x[shiftOff + 1] ushr innerShift)
                c.dw2 = (innerShiftNonZeroMask and (x[shiftOff + 3] shl leftShift)) or (x[shiftOff + 2] ushr innerShift)
                c.dw3 = x[shiftOff + 3] ushr innerShift
                setDigitLen256(c)
            }

            5 -> {
                c.dw0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                c.dw1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl leftShift)) or (x[shiftOff + 1] ushr innerShift)
                c.dw2 = (innerShiftNonZeroMask and (x[shiftOff + 3] shl leftShift)) or (x[shiftOff + 2] ushr innerShift)
                c.dw3 = (innerShiftNonZeroMask and (x[shiftOff + 4] shl leftShift)) or (x[shiftOff + 3] ushr innerShift)
                val dw4 = x[shiftOff + 4] ushr innerShift
                if (dw4 != 0L)
                    throw RuntimeException("overflow")
                setDigitLen256(c)
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
                c.dw0 = nonZeroVal ; setDigitLen64(c)
            }

            1 -> {
                c.dw0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                c.dw1 = nonZeroVal
                setDigitLen128(c)
            }

            2 -> {
                c.dw0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                c.dw1 = ((x[4] shl -s).toLong() shl 32) or (((x[3].toLong() shl 32) or (x[2].toLong() and MASK32)) shr s)
                c.dw2 = nonZeroVal
                setDigitLen192(c)
            }

            3 -> {
                c.dw0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                c.dw1 = ((x[4] shl -s).toLong() shl 32) or (((x[3].toLong() shl 32) or (x[2].toLong() and MASK32)) shr s)
                c.dw2 = ((x[6] shl -s).toLong() shl 32) or (((x[5].toLong() shl 32) or (x[4].toLong() and MASK32)) shr s)
                c.dw3 = nonZeroVal;
                setDigitLen256(c)
            }

            else -> throw RuntimeException("overflow")
        }
    }

}
