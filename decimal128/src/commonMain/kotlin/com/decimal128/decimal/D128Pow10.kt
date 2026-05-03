package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.decimalFNZ
import com.decimal128.decimal.Decimal.Companion.decimalFinite

internal fun d128ScaleCoeffUpPow10(xSign: Boolean, x: Decimal, pow10: Int, negate: Boolean = false): Decimal {
    verify { pow10 > 0 }
    val x0 = x.dw0
    val pow10Offset = (pow10 shl 1) and POW10_BCE
    val dw1Pow10 = POW10[pow10Offset + 1]
    val dw0Pow10 = POW10[pow10Offset    ]
    val p0 = x0 * dw0Pow10
    val p1 = unsignedMulHi(x0, dw0Pow10) + (x0 * dw1Pow10) + (x.dw1 * dw0Pow10)
    return decimalFNZ(xSign, x.qExp - pow10, p1, p0)
}

/**
 * Unsigned fused subtract multiply by power of 10.
 * Guaranteed that the multiply will not exceed 128 bits.
 * Guaranteed that y less than the scaled product.
 */
internal fun d128FusedSubtractMulPow10(sign: Boolean, m: Decimal, n: Decimal, pow10: Int): Decimal {
    verify { pow10 > 0 }
    verify { n.bitLen + pow10BitLen(pow10) <= 128 }
    val pow10Offset = (pow10 shl 1) and POW10_BCE
    val dw1Pow10 = POW10[pow10Offset + 1]
    val dw0Pow10 = POW10[pow10Offset    ]
    val n0 = n.dw0
    val p0 = n0 * dw0Pow10
    val p1 = unsignedMulHi(n0, dw0Pow10) + (n0 * dw1Pow10) + (n.dw1 * dw0Pow10)

    val d0 = m.dw0 - p0
    val borrow0 = if (unsignedLT(m.dw0, d0)) 1L else 0L
    val d1 = m.dw1 - p1 - borrow0
    return decimalFinite(sign, m.qExp, d1, d0)
}

/**
 * Unsigned fused multiply by power of 10 and subtract.
 * Guaranteed that the multiply will not exceed 128 bits.
 * Guaranteed that y less than the scaled product.
 */
internal fun d128FusedMulPow10Subtract(sign: Boolean, x: Decimal, pow10: Int, y: Decimal, ctx: DecContext): Decimal {
    verify { pow10 > 0 }
    val x0 = x.dw0
    val pow10Offset = (pow10 shl 1) and POW10_BCE
    val dw1Pow10 = POW10[pow10Offset + 1]
    val dw0Pow10 = POW10[pow10Offset    ]
    val p0 = x0 * dw0Pow10
    val p1 = unsignedMulHi(x0, dw0Pow10) + (x0 * dw1Pow10) + (x.dw1 * dw0Pow10)

    val d0 = p0 - y.dw0
    val borrow0 = if (unsignedLT(p0, d0)) 1L else 0L
    val d1 = p1 - y.dw1 - borrow0
    return decFinalizeFinite(sign, y.qExp, d1, d0, ctx)
}

internal fun d128IsExactPowerOfTen(x: Decimal): Boolean {
    val steal = x.steal
    if (stealIsPositiveFNZ(steal)) {
        val digitLen = stealDigitLen(steal)
        verify { digitLen < MIN_POW10_DIGIT_LEN_192 }
        if (digitLen > 0) {
            val pow10Offset = pow10Offset(digitLen - 1) and POW10_BCE
            if (x.dw0 == POW10[pow10Offset] && x.dw1 == POW10[pow10Offset + 1])
                return true
        }
    }
    return false
}

