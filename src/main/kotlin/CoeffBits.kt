package com.decimal128

import com.decimal128.CoeffDigitCount.setDigitCount64
import com.decimal128.CoeffDigitCount.setDigitCount128
import com.decimal128.CoeffDigitCount.setDigitCount192
import com.decimal128.CoeffDigitCount.setDigitCount256
import com.decimal128.CoeffSet.coeffSetZero
import java.lang.Long.numberOfLeadingZeros


object CoeffBits {

    fun setLoBit(c: Coeff, dw0: Long) {
        val masked = dw0 and 1
        c.dw3 = 0L; c.dw2 = 0L; c.dw1 = 0L
        c.dw0 = masked
        c.digitCount = masked.toInt()
    }

    fun bitLength(c: Coeff): Int {
        return when {
            (c.dw3 != 0L) -> 192 + 64 - numberOfLeadingZeros(c.dw3)
            (c.dw2 != 0L) -> 128 + 64 - numberOfLeadingZeros(c.dw2)
            (c.dw1 != 0L) -> 64 + 64 - numberOfLeadingZeros(c.dw1)
            (c.dw0 != 0L) -> 64 - numberOfLeadingZeros(c.dw0)
            else -> 0
        }
    }

    fun coeffShiftRight(c: Coeff, bitShift: Int) {
        val wholeDwordCount = bitShift ushr 6
        val innerShift = bitShift and 0x3F
        val nonZeroMask = -innerShift.toLong() shr 63
        val leftShift = -innerShift
        when (wholeDwordCount) {
            0 -> {
                c.dw0 = (nonZeroMask and (c.dw1 shl leftShift)) or (c.dw0 ushr innerShift)
                c.dw1 = (nonZeroMask and (c.dw2 shl leftShift)) or (c.dw1 ushr innerShift)
                c.dw2 = (nonZeroMask and (c.dw3 shl leftShift)) or (c.dw2 ushr innerShift)
                c.dw3 = c.dw3 ushr innerShift
                setDigitCount256(c)
            }

            1 -> {
                c.dw0 = (nonZeroMask and (c.dw2 shl leftShift)) or (c.dw1 ushr innerShift)
                c.dw1 = (nonZeroMask and (c.dw3 shl leftShift)) or (c.dw2 ushr innerShift)
                c.dw2 = c.dw3 ushr innerShift
                c.dw3 = 0L
                setDigitCount192(c)
            }

            2 -> {
                c.dw0 = (nonZeroMask and (c.dw3 shl leftShift)) or (c.dw2 ushr innerShift)
                c.dw1 = c.dw3 ushr innerShift
                c.dw2 = 0L; c.dw3 = 0L
                setDigitCount128(c)
            }

            3 -> {
                c.dw0 = c.dw3 ushr innerShift
                c.dw1 = 0L; c.dw2 = 0L; c.dw3 = 0L
                setDigitCount64(c)
            }

            else -> coeffSetZero(c)
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

}
