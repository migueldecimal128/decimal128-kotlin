package com.decimal128

import com.decimal128.CoeffMul.mulCoeff
import com.decimal128.CoeffFma.coeffFma
import com.decimal128.CoeffDigitLen.POW10
import com.decimal128.CoeffDigitLen.pow10BitLen
import com.decimal128.CoeffDigitLen.pow10Offset
import com.decimal128.CoeffFusedMulAbsDiff.coeffFusedMulAbsDiff
import com.decimal128.Residue.Companion.EXACT
import kotlin.math.max


object CoeffScalePow10 {

    fun coeffScaleUpPow10(z: Coeff, x: Coeff, pow10: Int) {
        if (x.digitLen == 0 || pow10 == 0) {
            z.coeffSet(x)
            return
        }

        val pow10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)

        val productBitLen = x.bitLen + pow10BitLen
        if (productBitLen > 257) // 257 is OK, because it might come in at 256
            throw RuntimeException("coefficient overflow")
        when {
            (pow10BitLen <= 64) -> {
                mulCoeff(z, x, pow10BitLen, POW10[pow10Offset + 0])
            }

            (pow10BitLen <= 128) -> {
                mulCoeff(z, x, pow10BitLen, POW10[pow10Offset + 1], POW10[pow10Offset + 0])
            }

            (pow10BitLen <= 192) -> {
                mulCoeff(z, x, pow10BitLen, POW10[pow10Offset + 2], POW10[pow10Offset + 1], POW10[pow10Offset + 0])
            }

            (pow10BitLen <= 256) -> {
                mulCoeff(z, x, pow10BitLen, POW10[pow10Offset + 3], POW10[pow10Offset + 2], POW10[pow10Offset + 1], POW10[pow10Offset + 0])
            }

            else -> throw RuntimeException("?que?")
        }
        assert(z.hasValidLengths())
    }

    fun coeffScaleDownPow10(z: Coeff, x: Coeff, pow10: Int): Residue {
        if (x.digitLen == 0 || pow10 == 0) {
            z.coeffSet(x)
            return EXACT
        }
        val productDigitCount = x.digitLen - pow10
        if (productDigitCount <= 0) {
            val residue = if (productDigitCount == 0) Residue.residueFrom(x) else Residue.LT_HALF
            z.coeffSetZero()
            return residue
        }
        return RecipMulPow10.divPow10(z, x, pow10)
    }

    fun coeffScaleFmaPow10(z: Coeff, x: Coeff, pow10: Int, a: Coeff) {
        assert(pow10 > 0)
        assert((x.dw3 or x.dw2) == 0L)
        assert((a.dw3 or a.dw2) == 0L)

        val minProductDigitCount = if (x.digitLen == 0) a.digitLen else max(x.digitLen + pow10, a.digitLen)
        assert(minProductDigitCount < MAX_DIGIT_LEN)

        val aDigitCount = a.digitLen
        val a1 = a.dw1
        val a0 = a.dw0
        _scaleFmaPow10(z, x, pow10, aDigitCount, a1, a0)
        assert(z.hasValidLengths())
        assert(z.digitLen == minProductDigitCount || z.digitLen == minProductDigitCount + 1)
    }

    private fun _scaleFmaPow10(p: Coeff, x: Coeff, pow10: Int, aDigitCount: Int, a1: Long, a0: Long) {
        // note that this is a little lie
        // digitCount is actually pow10 + 1
        // but this works OK because multiplying by a power of 10 will increase the productDigitCount by exactly pow10
        val pow10DigitCount = pow10
        val pow10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)

        when {
            (pow10 < MIN_POW10_DIGIT_LEN_128) -> {
                coeffFma(p, x, pow10DigitCount, POW10[pow10Offset + 0], aDigitCount, a1, a0)
            }

            (pow10 < MIN_POW10_DIGIT_LEN_192) -> {
                coeffFma(p, x, pow10DigitCount, POW10[pow10Offset + 1], POW10[pow10Offset + 0], aDigitCount, a1, a0)
            }

            (pow10 < MIN_POW10_DIGIT_LEN_256) -> {
                coeffFma(
                    p, x,
                    pow10DigitCount, POW10[pow10Offset + 2], POW10[pow10Offset + 1], POW10[pow10Offset + 0], aDigitCount, a1, a0
                )
            }

            (pow10 < MAX_DIGIT_LEN) -> {
                coeffFma(
                    p, x,
                    pow10DigitCount, POW10[pow10Offset + 3], POW10[pow10Offset + 2], POW10[pow10Offset + 1], POW10[pow10Offset + 0],
                    aDigitCount, a1, a0
                )
            }

            else -> throw RuntimeException("?que?")
        }
    }

    fun coeffScaleFusedMulAbsDiffPow10(z: Coeff, x: Coeff, pow10: Int, a: Coeff): Residue {
        assert(pow10 > 0)
        assert((x.dw3 or x.dw2) == 0L)
        assert((a.dw3 or a.dw2) == 0L)

        val minProductDigitCount = Math.max(x.digitLen + pow10, a.digitLen)
        assert(minProductDigitCount < MAX_DIGIT_LEN)

        val aDigitCount = a.digitLen
        val a1 = a.dw1
        val a0 = a.dw0
        return _scaleFusedMulAbsDiffPow10(z, x, pow10, aDigitCount, a1, a0)
    }

    private fun _scaleFusedMulAbsDiffPow10(z: Coeff, x: Coeff, pow10: Int,
                                           aDigitCount: Int, a1: Long, a0: Long): Residue {
        // note that this is a litle lie
        // digitCount is actually pow10 + 1
        // but this works OK because multiplying by a power of 10 will increase the productDigitCount by exactly pow10
        val pow10DigitCount = pow10
        val pow10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)

        return when {
            (pow10 < MIN_POW10_DIGIT_LEN_128) -> {
                coeffFusedMulAbsDiff(z, x, pow10DigitCount, POW10[pow10Offset + 0], aDigitCount, a1, a0)
            }

            (pow10 < MIN_POW10_DIGIT_LEN_192) -> {
                coeffFusedMulAbsDiff(z, x, pow10DigitCount, POW10[pow10Offset + 1], POW10[pow10Offset + 0], aDigitCount, a1, a0)
            }

            (pow10 < MIN_POW10_DIGIT_LEN_256) -> {
                coeffFusedMulAbsDiff(
                    z, x,
                    pow10DigitCount, POW10[pow10Offset + 2], POW10[pow10Offset + 1], POW10[pow10Offset + 0], aDigitCount, a1, a0
                )
            }

            (pow10 < MAX_DIGIT_LEN) -> {
                coeffFusedMulAbsDiff(
                    z, x,
                    pow10DigitCount, POW10[pow10Offset + 3], POW10[pow10Offset + 2], POW10[pow10Offset + 1], POW10[pow10Offset + 0],
                    aDigitCount, a1, a0
                )
            }

            else -> throw RuntimeException("?que?")
        }
    }

}