package com.decimal128.decimal

import com.decimal128.decimal.U256Bits.calcBitLen128
import com.decimal128.decimal.DecEnv.Companion.DECIMAL128
import kotlin.math.max
import kotlin.math.min

class DecOld private constructor(
    @field: JvmField internal val dw1: Long,
    @field: JvmField internal val dw0: Long,
    @field: JvmField internal val packedLengths: Short,
    @field: JvmField internal val signExp: Short) {

    internal val bitLen: Int
        get() = packedLengths.toInt() and 0x1FF
    internal val digitLen: Int
        get() = (packedLengths.toInt() and 0xFFFF) shr 9

    internal val sign: Boolean
        get() = signExp.toInt() < 0
    internal val sign01: Int
        get() = signExp.toInt() ushr 31
    internal val sign0Neg1: Int
        get() = signExp.toInt() shr 31
    internal val qExp: Int
        get() = (signExp.toInt() shl 17) shr 17
    internal val sciExp: Int
        get() = qExp + (digitLen - (-digitLen ushr 31))

    constructor(sign: Boolean, dw1: Long, dw0: Long, bitLen: Int, digitLen: Int, qExp: Int) :
            this(dw1, dw0, packLengths(digitLen, bitLen), packSignExp(sign, qExp))

    constructor(sign: Boolean, dw1: Long, dw0: Long, qExp: Int) :
            this(dw1, dw0, calcPackedLengths(dw1, dw0), packSignExp(sign, qExp))

    companion object {
        val POS_ZERO = DecOld(0L, 0L, 0, 0)
        val NEG_ZERO = DecOld(0L, 0L, 0, Short.MIN_VALUE)
        val ZERO = POS_ZERO
        val POS_ONE = DecOld(0L, 1L, 1, 0)
        val NEG_ONE = DecOld(0L, 1L, 1, 0x8000.toShort())
        val ONE = POS_ONE
        val POS_INFINITY = DecOld(0L, 0L, 0, NON_FINITE_INF.toShort())
        val NEG_INFINITY = DecOld(0L, 0L, 0, packSignExp(true, NON_FINITE_INF))
        val INFINITY = POS_INFINITY
        val POS_QNAN = DecOld(0L, 0L, 0, NON_FINITE_QNAN.toShort())
        val NEG_QNAN = DecOld(0L, 0L, 0, (0x8000 or NON_FINITE_QNAN).toShort())
        val NaN = POS_QNAN
        val POS_SNAN = DecOld(0L, 0L, 0, NON_FINITE_SNAN.toShort())
        val NEG_SNAN = DecOld(0L, 0L, 0, (0x8000 or NON_FINITE_SNAN).toShort())
        val sNaN = POS_SNAN

        fun newZero(sign: Boolean, qExp: Int, env: DecEnv): DecOld {
            if (qExp == 0)
                return if (sign) NEG_ZERO else ZERO
            val finalExp = max(min(qExp, env.qMax), env.qTiny)
            val signExp = packSignExp(sign, finalExp)
            val zero = DecOld(0L, 0L, 0, signExp)
            return zero
        }

        fun newZero(signExp: Short, env: DecEnv): DecOld {
            return when {
                signExp.toInt() == 0 -> POS_ZERO
                signExp.toInt() == Short.MIN_VALUE.toInt() -> NEG_ZERO
                else -> {
                    val finalExp = max(min(unpackExp(signExp), env.qMax), env.qTiny)
                    val finalSignExp =
                        (finalExp or (signExp.toInt() and Short.MIN_VALUE.toInt())).toShort()
                    val zero = DecOld(0L, 0L, 0, finalSignExp)
                    return zero
                }
            }
        }

        fun from(n: Int) = from(n.toLong())

        fun from(l: Long): DecOld {
            return when {
                l == 0L -> ZERO
                l < 0L -> DecOld(0L, -l, calcPackedLengths(-l), Short.MIN_VALUE)
                l == 1L -> ONE
                else -> DecOld(0L, l, calcPackedLengths(l), 0)
            }
        }

        fun from(str: String) = DecOld.from(DECIMAL128.decTemps.mutDecResult.set(str))

        fun from(mutDec: MutDec): DecOld {
            require(mutDec.digitLen <= 34)
            require(mutDec.qExp <= DecFormat.DECIMAL_128.qMax || mutDec.qExp >= MIN_SPECIAL_VALUE)
            val dec = DecOld(mutDec.sign, mutDec.dw1, mutDec.dw0, mutDec.bitLen, mutDec.digitLen, mutDec.qExp)
            return dec
        }

        fun from(dw1: Long, dw0: Long, signExp: Short): DecOld {
            val lengths = calcPackedLengths(dw1, dw0)
            val dec = DecOld(dw1, dw0, lengths, signExp)
            return dec
        }

        fun from(dw1: Long, dw0: Long, packedLengths: Short, signExp: Short): DecOld {
            val dec = DecOld(dw1, dw0, packedLengths, signExp)
            return dec
        }

        fun from(sign: Boolean, dw1: Long, dw0: Long, qExp: Int): DecOld {
            val packedLengths = calcPackedLengths(dw1, dw0)
            val signExp = packSignExp(sign, qExp)
            val dec = DecOld(dw1, dw0, packedLengths, signExp)
            return dec
        }

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun bothFnz(x: DecOld, y: DecOld): Boolean {
            // both x.qExp and y.qExp must < MIN_SPECIAL_VALUE
            // and x and y must have non-zero bitLens
            // the only thing important in the following line is the sign bits
            return ((x.qExp - MIN_SPECIAL_VALUE) and
                    (y.qExp - MIN_SPECIAL_VALUE) and
                    -x.bitLen and
                    -y.bitLen) < 0
        }

        fun qNaN(sign: Boolean) = if (sign) NEG_QNAN else POS_QNAN

        fun qNaN(sign: Boolean, dw1: Long, dw0: Long): DecOld {
            val payloadIsZero = (dw1 or dw0) == 0L
            return when {
                payloadIsZero && sign -> NEG_QNAN
                payloadIsZero         -> POS_QNAN
                else -> DecOld(dw1, dw0, calcPackedLengths(dw1, dw0), packSignExp(sign, NON_FINITE_QNAN))
            }
        }

        fun sNaN(sign: Boolean) = if (sign) NEG_SNAN else POS_SNAN

        fun sNaN(sign: Boolean, dw1: Long, dw0: Long): DecOld {
            val payloadIsZero = (dw1 or dw0) == 0L
            return when {
                payloadIsZero && sign -> NEG_SNAN
                payloadIsZero         -> POS_SNAN
                else -> DecOld(dw1, dw0, calcPackedLengths(dw1, dw0), packSignExp(sign, NON_FINITE_SNAN))
            }
        }
        fun infinity(sign: Boolean) = if (sign) NEG_INFINITY else POS_INFINITY

        internal fun hasNaN(x: DecOld, y: DecOld): Boolean =
            max(x.qExp, y.qExp) >= NON_FINITE_QNAN

    }

    fun isZero() = this.packedLengths.toInt() == 0 && this.qExp < MIN_SPECIAL_VALUE
    fun isNotZero() = !isZero()
    fun isPosZero() = isZero() && !sign
    fun isNegZero() = isZero() && sign
    fun isOne() = this.packedLengths.toInt() == (1 shl 9) or 1
    fun isNaN() = this.qExp >= NON_FINITE_QNAN
    fun isFinite() = this.qExp < NON_FINITE_INF

    // 5.7.3 Decimal operation
    fun sameQuantum(other: DecOld) = (this.qExp == other.qExp)

    fun abs() = if (sign) negate() else this

    fun negate(): DecOld {
        return when {
            qExp < NON_FINITE_INF -> DecOld(dw1, dw0, packedLengths, (signExp.toInt() xor 0x8000).toShort())
            qExp == NON_FINITE_INF -> infinity(sign)
            packedLengths.toInt() != 0 -> DecOld(dw1, dw0, packedLengths, (signExp.toInt() xor 0x8000).toShort())
            qExp == NON_FINITE_QNAN -> qNaN(! sign)
            else -> sNaN(! sign)
        }
    }

    internal fun validate(): Boolean {
        if (bitLen != calcBitLen128(dw1, dw0))
            return false
        if (digitLen != U256Pow10.calcDigitLen128(bitLen, dw1, dw0))
            return false;
        return true
    }

    fun add(other: DecOld): DecOld = D128AddSub.addImpl(this, other.sign, other, DECIMAL128)
    fun mul(other: DecOld): DecOld = D128Mul.mulImpl(this, other, DECIMAL128)
    fun div(other: DecOld): DecOld = D128Div.divImpl(this, other, DECIMAL128)

    fun compareTo(other: DecOld): Int = D128Compare.compare(this, other)
    fun magnitudeCompareTo(other: DecOld): Int = D128Compare.magnitudeCompare(this, other)

    override fun toString(): String {
        val mutDec = MutDec()
        mutDec.set(this)
        return mutDec.toString()
    }


}
