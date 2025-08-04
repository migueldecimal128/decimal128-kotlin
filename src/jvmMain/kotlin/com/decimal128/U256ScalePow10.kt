package com.decimal128

import com.decimal128.U256Mul.u256Mul
import com.decimal128.U256Pow10.pow10BitLen
import com.decimal128.U256Pow10.pow10Offset
import com.decimal128.Residue.Companion.EXACT


internal object U256ScalePow10 {

    fun u256ScaleUpPow10(z: U256, x: U256, pow10: Int) {
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
                            z.u256Set192(p2, p1, p0)
                            return
                        }
                        u256Mul(z, x, pow10BitLen, pow10dw0)
                    }
                    (pow10BitLen <= 128) -> {
                        if (maxBitLen <= 192) {
                            val (p2, p1, p0) = umul128x128to192(x.dw1, x.dw0, pow10dw1, pow10dw0)
                            z.u256Set192(p2, p1, p0)
                            return
                        }
                        u256Mul(z, x, pow10BitLen, pow10dw1, pow10dw0)
                    }
                    (pow10BitLen <= 192) -> {
                        u256Mul(z, x, pow10BitLen, POW10[pow10Offset + 2], pow10dw1, pow10dw0)
                    }
                    (pow10BitLen <= 256) -> {
                        u256Mul(z, x, pow10BitLen, POW10[pow10Offset + 3], POW10[pow10Offset + 2], pow10dw1, pow10dw0)
                    }
                    else -> throw RuntimeException()
                }
            }
            pow10 == 0 -> z.u256Set(x)
            else -> throw RuntimeException()
        }
    }

    fun u256ScaleDownPow10(z: U256, x: U256, pow10: Int): Residue {
        if (x.bitLen > 0 && pow10 > 0) {
            val productDigitCount = x.digitLen - pow10
            if (productDigitCount <= 0) {
                val residue = if (productDigitCount == 0) Residue.residueFrom(x) else Residue.LT_HALF
                z.u256SetZero()
                return residue
            }
            return U256DivPow10.divPow10(z, x, pow10)
        }
        z.u256Set(x)
        return EXACT
    }

    fun u256ScaleFmaPow10(z: U256, x: U256, pow10: Int, a: U256) {
        U256Fma.u256FmaPow10(z, x, pow10, a)
    }

}