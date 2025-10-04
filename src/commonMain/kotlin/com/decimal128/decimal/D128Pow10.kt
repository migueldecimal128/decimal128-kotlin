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
}
