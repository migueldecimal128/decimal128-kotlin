package com.decimal128.decimal

object D128Pow10 {

    fun scaleCoeffUpPow10(x: Decimal, pow10: Int): Decimal {
        check (pow10 > 0)
        val pow10BitLen = U256Pow10.pow10BitLen(pow10)
        val pow10Offset = U256Pow10.pow10Offset(pow10)
        val dw0Pow10 = POW10[pow10Offset + 0]
        val dw1Pow10 = POW10[pow10Offset + 1] and ((64 - pow10BitLen) shr 31).toLong()
        val p0 = x.dw0 * dw0Pow10
        val p1 = unsignedMulHi(x.dw0, dw0Pow10) + (x.dw0 * dw1Pow10) + (x.dw1 * dw0Pow10)
        return Decimal(x.sign, p1, p0, x.qExp - pow10)
    }

    fun fmaCoeffPow10(x: Decimal, pow10: Int, y: Decimal): Decimal {
        check (pow10 > 0)
        val pow10BitLen = U256Pow10.pow10BitLen(pow10)
        val pow10Offset = U256Pow10.pow10Offset(pow10)
        val dw0Pow10 = POW10[pow10Offset + 0]
        val dw1Pow10 = POW10[pow10Offset + 1] and ((64 - pow10BitLen) shr 31).toLong()
        val p0 = x.dw0 * dw0Pow10
        val p1 = unsignedMulHi(x.dw0, dw0Pow10) + (x.dw0 * dw1Pow10) + (x.dw1 * dw0Pow10)

        val s0 = p0 + y.dw0
        val carry0 = if (unsignedLT(s0, p0)) 1L else 0L
        val s1 = p1 + y.dw1 + carry0
        return Decimal(x.sign, s1, s0, y.qExp)
    }

    /**
     * Unsigned fused multiply by power of 10 and subtract.
     * Guaranteed that the multiply will not exceed 128 bits.
     * Guaranteed that y less than the scaled product.
     */
    fun fusedMulPow10Subtract(x: Decimal, pow10: Int, y: Decimal, sign: Boolean): Decimal {
        check (pow10 > 0)
        val pow10BitLen = U256Pow10.pow10BitLen(pow10)
        val pow10Offset = U256Pow10.pow10Offset(pow10)
        val dw0Pow10 = POW10[pow10Offset + 0]
        val dw1Pow10 = POW10[pow10Offset + 1] and ((64 - pow10BitLen) shr 31).toLong()
        val p0 = x.dw0 * dw0Pow10
        val p1 = unsignedMulHi(x.dw0, dw0Pow10) + (x.dw0 * dw1Pow10) + (x.dw1 * dw0Pow10)

        val d0 = p0 - y.dw0
        val borrow0 = if (unsignedLT(p0, d0)) 1L else 0L
        val d1 = p1 - y.dw1 - borrow0
        return Decimal(sign, d1, d0, y.qExp)
    }

    /**
     * Unsigned fused subtract multiply by power of 10.
     * Guaranteed that the multiply will not exceed 128 bits.
     * Guaranteed that y less than the scaled product.
     */
    fun fusedSubtractMulPow10(x: Decimal, y: Decimal, pow10: Int, sign: Boolean): Decimal {
        check (pow10 > 0)
        val pow10BitLen = U256Pow10.pow10BitLen(pow10)
        val pow10Offset = U256Pow10.pow10Offset(pow10)
        val dw0Pow10 = POW10[pow10Offset + 0]
        val dw1Pow10 = POW10[pow10Offset + 1] and ((64 - pow10BitLen) shr 31).toLong()
        val p0 = y.dw0 * dw0Pow10
        val p1 = unsignedMulHi(y.dw0, dw0Pow10) + (y.dw0 * dw1Pow10) + (y.dw1 * dw0Pow10)

        val d0 = x.dw0 - p0
        val borrow0 = if (unsignedLT(x.dw0, d0)) 1L else 0L
        val d1 = x.dw1 - p1 - borrow0
        return Decimal(sign, d1, d0, x.qExp)
    }
}
