@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.U256Bits.calcBitLen128
import com.decimal128.decimal.U256Bits.calcBitLen64
import com.decimal128.decimal.U256Pow10.calcDigitLen128
import com.decimal128.decimal.U256Pow10.calcDigitLen64
import com.decimal128.hugeint.Magia
import kotlin.math.max

internal inline fun packSeal(sign: Boolean, qExp: Int, digitLen: Int, bitLen: Int): Int =
    (if (sign) Int.MIN_VALUE else 0) or (((qExp and 0x7FFF) shl 16) or
            (digitLen shl 9) or bitLen)

internal inline fun packSeal(sign01: Int, qExp: Int, digitLen: Int, bitLen: Int): Int =
    (sign01 shl 31) or (((qExp and 0x7FFF) shl 16) or (digitLen shl 9) or bitLen)

internal fun calcSeal(sign: Boolean, qExp: Int, dw1: ULong, dw0: ULong): Int {
    val bitLen = calcBitLen128(dw1, dw0)
    val digitLen = calcDigitLen128(bitLen, dw1, dw0)
    return packSeal(sign, qExp, digitLen, bitLen)
}

internal fun calcSeal(sign01: Int, qExp: Int, dw1: ULong, dw0: ULong): Int {
    val bitLen = calcBitLen128(dw1, dw0)
    val digitLen = calcDigitLen128(bitLen, dw1, dw0)
    return packSeal(sign01, qExp, digitLen, bitLen)
}

internal fun calcSeal(sign01: Int, dw0: ULong): Int {
    val bitLen = calcBitLen64(dw0)
    val digitLen = calcDigitLen64(bitLen, dw0)
    return packSeal(sign01, 0, digitLen, bitLen)
}

private const val SIGN_0 = 0
private const val SIGN_1 = 1

class Dec2 private constructor(
    // pronounced:
    // seal = Sign Exponent And Lengths
    internal val seal: Int,
    internal val dw1: ULong,
    internal val dw0: ULong
) {
    internal val bitLen: Int
        get() = seal and 0x1FF
    internal val digitLen: Int
        get() = (seal shr 9) and 0x7F

    internal val sign: Boolean
        get() = seal < 0
    internal val sign01: Int
        get() = seal ushr 31
    internal val sign0Neg1: Int
        get() = seal shr 31
    internal val qExp: Int
        get() = (seal shl 1) shr 17
    internal val sciExp: Int
        get() = qExp + (digitLen - (-digitLen ushr 31))

    constructor(sign: Boolean, qExp: Int, dw1: ULong, dw0: ULong) :
            this(calcSeal(sign, qExp, dw1, dw0), dw1, dw0)

    constructor(sign: Boolean, qExp: Int, digitLen: Int, bitLen: Int, dw1: ULong, dw0: ULong) :
            this(packSeal(sign, qExp, digitLen, bitLen), dw1, dw0)


    companion object {
        val POS_ZEROe0 = Dec2(SIGN_0, 0uL, 0uL)
        val NEG_ZEROe0 = POS_ZEROe0.negate()
        val POS_ONEe0 = from(1)
        val NEG_ONEe0 = POS_ONEe0.negate()
        val POS_INFINITY = Dec2(false, NON_FINITE_INF, 0, 0, 0uL, 0uL)
        val NEG_INFINITY = POS_INFINITY.negate()
        val POS_QNAN = Dec2(false, NON_FINITE_QNAN, 0, 0, 0uL, 0uL)
        val NEG_QNAN = POS_QNAN.negate()
        val POS_SNAN = Dec2(false, NON_FINITE_SNAN, 0, 0, 0uL, 0uL)
        val NEG_SNAN = POS_SNAN.negate()

        fun from(n: Int): Dec2 = from(n.toLong())

        fun from(w: UInt): Dec2 = from(w.toULong())

        fun from(l: Long): Dec2 {
            val mask = l shr 63
            val abs = ((l xor mask) - mask).toULong()
            return Dec2(calcSeal(mask.toInt(), abs), 0uL, abs)
        }

        fun from(dw: ULong): Dec2 = Dec2(calcSeal(SIGN_0, dw), 0uL, dw)

        fun from(str: String): Dec2 {
            // parse only Decimal128
            // 34 digits ... exponent in range
            // unsignedMulHi will give up to 128 bits ... good
            // no rounding.
            // Any parse error throws IllegalArgumentException("invalid decimal format")
            // if someone wants something more complicated then they use DecEnv.parse()
            TODO()
        }

        fun infinity(sign: Boolean) = if (sign) NEG_INFINITY else POS_INFINITY

    }

    fun negate(): Dec2 = Dec2(seal xor Int.MIN_VALUE, dw1, dw0)

    override fun toString(): String = Dec2ParsePrint.toString(this)
}

