package com.decimal128.decimal

import com.decimal128.decimal.U256Pow10.BARRETT_POW10_MAXX
import com.decimal128.decimal.U256Pow10.MAGIC_POW10_MAXX
import kotlin.math.max

object U256DivPow10 {

    fun divPow10(z: C256, x: C256, pow10: Int): Residue {
        check(pow10 >= 0)
        val xBitLen = x.bitLen
        if (pow10 < MAGIC_POW10_MAXX) {
            if (xBitLen <= 64)
                return DivMagic.magicDivPow10_64(z, x.dw0, pow10)
            if (pow10 < BARRETT_POW10_MAXX)
                return DivBarrett.barrettDivPow10(z, x, pow10)
        }
        val pow10BitLen = U256Pow10.pow10BitLen(pow10)
        if (xBitLen < pow10BitLen) {
            z.c256SetZero()
            val halfPow10BitLen = pow10BitLen - 1
            return (
                    if (xBitLen < halfPow10BitLen) {
                        if (xBitLen == 0) Residue.EXACT else Residue.LT_HALF
                    } else {
                        Residue.residueFrom(x)
                    })
        }
        if (pow10 < K_MAXX)
            return DivRangeRecipMulPow10.rangeDivPow10(z, x, pow10)
        // perform a two-step
        val step1a = K_MAXX - 1
        val step2a = pow10 - step1a
        check(step2a < K_MAXX)
        val step2 = max(step2a, BARRETT_POW10_MAXX - 1)
        val step1 = pow10 - step2
        val residue1 = DivRangeRecipMulPow10.rangeDivPow10(z, x, step1)
        val residue2 = divPow10(z, z, step2)
        val residue = residue2.merge(residue1)
        return residue
    }

}
