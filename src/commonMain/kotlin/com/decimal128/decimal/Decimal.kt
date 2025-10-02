package com.decimal128.decimal

import kotlin.math.max
import kotlin.math.min

@Suppress("NOTHING_TO_INLINE")
inline fun packLengths(digitLen: Int, bitLen: Int) =
    ((digitLen shl 9) or bitLen).toShort()

@Suppress("NOTHING_TO_INLINE")
inline fun packSignExp(sign: Boolean, qExp: Int): Short = ((if (sign) 0x8000 else 0) or qExp).toShort()

@Suppress("NOTHING_TO_INLINE")
private inline fun calcPackedLengths(dw0: Long): Short {
    val bitLen = calcBitLen64(dw0)
    val digitLen = U256Pow10.calcDigitLen64(bitLen, dw0)
    val packed = packLengths(digitLen, bitLen)
    return packed
}

@Suppress("NOTHING_TO_INLINE")
private inline fun calcPackedLengths(dw1: Long, dw0: Long): Short {
    val bitLen = calcBitLen128(dw1, dw0)
    val digitLen = U256Pow10.calcDigitLen128(bitLen, dw1, dw0)
    val packed = packLengths(digitLen, bitLen)
    return packed
}

@Suppress("NOTHING_TO_INLINE")
inline fun capExponentRange(e: Int): Int {
    return min(max(e, CAPPED_EXP_MIN), CAPPED_EXP_MAX)
}

class Decimal private constructor(
    @field: JvmField internal val dw1: Long,
    @field: JvmField internal val dw0: Long,
    @field: JvmField internal val lengths: Short,
    @field: JvmField internal val signExp: Short) {

    internal val bitLen: Int
        get() = lengths.toInt() and 0x1FF
    internal val digitLen: Int
        get() = (lengths.toInt() and 0xFFFF) shr 9

    internal val sign: Boolean
        get() = signExp.toInt() < 0
    internal val sign01: Int
        get() = signExp.toInt() ushr 31
    internal val sign0Neg1: Int
        get() = signExp.toInt() shr 31
    internal val qExp: Int
        get() = (signExp.toInt() shl 17) shr 17

    constructor(sign: Boolean, dw1: Long, dw0: Long, bitLen: Int, digitLen: Int, qExp: Int) :
            this(dw1, dw0, packLengths(digitLen, bitLen), packSignExp(sign, qExp))

    companion object {
        val ZERO = Decimal(0L, 0L, 0, 0)
        val NEG_ZERO = Decimal(0L, 0L, 0, Short.MIN_VALUE)
        val ONE = Decimal(0L, 1L, 1, 0)
        val POS_INFINITY = Decimal(0L, 0L, 0, NON_FINITE_INF.toShort())
        val NEG_INFINITY = Decimal(0L, 0L, 0, packSignExp(true, NON_FINITE_INF))
        val NaN = Decimal(0L, 0L, 0, NON_FINITE_QNAN.toShort())
        val sNaN = Decimal(0L, 0L, 0, NON_FINITE_SNAN.toShort())

        fun newZero(sign: Boolean, qExp: Int): Decimal {
            if (qExp == 0)
                return if (sign) NEG_ZERO else ZERO
            val cappedExp = capExponentRange(qExp)
            val signExp = packSignExp(sign, cappedExp)
            val zero = Decimal(0L, 0L, 0, signExp)
            return zero
        }

        fun from(n: Int) = from(n.toLong())

        fun from(l: Long): Decimal {
            return when {
                l == 0L -> ZERO
                l < 0L -> Decimal(0L, -l, calcPackedLengths(-l), Short.MIN_VALUE)
                l == 1L -> ONE
                else -> Decimal(0L, l, calcPackedLengths(l), 0)
            }
        }

        fun from(mutDec: MutDec): Decimal {
            require(mutDec.digitLen <= 34)
            require(mutDec.qExp <= DecFormat.DECIMAL_128.qMax || mutDec.qExp >= MIN_SPECIAL_VALUE)
            val dec = Decimal(mutDec.sign, mutDec.dw1, mutDec.dw0, mutDec.bitLen, mutDec.digitLen, mutDec.qExp)
            return dec
        }

        fun from(dw1: Long, dw0: Long, signExp: Short): Decimal {
            val lengths = calcPackedLengths(dw1, dw0)
            val dec = Decimal(dw1, dw0, lengths, signExp)
            return dec
        }
     }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun isZero() = lengths.toInt() == 0

    fun abs() = if (sign) negate() else this

    fun negate(): Decimal {
        return when {
            qExp < NON_FINITE_INF -> Decimal(dw1, dw0, lengths, (signExp.toInt() xor 0x8000).toShort())
            qExp == NON_FINITE_INF -> if (sign) POS_INFINITY else NEG_INFINITY
            qExp == NON_FINITE_QNAN -> this
            else -> NaN // sNaN -> qNaN
        }
    }

    internal fun validate(): Boolean {
        if (bitLen != calcBitLen128(dw1, dw0))
            return false
        if (digitLen != U256Pow10.calcDigitLen128(bitLen, dw1, dw0))
            return false;
        return true
    }




}
