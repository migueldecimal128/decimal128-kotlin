@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.U256Bits.calcBitLen128
import com.decimal128.decimal.U256Pow10.calcDigitLen128

// SEAL = Sign Exponent And Lengths
@JvmInline
value class Seal private constructor(val seal: Int) {
    companion object {

        operator fun invoke(signBit: Int, qExp: Int, digitLen: Int, bitLen: Int): Seal {
            check (signBit in 0..1)
            check (qExp in -6176..6111 || qExp in NON_FINITE_INF..NON_FINITE_SNAN)
            check (digitLen in 0..38)
            check (bitLen in 0..127)
            return Seal((signBit shl 31) or
                    ((qExp and 0x7FFF) shl 16) or
                    (digitLen shl 9) or
                    bitLen)
        }

        operator fun invoke(sign: Boolean, qExp: Int, digitLen: Int, bitLen: Int): Seal =
            Seal(if (sign) 1 else 0, qExp, digitLen, bitLen)

        operator fun invoke(sign: Boolean, qExp: Int, dw1: ULong, dw0: ULong): Seal {
            val bitLen = calcBitLen128(dw1, dw0)
            val digitLen = calcDigitLen128(bitLen, dw1, dw0)
            return Seal(if (sign) 1 else 0, qExp, digitLen, bitLen)
        }

        internal fun calcSeal(signBit: Int, qExp: Int, dw1: ULong, dw0: ULong): Seal {
            val bitLen = calcBitLen128(dw1, dw0)
            val digitLen = calcDigitLen128(bitLen, dw1, dw0)
            return Seal(signBit, qExp, digitLen, bitLen)
        }

    }
    internal val bitLen: Int
        get() = seal and 0x1FF
    internal val digitLen: Int
        get() = (seal shr 9) and 0x7F

    internal val isNegative: Boolean
        get() = seal < 0
    internal val isPositive: Boolean
        get() = seal >= 0
    internal val signBit: Int
        get() = seal ushr 31
    internal val signMask: Int
        get() = seal shr 31
    internal val qExp: Int
        get() = (seal shl 1) shr 17
}