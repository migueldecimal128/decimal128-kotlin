package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul4
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul3
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul2
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul1
import java.math.BigInteger
import java.math.BigInteger.ZERO
import java.math.BigInteger.ONE
import java.math.BigInteger.TWO
import java.math.BigInteger.TEN
import kotlin.math.ceil

object CoeffDivPow10 {

    fun divPow10(z: Coeff, x: Coeff, pow10: Int): Residue {
        assert(pow10 >= 0)
        val xBitLen = x.bitLen
        if (pow10 < MAGIC_POW10_MAX) {
            if (xBitLen <= 64)
                return DivMagic.magicDivPow10_64(z, x.dw0, pow10)
            if (pow10 < BARRETT_POW10_MAX)
                return DivBarrett.barrettDivPow10(z, x, pow10)
        }
        val pow10BitLen = CoeffPow10.pow10BitLen(pow10)
        if (xBitLen < pow10BitLen) {
            z.coeffSetZero()
            val halfPow10BitLen = pow10BitLen - 1
            return (
                    if (xBitLen < halfPow10BitLen) {
                        if (xBitLen == 0) Residue.EXACT else Residue.LT_HALF
                    } else {
                        Residue.residueFrom(x)
                    })
        }
        if (pow10 < MAX_DIVISOR_POW10)
            return RecipMulPow10.rangeDivPow10(z, x, pow10)
        // perform a two-step
        val step1a = MAX_DIVISOR_POW10 - 1
        val step2a = pow10 - step1a
        assert(step2a < MAX_DIVISOR_POW10)
        val step2 = Math.max(step2a, BARRETT_POW10_MAX - 1)
        val step1 = pow10 - step2
        val residue1 = RecipMulPow10.rangeDivPow10(z, x, step1)
        val residue2 = divPow10(z, z, step2)
        val residue = residue2.merge(residue1)
        return residue
    }

}
