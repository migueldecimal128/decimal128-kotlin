package com.decimal128.decimal

import kotlin.math.max

internal fun c256SetDivPow10(z: C256, x: C256, pow10: Int, pentad: Pentad): Residue {
    verify { pow10 >= 0 }
    val xBitLen = x.bitLen
    if (pow10 < MAGIC_POW10_M_MAXX) {
        if (xBitLen <= 64)
            return DivMagic.magicDivPow10_64(z, x.dw0, pow10)
        if (pow10 < BARRETT_POW10_MAXX)
            return barrettDivPow10(z, x, pow10)
    }
    val pow10BitLen = pow10BitLen(pow10)
    if (xBitLen < pow10BitLen) {
        z.c256SetZero()
        val halfPow10BitLen = pow10BitLen - 1
        return (
                if (xBitLen < halfPow10BitLen) {
                    if (xBitLen == 0) Residue.EXACT else Residue.LT_HALF
                } else {
                    Residue.fromValueDecade(x)
                })
    }
    if (pow10 < RRMP10_K_MAXX)
        return divRangeRecipMulPow10(z, x, pow10, pentad)
    // perform a two-step
    val step1a = RRMP10_K_MAXX - 1
    val step2a = pow10 - step1a
    verify { step2a < RRMP10_K_MAXX }
    val step2 = max(step2a, BARRETT_POW10_MAXX - 1)
    val step1 = pow10 - step2
    val residue1 = divRangeRecipMulPow10(z, x, step1, pentad)
    val residue2 = c256SetDivPow10(z, z, step2, pentad)
    val residue = residue2.merge(residue1)
    return residue
}

