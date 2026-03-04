// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.C128ScalePow10.c128ScaleDownPow10
import com.decimal128.decimal.DecCompare1.compareQuiet754
import com.decimal128.decimal.DecCompare1.compareSignaling754
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
import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.Residue.Companion.LT_HALF
import kotlin.math.max
import kotlin.math.min

// commonMain
expect open class DecimalRep(steal: Int, seal: Int, dw1: Long, dw0: Long) {
    internal val steal: Int
    internal val seal: Int
    internal val dw1: Long
    internal val dw0: Long
}

class Decimal private constructor(
    steal: Int,
    seal: Int,
    dw1: Long,
    dw0: Long):
    DecimalRep(steal, seal, dw1, dw0), Comparable<Decimal> {

    internal val bitLen: Int
        // get() = stealBitLen(steal) // problems with non-Canonical ... as expected
        get() = seal and 0x1FF
    internal val digitLen: Int
        // get() = stealDigitLen(steal)
        get() = (seal shr 9) and 0x3F

    internal val sign: Boolean
        get() = steal < 0
    internal val signBit: Int
        get() = steal ushr 31
    internal val signMask: Int
        get() = steal shr 31

    internal val qExp: Int
        // get() = stealQexp(steal) // lots of problems due to code expecting qExp >= NON_FINITE_INF
        get() = (seal shl 1) shr 17

    internal val eExp: Int
        get() = qExp + (digitLen - (-digitLen ushr 31))

    // the lower/upper bound of the normalized binary exponent interval
    // what is the range of binary exponents given a decimal with
    // bitLen bits in the coeff and qExp
    internal val bExpMin: Int
        get() = calcBExpMin(bitLen, qExp)
    internal val bExpMax: Int
        get() = calcBExpMax(bitLen, qExp)


    companion object {

        private const val BIT31 = Int.MIN_VALUE

        internal const val NON_FINITE_BIT  = 0x4000_0000
        internal const val QNAN_BIT        = 0x1000_0000
        internal const val SNAN_BIT        = 0x0800_0000
        internal const val INF_BIT         = 0x0400_0000

        internal const val QNAN_SEAL       = NON_FINITE_BIT or QNAN_BIT
        internal const val SNAN_SEAL       = NON_FINITE_BIT or SNAN_BIT
        internal const val INF_SEAL        = NON_FINITE_BIT or INF_BIT

        internal operator fun invoke(sign: Boolean, qExp: Int, dw1: Long, dw0: Long): Decimal {
            verify { qExp >= -16384 && qExp <= 16383 }
            val steal = stealRaw(sign, qExp, dw1, dw0)
            return Decimal(
                steal,
                (if (sign) BIT31 else 0) or
                        ((qExp and 0x7FFF) shl 16) or
                        calcPackedLengths128(dw1, dw0),
                dw1, dw0)
        }

        internal operator fun invoke(sign: Boolean, qExp: Int,
                                     digitLen: Int, bitLen: Int,
                                     dw1: Long, dw0: Long,
                                     allowNonCanonical: Boolean = false): Decimal {
            verify { bitLen == calcBitLen128(dw1, dw0) }
            verify { digitLen == calcDigitLen128(bitLen, dw1, dw0) }
            verify { digitLen <= 38 }
            verify { digitLen <= 34 || allowNonCanonical }
            val steal = stealRaw(sign, qExp, dw1, dw0)
            return Decimal(
                steal,
                (if (sign) BIT31 else 0) or
                        ((qExp and 0x7FFF) shl 16) or
                        (digitLen shl 9) or
                        bitLen,
                dw1, dw0)
        }

        internal operator fun invoke(sign: Boolean, qExp: Int,
                                     digitLen: Int, bitLen: Int,
                                     dw1: Long, dw0: Long,
                                     ctx: DecContext): Decimal {
            verify { bitLen == calcBitLen128(dw1, dw0) }
            verify { digitLen == calcDigitLen128(bitLen, dw1, dw0) }
            verify { when {
                qExp < NON_FINITE_INF -> digitLen < ctx.precision
                qExp == NON_FINITE_INF -> digitLen == 0
                else -> digitLen <= ctx.decFormat.nanPayloadPrecision
            } }
            val steal = stealRaw(sign, qExp, dw1, dw0)
            return Decimal(
                steal,
                (if (sign) BIT31 else 0) or
                        ((qExp and 0x7FFF) shl 16) or
                        (digitLen shl 9) or
                        bitLen,
                dw1, dw0)
        }

        internal operator fun invoke(sign: Boolean, qExp: Int,
                                     dw1: Long, dw0: Long,
                                     allowNonCanonical: Boolean = false): Decimal {
            verify {
                val digitLen = calcDigitLen128(dw1, dw0)
                digitLen <= 34 || (allowNonCanonical && digitLen <= 38)
            }
            val steal = stealRaw(sign, qExp, dw1, dw0)
            return Decimal(
                steal,
                (if (sign) BIT31 else 0) or
                        ((qExp and 0x7FFF) shl 16) or
                        calcPackedLengths128(dw1, dw0),
                dw1, dw0
            )
        }

        val POS_ZEROe0 = Decimal(stealEncodeZER(0, 0),     0, 0L, 0L)
        val NEG_ZEROe0 = Decimal(stealEncodeZER(1, 0), BIT31, 0L, 0L)
        val ZERO = POS_ZEROe0
        val POS_ONE = Decimal(stealEncodeFNZ(0, 0, 0L, 1L),          0x0201, 0L, 1L)
        val NEG_ONE = Decimal(stealEncodeFNZ(1, 0, 0L, 1L), BIT31 or 0x0201, 0L, 1L)
        val ONE = POS_ONE
        val POS_INFINITY = Decimal(stealEncodeINF(0),          (NON_FINITE_INF shl 16), 0L, 0L)
        val NEG_INFINITY = Decimal(stealEncodeINF(1), BIT31 or (NON_FINITE_INF shl 16), 0L, 0L)
        val INFINITY = POS_INFINITY
        val POS_QNAN = Decimal(stealEncodeQNAN(0, 0L, 0L),          (NON_FINITE_QNAN shl 16), 0L, 0L)
        val NEG_QNAN = Decimal(stealEncodeQNAN(1, 0L, 0L), BIT31 or (NON_FINITE_QNAN shl 16), 0L, 0L)
        val NaN = POS_QNAN
        val POS_SNAN = Decimal(stealEncodeSNAN(0, 0L, 0L),          (NON_FINITE_SNAN shl 16), 0L, 0L)
        val NEG_SNAN = Decimal(stealEncodeSNAN(1, 0L, 0L), BIT31 or (NON_FINITE_SNAN shl 16), 0L, 0L)
        val sNaN = POS_SNAN

        private const val DECIMAL128_QTINY_Neg6176 = -6176
        private const val DECIMAL128_QMAX_6111 = 6111

        private const val HASH_CODE_SIGN_FALSE = 1237 * 31 * 31 * 31
        private const val HASH_CODE_SIGN_TRUE = 1231 * 31 * 31 * 31
        private const val HASH_CODE_POS_ZERO = 36851467
        private const val HASH_CODE_NEG_ZERO = 36672721
        private const val HASH_CODE_POS_INFINITY = 52593608
        private const val HASH_CODE_NEG_INFINITY = 52414862
        private const val HASH_CODE_NAN = 52594569

        fun zero(sign: Boolean): Decimal = if (sign) NEG_ZEROe0 else POS_ZEROe0

        fun zero(sign: Boolean, qExp: Int, ctx: DecContext): Decimal {
            if (qExp == 0)
                return if (sign) NEG_ZEROe0 else POS_ZEROe0
            val qClamped = max(min(qExp, ctx.qMax), ctx.qTiny)
            val seal =
                (if (sign) BIT31 else 0) or ((qClamped and 0x7FFF) shl 16)
            val signBit = if (sign) 1 else 0
            val steal = stealEncodeZER(signBit, qClamped)
            val zero = Decimal(stealEncodeZER(signBit, qClamped), seal, 0L, 0L)
            return zero
        }

        fun from(n: Int) = from(n.toLong())

        fun from(l: Long): Decimal {
            return when {
                l == 0L -> ZERO
                l < 0L -> Decimal(sign = true, 0, 0L, -l)
                l == 1L -> ONE
                else -> Decimal(sign = false, 0, 0L, l)
            }
        }

        fun fromDPD(dwHi: Long, dwLo: Long): Decimal {
            TODO()
        }

        /**
         * Parses a decimal128 value from its textual representation.
         *
         * This is a strict, context-free parser for the **decimal128** interchange
         * format. It accepts only valid decimal128 encodings:
         *
         *  • up to 34 decimal digits in the coefficient
         *  • exponent within the decimal128 range (−6143 to +6144)
         *  • optional leading sign
         *  • no rounding is performed; the input must fit exactly
         *
         * The parser produces a fully-formed `Decimal2` value using only the
         * decimal128 rules. More flexible or environment-dependent parsing
         * (including rounding, alternate syntaxes, or extended formats) should
         * be performed via `DecContext.parse()`.
         *
         * Any malformed input results in:
         *  ```
         *  IllegalArgumentException("invalid decimal format")
         *  ```
         *
         * @param str a textual representation of a decimal128 value
         * @return the parsed `Decimal2` value
         * @throws IllegalArgumentException if the text does not encode a valid decimal128
         */
        fun from(str: String, ctx: DecContext = DecContext.DECIMAL128) =
            D128ParsePrint.parseDecimal(str, ctx)

        fun from(mutDec: MutDec, ctx: DecContext = DecContext.DECIMAL128): Decimal {
            require(mutDec.digitLen <= ctx.precision)
            require(mutDec.qExp <= ctx.qMax || mutDec.qExp >= MIN_SPECIAL_VALUE)
            val dec = Decimal(mutDec.sign, mutDec.qExp, mutDec.dw1, mutDec.dw0)
            return dec
        }

        fun qNaN(sign: Boolean) = if (sign) NEG_QNAN else POS_QNAN

        fun qNaN(sign: Boolean, dw1: Long, dw0: Long): Decimal {
            val payloadIsZero = (dw1 or dw0) == 0L
            return when {
                payloadIsZero && sign -> NEG_QNAN
                payloadIsZero         -> POS_QNAN
                else -> Decimal(sign, NON_FINITE_QNAN, dw1, dw0)
            }
        }

        fun sNaN(sign: Boolean) = if (sign) NEG_SNAN else POS_SNAN

        fun sNaN(sign: Boolean, dw1: Long, dw0: Long): Decimal {
            val payloadIsZero = (dw1 or dw0) == 0L
            return when {
                payloadIsZero && sign -> NEG_SNAN
                payloadIsZero         -> POS_SNAN
                else -> Decimal(sign, NON_FINITE_QNAN, dw1, dw0)
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
            // FIXME - this routine is only used by Intel BID
            //  should use the following with the ctx
            if ((payloadDw1 or payloadDw0) == 0L)
                return NaN(sign, signaling)
            val qExp = if (signaling) NON_FINITE_SNAN else NON_FINITE_QNAN
            val bitLen = calcBitLen128(payloadDw1, payloadDw0)
            val digitLen = calcDigitLen128(bitLen, payloadDw1, payloadDw0)
            if (digitLen > 33 && !allowOversizePayload)
                return NaN(sign, signaling)
            return Decimal(sign, qExp, digitLen, bitLen, payloadDw1, payloadDw0, allowOversizePayload)
        }

        fun NaN(
            sign: Boolean = false, signaling: Boolean = false,
            payloadDw1: Long, payloadDw0: Long = 0L, ctx: DecContext
        ): Decimal {
            if ((payloadDw1 or payloadDw0) == 0L)
                return NaN(sign, signaling)
            val qExp = if (signaling) NON_FINITE_SNAN else NON_FINITE_QNAN
            val bitLen = calcBitLen128(payloadDw1, payloadDw0)
            val digitLen = calcDigitLen128(bitLen, payloadDw1, payloadDw0)
            if (digitLen > ctx.decFormat.nanPayloadPrecision)
                return NaN(sign, signaling)
            return Decimal(sign, qExp, digitLen, bitLen, payloadDw1, payloadDw0, ctx)
        }


        fun infinity(sign: Boolean) = if (sign) NEG_INFINITY else POS_INFINITY

        fun infinityNonCanonical(sign: Boolean, dw1: Long, dw0: Long): Decimal =
            if ((dw1 or dw0) == 0L)
                infinity(sign)
            else
                Decimal(sign, NON_FINITE_INF, dw1, dw0, allowNonCanonical = true)

        internal fun hasNaN(x: Decimal, y: Decimal): Boolean {
            val result1 = max(x.qExp, y.qExp) >= NON_FINITE_QNAN
            val result2 = stealIsNAN(x.steal) or stealIsNAN(y.steal)
            check (result1 == result2)
            return result2
        }

        internal fun neitherIsNaN(x: Decimal, y: Decimal) = !hasNaN(x, y)
//            x.qExp < NON_FINITE_QNAN && y.qExp < NON_FINITE_QNAN

    }

    fun precision(): Int = if (isFinite()) digitLen else Int.MIN_VALUE
    fun qExponent(): Int = if (isFinite()) qExp else Int.MIN_VALUE
    fun eExponent(): Int = if (isFinite()) eExp else Int.MIN_VALUE

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
        val sign = sign; val qExp = qExp; val bitLen = bitLen
        return when {
            qExp >= NON_FINITE_INF -> when {
                qExp == NON_FINITE_QNAN -> { verify { stealIsQNAN(steal) } ; quietNaN }
                qExp > NON_FINITE_QNAN -> { verify {stealIsSNAN(steal) } ; signalingNaN }
                sign -> { verify { stealIsINF(steal) } ; negativeInfinity }
                else -> { verify { stealIsINF(steal) } ; positiveInfinity }
            }
            bitLen == 0 && sign -> { verify { stealIsZER(steal) } ; negativeZero }
            bitLen == 0 -> { verify { stealIsZER(steal) } ; positiveZero }
            eExp < -6143 && sign -> { verify { stealIsFNZ(steal) } ; negativeSubnormal }
            eExp < -6143 -> { verify { stealIsFNZ(steal) } ; positiveSubnormal }
            sign -> { verify { stealIsZER(steal) } ; negativeNormal }
            else -> { verify { stealIsZER(steal) } ; positiveNormal }
        }
    }

    /**
     * isSignMinus(x) is true if and only if x has negative sign. isSignMinus applies to zeros and NaNs
     * as well.
     */
    fun isSignMinus(): Boolean = seal < 0 // IEEE754 5.7.2
    fun isNegative(): Boolean = seal < 0

    /**
     * isNormal(x) is true if and only if x is normal (not zero, subnormal, infinite, or NaN).
     * In this implementation, the check for subnormal is hardwired to the
     * decimal128 `eExp` adjusted (scientific) exponent -6143 and 34 digits.
     */
    fun isNormal(): Boolean {
        val result1 =  qExp < NON_FINITE_INF &&
                digitLen <= 34 &&
                bitLen > 0 &&
                eExp >= -6143
        val result2 = stealIsFNZ(steal) && digitLen <= 34 && eExp >= -6143
        check (result1 == result2)
        return result2
    }

    /**
     * isSubnormal(x) is true if and only if x is subnormal
     * (not zero, normal, infinite, or NaN).
     * In this implementation, the check for subnormal is hardwired to the
     * decimal128 `eExp` adjusted (scientific) exponent -6143.
     *
     * This last test is the same as: `qExp == -6176 && digitLen < 34`
     */
    fun isSubnormal(): Boolean = stealIsFNZ(steal) && eExp < -6143

    /**
     * isFinite(x) is true if and only if x is zero, normal, or subnormal
     * (not infinite or NaN).
     */
    fun isFinite(): Boolean {
        verify { stealIsFinite(steal) == qExp < NON_FINITE_INF }
        return stealIsFinite(steal)
    }

    /**
     * isFiniteNonZero(x) is true if and only if x is normal or subnormal
     * (not zero, infinite, or NaN).
     */
    fun isFiniteNonZero(): Boolean {
        val result1 = qExp < NON_FINITE_INF && bitLen > 0
        //verify { stealIsFNZ(steal) == qExp < NON_FINITE_INF && bitLen > 0 }
        val result2 = stealIsFNZ(steal)
        if (result1 != result2)
            println("foo!")
        check (result1 == result2)
        return result2
    }

    /**
     * isZero(x) is true if and only if x is ±0.
     *
     * Recall that in the decimal floating point world, the zero cohort
     * consists of all valid exponents with the zero coefficient.
     *
     * This version tests for Zero, even on oversized coefficients.
     */
    // FIXME - don't allow oversized coefficients here
    //  this is too fragile ... figure out another way to pass unit tests
    //fun isZero(): Boolean = isFinite() && (bitLen == 0 || digitLen > 34)
    // FIXME ... a few problems in unexpected places
    fun isZero(): Boolean = stealIsZER(steal)

    fun isNotZero() = !isZero()

    /**
     * isCanonicalZero(x) is true if x is ±0 and the coefficient
     * digit length <= 34.
     *
     * IEEE rules state that nonCanonical coefficients must be treated
     * as zero. Therefore, more than 34 digits == 0
     */
    fun isCanonicalZero(): Boolean {
        val a = isFinite()
        val b = bitLen == 0
        val c = digitLen > 34
        val result1 = isFinite() && (bitLen == 0 || digitLen > 34)
        val result2 = stealIsZER(steal)
        if (result1 != result2)
            println("foo!")
        check(result1 == result2)
        return result2
    }

    /**
     * isInfinite(x) is true if and only if x is infinite.
     *
     * This includes positiveInfinity and negativeInfinity.
     */
    fun isInfinite(): Boolean {
        verify { stealIsINF(steal) == (qExp == NON_FINITE_INF) }
        return stealIsINF(steal)
    }

    /**
     * isNaN(x) is true if and only if x is a NaN.
     *
     * NaN Not A Number values may be quiet or signaling.
     */
    fun isNaN(): Boolean {
        verify { stealIsNAN(steal) == (qExp >= NON_FINITE_QNAN) }
        return stealIsNAN(steal)
    }

    /**
     * isSignaling(x) is true if and only if x is a signaling NaN.
     *
     * Generally, when sNaN signaling NaN values are encountered during
     * operations an environment flag is set and trapping to an
     * optional exception handler may take place.
     */
    fun isSignaling(): Boolean {
        verify { stealIsSNAN(steal) == (qExp == NON_FINITE_SNAN) }
        return stealIsSNAN(steal)
        //return qExp == NON_FINITE_SNAN
    }

    /**
     * In the context of Decimal128 BID encoding, the only
     * non-canonical encodings are coeff
     * - finite: digits > 34
     * - infinite: non-zero payload/coeff
     * - NaN: payload digits > 33
     */
    fun isCanonical(): Boolean {
        val qExp = qExp; val digitLen = digitLen; val bitLen = bitLen;
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
    fun isSameQuantum(other: Decimal) =
        (this.qExp == other.qExp) || (this.qExp >= NON_FINITE_QNAN && other.qExp >= NON_FINITE_QNAN)

    fun abs() = if (steal < 0) negate() else this

    fun negate(): Decimal {
        return when {
            qExp != NON_FINITE_INF -> Decimal(steal xor BIT31, seal xor BIT31, dw1, dw0)
            sign -> POS_INFINITY
            else -> NEG_INFINITY
        }
    }

    fun copySign(signDonor: Decimal): Decimal =
        if (this.sign == signDonor.sign) this else this.negate()

    internal fun validate(): Boolean {
        if (bitLen != calcBitLen128(dw1, dw0))
            return false
        if (digitLen != calcDigitLen128(bitLen, dw1, dw0))
            return false;
        return true
    }

    // IEEE754-2008 5.3.3
    fun logB(ctx: DecContext): Decimal {
        val qExp = qExp
        when {
            isZero() -> return ctx.signalDivByZero(NEG_INFINITY)
            qExp < NON_FINITE_INF -> return from(eExp)
            qExp == NON_FINITE_INF -> return POS_INFINITY
            else -> return nanOperandFound(this, ctx)
        }

    }

    fun scaleB(pow10Delta: Int, ctx: DecContext): Decimal {
        val sign = sign; val qExp = qExp
        when {
            qExp < NON_FINITE_INF -> {
                val pow10DeltaCapped = max(min(pow10Delta, 100_000), -100_000)
                val qNew = qExp + pow10DeltaCapped
                return decFinalizeFinite(sign, dw1, dw0, qNew, ctx)
            }
            qExp == NON_FINITE_INF -> return this
            else -> return nanOperandFound(this, ctx)
        }
    }

    fun quantumExponent(ctx: DecContext): Int {
        if (isFinite())
            return qExp
        ctx.signalInvalid()
        return Int.MIN_VALUE
    }

    fun nextUp(ctx: DecContext): Decimal = nextUpOrDown(isUp = true, this, ctx)

    fun nextDown(ctx: DecContext): Decimal = nextUpOrDown(isUp = false, this, ctx)

    fun stripTrailingZeros(ctx: DecContext): Decimal = stripTrailingZerosImpl(this, ctx)

    fun withScale(decimalScale: Int, ctx: DecContext): Decimal = withScale(this, decimalScale, ctx)
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
     * totalOrder, etc.), use the operations provided by `DecContext`.
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
        ((this.seal - other.seal).toLong() or
                (this.dw1 - other.dw1) or
                (this.dw0 - other.dw0)) == 0L

    fun magnitudeCompareTo(other: Decimal): Int = D128Compare.magnitudeCompare(this, other)

    // IEEE754-2019 5.6.1 Comparisons

    fun compareQuiet(other: Decimal, ctx: DecContext): Compare754Result =
        compareQuiet754(this, other, ctx)

    fun compareQuietEqual(other: Decimal, ctx: DecContext): Boolean =
        compareQuiet754(this, other, ctx) == IEEE754_EQ

    fun compareQuietNotEqual(other: Decimal, ctx: DecContext): Boolean =
        compareQuiet754(this, other, ctx) != IEEE754_EQ

    fun compareSignalingEqual(other: Decimal, ctx: DecContext): Boolean =
        compareSignaling754(this, other, ctx) == IEEE754_EQ

    fun compareSignalingGreater(other: Decimal, ctx: DecContext): Boolean =
        compareSignaling754(this, other, ctx) == IEEE754_GT

    fun compareSignalingGreaterEqual(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = compareSignaling754(this, other, ctx)
        return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_EQ)
    }

    fun compareSignalingLess(other: Decimal, ctx: DecContext): Boolean =
        compareSignaling754(this, other, ctx) == IEEE754_LT

    fun compareSignalingLessEqual(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = compareSignaling754(this, other, ctx)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_EQ)
    }

    fun compareSignalingNotEqual(other: Decimal, ctx: DecContext): Boolean =
        compareSignaling754(this, other, ctx) != IEEE754_EQ

    fun compareSignalingNotGreater(other: Decimal, ctx: DecContext): Boolean =
        compareSignaling754(this, other, ctx) != IEEE754_GT

    fun compareSignalingLessUnordered(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = compareSignaling754(this, other, ctx)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_UNORDERED)
    }

    fun compareSignalingNotLess(other: Decimal, ctx: DecContext): Boolean =
        compareSignaling754(this, other, ctx) != IEEE754_LT

    fun compareSignalingGreaterUnordered(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = compareSignaling754(this, other, ctx)
        return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_UNORDERED)
    }

    fun compareQuietGreater(other: Decimal, ctx: DecContext): Boolean =
        compareQuiet754(this, other, ctx) == IEEE754_GT

    fun compareQuietGreaterEqual(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = compareQuiet754(this, other, ctx)
        return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_EQ)
    }

    fun compareQuietLess(other: Decimal, ctx: DecContext): Boolean =
        compareQuiet754(this, other, ctx) == IEEE754_LT

    fun compareQuietLessEqual(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = compareQuiet754(this, other, ctx)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_EQ)
    }

    fun compareQuietUnordered(other: Decimal, ctx: DecContext): Boolean =
        compareQuiet754(this, other, ctx) == IEEE754_UNORDERED

    fun compareQuietNotGreater(other: Decimal, ctx: DecContext): Boolean =
        compareQuiet754(this, other, ctx) != IEEE754_GT

    fun compareQuietLessUnordered(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = compareQuiet754(this, other, ctx)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_UNORDERED)
    }

    fun compareQuietNotLess(other: Decimal, ctx: DecContext): Boolean =
        compareQuiet754(this, other, ctx) != IEEE754_LT

    fun compareQuietGreaterUnordered(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = compareQuiet754(this, other, ctx)
        return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_UNORDERED)
    }

    fun compareQuietOrdered(other: Decimal, ctx: DecContext): Boolean =
        compareQuiet754(this, other, ctx) != IEEE754_UNORDERED


    // this is only provided for IEEE754-2019 completeness.
    // in an immutable world it serves no purpose
    fun copy(): Decimal = Decimal(steal, seal, dw1, dw0)

    override fun hashCode(): Int {
        val sign = sign; val qExp = qExp
        return when {
            isFiniteNonZero() -> {
                var r1 = dw1
                var r0 = dw0
                var rQ = qExp
                if (qExp < DECIMAL128_QMAX_6111) {
                    val maxNtzdClamp = DECIMAL128_QMAX_6111 - qExp
                    val (t1, t0, ntzd) = DecNtzd.ntzdU128(r1, r0)
                    r1 = t1
                    r0 = t0
                    rQ += ntzd
                    if (ntzd > maxNtzdClamp) {
                        verify { rQ > DECIMAL128_QMAX_6111 }
                        // oops ... we removed too many trailing zeros and pushed qExp too hi
                        // give back some zeros to bring down qExp
                        val giveBack = ntzd - maxNtzdClamp
                        val (t1, t0) = umul128xPow10to128(r1, r0, giveBack)
                        r1 = t1
                        r0 = t0
                        rQ = DECIMAL128_QMAX_6111
                    }
                }
                val hcSign = if (sign) HASH_CODE_SIGN_TRUE else HASH_CODE_SIGN_FALSE
                val hcQExp = rQ * 31 * 31
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

    /**
     * Returns the canonical text form of this value.
     *
     * Formatting is compatible with Java’s `BigDecimal.toString()`,
     * including the use of an uppercase **'E'** for scientific notation.
     *
     * The exact formatting rules (plain decimal vs scientific notation)
     * follow the same general conventions as `BigDecimal`: values with
     * very large or very small exponents may be rendered using an
     * `E`-notation exponent; others appear as a decimal-point string.
     *
     * @return a canonical decimal128 textual representation of this value
     */
    override fun toString(): String = D128ParsePrint.toString(this)

    operator fun plus(other: Decimal): Decimal = addImpl(this, other)
    context(decContext: DecContext)
    operator fun plus(other: Decimal): Decimal = addImpl(this, other, decContext)

    operator fun minus(other: Decimal): Decimal = subImpl(this, other)
    context(decContext: DecContext)
    operator fun minus(other: Decimal): Decimal = subImpl(this, other, decContext)

    operator fun times(other: Decimal): Decimal = mulImpl(this, other)
    context(decContext: DecContext)
    operator fun times(other: Decimal): Decimal = mulImpl(this, other, decContext)

    operator fun div(other: Decimal): Decimal = divImpl(this, other)
    context(decContext: DecContext)
    operator fun div(other: Decimal): Decimal = divImpl(this, other, decContext)

    operator fun rem(other: Decimal): Decimal = remTruncImpl(this, other)
    context(decContext: DecContext)
    operator fun rem(other: Decimal): Decimal = remTruncImpl(this, other, decContext)

    inline fun remainderTruncate(other: Decimal): Decimal = rem(other)
    fun remainderNear(other: Decimal): Decimal = rem(other)

    fun roundToIntegralTiesToEven(ctx: DecContext) =
        roundToIntegral(DecRounding.ROUND_TIES_TO_EVEN, ctx, beQuiet = true)

    fun roundToIntegralTiesToAway(ctx: DecContext) =
        roundToIntegral(DecRounding.ROUND_TIES_TO_AWAY, ctx, beQuiet = true)

    fun roundToIntegralTowardZero(ctx: DecContext) =
        roundToIntegral(DecRounding.ROUND_TOWARD_ZERO, ctx, beQuiet = true)

    fun roundToIntegralTowardPositive(ctx: DecContext) =
        roundToIntegral(DecRounding.ROUND_TOWARD_POSITIVE, ctx, beQuiet = true)

    fun roundToIntegralTowardNegative(ctx: DecContext) =
        roundToIntegral(DecRounding.ROUND_TOWARD_NEGATIVE, ctx, beQuiet = true)

    fun roundToIntegralExact(ctx: DecContext) =
        roundToIntegral(ctx.decRounding, ctx, beQuiet = false)

    fun roundToIntegral(rounding: DecRounding, ctx: DecContext, beQuiet: Boolean = false): Decimal {
        val sign = sign; val qExp = qExp; val dw1 = dw1; val dw0 = dw0
        if (qExp >= 0) {
            if (qExp < NON_FINITE_SNAN)
                return this
            return nanOperandFound(this, ctx)
        }
        if (isZero())
            return zero(sign)
        val pow10 = -qExp
        val tmpPair = ctx.decTmps.dwPair1
        tmpPair.dw0 = 0L
        tmpPair.dw1 = 0L
        val digitLen = this.digitLen
        val residue: Residue = when {
            pow10 > digitLen -> LT_HALF
            pow10 == digitLen -> Residue.fromValuePow10(dw1, dw0, digitLen)
            else -> C128ScalePow10.c128ScaleDownPow10(tmpPair, dw1, dw0, pow10)
        }
        return decRoundAndFinalizeFinite(sign, tmpPair.dw1, tmpPair.dw0,
            residue, 0, rounding, ctx, beQuiet)
    }

    /**
     * Converts this decimal to a [Long] using IEEE 754-2019 `convertToIntegerTiesToEven`.
     * Rounds to nearest, ties to even. Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toLongTiesToEven(ctx: DecContext) =
        convertToLong(DecRounding.ROUND_TIES_TO_EVEN, ctx, suppressInexact = true)

    /**
     * Converts this decimal to a [Long] using IEEE 754-2019 `convertToIntegerTiesToAway`.
     * Rounds to nearest, ties away from zero. Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toLongTiesToAway(ctx: DecContext) =
        convertToLong(DecRounding.ROUND_TIES_TO_AWAY, ctx, suppressInexact = true)

    /**
     * Converts this decimal to a [Long] using IEEE 754-2019 `convertToIntegerTowardZero`.
     * Rounds toward zero (truncation). Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toLongTowardZero(ctx: DecContext) =
        convertToLong(DecRounding.ROUND_TOWARD_ZERO, ctx, suppressInexact = true)

    /**
     * Converts this decimal to a [Long] using IEEE 754-2019 `convertToIntegerTowardPositive`.
     * Rounds toward positive infinity (ceiling). Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toLongTowardPositive(ctx: DecContext) =
        convertToLong(DecRounding.ROUND_TOWARD_POSITIVE, ctx, suppressInexact = true)

    /**
     * Converts this decimal to a [Long] using IEEE 754-2019 `convertToIntegerTowardNegative`.
     * Rounds toward negative infinity (floor). Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toLongTowardNegative(ctx: DecContext) =
        convertToLong(DecRounding.ROUND_TOWARD_NEGATIVE, ctx, suppressInexact = true)

    /**
     * Converts this decimal to a [Long] using IEEE 754-2019 `convertToIntegerExactTiesToEven`.
     * Rounds to nearest, ties to even. Signals [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toLongExactTiesToEven(ctx: DecContext) =
        convertToLong(DecRounding.ROUND_TIES_TO_EVEN, ctx, suppressInexact = false)

    /**
     * Converts this decimal to a [Long] using IEEE 754-2019 `convertToIntegerExactTiesToAway`.
     * Rounds to nearest, ties away from zero. Signals [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toLongExactTiesToAway(ctx: DecContext) =
        convertToLong(DecRounding.ROUND_TIES_TO_AWAY, ctx, suppressInexact = false)

    /**
     * Converts this decimal to a [Long] using IEEE 754-2019 `convertToIntegerExactTowardZero`.
     * Rounds toward zero (truncation). Signals [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toLongExactTowardZero(ctx: DecContext) =
        convertToLong(DecRounding.ROUND_TOWARD_ZERO, ctx, suppressInexact = false)

    /**
     * Converts this decimal to a [Long] using IEEE 754-2019 `convertToIntegerExactTowardPositive`.
     * Rounds toward positive infinity (ceiling). Signals [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toLongExactTowardPositive(ctx: DecContext) =
        convertToLong(DecRounding.ROUND_TOWARD_POSITIVE, ctx, suppressInexact = false)

    /**
     * Converts this decimal to a [Long] using IEEE 754-2019 `convertToIntegerExactTowardNegative`.
     * Rounds toward negative infinity (floor). Signals [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toLongExactTowardNegative(ctx: DecContext) =
        convertToLong(DecRounding.ROUND_TOWARD_NEGATIVE, ctx, suppressInexact = false)

    /**
     * Core implementation for all decimal-to-[Long] conversions.
     *
     * Implements the IEEE 754-2019 `convertToInteger` family of operations for signed 64-bit integers.
     * The invalid sentinel [Long.MIN_VALUE] matches Intel's decimal library (bid128_to_int64_*).
     *
     * @param rounding the rounding mode to apply when the value is not exactly representable as a [Long]
     * @param ctx the decimal context for signaling flags
     * @param suppressInexact if true, suppresses [DecException.INEXACT] (used by the non-exact IEEE 754 variants).
     *   [DecException.INVALID_OPERATION] is always signaled regardless of this flag.
     * @return the converted [Long], or [Long.MIN_VALUE] if the value is NaN, infinite, or out of range
     */
    fun convertToLong(rounding: DecRounding, ctx: DecContext, suppressInexact: Boolean = false): Long {
        val signMaskLong = signMask.toLong()
        val sign = sign
        val qExp = qExp
        val bitLen = bitLen
        val digitLen = digitLen
        val dw0 = dw0
        when {
            qExp == 0 -> {
                if (bitLen < 64)
                    return (dw0 xor signMaskLong) - signMaskLong
                if (bitLen == 64 && dw0 == Long.MIN_VALUE && sign)
                    return Long.MIN_VALUE
                // return signalInvalid
            }
            qExp >= NON_FINITE_INF -> {
                // return signalInvalid
            }
            bitLen == 0 -> return 0L
            qExp > 0 -> {
                // if there is headroom then scale it up
                if (digitLen + qExp <= 19) {
                    val result = dw0 * pow10_64(qExp)
                    if (result > 0)
                        return (result xor signMaskLong) - signMaskLong
                    // Long.MIN_VALUE && sign is not possible ...
                    // ... because we just multiplied by 10**qExp
                    // ... so the value ends in 0
                    // ... but Long.MIN_VALUE ends in 8
                }
                // return signalInvalid
            }
            else -> { // qExp < 0
                // at least some fractional digits, perhaps 0 digits
                val fracDigitLen = -qExp
                if (fracDigitLen >= digitLen) {
                    // all fractional digits
                    val residue: Residue
                    if (fracDigitLen > digitLen)
                        residue = LT_HALF
                    else {
                        residue = Residue.fromValueDecade(this)
                        verify { residue != EXACT }
                    }
                    val roundUp = residue.ulpRoundUp(rounding.negate(sign), 0L)
                    val ret = if (! roundUp) 0L else (signMaskLong shl 1) or 1L
                    if (! suppressInexact)
                        ctx.signalInexact(this)
                    return ret
                }
                // both integral and fractional digits
                val intDigitLen = digitLen - fracDigitLen
                if (intDigitLen <= 19) {
                    val dwPair = ctx.decTmps.dwPair1
                    val residue = c128ScaleDownPow10(dwPair, dw1, dw0, fracDigitLen)
                    // DANGER! CAUTION! r0 might roll over to ZEEERO with this roundUp
                    var r0 = dwPair.dw0
                    val roundUp01 = residue.ulpRoundUp01L(rounding.negate(sign), r0)
                    r0 += roundUp01
                    verify { dwPair.dw1 == 0L }
                    if (r0 > 0L || r0 == Long.MIN_VALUE && sign) {
                        val ret = (r0 xor signMaskLong) - signMaskLong
                        if (!suppressInexact && residue != EXACT)
                            ctx.signalInexact(ret)
                        return ret
                    }
                }
                // return signalInvalid
            }
        }
        // return signalInvalid
        ctx.signalInvalid(this)
        return Long.MIN_VALUE
    }
    /**
     * Converts this decimal to an [Int] using IEEE 754-2019 `convertToIntegerTiesToEven`.
     * Rounds to nearest, ties to even. Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toIntTiesToEven(ctx: DecContext) =
        convertToInt(DecRounding.ROUND_TIES_TO_EVEN, ctx, suppressInexact = true)

    /**
     * Converts this decimal to an [Int] using IEEE 754-2019 `convertToIntegerTiesToAway`.
     * Rounds to nearest, ties away from zero. Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toIntTiesToAway(ctx: DecContext) =
        convertToInt(DecRounding.ROUND_TIES_TO_AWAY, ctx, suppressInexact = true)

    /**
     * Converts this decimal to an [Int] using IEEE 754-2019 `convertToIntegerTowardZero`.
     * Rounds toward zero (truncation). Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toIntTowardZero(ctx: DecContext) =
        convertToInt(DecRounding.ROUND_TOWARD_ZERO, ctx, suppressInexact = true)

    /**
     * Converts this decimal to an [Int] using IEEE 754-2019 `convertToIntegerTowardPositive`.
     * Rounds toward positive infinity (ceiling). Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toIntTowardPositive(ctx: DecContext) =
        convertToInt(DecRounding.ROUND_TOWARD_POSITIVE, ctx, suppressInexact = true)

    /**
     * Converts this decimal to an [Int] using IEEE 754-2019 `convertToIntegerTowardNegative`.
     * Rounds toward negative infinity (floor). Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toIntTowardNegative(ctx: DecContext) =
        convertToInt(DecRounding.ROUND_TOWARD_NEGATIVE, ctx, suppressInexact = true)

    /**
     * Converts this decimal to an [Int] using IEEE 754-2019 `convertToIntegerExactTiesToEven`.
     * Rounds to nearest, ties to even. Signals [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toIntExactTiesToEven(ctx: DecContext) =
        convertToInt(DecRounding.ROUND_TIES_TO_EVEN, ctx, suppressInexact = false)

    /**
     * Converts this decimal to an [Int] using IEEE 754-2019 `convertToIntegerExactTiesToAway`.
     * Rounds to nearest, ties away from zero. Signals [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toIntExactTiesToAway(ctx: DecContext) =
        convertToInt(DecRounding.ROUND_TIES_TO_AWAY, ctx, suppressInexact = false)

    /**
     * Converts this decimal to an [Int] using IEEE 754-2019 `convertToIntegerExactTowardZero`.
     * Rounds toward zero (truncation). Signals [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toIntExactTowardZero(ctx: DecContext) =
        convertToInt(DecRounding.ROUND_TOWARD_ZERO, ctx, suppressInexact = false)

    /**
     * Converts this decimal to an [Int] using IEEE 754-2019 `convertToIntegerExactTowardPositive`.
     * Rounds toward positive infinity (ceiling). Signals [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toIntExactTowardPositive(ctx: DecContext) =
        convertToInt(DecRounding.ROUND_TOWARD_POSITIVE, ctx, suppressInexact = false)

    /**
     * Converts this decimal to an [Int] using IEEE 754-2019 `convertToIntegerExactTowardNegative`.
     * Rounds toward negative infinity (floor). Signals [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toIntExactTowardNegative(ctx: DecContext) =
        convertToInt(DecRounding.ROUND_TOWARD_NEGATIVE, ctx, suppressInexact = false)

    /**
     * Core implementation for all decimal-to-[Int] conversions.
     *
     * Implements the IEEE 754-2019 `convertToInteger` family of operations for signed 32-bit integers.
     * All internal arithmetic is performed in [Long] to avoid overflow during intermediate calculations.
     * The invalid sentinel [Int.MIN_VALUE] matches Intel's decimal library (bid128_to_int32_*).
     *
     * @param rounding the rounding mode to apply when the value is not exactly representable as an [Int]
     * @param ctx the decimal context for signaling flags
     * @param suppressInexact if true, suppresses [DecException.INEXACT] (used by the non-exact IEEE 754 variants).
     *   [DecException.INVALID_OPERATION] is always signaled regardless of this flag.
     * @return the converted [Int], or [Int.MIN_VALUE] if the value is NaN, infinite, or out of range
     */
    fun convertToInt(rounding: DecRounding, ctx: DecContext, suppressInexact: Boolean = false): Int {
        val signMask = signMask
        val sign = sign
        val qExp = qExp
        val bitLen = bitLen
        val digitLen = digitLen
        val dw0 = dw0
        val w0 = dw0.toInt()
        when {
            qExp == 0 -> {
                if (bitLen < 32)
                    return (w0 xor signMask) - signMask
                if (bitLen == 32 && w0 == Int.MIN_VALUE && sign)
                    return Int.MIN_VALUE
                // return signalInvalid
            }
            qExp >= NON_FINITE_INF -> {
                // return signalInvalid
            }
            bitLen == 0 -> return 0
            qExp > 0 -> {
                // if there is headroom then scale it up
                if (digitLen + qExp <= 10) {
                    val result = dw0 * pow10_64(qExp)
                    if (result <= Int.MAX_VALUE.toLong())
                        return (result.toInt() xor signMask) - signMask
                    // Long.MIN_VALUE && sign is not possible ...
                    // ... because we just multiplied by 10**qExp
                    // ... so the value ends in 0
                    // ... but Long.MIN_VALUE ends in 8
                }
                // return signalInvalid
            }
            else -> { // qExp < 0
                // at least some fractional digits, perhaps 0 digits
                val fracDigitLen = -qExp
                if (fracDigitLen >= digitLen) {
                    // all fractional digits
                    val residue: Residue
                    if (fracDigitLen > digitLen)
                        residue = LT_HALF
                    else {
                        residue = Residue.fromValueDecade(this)
                        verify { residue != Residue.EXACT }
                    }
                    val roundUp = residue.ulpRoundUp(rounding.negate(sign), 0L)
                    val ret = if (! roundUp) 0 else (signMask shl 1) or 1
                    if (! suppressInexact)
                        ctx.signalInexact(this)
                    return ret
                }
                // both integral and fractional digits
                val intDigitLen = digitLen - fracDigitLen
                if (intDigitLen <= 10) {
                    val dwPair = ctx.decTmps.dwPair1
                    val residue = c128ScaleDownPow10(dwPair, dw1, dw0, fracDigitLen)
                    var r0 = dwPair.dw0
                    val roundUp01 = residue.ulpRoundUp01L(rounding.negate(sign), r0)
                    verify { dwPair.dw1 == 0L }
                    // r0 cannot roll over
                    // worse case is 10 9s 99999_99999 which rolls up to 11 digits
                    r0 += roundUp01
                    if (r0 <= Int.MAX_VALUE.toLong() ||
                        r0 == -(Int.MIN_VALUE.toLong()) && sign) {
                        val ret = (r0.toInt() xor signMask) - signMask
                        if (!suppressInexact && residue != EXACT)
                            ctx.signalInexact(this)
                        return ret
                    }
                }
                // return signalInvalid
            }
        }
        // return signalInvalid
        ctx.signalInvalid(this)
        return Int.MIN_VALUE
    }

}

context(ctx: DecContext)
fun String.toDecimal(): Decimal = Decimal.from(this, ctx)
