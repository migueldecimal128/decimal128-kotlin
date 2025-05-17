package com.decimal128

import com.decimal128.CoeffMul.mulCoeff
import com.decimal128.CoeffPow10.pow10BitLen
import com.decimal128.CoeffPow10.pow10Offset
import com.decimal128.Residue.Companion.EXACT


object CoeffScalePow10 {

    fun coeffScaleUpPow10(z: Coeff, x: Coeff, pow10: Int) {
        when {
            pow10 > 0 -> {
                val pow10BitLen = pow10BitLen(pow10)
                val pow10Offset = pow10Offset(pow10)
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
                    else -> throw RuntimeException()
                }
            }
            pow10 == 0 -> z.coeffSet(x)
            else -> throw RuntimeException()
        }
    }

    fun coeffScaleDownPow10(z: Coeff, x: Coeff, pow10: Int): Residue {
        if (x.digitLen > 0 && pow10 > 0) {
            val productDigitCount = x.digitLen - pow10
            if (productDigitCount <= 0) {
                val residue = if (productDigitCount == 0) Residue.residueFrom(x) else Residue.LT_HALF
                z.coeffSetZero()
                return residue
            }
            return RecipMulPow10.divPow10(z, x, pow10)
        }
        z.coeffSet(x)
        return EXACT
    }

    fun coeffScaleFmaPow10(z: Coeff, x: Coeff, pow10: Int, a: Coeff) {
        CoeffFma.coeffFmaPow10(z, x, pow10, a)
    }

    fun coeffScaleFusedMulAbsDiffPow10(z: Coeff, x: Coeff, pow10: Int, a: Coeff): Residue {
        return CoeffFusedMulAbsDiff.coeffFmadPow10(z, x, pow10, a)
    }

}