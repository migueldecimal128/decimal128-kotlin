package com.decimal128

import com.decimal128.CoeffMul.Companion.mulCoeff

class CoeffScalePow10 {
    companion object {

        fun scalePow10(p: Coeff, sign: Boolean, x: Coeff, pow10: Int, ctx: Decimal128Context) {
            if (x.digitCount == 0 || pow10 == 0) {
                p.set(x)
                return
            }
            if (pow10 > 0)
                scaleUpPow10(p, x, pow10, ctx)
            else
                scaleDownPow10(p, sign, x, -pow10, ctx)
        }

        private fun scaleUpPow10(p: Coeff, x: Coeff, pow10: Int, ctx: Decimal128Context) {
            assert(pow10 > 0)
            assert(x.digitCount > 0)

            // note that this is a litle lie
            // digitCount is actually pow10 + 1
            // but this works OK because multiplying by a power of 10 will increase the productDigitCount by exactly pow10
            val pow10DigitCount = pow10

            val productDigitCount = x.digitCount + pow10
            if (productDigitCount >= MAX_COEFF_DIGIT_COUNT)
                throw RuntimeException("coefficient overflow")
            when {
                (pow10 < POW10_128_OFFSET) ->
                { val index = pow10; mulCoeff(p, x, pow10DigitCount, POW10[index + 0]) }
                (pow10 < POW10_192_OFFSET) ->
                { val index = POW10_128_DWORD_INDEX + 2*(pow10 - POW10_128_OFFSET);
                    mulCoeff(p, x, pow10DigitCount, POW10[index + 1], POW10[index + 0]) }
                (pow10 < POW10_256_OFFSET) ->
                { val index = POW10_192_DWORD_INDEX + 3*(pow10 - POW10_192_OFFSET);
                    mulCoeff(p, x, pow10DigitCount, POW10[index + 2], POW10[index + 1], POW10[index + 0]) }
                (pow10 < POW10_MAX_OFFSET) ->
                { val index = POW10_256_DWORD_INDEX + 4*(pow10 - POW10_256_OFFSET);
                    mulCoeff(p, x, pow10DigitCount, POW10[index + 3], POW10[index + 2], POW10[index + 1], POW10[index + 0]) }
                else -> throw RuntimeException("?que?")
            }
            p.digitCount = productDigitCount
            assert(p.isValidDigitCount())
        }

        private fun scaleDownPow10(p: Coeff, sign: Boolean, x: Coeff, pow10: Int, ctx: Decimal128Context) {
            assert(pow10 > 0)
            assert(x.digitCount > 0)

            val productDigitCount = x.digitCount - pow10
            if (productDigitCount <= 0) {
                val residue = if (productDigitCount == 0) Residue.residueFrom(x) else Residue.LT_HALF
                val roundUp = residue.ulpBias(ctx.roundingDirection.negate(sign), 0L)
                p.setZero()
                if (roundUp > 0) {
                    p.dw0 = 1
                    p.digitCount = 1
                }
                ctx.setInexact()
                return
            }
            RecipMulPow10.divPow10(p, sign, x.digitCount, x.dw3, x.dw2, x.dw1, x.dw0, pow10, ctx)
        }
    }
}