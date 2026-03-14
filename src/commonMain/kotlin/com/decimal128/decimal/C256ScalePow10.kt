package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.min


internal fun c256SetScaleUpPow10(z: C256, x: C256, pow10: Int, pentad: Pentad) {
    when {
        pow10 > 0 -> {
            val pow10BitLen = pow10BitLen(pow10)
            val pow10Offset = pow10Offset(pow10) and POW10_BCE
            val pow10dw0 = POW10[pow10Offset    ]
            val pow10dw1 = POW10[pow10Offset + 1] and ((64 - pow10BitLen) shr 31).toLong()
            val maxBitLen = x.bitLen + pow10BitLen
            when {
                (pow10BitLen <= 64) -> {
                    if (maxBitLen <= 192) {
                        z.c256Set192(umul192x64to192(pentad, x.dw2, x.dw1, x.dw0, pow10dw0))
                        return
                    }
                    c256SetMul(z, x, pow10BitLen, pow10dw0, pentad)
                }

                (pow10BitLen <= 128) -> {
                    if (maxBitLen <= 192) {
                        z.c256Set192(umul128x128to192(pentad, x.dw1, x.dw0, pow10dw1, pow10dw0))
                        return
                    }
                    c256SetMul(z, x, pow10BitLen, pow10dw1, pow10dw0, pentad)
                }

                (pow10BitLen <= 192) -> {
                    c256SetMul(z, x, pow10BitLen, POW10[pow10Offset + 2], pow10dw1, pow10dw0, pentad)
                }

                (pow10BitLen <= 256) -> {
                    c256SetMul(z, x,
                        pow10BitLen, POW10[pow10Offset + 3], POW10[pow10Offset + 2], pow10dw1, pow10dw0,
                        pentad)
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

/**
 * Count of Trailing Zero Digits
 */
internal fun c256CountTrailingZeroDigitsDestructive(c: C256): Int {
    if (c.c256IsZero())
        return -1
    var ctzd = 0
    while (true) {
        val ctzBits = c.dw0.countTrailingZeroBits()
        if (ctzBits == 0)
            return ctzd
        val chunk = min(ctzBits, BARRETT_POW10_MAX)
        val rem = barrettDivModPow10(c, c, chunk)
        if (rem != 0L) {
            val ntzdRem = ctzdU64(rem)
            return ctzd + ntzdRem
        }
        ctzd += chunk
    }
}

private inline fun ctzdU64(dw: Long): Int {
    var t: ULong = dw.toULong()
    var ctzBits = dw.countTrailingZeroBits()
    var ctzd = 0

    if (ctzBits >= 16 && t >= 1_0000_0000_0000_0000uL && t % 1_0000_0000_0000_0000uL == 0uL) {
        t /= 1_0000_0000_0000_0000uL
        ctzBits = t.countTrailingZeroBits()
        ctzd += 16
    }
    if (ctzBits >= 8 && t >= 1_0000_0000uL && t % 1_0000_0000uL == 0uL) {
        t /= 1_0000_0000uL
        ctzBits = t.countTrailingZeroBits()
        ctzd += 8
    }
    if (ctzBits >= 4 && t >= 1_0000uL && t % 1_0000uL == 0uL) {
        t /= 1_0000u
        ctzBits = t.countTrailingZeroBits()
        ctzd += 4
    }
    if (ctzBits >= 2 && t >= 100uL && t % 100uL == 0uL) {
        t /= 100uL
        ctzBits = t.countTrailingZeroBits()
        ctzd += 2
    }
    if (ctzBits > 0 && t >= 10uL && t % 10uL == 0uL) {
        ctzd += 1
    }

    return ctzd
}
