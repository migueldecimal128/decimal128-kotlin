package com.decimal128

import com.decimal128.CoeffDigitLen.isValidDigitLen
import java.math.BigInteger

private const val MASK32 = 0xFFFFFFFFL

object CoeffSet {

    fun coeffSet(c: Coeff, bi: BigInteger) {
        require(bi.bitLength() <= 256)
        c.setCoeff256(bi.shiftRight(192).toLong(), bi.shiftRight(128).toLong(), bi.shiftRight(64).toLong(), bi.toLong())
    }

    fun coeffSet(c: Coeff, x:Coeff) {
        c.set(x)
    }

    fun coeffSet(c: Coeff, str: String) = coeffSet(c, BigInteger(str))

    fun coeffSet(c: Coeff, x: LongArray, xOff: Int, xLen: Int) {
        c.setZero()
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
                val c0 = nonZeroVal
                c.setCoeff64(c0)
            }

            1 -> {
                val c0 = x[xOff + 0]
                val c1 = nonZeroVal
                c.setCoeff128(c1, c0)
            }

            2 -> {
                val c0 = x[xOff + 0]
                val c1 = x[xOff + 1]
                val c2 = nonZeroVal
                c.setCoeff192(c2, c1, c0)
            }

            3 -> {
                val c0 = x[xOff + 0]
                val c1 = x[xOff + 1]
                val c2 = x[xOff + 2]
                val c3 = nonZeroVal
                c.setCoeff256(c3, c2, c1, c0)
            }

