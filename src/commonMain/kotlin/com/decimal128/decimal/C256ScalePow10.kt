package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT


internal fun c256SetScaleUpPow10(z: C256, x: C256, pow10: Int, tmpDwQuad: DwQuad) {
    when {
        pow10 > 0 -> {
            val pow10BitLen = pow10BitLen(pow10)
            val pow10Offset = pow10Offset(pow10)
            val pow10dw0 = POW10[pow10Offset + 0]
            val pow10dw1 = POW10[pow10Offset + 1] and ((64 - pow10BitLen) shr 31).toLong()
            val maxBitLen = x.bitLen + pow10BitLen
            when {
                (pow10BitLen <= 64) -> {
                    if (maxBitLen <= 192) {
                        val (p2, p1, p0) = umul192x64to192(x.dw2, x.dw1, x.dw0, pow10dw0)
                        z.c256Set192(p2, p1, p0)
                        return
                    }
                    c256SetMul(z, x, pow10BitLen, pow10dw0, tmpDwQuad)
                }

                (pow10BitLen <= 128) -> {
                    if (maxBitLen <= 192) {
                        val (p2, p1, p0) = umul128x128to192(x.dw1, x.dw0, pow10dw1, pow10dw0)
                        z.c256Set192(p2, p1, p0)
                        return
                    }
                    c256SetMul(z, x, pow10BitLen, pow10dw1, pow10dw0, tmpDwQuad)
                }

                (pow10BitLen <= 192) -> {
                    c256SetMul(z, x, pow10BitLen, POW10[pow10Offset + 2], pow10dw1, pow10dw0, tmpDwQuad)
                }

                (pow10BitLen <= 256) -> {
                    c256SetMul(z, x,
                        pow10BitLen, POW10[pow10Offset + 3], POW10[pow10Offset + 2], pow10dw1, pow10dw0,
                        tmpDwQuad)
                }

                else -> throw RuntimeException()
            }
        }

        pow10 == 0 -> z.c256Set(x)
        else -> throw RuntimeException()
    }
}

internal fun c256SetScaleDownPow10(z: C256, x: C256, pow10: Int): Residue {
    if (x.bitLen > 0 && pow10 > 0) {
        val productDigitCount = x.digitLen - pow10
        if (productDigitCount <= 0) {
            val residue = if (productDigitCount == 0) Residue.fromValueDecade(x) else Residue.LT_HALF
            z.c256SetZero()
            return residue
        }
        return c256SetDivPow10(z, x, pow10)
    }
    z.c256Set(x)
    return EXACT
}
