package com.decimal128.decimal

import com.decimal128.decimal.DecEnv.Companion.DECIMAL128
import com.decimal128.decimal.Ieee754Class.negativeInfinity
import com.decimal128.decimal.Ieee754Class.negativeNormal
import com.decimal128.decimal.Ieee754Class.negativeSubnormal
import com.decimal128.decimal.Ieee754Class.negativeZero
import com.decimal128.decimal.Ieee754Class.positiveInfinity
import com.decimal128.decimal.Ieee754Class.positiveNormal
import com.decimal128.decimal.Ieee754Class.positiveSubnormal
import com.decimal128.decimal.Ieee754Class.positiveZero
import com.decimal128.decimal.Ieee754Class.quietNaN
import com.decimal128.decimal.Ieee754Class.signalingNaN
import kotlin.math.max
import kotlin.math.min

class Decimal private constructor(
    @field: JvmField internal val dw1: Long,
    @field: JvmField internal val dw0: Long,
    @field: JvmField internal val packedLengths: Short,
    @field: JvmField internal val signExp: Short): Comparable<Decimal> {

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
    internal val eExp: Int
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

        internal fun neitherIsNaN(x: Decimal, y: Decimal) =
            x.qExp < NON_FINITE_QNAN && y.qExp < NON_FINITE_QNAN

    }

    fun isNotZero() = !isZero()
    fun isPosZero() = isZero() && !sign
    fun isNegZero() = isZero() && sign
    fun isOne() = this.packedLengths.toInt() == (1 shl 9) or 1

    // IEEE754-2019 5.7.2

    /**
     * Returns the IEEE-754-2019 *class* of this decimal128 value.
     *
     * This implements the classification defined in IEEE-754-2019 §7.5.2
     * (“General operations”), which specifies a 10-way partition of all
     * floating-point values into NaNs, infinities, zeros, subnormals,
     * and normals, each distinguished by sign and (for NaNs) signaling
     * behavior.
     *
     * The returned value is one of:
     *
     *  * **quietNaN** – a non-finite datum encoded as a quiet NaN
     *  * **signalingNaN** – a non-finite datum encoded as a signaling NaN
     *  * **positiveInfinity / negativeInfinity** – signed infinities
     *  * **positiveZero / negativeZero** – signed zeros
     *  * **positiveSubnormal / negativeSubnormal** – finite values whose
     *    *derived scientific exponent* `eExp` is below the minimum normal exponent
     *  * **positiveNormal / negativeNormal** – all other finite, non-zero decimals
     *
     * Classification is determined using the value’s encoded sign,
     * coefficient bit-length, quantum exponent (`qExp`), and its
     * derived adjusted/scientific exponent (`eExp`).
     * NaNs are identified first from the non-finite `qExp` range; zeros
     * are detected via a zero coefficient; and subnormals are identified
     * by comparing `eExp` with the minimum normal exponent for decimal128 (–6143).
     *
     * This routine performs no rounding, signaling, or exception
     * processing; it only inspects the encoding to produce the IEEE-754
     * class of the operand for decimal128
     *
     * @return the IEEE-754 class of this decimal128 value, in accordance
     *         with IEEE-754-2019 §7.5.2.
     */
    fun ieeeClass(): Ieee754Class {
        return when {
            qExp >= NON_FINITE_INF -> when {
                qExp == NON_FINITE_QNAN -> quietNaN
                qExp > NON_FINITE_QNAN -> signalingNaN
                sign -> negativeInfinity
                else -> positiveInfinity
            }
            bitLen == 0 && sign -> negativeZero
            bitLen == 0 -> positiveZero
            eExp < -6143 && sign -> negativeSubnormal
            eExp < -6143 -> positiveSubnormal
            sign -> negativeNormal
            else -> positiveNormal
        }
    }

    /**
     * isSignMinus(x) is true if and only if x has negative sign. isSignMinus applies to zeros and NaNs
     * as well.
     */
    fun isSignMinus(): Boolean = sign // IEEE754 5.7.2
    fun isNegative(): Boolean = sign

    /**
     * isNormal(x) is true if and only if x is normal (not zero, subnormal, infinite, or NaN).
     * In this implementation, the check for subnormal is hardwired to the
     * decimal128 `eExp` adjusted (scientific) exponent -6143 and 34 digits.
     */
    fun isNormal(): Boolean =
        qExp < NON_FINITE_INF &&
                digitLen <= 34 &&
                bitLen > 0 &&
                eExp >= -6143

    /**
     * isSubnormal(x) is true if and only if x is subnormal
     * (not zero, normal, infinite, or NaN).
     * In this implementation, the check for subnormal is hardwired to the
     * decimal128 `eExp` adjusted (scientific) exponent -6143.
     *
     * This last test is the same as: `qExp == -6176 && digitLen < 34`
     */
    fun isSubnormal(): Boolean = qExp < NON_FINITE_INF && bitLen > 0 && eExp < -6143

    /**
     * isFinite(x) is true if and only if x is zero, normal, or subnormal
     * (not infinite or NaN).
     */
    fun isFinite(): Boolean = qExp < NON_FINITE_INF

    /**
     * isFiniteNonZero(x) is true if and only if x is normal or subnormal
     * (not zero, infinite, or NaN).
     */
    fun isFiniteNonZero(): Boolean = qExp < NON_FINITE_INF && bitLen > 0

    /**
     * isZero(x) is true if and only if x is ±0.
     *
     * Recall that in the decimal floating point world, the zero cohort
     * consists of all valid exponents with the zero coefficient.
     *
     * This version tests for Zero, even on oversized coefficients.
     */
    fun isZero(): Boolean = isFinite() && (bitLen == 0 || digitLen > 34)

    /**
     * isCanonicalZero(x) is true if x is ±0 and the coefficient
     * digit length <= 34.
     *
     * IEEE rules state that nonCanonical coefficients must be treated
     * as zero. Therefore, more than 34 digits == 0
     */
    fun isCanonicalZero(): Boolean = isFinite() && (bitLen == 0 || digitLen > 34)

    /**
     * isInfinite(x) is true if and only if x is infinite.
     *
     * This includes positiveInfinity and negativeInfinity.
     */
    fun isInfinite(): Boolean = qExp == NON_FINITE_INF

    /**
     * isNaN(x) is true if and only if x is a NaN.
     *
     * NaN Not A Number values may be quiet or signaling.
     */
    fun isNaN(): Boolean = qExp >= NON_FINITE_QNAN

    /**
     * isSignaling(x) is true if and only if x is a signaling NaN.
     *
     * Generally, when sNaN signaling NaN values are encountered during
     * operations an environment flag is set and trapping to an
     * optional exception handler may take place.
     */
    fun isSignaling(): Boolean = qExp == NON_FINITE_SNAN

    /**
     * In the context of Decimal128 BID encoding, the only
     * non-canonical encodings are coeff
     * - finite: digits > 34
     * - infinite: non-zero payload/coeff
     * - NaN: payload digits > 33
     */
    fun isCanonical(): Boolean {
        verify { bitLen == calcBitLen128(dw1, dw0) }
        verify { digitLen == calcDigitLen128(bitLen, dw1, dw0) }
        return (qExp in -6176..6111 && digitLen <= 34) ||
                (qExp == NON_FINITE_INF && bitLen == 0) ||
                (qExp > NON_FINITE_INF && digitLen <= 33)
    }

    // 5.7.3 Decimal operation
    /**
     * Returns `true` if this value and [other] have the **same quantum**, i.e.,
     * if they use the **same encoded exponent (qExp)**,
     * following IEEE 754-2019 §5.7.3 Decimal2 Operation.
     *
     * In IEEE 754-2019 terminology, two decimal numbers have the *same quantum*
     * when their exponents are identical after encoding. This is a structural
     * property of the representation, not of numerical value. For example,
     * `1.230 × 10⁻²` and `12.30 × 10⁻³` are numerically equal but **do not**
     * have the same quantum because their exponents differ.
     *
     * This method performs no normalization or coefficient adjustments; it
     * simply compares the raw `qExp` fields.
     */
    fun sameQuantum(other: Decimal) =
        (this.qExp == other.qExp) || (this.qExp >= NON_FINITE_QNAN && other.qExp >= NON_FINITE_QNAN)

    fun abs() = if (sign) negate() else this

    fun negate(): Decimal {
        return when {
            qExp < NON_FINITE_INF -> Decimal(dw1, dw0, packedLengths, (signExp.toInt() xor 0x8000).toShort())
            qExp == NON_FINITE_INF -> if (sign) POS_INFINITY else NEG_INFINITY
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

    /**
     * Compares this decimal128 value with [other] using the IEEE-754
     * *totalOrder* relation.
     *
     * This operation follows the ordering referenced in
     * IEEE-754-2019 **§5.7.2 General operations** and defined in
     * **§5.10 Details of totalOrder predicate**, which specifies a *total*
     * ordering over all floating-point values, including NaNs, signed zeros,
     * subnormals, infinities, and normal numbers.
     *
     * Unlike the usual <, ≤, >, and ≥ comparisons, the `totalOrder`
     * relation produces a complete ordering for **all** values, including:
     *
     *  • members of the same *cohort* that represent the same numeric value
     *  • signaling and quiet NaNs, which are generally unordered
     *
     * Ordering is determined by IEEE-754 rules based on:
     *
     *  • **sign**
     *  • **magnitude**, using canonical comparison of exponents and
     *    coefficients
     *  • **NaN category**, where signaling NaNs precede quiet NaNs, and
     *    NaNs are ordered by their payloads
     *
     * The return value uses the comparison convention:
     *
     *  • **−1** → `this` is less than `other`
     *  • **0**  → `this` and `other` are equal in totalOrder
     *  • **+1** → `this` is greater than `other`
     *
     * No rounding, exceptions, or signaling behavior are produced.
     *
     * @return −1, 0, or +1 indicating the total-order relationship between
     *         this value and [other].
     */

    fun compareTotalOrderTo(other: Decimal) = DecCompare1.cmpTotalOrder(this, other)

    fun isTotalOrder(other: Decimal) = compareTotalOrderTo(other) <= 0

    /**
     * Compares the *magnitudes* of two decimal128 values according to the
     * IEEE-754-2019 totalOrder rules (see §§5.10 and 5.7.2).
     *
     * This handles ordering among zeros, finite non-zero values, infinities,
     * and NaNs. Sign is *not* considered here.
     *
     * @return −1, 0, or +1 describing the total-order magnitude relation.
     */
    fun compareTotalOrderMagTo(other: Decimal) = DecCompare1.cmpTotalOrderMag(this, other)

    fun isTotalOrderMag(other: Decimal) = compareTotalOrderMagTo(other) <= 0

    /**
     * Compares this decimal128 value with [other] using **Java-style numeric
     * comparison** semantics.
     *
     * This follows the ordering defined by Java’s `Double.compare` and
     * `BigDecimal.compareTo`:
     *
     *  • All **finite non-zero** cohort members (different encodings of the
     *    same numerical value) compare as **equal**.
     *
     *  • **−0** compares less than **+0**, matching Java’s tie-break rule
     *    on the sign bit of zero.
     *
     *  • All **NaNs** compare greater than any non-NaN, and all NaNs are
     *    considered **equal**, ignoring sign, payload and signaling/quiet
     *    distinction.
     *
     *  • **−∞ < -normal < -0 < +0 < +normal < +∞ < NaN**
     *
     * No IEEE-754 flags are set, and signaling NaNs do not trigger
     * exceptions; they are treated as ordinary NaNs in the Java-style
     * sense.
     *
     * The return value uses the Kotlin/Java comparison convention:
     *
     *  • **−1** → `this` is less than [other]
     *  • **0**  → `this` and [other] are equal under Java-style rules
     *  • **+1** → `this` is greater than [other]
     *
     * @return −1, 0, or +1 describing the Java-style ordering of this
     *         value relative to [other].
     */
    fun compareJavaStyleTo(other: Decimal): Int = DecCompare1.cmpJavaStyle(this, other)

    fun equalsJavaStyle(other: Decimal): Boolean = DecCompare1.eqJavaStyle(this, other)

    /**
     * Compares this decimal128 value with [other] using **Java-style numeric
     * comparison** semantics. This is the default comparison used by the
     * Kotlin `Comparable` interface.
     *
     * The ordering matches Java’s `Double.compare` / `BigDecimal.compareTo`
     * behavior:
     *
     *  • All **finite non-zero** cohort members compare as **equal**
     *    (encoding differences such as trailing zeros are ignored).
     *
     *  • **−0 < +0**, matching Java’s signed-zero tie-break rule.
     *
     *  • Any finite value is less than **+∞**, and greater than **−∞**.
     *
     *  • **NaN** compares greater than all non-NaN values, and all NaNs
     *    compare equal to each other (payload and sNaN/qNaN differences
     *    are ignored).
     *
     * This comparison performs **no IEEE-754 signaling**, and does not
     * examine **NaN signs** or **NaN payloads**. It is a pure, deterministic,
     * context-free ordering suitable for sorting, sets, and all Kotlin/Java
     * comparison operations.
     *
     * For IEEE-754–compliant comparisons (quiet/signaling, ordered/unordered,
     * totalOrder, etc.), use the operations provided by `DecEnv`.
     *
     * @return −1, 0, or +1 according to Java-style numeric ordering.
     */
    override fun compareTo(other: Decimal): Int = compareJavaStyleTo(other)

    /**
     * Java-style equality: compares numerical value only.
     * Signed zeros compare as equal, and all NaNs compare equal to each other.
     */
    override fun equals(other: Any?): Boolean =
        other is Decimal && equalsJavaStyle(other)

    /**
     * Returns `true` if and only if this `Decimal2` value has the exact same
     * internal bitwise representation as [other].
     *
     * This performs a strict, field-by-field comparison of the underlying
     * 128-bit Decimal128 value, including:
     *
     *  * the sign bit
     *  * the exponent field (including the encoded NaN/sNaN pattern)
     *  * the coefficient field (including any NaN payload bits)
     *
     * No numeric interpretation is applied. This is therefore stricter than
     * IEEE 754 *numeric equality*. In particular:
     *
     *  * `+0` and `-0` are **not** equal
     *  * all NaNs are unequal unless every representation bit matches
     *  * canonical vs non-canonical encodings would differ (although this
     *    implementation produces only canonical encodings)
     *
     * This operator is intended for conformance and regression testing, where
     * the produced Decimal128 representation must match the expected value
     * exactly.
     *
     * Example:
     *
     * ```
     * assertTrue(observed bitwiseEQ expected)
     * ```
     */
    infix fun bitwiseEQ(other: Decimal): Boolean =
        this.packedLengths == other.packedLengths &&
                this.signExp == other.signExp &&
                this.dw1 == other.dw1 && this.dw0 == other.dw0

    fun add(other: Decimal): Decimal = D128AddSub.addImpl(this, other.sign, other, DECIMAL128)
    fun mul(other: Decimal): Decimal = D128Mul.mulImpl(this, other, DECIMAL128)
    fun div(other: Decimal): Decimal = D128Div.divImpl(this, other, DECIMAL128)

    fun magnitudeCompareTo(other: Decimal): Int = D128Compare.magnitudeCompare(this, other)

    // this is only provided for IEEE754-2019 completeness.
    // in an immutable world it serves no purpose
    fun copy(): Decimal = Decimal(dw1, dw0, packedLengths, signExp)

    override fun hashCode(): Int {
        return when {
            isFiniteNonZero() -> {
                var r1 = dw1
                var r0 = dw0
                var rQExp = qExp
                if (qExp < DECIMAL128_QMAX_6111) {
                    val maxNtzdClamp = DECIMAL128_QMAX_6111 - qExp
                    val (t1, t0, ntzd) = DecNtzd.ntzdU128(r1, r0)
                    r1 = t1
                    r0 = t0
                    rQExp += ntzd
                    if (ntzd > maxNtzdClamp) {
                        verify { rQExp > DECIMAL128_QMAX_6111 }
                        // oops ... we removed too many trailing zeros and pushed qExp too hi
                        // give back some zeros to bring down qExp
                        val giveBack = ntzd - maxNtzdClamp
                        val (t1, t0) = DecPow10.umul128Pow10(r1, r0, giveBack)
                        r1 = t1
                        r0 = t0
                        rQExp = DECIMAL128_QMAX_6111
                    }
                }
                val hcSign = if (sign) HASH_CODE_SIGN_TRUE else HASH_CODE_SIGN_FALSE
                val hcQExp = rQExp * 31 * 31
                val hcDw1 = (r1 xor (r1 shr 32)).toInt() * 31
                val hcDw0 = (r0 xor (r0 shr 32)).toInt()
                hcSign + hcQExp + hcDw1 + hcDw0
            }
            isNegative() && isZero() -> HASH_CODE_NEG_ZERO
            isZero() -> HASH_CODE_POS_ZERO
            isNegative() && isInfinite() -> HASH_CODE_NEG_INFINITY
            isInfinite() -> HASH_CODE_POS_INFINITY
            else -> HASH_CODE_NAN
        }
    }

    override fun toString(): String {
        val mutDec = MutDec()
        mutDec.set(this)
        return mutDec.toString()
    }

}
