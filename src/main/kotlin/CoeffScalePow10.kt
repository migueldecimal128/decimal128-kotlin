package com.decimal128

import com.decimal128.CoeffMul.mulCoeff
import com.decimal128.CoeffFma.coeffFma
import com.decimal128.CoeffDigitCount.POW10
import com.decimal128.CoeffFusedMulAbsDiff.coeffFusedMulAbsDiff
import com.decimal128.Residue.Companion.EXACT
import kotlin.math.max


object CoeffScalePow10 {

    fun coeffScaleUpPow10(z: Coeff, x: Coeff, pow10: Int) {
        if (x.digitCount == 0 || pow10 == 0) {
            z.set(x)
            return
        }

        // note that this is a litle lie
        // digitCount is actually pow10 + 1
        // but this works OK because multiplying by a power of 10 will increase the productDigitCount by exactly pow10
        val pow10DigitCount = pow10

        val productDigitCount = x.digitCount + pow10
        if (productDigitCount >= MAX_COEFF_DIGIT_COUNT)
            throw RuntimeException("coefficient overflow")
        when {
            (pow10 < POW10_128_OFFSET) -> {
                val index = pow10; mulCoeff(z, x, pow10DigitCount, POW10[index + 0])
            }

            (pow10 < POW10_192_OFFSET) -> {
                val index = POW10_128_DWORD_INDEX + 2 * (pow10 - POW10_128_OFFSET);
                mulCoeff(z, x, pow10DigitCount, POW10[index + 1], POW10[index + 0])
            }

            (pow10 < POW10_256_OFFSET) -> {
                val index = POW10_192_DWORD_INDEX + 3 * (pow10 - POW10_192_OFFSET);
                mulCoeff(z, x, pow10DigitCount, POW10[index + 2], POW10[index + 1], POW10[index + 0])
            }

            (pow10 < POW10_MAX_OFFSET) -> {
                val index = POW10_256_DWORD_INDEX + 4 * (pow10 - POW10_256_OFFSET);
                mulCoeff(z, x, pow10DigitCount, POW10[index + 3], POW10[index + 2], POW10[index + 1], POW10[index + 0])
            }

            else -> throw RuntimeException("?que?")
        }
        assert(z.digitCount == productDigitCount)
        assert(z.isValidDigitCount())
    }

    fun coeffScaleDownPow10(z: Coeff, x: Coeff, pow10: Int): Residue {
        if (x.digitCount == 0 || pow10 == 0) {
            z.set(x)
            return EXACT
        }
        val productDigitCount = x.digitCount - pow10
        if (productDigitCount <= 0) {
            val residue = if (productDigitCount == 0) Residue.residueFrom(x) else Residue.LT_HALF
            return residue
        }
        return RecipMulPow10.divPow10(z, x, pow10)
    }

    fun coeffScaleFmaPow10(z: Coeff, x: Coeff, pow10: Int, a: Coeff) {
        assert(pow10 > 0)
        assert((x.dw3 or x.dw2) == 0L)
        assert((a.dw3 or a.dw2) == 0L)

        val minProductDigitCount = if (x.digitCount == 0) a.digitCount else max(x.digitCount + pow10, a.digitCount)
        assert(minProductDigitCount < MAX_COEFF_DIGIT_COUNT)

        val aDigitCount = a.digitCount
        val a1 = a.dw1
        val a0 = a.dw0
        _scaleFmaPow10(z, x, pow10, aDigitCount, a1, a0)
        assert(z.isValidDigitCount())
        assert(z.digitCount == minProductDigitCount || z.digitCount == minProductDigitCount + 1)
    }

