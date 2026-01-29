package com.decimal128.decimal

import com.decimal128.decimal.DecEnv.Companion.DECIMAL128
import kotlin.math.max
import kotlin.math.min

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

    // the lower/upper bound of the normalized binary exponent interval
    // what is the range of binary exponents given a decimal with
    // bitLen bits in the coeff and qExp
    internal val bExpMin: Int
        get() = calcBExpMin(bitLen, qExp)
    internal val bExpMax: Int
        get() = calcBExpMax(bitLen, qExp)


    constructor(sign: Boolean, dw1: Long, dw0: Long, bitLen: Int, digitLen: Int, qExp: Int) :
            this(dw1, dw0, packLengths(digitLen, bitLen), packSignExp(sign, qExp))

    constructor(sign: Boolean, dw1: Long, dw0: Long, qExp: Int) :
            this(dw1, dw0, calcPackedLengths(dw1, dw0), packSignExp(sign, qExp))

    companion object {

        internal operator fun invoke(sign: Boolean, qExp: Int,
                                     digitLen: Int, bitLen: Int,
                                     dw1: Long, dw0: Long,
                                     allowNonCanonical: Boolean = false): Decimal {
            verify { bitLen == calcBitLen128(dw1, dw0) }
            verify { digitLen == calcDigitLen128(bitLen, dw1, dw0) }
            verify { digitLen <= 38 }
            verify { digitLen <= 34 || allowNonCanonical }
            return Decimal(dw1, dw0, packLengths(digitLen, bitLen), packSignExp(sign, qExp))
        }



        val POS_ZERO = Decimal(0L, 0L, 0, 0)
        val NEG_ZERO = Decimal(0L, 0L, 0, Short.MIN_VALUE)
        val ZERO = POS_ZERO
        val POS_ONE = Decimal(0L, 1L, 1, 0)
        val NEG_ONE = Decimal(0L, 1L, 1, 0x8000.toShort())
        val ONE = POS_ONE
        val POS_INFINITY = Decimal(0L, 0L, 0, NON_FINITE_INF.toShort())
        val NEG_INFINITY = Decimal(0L, 0L, 0, packSignExp(true, NON_FINITE_INF))
        val INFINITY = POS_INFINITY
        val POS_QNAN = Decimal(0L, 0L, 0, NON_FINITE_QNAN.toShort())
        val NEG_QNAN = Decimal(0L, 0L, 0, (0x8000 or NON_FINITE_QNAN).toShort())
        val NaN = POS_QNAN
        val POS_SNAN = Decimal(0L, 0L, 0, NON_FINITE_SNAN.toShort())
        val NEG_SNAN = Decimal(0L, 0L, 0, (0x8000 or NON_FINITE_SNAN).toShort())
        val sNaN = POS_SNAN

        // These have an implied binary decimal point at 2**32
        private const val LOG2_10_FLOOR: Long = 14_267_572_564L
        private const val LOG2_10_CEIL: Long = 14_267_572_565L

        private const val DECIMAL128_QTINY_Neg6176 = -6176
        private const val DECIMAL128_QMAX_6111 = 6111

        private const val HASH_CODE_SIGN_FALSE = 1237 * 31 * 31 * 31
        private const val HASH_CODE_SIGN_TRUE = 1231 * 31 * 31 * 31
        private const val HASH_CODE_POS_ZERO = 36851467
        private const val HASH_CODE_NEG_ZERO = 36672721
        private const val HASH_CODE_POS_INFINITY = 52593608
        private const val HASH_CODE_NEG_INFINITY = 52414862
        private const val HASH_CODE_NAN = 52594569

        private const val NINES_33_HI = 0x044B82FA09B5A53FL
        private const val NINES_33_LO = Long.MIN_VALUE or 0x06C2ABFAFF7FFFFFL
        private const val NINES_33_BITLEN = 112

        private const val NINES_38_HI = 0x4B3B4CA85A86C47AL
        private const val NINES_38_LO = 0x098A223FFFFFFFFFL
        private const val NINES_38_BITLEN = 127





        fun newZero(sign: Boolean, qExp: Int, env: DecEnv): Decimal {
            if (qExp == 0)
                return if (sign) NEG_ZERO else ZERO
            val finalExp = max(min(qExp, env.qMax), env.qTiny)
            val signExp = packSignExp(sign, finalExp)
            val zero = Decimal(0L, 0L, 0, signExp)
            return zero
        }

        fun newZero(signExp: Short, env: DecEnv): Decimal {
            return when {
                signExp.toInt() == 0 -> POS_ZERO
                signExp.toInt() == Short.MIN_VALUE.toInt() -> NEG_ZERO
                else -> {
                    val finalExp = max(min(unpackExp(signExp), env.qMax), env.qTiny)
                    val finalSignExp =
                        (finalExp or (signExp.toInt() and Short.MIN_VALUE.toInt())).toShort()
                    val zero = Decimal(0L, 0L, 0, finalSignExp)
                    return zero
                }
            }
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

        fun from(dw1: Long, dw0: Long, packedLengths: Short, signExp: Short): Decimal {
            val dec = Decimal(dw1, dw0, packedLengths, signExp)
            return dec
        }

        fun from(sign: Boolean, dw1: Long, dw0: Long, qExp: Int): Decimal {
            val packedLengths = calcPackedLengths(dw1, dw0)
            val signExp = packSignExp(sign, qExp)
            val dec = Decimal(dw1, dw0, packedLengths, signExp)
            return dec
        }

        /**
         * Computes a conservative **lower bound** on the binary exponent of a finite
         * decimal value whose coefficient has the given `bitLen` and whose decimal
         * exponent is `qExp`.
         *
         * ## Binary-Exponent Interval
         *
         * A finite decimal value can be written as:
         *
         *     x = coeff × 10^qExp
         *
         * Its exact (base-2) magnitude satisfies:
         *
         *     log2(|x|) = log2(coeff) + qExp * log2(10)
         *
         * Let:
         *
         *     bExp = floor(log2(|x|))
         *
         * This method computes **bExpMin**, a lower bound such that:
         *
         *     bExpMin ≤ bExp
         *
         * The true exponent always lies in the interval:
         *
         *     [bExpMin , bExpMax]
         *
         * ## Integer Approximation of qExp * log2(10)
         *
         * We approximate `qExp * log2(10)` using fixed-point multiplication:
         *
         *   log2(10) ≈ LOG2_10_{FLOOR,CEIL} / 2^32
         *
         * where LOG2_10_FLOOR and LOG2_10_CEIL are exact integer bounds such that:
         *
         *     LOG2_10_FLOOR / 2^32 ≤ log2(10) ≤ LOG2_10_CEIL / 2^32
         *
         * For qExp ≥ 0 we multiply by LOG2_10_FLOOR (still ≤ true value).
         * For qExp < 0 we multiply by LOG2_10_CEIL (more negative, still ≤ true value).
         *
         * The product is then shifted right by 32 bits, which performs:
         *
         *     floor( scaled / 2^32 )
         *
         * for both positive and negative values, because arithmetic right-shift in
         * two’s-complement is mathematically equal to floor division by powers of two.
         *
         * ## Final Lower Bound
         *
         * The coefficient contributes `(bitLen - 1)` to the binary exponent range.
         * Adding the scaled qExp term yields:
         *
         *     bExpMin = floor( (bitLen - 1) + qExp * log2(10) )
         *
         * The result is further masked so that a zero-length coefficient (`bitLen == 0`)
         * yields `bExpMin = 0`.
         *
         * This method is branch-free and guaranteed correct: the returned value is
         * always ≤ the true binary exponent, never above it.
         */
        internal fun calcBExpMin(bitLen: Int, qExp: Int): Int {
            val bitLenNonZeroMask = -bitLen shr 31
            val q64 = qExp.toLong()
            val bitLenLess1 = bitLen - 1
            val bExpMinWhenPos =
                (bitLenLess1 + ((q64 * LOG2_10_FLOOR) shr 32)).toInt() and bitLenNonZeroMask
            val bExpMinWhenNeg =
                (bitLenLess1 + ((q64 * LOG2_10_CEIL) shr 32)).toInt() and bitLenNonZeroMask
            val signMask = qExp shr 31
            val bExpMin = (bExpMinWhenNeg and signMask) or (bExpMinWhenPos and signMask.inv())
            return bExpMin
        }

        /**
         * Computes a conservative **upper bound** on the binary exponent of a finite
         * decimal value whose coefficient has the given `bitLen` and whose decimal
         * exponent is `qExp`.
         *
         * ## Binary-Exponent Upper Bound
         *
         * For:
         *
         *     x = coeff × 10^qExp
         *
         * let:
         *
         *     bExp = floor(log2(|x|))
         *
         * This method computes **bExpMax**, an upper bound such that:
         *
         *     bExp ≤ bExpMax
         *
         * Together with `bExpMin`, this forms the binary-exponent interval:
         *
         *     [bExpMin , bExpMax]
         *
         * ## Deriving the Upper Bound
         *
         * We want:
         *
         *     bExpMax ≥ ceil( bitLen + qExp * log2(10) ) - 1
         *
         * To compute the ceiling of `qExp * log2(10)` using only integer arithmetic,
         * we use the same fixed-point scaling as `calcBExpMin`, but invert signs so
         * that a simple arithmetic right-shift produces a ceiling instead of a floor.
         *
         * For any value `x`:
         *
         *     ceil(x) = -floor(-x)
         *
         * Thus we multiply by the negated LOG2_10 constant:
         *
         *   q ≥ 0 → use -LOG2_10_CEIL     (upper bound)
         *   q < 0 → use -LOG2_10_FLOOR
         *
         * Then shift right by 32 bits, which computes:
         *
         *     floor( negatedScaled / 2^32 )
         *
         * and negating again yields the desired ceiling.
         *
         * Combining coefficient and exponent terms produces:
         *
         *     bExpMax = ceil( bitLen + qExp * log2(10) ) - 1
         *
         * Finally, a mask forces the result to zero when `bitLen == 0`.
         *
         * This method is branch-free and guaranteed correct: the returned value is
         * always ≥ the true binary exponent, never below it.
         */
        internal fun calcBExpMax(bitLen: Int, qExp: Int): Int {
            val bitLenNonZeroMask = -bitLen shr 31
            val q64 = qExp.toLong()
            // shifting 2's complement takes the floor for pos and neg
            // we need the ceiling
            // the negative of the negated floor is the ceiling
            // we'll negate the constants ... the first negation ... before the shift
            // we'll subtract (the negative bShift) the from bitLen ... the second negation
            val bExpMaxWhenPos =
                (bitLen - ((q64 * -LOG2_10_CEIL) shr 32) - 1).toInt() and bitLenNonZeroMask
            val bExpMaxWhenNeg =
                (bitLen - ((q64 * -LOG2_10_FLOOR) shr 32) - 1).toInt() and bitLenNonZeroMask
            val signMask = qExp shr 31
            val bExpMax = (bExpMaxWhenNeg and signMask) or (bExpMaxWhenPos and signMask.inv())
            return bExpMax
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

        fun qNaN(sign: Boolean) = if (sign) NEG_QNAN else POS_QNAN

        fun qNaN(sign: Boolean, dw1: Long, dw0: Long): Decimal {
            val payloadIsZero = (dw1 or dw0) == 0L
            return when {
                payloadIsZero && sign -> NEG_QNAN
                payloadIsZero         -> POS_QNAN
                else -> Decimal(dw1, dw0, calcPackedLengths(dw1, dw0), packSignExp(sign, NON_FINITE_QNAN))
            }
        }

        fun sNaN(sign: Boolean) = if (sign) NEG_SNAN else POS_SNAN

        fun sNaN(sign: Boolean, dw1: Long, dw0: Long): Decimal {
            val payloadIsZero = (dw1 or dw0) == 0L
            return when {
                payloadIsZero && sign -> NEG_SNAN
                payloadIsZero         -> POS_SNAN
                else -> Decimal(dw1, dw0, calcPackedLengths(dw1, dw0), packSignExp(sign, NON_FINITE_SNAN))
            }
        }

        fun NaN(sign: Boolean, signaling: Boolean = false): Decimal {
            return when {
                !signaling && !sign -> POS_QNAN
                !signaling && sign -> NEG_QNAN
                sign -> NEG_SNAN
                else -> POS_SNAN
            }
        }

        /**
         * Constructs a quiet or signaling NaN with an optional diagnostic payload.
         *
         * The payload is provided as a full 128-bit unsigned integer, split into a
         * high 64-bit word (`payloadDw1`) and a low 64-bit word (`payloadDw0`). If the
         * payload is zero, the canonical quiet NaN or signaling NaN (with no payload)
         * is returned.
         *
         * Decimal128 NaN payloads have a canonical limit of **33 decimal digits**.
         * If `allowOversizePayload` is `false` and the supplied payload exceeds this
         * limit, the payload is clamped to the maximal canonical value of
         * 33 nines.
         *
         * If `allowOversizePayload` is `true`, oversized payloads are accepted
         * verbatim, up to the full 128-bit range, and no clamping is performed.
         *
         * The `signaling` flag selects between quiet NaN (`qNaN`) and signaling NaN
         * (`sNaN`); the sign bit is preserved as provided, although it has no numeric
         * meaning.
         *
         * @param sign whether the NaN has its sign bit set
         * @param signaling whether to construct an `sNaN` instead of a `qNaN`
         * @param payloadDw1 the high 64 bits of the diagnostic payload
         * @param payloadDw0 the low 64 bits of the diagnostic payload
         * @param allowOversizePayload whether payloads longer than the canonical
         *        33-digit limit should be accepted verbatim rather than clamped
         *
         * @return a `Decimal2` representing a quiet or signaling NaN
         */
        fun NaN(
            sign: Boolean = false, signaling: Boolean = false,
            payloadDw1: Long, payloadDw0: Long = 0L, allowOversizePayload: Boolean = false
        ): Decimal {
            if ((payloadDw1 or payloadDw0) == 0L)
                return NaN(sign, signaling)
            val qExp = if (signaling) NON_FINITE_SNAN else NON_FINITE_QNAN
            var p0 = payloadDw0
            var p1 = payloadDw1
            var bitLen = calcBitLen128(payloadDw1, payloadDw0)
            var digitLen = calcDigitLen128(bitLen, payloadDw1, payloadDw0)
            if (digitLen > 33) {
                if (! allowOversizePayload) {
                    p1 = NINES_33_HI
                    p0 = NINES_33_LO
                    bitLen = NINES_33_BITLEN
                    digitLen = 33
                } else if (digitLen > 38) {
                    p1 = NINES_38_HI
                    p0 = NINES_38_LO
                    bitLen = NINES_38_BITLEN
                    digitLen = 38
                }
            }
            return Decimal(sign, qExp, digitLen, bitLen, p1, p0, allowOversizePayload)
        }


        fun infinity(sign: Boolean) = if (sign) NEG_INFINITY else POS_INFINITY

        internal fun hasNaN(x: Decimal, y: Decimal): Boolean =
            max(x.qExp, y.qExp) >= NON_FINITE_QNAN

    }

    fun isZero() = this.packedLengths.toInt() == 0 && this.qExp < MIN_SPECIAL_VALUE
    fun isNotZero() = !isZero()
    fun isPosZero() = isZero() && !sign
    fun isNegZero() = isZero() && sign
    fun isOne() = this.packedLengths.toInt() == (1 shl 9) or 1
    fun isNaN() = this.qExp >= NON_FINITE_QNAN
    fun isFinite() = this.qExp < NON_FINITE_INF

    // 5.7.3 Decimal2 operation
    fun sameQuantum(other: Decimal) = (this.qExp == other.qExp)

    fun abs() = if (sign) negate() else this

    fun negate(): Decimal {
        return when {
            qExp < NON_FINITE_INF -> Decimal(dw1, dw0, packedLengths, (signExp.toInt() xor 0x8000).toShort())
            qExp == NON_FINITE_INF -> infinity(sign)
            packedLengths.toInt() != 0 -> Decimal(dw1, dw0, packedLengths, (signExp.toInt() xor 0x8000).toShort())
            qExp == NON_FINITE_QNAN -> qNaN(! sign)
            else -> sNaN(! sign)
        }
    }

    internal fun validate(): Boolean {
        if (bitLen != calcBitLen128(dw1, dw0))
            return false
        if (digitLen != calcDigitLen128(bitLen, dw1, dw0))
            return false;
        return true
    }

    fun add(other: Decimal): Decimal = D128AddSub.addImpl(this, other.sign, other, DECIMAL128)
    fun mul(other: Decimal): Decimal = D128Mul.mulImpl(this, other, DECIMAL128)
    fun div(other: Decimal): Decimal = D128Div.divImpl(this, other, DECIMAL128)

    fun compareTo(other: Decimal): Int = D128Compare.compare(this, other)
    fun magnitudeCompareTo(other: Decimal): Int = D128Compare.magnitudeCompare(this, other)

    override fun toString(): String {
        val mutDec = MutDec()
        mutDec.set(this)
        return mutDec.toString()
    }


}
