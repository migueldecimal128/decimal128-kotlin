package com.decimal128

import com.decimal128.CoeffDigitCount.setDigitCount64
import com.decimal128.CoeffDigitCount.setDigitCount128
import com.decimal128.CoeffDigitCount.setDigitCount192
import com.decimal128.CoeffDigitCount.setDigitCount256
import com.decimal128.CoeffDigitCount.setDigitCount
import com.decimal128.CoeffDigitCount.isValidDigitCount
import java.math.BigInteger

object CoeffSet {

    fun coeffSetZero(c: Coeff) {
        c.dw3 = 0L; c.dw2 = 0L; c.dw1 = 0L; c.dw0 = 0L; c.digitCount = 0
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
        var nonZeroIndex = xLen - 1
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

            4 -> throw RuntimeException("overflow")
        }
    }

}