    private fun _scaleFmaPow10(p: Coeff, x: Coeff, pow10: Int, aDigitCount: Int, a1: Long, a0: Long) {
        // note that this is a little lie
        // digitCount is actually pow10 + 1
        // but this works OK because multiplying by a power of 10 will increase the productDigitCount by exactly pow10
        val pow10DigitCount = pow10

        when {
            (pow10 < POW10_128_OFFSET) -> {
                val index = pow10;
                coeffFma(p, x, pow10DigitCount, POW10[index + 0], aDigitCount, a1, a0)
            }

            (pow10 < POW10_192_OFFSET) -> {
                val index = POW10_128_DWORD_INDEX + 2 * (pow10 - POW10_128_OFFSET);
                coeffFma(p, x, pow10DigitCount, POW10[index + 1], POW10[index + 0], aDigitCount, a1, a0)
            }

            (pow10 < POW10_256_OFFSET) -> {
                val index = POW10_192_DWORD_INDEX + 3 * (pow10 - POW10_192_OFFSET);
                coeffFma(
                    p, x,
                    pow10DigitCount, POW10[index + 2], POW10[index + 1], POW10[index + 0], aDigitCount, a1, a0
                )
            }

            (pow10 < POW10_MAX_OFFSET) -> {
                val index = POW10_256_DWORD_INDEX + 4 * (pow10 - POW10_192_OFFSET);
                coeffFma(
                    p, x,
                    pow10DigitCount, POW10[index + 3], POW10[index + 2], POW10[index + 1], POW10[index + 0],
                    aDigitCount, a1, a0
                )
            }

            else -> throw RuntimeException("?que?")
        }
    }

    fun coeffScaleFmaPow10AbsDiff(z: Coeff, x: Coeff, pow10: Int, a: Coeff) {
        assert(pow10 > 0)
        assert(x.digitCount > 0)
        assert((x.dw3 or x.dw2) == 0L)
        assert((a.dw3 or a.dw2) == 0L)

        val minProductDigitCount = Math.max(x.digitCount + pow10, a.digitCount)
        assert(minProductDigitCount < MAX_COEFF_DIGIT_COUNT)

        val aDigitCount = a.digitCount
        val a1 = a.dw1
        val a0 = a.dw0
        _scaleFmaPow10(z, x, pow10, aDigitCount, a1, a0)
        assert(z.isValidDigitCount())
        assert(z.digitCount == minProductDigitCount || z.digitCount == minProductDigitCount + 1)
    }

    private fun _scaleFmaPow10AbsDiff(z: Coeff, x: Coeff, pow10: Int, aDigitCount: Int, a1: Long, a0: Long) {
        // note that this is a litle lie
        // digitCount is actually pow10 + 1
        // but this works OK because multiplying by a power of 10 will increase the productDigitCount by exactly pow10
        val pow10DigitCount = pow10

        when {
            (pow10 < POW10_128_OFFSET) -> {
                val index = pow10;
                coeffFusedMulAbsDiff(z, x, pow10DigitCount, POW10[index + 0], aDigitCount, a1, a0)
            }

            (pow10 < POW10_192_OFFSET) -> {
                val index = POW10_128_DWORD_INDEX + 2 * (pow10 - POW10_128_OFFSET);
                coeffFusedMulAbsDiff(z, x, pow10DigitCount, POW10[index + 1], POW10[index + 0], aDigitCount, a1, a0)
            }

            (pow10 < POW10_256_OFFSET) -> {
                val index = POW10_192_DWORD_INDEX + 3 * (pow10 - POW10_192_OFFSET);
                coeffFusedMulAbsDiff(
                    z, x,
                    pow10DigitCount, POW10[index + 2], POW10[index + 1], POW10[index + 0], aDigitCount, a1, a0
                )
            }

            (pow10 < POW10_MAX_OFFSET) -> {
                val index = POW10_256_DWORD_INDEX + 4 * (pow10 - POW10_192_OFFSET);
                coeffFusedMulAbsDiff(
                    z, x,
                    pow10DigitCount, POW10[index + 3], POW10[index + 2], POW10[index + 1], POW10[index + 0],
                    aDigitCount, a1, a0
                )
            }

            else -> throw RuntimeException("?que?")
        }
    }

}