package com.decimal128.decimal

object D128Pow10 {

    fun scaleCoeffUpPow10(xSign: Boolean, x: Decimal, pow10: Int, negate: Boolean = false): Decimal {
        verify { pow10 > 0 }
        val x0 = x.dw0
        val (dw1Pow10, dw0Pow10) = pow10_128(pow10)
        val p0 = x0 * dw0Pow10
        val p1 = unsignedMulHi(x0, dw0Pow10) + (x0 * dw1Pow10) + (x.dw1 * dw0Pow10)
        return Decimal(xSign, x.qExp - pow10, p1, p0)
    }

    fun fmaCoeffPow10(xSign: Boolean, x: Decimal, pow10: Int, y: Decimal): Decimal {
        verify { pow10 > 0 }
        val x0 = x.dw0
        val (dw1Pow10, dw0Pow10) = pow10_128(pow10)
        val p0 = x0 * dw0Pow10
        val p1 = unsignedMulHi(x0, dw0Pow10) + (x0 * dw1Pow10) + (x.dw1 * dw0Pow10)

        val s0 = p0 + y.dw0
        val carry0 = if (unsignedLT(s0, p0)) 1L else 0L
        val s1 = p1 + y.dw1 + carry0
        return Decimal(xSign, y.qExp, s1, s0)
    }

    /**
     * Unsigned fused subtract multiply by power of 10.
     * Guaranteed that the multiply will not exceed 128 bits.
     * Guaranteed that y less than the scaled product.
     */
    fun fusedSubtractMulPow10(sign: Boolean, m: Decimal, n: Decimal, pow10: Int): Decimal {
        verify { pow10 > 0 }
        verify { n.bitLen + pow10BitLen(pow10) <= 128}
        val (dw1Pow10, dw0Pow10) = pow10_128(pow10)
        val n0 = n.dw0
        val p0 = n0 * dw0Pow10
        val p1 = unsignedMulHi(n0, dw0Pow10) + (n0 * dw1Pow10) + (n.dw1 * dw0Pow10)

        val d0 = m.dw0 - p0
        val borrow0 = if (unsignedLT(m.dw0, d0)) 1L else 0L
        val d1 = m.dw1 - p1 - borrow0
        return Decimal(sign, m.qExp, d1, d0)
    }

    /**
     * Unsigned fused multiply by power of 10 and subtract.
     * Guaranteed that the multiply will not exceed 128 bits.
     * Guaranteed that y less than the scaled product.
     */
    fun fusedMulPow10Subtract(sign: Boolean, x: Decimal, pow10: Int, y: Decimal, ctx: DecContext): Decimal {
        verify { pow10 > 0 }
        val x0 = x.dw0
        val (dw1Pow10, dw0Pow10) = pow10_128(pow10)
        val p0 = x0 * dw0Pow10
        val p1 = unsignedMulHi(x0, dw0Pow10) + (x0 * dw1Pow10) + (x.dw1 * dw0Pow10)

        val d0 = p0 - y.dw0
        val borrow0 = if (unsignedLT(p0, d0)) 1L else 0L
        val d1 = p1 - y.dw1 - borrow0
        return decFinalizeFinite(sign, d1, d0, y.qExp, ctx)
    }

    /**
     * Unsigned fused subtract multiply by power of 10.
     * Guaranteed that the multiply will not exceed 128 bits.
     * Guaranteed that y less than the scaled product.
     */
    fun fusedSubtractMulPow10(sign: Boolean, m: Decimal, n: Decimal, pow10: Int, ctx: DecContext): Decimal {
        verify { pow10 > 0 }
        verify { n.bitLen + pow10BitLen(pow10) <= 128}
        val (dw1Pow10, dw0Pow10) = pow10_128(pow10)
        val n0 = n.dw0
        val p0 = n0 * dw0Pow10
        val p1 = unsignedMulHi(n0, dw0Pow10) + (n0 * dw1Pow10) + (n.dw1 * dw0Pow10)

        val d0 = m.dw0 - p0
        val borrow0 = if (unsignedLT(m.dw0, d0)) 1L else 0L
        val d1 = m.dw1 - p1 - borrow0
        return decFinalizeFinite(sign, d1, d0, m.qExp, ctx)
    }
}