            else -> throw RuntimeException("overflow")
        }
    }

    fun coeffSet(c: Coeff, x: IntArray, xLen: Int) {
        c.setZero()
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
                val c0 = nonZeroVal
                c.setCoeff64(c0)
            }

            1 -> {
                val c0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32)
                val c1 = nonZeroVal
                c.setCoeff128(c1, c0)
            }

            2 -> {
                val c0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32)
                val c1 = (x[3].toLong() shl 32) or (x[2].toLong() and MASK32)
                val c2 = nonZeroVal
                c.setCoeff192(c2, c1, c0)
            }

            3 -> {
                val c0 = (x[1].toLong() shl 32) or (x[0].toLong() and MASK32);
                val c1 = (x[3].toLong() shl 32) or (x[2].toLong() and MASK32);
                val c2 = (x[5].toLong() shl 32) or (x[4].toLong() and MASK32);
                val c3 = nonZeroVal;
                c.setCoeff256(c3, c2, c1, c0)
            }

            else -> throw RuntimeException("overflow")
        }
    }

    fun coeffSetShiftRight(z: Coeff, x: Coeff, bitShift: Int) {
        if (x.digitLen < POW10_128_OFFSET) {
            val le63Mask = if (bitShift <= 63) -1L else 0L
            val r = (x.dw0 ushr bitShift) and le63Mask
            z.setCoeff64(r)
            return
        }
        val wholeDwordCount = bitShift ushr 6
        val innerShift = bitShift and 0x3F
        val nonZeroMask = -innerShift.toLong() shr 63
        val leftShift = -innerShift
        when (wholeDwordCount) {
            0 -> {
                val z0 = (nonZeroMask and (x.dw1 shl leftShift)) or (x.dw0 ushr innerShift)
                val z1 = (nonZeroMask and (x.dw2 shl leftShift)) or (x.dw1 ushr innerShift)
                val z2 = (nonZeroMask and (x.dw3 shl leftShift)) or (x.dw2 ushr innerShift)
                val z3 = x.dw3 ushr innerShift
                if (innerShift <= 3) {
                    //FIXME less than one digit change going on here
                    // tweak the digit count instead of recalculating
                }
                z.setCoeff256(z3, z2, z1, z0)
            }

            1 -> {
                val z0 = (nonZeroMask and (x.dw2 shl leftShift)) or (x.dw1 ushr innerShift)
                val z1 = (nonZeroMask and (x.dw3 shl leftShift)) or (x.dw2 ushr innerShift)
                val z2 = x.dw3 ushr innerShift
                z.setCoeff192(z2, z1, z0)
            }

            2 -> {
                val z0 = (nonZeroMask and (x.dw3 shl leftShift)) or (x.dw2 ushr innerShift)
                val z1 = x.dw3 ushr innerShift
                z.setCoeff128(z1, z0)
            }

            3 -> {
                val z0 = x.dw3 ushr innerShift
                z.setCoeff64(z0)
            }

            else -> z.setZero()
        }
    }

    fun coeffSetShiftLeft(z: Coeff, x: Coeff, s: Int) {
        val wholeDwordCount = s ushr 6
        val innerL = s and 0x3F
        val nonZeroMask = -innerL.toLong() shr 63
        val innerR = -innerL
        val topBitsMask = nonZeroMask shl innerR
        //FIXME need to check for non-zero bit overflow out the top
        var z3 = 0L
        var z2 = 0L
        var z1 = 0L
        var z0 = 0L
        when (wholeDwordCount) {
            0 -> {
                if (x.dw3 and topBitsMask != 0L)
                    throw RuntimeException("coefficientOverflow")
                z3 = (x.dw3 shl innerL) or ((x.dw2 ushr innerR) and nonZeroMask)
                z2 = (x.dw2 shl innerL) or ((x.dw1 ushr innerR) and nonZeroMask)
                z1 = (x.dw1 shl innerL) or ((x.dw0 ushr innerR) and nonZeroMask)
                z0 = (x.dw0 shl innerL)
                if (innerL <= 3) {
                    //FIXME less than one digit change going on here
                    // tweak the digit count instead of recalculating
                }
            }

            1 -> {
                if ((x.dw3 or (x.dw2 and topBitsMask)) != 0L)
                    throw RuntimeException("coefficientOverflow")
                z3 = (x.dw2 shl innerL) or ((x.dw1 ushr innerR) and nonZeroMask)
                z2 = (x.dw1 shl innerL) or ((x.dw0 ushr innerR) and nonZeroMask)
                z1 = (x.dw0 shl innerL)
            }

            2 -> {
                if ((x.dw3 or x.dw2 or (x.dw1 and topBitsMask)) != 0L)
                    throw RuntimeException("coefficientOverflow")
                z3 = (x.dw1 shl innerL) or ((x.dw0 ushr innerR) and nonZeroMask)
                z2 = (x.dw0 shl innerL)
            }

            3 -> {
                if ((x.dw3 or x.dw2 or x.dw1 or (x.dw0 and topBitsMask)) != 0L)
                    throw RuntimeException("coefficientOverflow")
                z3 = (x.dw0 shl innerL)
            }

            else -> {
                z.setZero()
                return
            }
        }
        z.setCoeff256(z3, z2, z1, z0)
    }

    fun coeffSetShiftRight(z: Coeff, x: LongArray, xOff: Int, xLen: Int, bitCount: Int) {

        z.setZero()
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
                val z0 = x[shiftOff + 0] ushr innerShift
                z.setCoeff64(z0)
            }

            2 -> {
                val z0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                val z1 = x[shiftOff + 1] ushr innerShift
                z.setCoeff128(z1, z0)
            }

            3 -> {
                val z0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                val z1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl leftShift)) or (x[shiftOff + 1] ushr innerShift)
                val z2 = x[shiftOff + 2] ushr innerShift
                z.setCoeff192(z2, z1, z0)
            }

            4 -> {
                val z0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                val z1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl leftShift)) or (x[shiftOff + 1] ushr innerShift)
                val z2 = (innerShiftNonZeroMask and (x[shiftOff + 3] shl leftShift)) or (x[shiftOff + 2] ushr innerShift)
                val z3 = x[shiftOff + 3] ushr innerShift
                z.setCoeff256(z3, z2, z1, z0)
            }

            5 -> {
                val z0 = (innerShiftNonZeroMask and (x[shiftOff + 1] shl leftShift)) or (x[shiftOff + 0] ushr innerShift)
                val z1 = (innerShiftNonZeroMask and (x[shiftOff + 2] shl leftShift)) or (x[shiftOff + 1] ushr innerShift)
                val z2 = (innerShiftNonZeroMask and (x[shiftOff + 3] shl leftShift)) or (x[shiftOff + 2] ushr innerShift)
                val z3 = (innerShiftNonZeroMask and (x[shiftOff + 4] shl leftShift)) or (x[shiftOff + 3] ushr innerShift)
                val z4 = x[shiftOff + 4] ushr innerShift
                if (z4 != 0L)
                    throw RuntimeException("overflow")
                z.setCoeff256(z3, z2, z1, z0)
            }

            else -> {
                throw RuntimeException("overflow")
            }
        }
    }

    fun coeffSetShiftRight(z: Coeff, x: IntArray, xLen: Int, s: Int) {
        assert(s < 32)
        if (s == 0) {
            coeffSet(z, x, xLen)
            return
        }
        z.setZero()
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
                val z0 = nonZeroVal
                z.setCoeff64(z0)
            }

            1 -> {
                val z0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                val z1 = nonZeroVal
                z.setCoeff128(z1, z0)
            }

            2 -> {
                val z0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                val z1 = ((x[4] shl -s).toLong() shl 32) or (((x[3].toLong() shl 32) or (x[2].toLong() and MASK32)) shr s)
                val z2 = nonZeroVal
                z.setCoeff192(z2, z1, z0)
            }

            3 -> {
                val z0 = ((x[2] shl -s).toLong() shl 32) or (((x[1].toLong() shl 32) or (x[0].toLong() and MASK32)) shr s)
                val z1 = ((x[4] shl -s).toLong() shl 32) or (((x[3].toLong() shl 32) or (x[2].toLong() and MASK32)) shr s)
                val z2 = ((x[6] shl -s).toLong() shl 32) or (((x[5].toLong() shl 32) or (x[4].toLong() and MASK32)) shr s)
                val z3 = nonZeroVal;
                z.setCoeff256(z3, z2, z1, z0)
            }

            else -> throw RuntimeException("overflow")
        }
    }

}
