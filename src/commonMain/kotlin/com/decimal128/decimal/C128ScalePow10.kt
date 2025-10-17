@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.U256Pow10.calcDigitLen128
import com.decimal128.decimal.U256Pow10.pow10BitLen
import com.decimal128.decimal.U256Pow10.pow10Offset

internal object C128ScalePow10 {

    fun c128ScaleUpPow10(x: Decimal, pow10: Int, signExp: Short): Decimal {
        check(pow10 > 0)
        val pow10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)
        val pow10dw0 = POW10[pow10Offset]
        val (p1, p0) =
            if (pow10BitLen <= 64) {
                umul128x64to128(x.dw1, x.dw0, pow10dw0)
            } else {
                val pow10dw1 = POW10[pow10Offset + 1]
                check(x.dw1 == 0L)
                umul128x64to128(pow10dw1, pow10dw0, x.dw0)
            }
        return Decimal.from(p1, p0, signExp)
    }

    fun c128ScaleUpPow10(sign: Boolean, dw1: Long, dw0: Long, qExp: Int, pow10: Int, env: DecEnv): Decimal {
        check(pow10 > 0)
        val pow10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)
        val pow10dw0 = POW10[pow10Offset]
        val (p1, p0) =
            if (pow10BitLen <= 64) {
                umul128x64to128(dw1, dw0, pow10dw0)
            } else {
                val pow10dw1 = POW10[pow10Offset + 1]
                check(dw1 == 0L)
                umul128x64to128(pow10dw1, pow10dw0, dw0)
            }
        val bitLen = calcBitLen128(dw1, dw0)
        val digitLen = calcDigitLen128(bitLen, dw1, dw0)
        return Decimal.from(sign, p1, p0, qExp - pow10)
    }

    fun c128ScaleUpPow10(dw1: Long, dw0: Long, pow10: Int): Pair<Long, Long> {
        check(pow10 > 0)
        val pow10BitLen = pow10BitLen(pow10)
        val pow10Offset = pow10Offset(pow10)
        val pow10dw0 = POW10[pow10Offset]
        return if (pow10BitLen <= 64) {
                umul128x64to128(dw1, dw0, pow10dw0)
            } else {
                val pow10dw1 = POW10[pow10Offset + 1]
                check(dw1 == 0L)
                umul128x64to128(pow10dw1, pow10dw0, dw0)
            }
    }

    inline fun c128ScaleDownPow10(dw1: Long, dw0: Long, pow10: Int): Triple<Long, Long, Residue> {
        TODO()
    }

}