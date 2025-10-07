package com.decimal128.decimal

import com.decimal128.decimal.DecEnv.Companion.DECIMAL128

class Decimal private constructor(
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
        val ZERO = Decimal(0L, 0L, 0, 0)
        val POS_ZERO = ZERO
        val NEG_ZERO = Decimal(0L, 0L, 0, Short.MIN_VALUE)
        val ONE = Decimal(0L, 1L, 1, 0)
        val INFINITY = Decimal(0L, 0L, 0, NON_FINITE_INF.toShort())
        val POS_INFINITY = INFINITY
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

        fun from(str: String) = Decimal.from(DECIMAL128.decTemps.mutDecResult.set(str))

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

        fun from(sign: Boolean, dw1: Long, dw0: Long, qExp: Int): Decimal {
            val packedLengths = calcPackedLengths(dw1, dw0)
            val signExp = packSignExp(sign, qExp)
            val dec = Decimal(dw1, dw0, packedLengths, signExp)
            return dec
        }

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun bothFnz(x: Decimal, y: Decimal): Boolean {
            // both x.qExp and y.qExp must < MIN_SPECIAL_VALUE
            // and x and y must have non-zero bitLens
            // the only thing important in the following line is the sign bits
            return ((x.qExp - MIN_SPECIAL_VALUE) and
                    (y.qExp - MIN_SPECIAL_VALUE) and
                    -x.bitLen and
                    -y.bitLen) < 0
        }

    }

    fun isZero() = this.packedLengths.toInt() == 0 && this.qExp < MIN_SPECIAL_VALUE
    fun isNotZero() = !isZero()
    fun isPosZero() = isZero() && !sign
    fun isNegZero() = isZero() && sign
    fun isOne() = this.packedLengths.toInt() == (1 shl 9) or 1
    fun isNaN() = this.qExp >= NON_FINITE_QNAN
    fun isFinite() = this.qExp < NON_FINITE_INF

    // 5.7.3 Decimal operation
    fun sameQuantum(other: Decimal) = (this.qExp == other.qExp)

    fun abs() = if (sign) negate() else this

    fun negate(): Decimal {
        return when {
            qExp < NON_FINITE_INF -> Decimal(dw1, dw0, packedLengths, (signExp.toInt() xor 0x8000).toShort())
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

    /* cannot have these because they take precedence over
     * my DecEnv member extension operators

    operator fun plus(other: Decimal): Decimal = D128Add.addImpl(this, other.sign, other, DECIMAL128)
    operator fun minus(other: Decimal): Decimal = D128Add.addImpl(this, !other.sign, other, DECIMAL128)
    operator fun times(other: Decimal): Decimal = D128Mul.mulImpl(this, other, DECIMAL128)
    operator fun div(other: Decimal): Decimal = D128Div.divImpl(this, other, DECIMAL128)

     */

    fun add(other: Decimal): Decimal = D128AddSub.addImpl(this, other.sign, other, DECIMAL128)
    fun sub(other: Decimal): Decimal = D128AddSub.addImpl(this, !other.sign, other, DECIMAL128)
    fun mul(other: Decimal): Decimal = D128Mul.mulImpl(this, other, DECIMAL128)
    fun div(other: Decimal): Decimal = D128Div.divImpl(this, other, DECIMAL128)

    fun compareTo(other: Decimal): Int = D128Compare.compare(this, other)
    fun magnitudeCompareTo(other: Decimal): Int = D128Compare.magnitudeCompare(this, other)

    fun coefficientCompareTo(other: Decimal): Int {
        val cmpBitLen = this.bitLen.compareTo(other.bitLen)
        if (cmpBitLen != 0)
            return cmpBitLen
        return ucmp128(this.dw1, this.dw0, other.dw1, other.dw0)
    }

    override fun toString(): String {
        val mutDec = MutDec()
        mutDec.set(this)
        return mutDec.toString()
    }

}
