package com.decimal128

import com.decimal128.CoeffDigitCount.setDigitCount
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
            (c.digitCount < 20 || c.digitCount == 20 && c.dw1 == 0L) ->
                64 - numberOfLeadingZeros(c.dw0)
            (c.digitCount < 39 || c.digitCount == 39 && c.dw2 == 0L) ->
                128 - numberOfLeadingZeros(c.dw1)
            (c.digitCount < 58 || c.digitCount == 58 && c.dw3 == 0L) ->
                192 - numberOfLeadingZeros(c.dw2)
            else ->
                256 - numberOfLeadingZeros(c.dw3)
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
                setDigitCount(c)
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


}
