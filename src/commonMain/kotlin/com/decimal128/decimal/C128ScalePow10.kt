@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal object C128ScalePow10 {

    fun c128ScaleUpPow10(x: Decimal, pow10: Int, signExp: Short): Decimal {
        // FIXME - UNTESTED!!
        verify { pow10 > 0 }
        val pow10BitLen = pow10BitLen(pow10)
        val (pow10dw1, pow10dw0) = pow10_128(pow10)
        val (p1, p0) = umul128x128to128(x.dw1, x.dw0, pow10dw1, pow10dw0)
        return Decimal.from(p1, p0, signExp)
    }

    fun c128ScaleUpPow10(sign: Boolean, dw1: Long, dw0: Long, qExp: Int, pow10: Int, ctx: DecContext): Decimal {
        verify { pow10 > 0 }
        val (pow10dw1, pow10dw0) = pow10_128(pow10)
        val (p1, p0) = umul128x128to128(dw1, dw0, pow10dw1, pow10dw0)
        return Decimal.from(sign, p1, p0, qExp - pow10)
    }

    fun c128ScaleUpPow10(dw1: Long, dw0: Long, pow10: Int): Pair<Long, Long> {
        verify { pow10 > 0 }
        val (pow10dw1, pow10dw0) = pow10_128(pow10)
        return umul128x128to128(dw1, dw0, pow10dw1, pow10dw0)
    }

    fun c128ScaleDownPow10(dw1: Long, dw0: Long, pow10: Int): Triple<Long, Long, Residue> {
        // FIXME!
        val t = C256()
        t.c256Set128(dw1, dw0)
        val s = C256()
        val residue = c256SetScaleDownPow10(s, t, pow10)
        return Triple(s.dw1, s.dw0, residue)
    }

}