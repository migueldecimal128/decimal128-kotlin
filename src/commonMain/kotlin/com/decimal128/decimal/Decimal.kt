// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Compare754Result.*
import com.decimal128.decimal.Decimal.Companion.INFINITY
import com.decimal128.decimal.Decimal.Companion.NEG_INFINITY
import com.decimal128.decimal.Decimal.Companion.NEG_ONEe0
import com.decimal128.decimal.Decimal.Companion.NEG_QNAN
import com.decimal128.decimal.Decimal.Companion.NEG_SNAN
import com.decimal128.decimal.Decimal.Companion.NEG_ZEROe0
import com.decimal128.decimal.Decimal.Companion.NaN
import com.decimal128.decimal.Decimal.Companion.ONE
import com.decimal128.decimal.Decimal.Companion.POS_INFINITY
import com.decimal128.decimal.Decimal.Companion.POS_ONEe0
import com.decimal128.decimal.Decimal.Companion.POS_QNAN
import com.decimal128.decimal.Decimal.Companion.POS_SNAN
import com.decimal128.decimal.Decimal.Companion.POS_ZEROe0
import com.decimal128.decimal.Decimal.Companion.ZERO
import com.decimal128.decimal.Decimal.Companion.sNaN
import com.decimal128.decimal.Ieee754Class.*
import kotlin.math.max
import kotlin.math.min

// commonMain
expect open class DecimalRep(steal: Int, dw1: Long, dw0: Long) {
    internal val steal: Int
    internal val dw1: Long
    internal val dw0: Long
}

/**
 * An IEEE 754-2019 decimal128 floating-point value.
 *
 * `Decimal` represents a 128-bit decimal floating-point number conforming to the
 * IEEE 754-2019 standard. Unlike binary floating-point (`Double`, `Float`), decimal
 * floating-point is exact for values that can be expressed as `coefficient × 10^exponent`,
 * making it appropriate for financial, scientific, and any domain where decimal
 * rounding behavior matters.
 *
 * ## Value Space
 *
 * Every finite `Decimal` represents a value of the form:
 * ```
 *   (−1)^sign × coefficient × 10^qExp
 * ```
 * where:
 * - **sign** is 0 (positive) or 1 (negative)
 * - **coefficient** is an integer with up to **34 decimal digits** (0–9999…9999)
 * - **qExp** (quantum exponent) is an integer in the range **−6176 to +6111**
 *
 * In addition to finite values, `Decimal` represents:
 * - **±Infinity** — overflow sentinel
 * - **Quiet NaN (qNaN)** — propagating not-a-number, with an optional diagnostic payload
 * - **Signaling NaN (sNaN)** — trapping not-a-number, with an optional diagnostic payload
 *
 * ## Cohorts and Quantum
 *
 * The same mathematical value can have multiple representations (a *cohort*).
 * For example, `1`, `1.0`, and `1.00` are numerically equal but *not* the same
 * `Decimal` — they have different quantum exponents (`qExp`). Arithmetic operations
 * and [equals]/[compareTo] compare numerical value, ignoring the quantum.
 * Use [isSameQuantum] to test whether two values share an exponent.
 *
 * ## Arithmetic and Context
 *
 * Arithmetic operators (`+`, `-`, `*`, `/`, `%`) are available in two forms:
 *
 * - **Context-free** (e.g., `a + b`): uses a default decimal128 context with
 *   round-half-to-even and no exception trapping.
 * - **Context-aware** (e.g., `with(ctx) { a + b }`): uses the supplied [DecContext]
 *   for rounding mode, precision, exponent limits, and exception handling.
 *
 * For production use, prefer the context-aware operators so that rounding and
 * overflow behavior is explicit.
 *
 * ## Construction
 *
 * Instances are **not** created directly. Use the factory functions on the companion object:
 *
 * ```kotlin
 * Decimal.from(42)                           // from Int
 * Decimal.from(42L)                          // from Long
 * Decimal.from("3.14159")                    // from String (decimal128 range, no rounding)
 * Decimal.from("3.14159", ctx)               // from String with context
 * Decimal.ZERO                               // canonical +0e0
 * Decimal.ONE                                // canonical +1e0
 * Decimal.POS_INFINITY                       // +∞
 * Decimal.NaN                                // quiet NaN
 * ```
 *
 * ## Predefined Constants
 *
 * | Constant | Value |
 * |---|---|
 * | [ZERO] / [POS_ZEROe0] | +0 |
 * | [NEG_ZEROe0] | −0 |
 * | [ONE] / [POS_ONEe0] | +1 |
 * | [NEG_ONEe0] | −1 |
 * | [POS_INFINITY] / [INFINITY] | +∞ |
 * | [NEG_INFINITY] | −∞ |
 * | [NaN] / [POS_QNAN] | quiet NaN |
 * | [NEG_QNAN] | negative quiet NaN |
 * | [sNaN] / [POS_SNAN] | signaling NaN |
 * | [NEG_SNAN] | negative signaling NaN |
 *
 * ## Equality and Ordering
 *
 * [equals] and [compareTo] (used by Kotlin's `<`, `>`, `<=`, `>=`, and sort functions)
 * follow **Java-style numeric** semantics:
 * - Cohort members compare as equal (`1.0 == 1.00`)
 * - `−0 < +0`
 * - All NaNs compare equal, and greater than every non-NaN
 *
 * Use [bitwiseEQ] for exact bitwise identity, including exponent and payload.
 * Use [compareTotalOrderTo] for the IEEE 754-2019 *totalOrder* predicate.
 * Use [compareQuiet] / [compareSignaling] variants for full IEEE 754-compliant comparisons.
 *
 * ## Thread Safety
 *
 * `Decimal` is **immutable**. Instances may be freely shared across threads.
 * [DecContext] is *not* thread-safe and should be kept thread-local.
 *
 * @see DecContext
 * @see DecRounding
 */
class Decimal private constructor(
    steal: Int,
    dw1: Long,
    dw0: Long):
    DecimalRep(steal, dw1, dw0), Comparable<Decimal> {

    internal val bitLen: Int
        inline get() = stealBitLen(steal)

    internal val signFlag: Boolean
        inline get() = steal < 0
    internal val signBit: Int
        inline get() = steal ushr 31
    internal val signMask: Int
        inline get() = steal shr 31

    internal val qExp: Int
        inline get() = stealQExp(steal)

    companion object {

        private const val BIT31 = Int.MIN_VALUE

        internal fun decimalFinite(sign: Boolean, qExp: Int, dw1: Long, dw0: Long): Decimal {
            verify { qExp >= Q_TINY && qExp <= Q_MAX }
            val signBit = if (sign) 1 else 0
            val steal = stealEncodeFinite(signBit, qExp, dw1, dw0)
            return Decimal(steal, dw1, dw0)
        }

        internal inline fun decimalFNZ(signFlag: Boolean, qExp: Int, dw1: Long, dw0: Long): Decimal =
            decimalFNZ(if (signFlag) 1 else 0, qExp, dw1, dw0)

        internal fun decimalFNZ(signBit: Int, qExp: Int, dw1: Long, dw0: Long): Decimal {
            verify { qExp >= Q_TINY && qExp <= Q_MAX }
            verify { dw0 or dw1 != 0L }
            val steal = stealEncodeFNZ(signBit, qExp, dw1, dw0)
            return Decimal(steal, dw1, dw0)
        }

        val POS_ZEROe0 = Decimal(stealEncodeZER(0, 0), 0L, 0L)
        val NEG_ZEROe0 = Decimal(stealEncodeZER(1, 0), 0L, 0L)
        val ZERO = POS_ZEROe0
        val POS_ONEe0 = Decimal(stealEncodeFNZ(0, 0, PACKED_LENGTHS_1_1), 0L, 1L)
        val NEG_ONEe0 = Decimal(stealEncodeFNZ(1, 0, PACKED_LENGTHS_1_1), 0L, 1L)
        val ONE = POS_ONEe0
        val POS_INFINITY = Decimal(stealEncodeINF(0), 0L, 0L)
        val NEG_INFINITY = Decimal(stealEncodeINF(1), 0L, 0L)
        val INFINITY = POS_INFINITY
        val POS_QNAN = Decimal(stealEncodeQNAN(0, 0L, 0L), 0L, 0L)
        val NEG_QNAN = Decimal(stealEncodeQNAN(1, 0L, 0L), 0L, 0L)
        val NaN = POS_QNAN
        val POS_SNAN = Decimal(stealEncodeSNAN(0, 0L, 0L), 0L, 0L)
        val NEG_SNAN = Decimal(stealEncodeSNAN(1, 0L, 0L), 0L, 0L)
        val sNaN = POS_SNAN

        private const val DECIMAL128_QMAX_6111 = 6111

        private const val HASH_CODE_SIGN_FALSE = 1237 * 31 * 31 * 31
        private const val HASH_CODE_SIGN_TRUE = 1231 * 31 * 31 * 31
        private const val HASH_CODE_POS_ZERO = 36851467
        private const val HASH_CODE_NEG_ZERO = 36672721
        private const val HASH_CODE_POS_INFINITY = 52593608
        private const val HASH_CODE_NEG_INFINITY = 52414862
        private const val HASH_CODE_NAN = 52594569

        /**
         * Returns ±0 with the given sign and `qExp = 0`.
         */
        fun zero(sign: Boolean): Decimal = if (sign) NEG_ZEROe0 else POS_ZEROe0

        /**
         * Returns ±0 with the given sign, with the quantum exponent clamped
         * to `[Q_TINY, Q_MAX]`.
         */
        fun zero(sign: Boolean, qExp: Int): Decimal {
            if (qExp == 0)
                return if (sign) NEG_ZEROe0 else POS_ZEROe0
            val qClamped = max(min(qExp, Q_MAX), Q_TINY)
            val signBit = if (sign) 1 else 0
            val steal = stealEncodeZER(signBit, qClamped)
            val zero = Decimal(steal, 0L, 0L)
            return zero
        }

        /**
         * Returns a `Decimal` with value [n].
         */
        fun from(n: Int) = from(n.toLong())

        /**
         * Returns a `Decimal` with value [l].
         */
        fun from(l: Long): Decimal {
            return when {
                l == 0L -> ZERO
                l == 1L -> ONE
                else -> {
                    val signMask = (l shr 63)
                    decimalFNZ((l ushr 63).toInt(), 0, 0L, (l xor signMask) - signMask)
                }
            }
        }

        fun fromBID(dwHi: Long, dwLo: Long): Decimal {
            // just do this thru MutDec.setBID()
            TODO()
        }

        fun fromDPD(dwHi: Long, dwLo: Long): Decimal {
            // just do this thru MutDec.setDPD()
            TODO()
        }

        /**
         * Parses a `Decimal` from its textual representation using the
         * strict decimal128 interchange format.
         *
         * The parser accepts:
         * - an optional leading sign (`+` or `−`)
         * - a decimal coefficient of up to **34 significant digits**
         * - an optional exponent (`E` or `e`) within the decimal128 range
         * - `"Infinity"`, `"+Infinity"`, `"-Infinity"`
         * - `"NaN"`, `"sNaN"`, with an optional numeric payload
         *
         * No rounding is performed. Input that would require rounding to fit
         * in decimal128 is rejected. For rounding-aware parsing, use
         * `ctx.parse(str)` instead.
         *
         * @throws IllegalArgumentException if [str] is not a valid decimal128 value.
         */
        fun from(str: String, ctx: DecContext = DecContext.current()) =
            D128Parse.parseDecimal(str, ctx)

        fun from(mutDec: MutDec, ctx: DecContext = DecContext.current()): Decimal {
            require(mutDec.digitLen <= ctx.precision)
            return when (mutDec.type) {
                STEAL_TYP_FNZ -> decimalFNZ(mutDec.signBit, mutDec.qExp, mutDec.dw1, mutDec.dw0)
                STEAL_TYP_ZER -> zero(mutDec.sign, mutDec.qExp)
                STEAL_TYP_INF -> infinity(mutDec.sign)
                else -> NaN(mutDec.sign, mutDec.isSignaling(), mutDec.dw1, mutDec.dw0)
            }
        }

        /**
         * Returns a quiet NaN with no payload.
         * @param sign whether to set the sign bit (has no numeric meaning for NaNs).
         */
        fun qNaN(sign: Boolean) = if (sign) NEG_QNAN else POS_QNAN

        /**
         * Returns a quiet NaN with a 128-bit diagnostic payload.
         * If the payload exceeds 33 digits, the canonical no-payload NaN is returned.
         */
        fun qNaN(sign: Boolean, dw1: Long, dw0: Long): Decimal {
            val payloadIsZero = (dw1 or dw0) == 0L
            return when {
                payloadIsZero && sign -> NEG_QNAN
                payloadIsZero -> POS_QNAN
                else -> Decimal(
                    stealEncodeQNAN(if (sign) 1 else 0, dw1, dw0), dw1, dw0)
            }
        }

        /**
         * Returns a signaling NaN with no payload.
         * @param sign whether to set the sign bit.
         */
        fun sNaN(sign: Boolean) = if (sign) NEG_SNAN else POS_SNAN

        /**
         * Returns a signaling NaN with a 128-bit diagnostic payload.
         * If the payload exceeds 33 digits, the canonical no-payload NaN is returned.
         */
        fun sNaN(sign: Boolean, dw1: Long, dw0: Long): Decimal {
            val payloadIsZero = (dw1 or dw0) == 0L
            return when {
                payloadIsZero && sign -> NEG_SNAN
                payloadIsZero -> POS_SNAN
                else -> Decimal(
                    stealEncodeSNAN(if (sign) 1 else 0, dw1, dw0), dw1, dw0)
            }
        }

        /**
         * Returns a quiet or signaling NaN with no payload.
         * @param sign whether to set the sign bit.
         * @param signaling `true` for sNaN, `false` (default) for qNaN.
         */
        fun NaN(sign: Boolean, signaling: Boolean = false): Decimal =
            when {
                !signaling && !sign -> POS_QNAN
                !signaling && sign -> NEG_QNAN
                sign -> NEG_SNAN
                else -> POS_SNAN
            }

        fun NaN(
            sign: Boolean = false, signaling: Boolean = false,
            payloadDw1: Long, payloadDw0: Long
        ): Decimal {
            if ((payloadDw1 or payloadDw0) != 0L) {
                val digitLen = calcDigitLen128(payloadDw1, payloadDw0)
                if (digitLen <= NAN_PAYLOAD_PRECISION) {
                    val signBit = if (sign) 1 else 0
                    val steal =
                        if (signaling) stealEncodeSNAN(signBit, payloadDw1, payloadDw0)
                        else stealEncodeQNAN(signBit, payloadDw1, payloadDw0)
                    return Decimal(steal, payloadDw1, payloadDw0)
                }
            }
            return NaN(sign, signaling)
        }


        fun infinity(sign: Boolean) = if (sign) NEG_INFINITY else POS_INFINITY

        fun infinity(signBit: Int) = if ((signBit and 1) != 0) NEG_INFINITY else POS_INFINITY

        fun infinityNonCanonical(sign: Boolean, dw1: Long, dw0: Long): Decimal {
            // 2026-03-24 ... removing support for non-canonical encodings of Infinity
            return if (sign) NEG_INFINITY else POS_INFINITY
        }

        internal fun hasNaN(x: Decimal, y: Decimal): Boolean = stealHasNAN(x.steal, y.steal)

        internal fun neitherIsNaN(x: Decimal, y: Decimal) = !hasNaN(x, y)

    }

    internal fun validate(): Boolean {
        val bitLen = stealBitLen(steal)
        if (bitLen != calcBitLen128(dw1, dw0))
            return false
        if (stealDigitLen(steal) != calcDigitLen128(bitLen, dw1, dw0))
            return false;
        return true
    }

    // ── Classification ────────────────────────────────────────────────────────

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
    fun ieeeClass(ctx: DecContext): Ieee754Class {
        val steal = steal
        val sign = stealSignFlag(steal)
        val type = stealTyp(steal)
        return when (type) {
            STEAL_TYP_FNZ -> {
                if (stealSciExp(steal) < ctx.decFormat.eMin) {
                    if (sign) negativeSubnormal else positiveSubnormal
                }
                if (sign) negativeNormal else positiveNormal
            }
            STEAL_TYP_INF -> {
                if (sign) negativeInfinity else positiveInfinity
            }
            STEAL_TYP_ZER -> {
                if (sign) negativeZero else positiveZero
            }
            else -> {
                if (stealIsQNAN(steal)) quietNaN else signalingNaN
            }
        }
    }

    /**
     * Returns `true` if this value is negative (sign bit set).
     *
     * Applies to zeros and NaNs as well as finite and infinite values.
     * Implements IEEE 754-2019 `isSignMinus` (§5.7.2).
     */
    fun isSignMinus(): Boolean = steal < 0 // IEEE754 5.7.2


    /** Alias for [isSignMinus]. */
    fun isNegative(): Boolean = steal < 0

    /**
     * Returns `true` if this value is a normal finite number — not zero,
     * subnormal, infinite, or NaN.
     *
     * The subnormal threshold is hardwired to the decimal128 minimum adjusted
     * exponent of −6143, equivalent to `eExp >= −6143` and `digitLen <= 34`.
     */
    fun isNormal(): Boolean = stealIsFNZ(steal) &&
            stealDigitLen(steal) <= 34 &&
            stealSciExp(steal) >= -6143

    /**
     * Returns `true` if this value is subnormal — finite, non-zero, and
     * below the normal range.
     *
     * A decimal128 value is subnormal when its adjusted (scientific) exponent
     * `eExp` is less than −6143. Equivalently: `qExp == −6176 && digitLen < 34`.
     */
    fun isSubnormal(): Boolean = stealIsFNZ(steal) && stealSciExp(steal) < -6143

    /**
     * Returns `true` if this value is finite — zero, subnormal, or normal.
     * Returns `false` for infinities and NaNs.
     */
    fun isFinite(): Boolean = stealIsFinite(steal)

    /**
     * isFiniteNonZero(x) is true if and only if x is normal or subnormal
     * (not zero, infinite, or NaN).
     */
    fun isFiniteNonZero(): Boolean = stealIsFNZ(steal)

    /**
     * Returns `true` if this value is ±0.
     *
     * In decimal floating-point, zero has an entire cohort of representations
     * (one per valid exponent). All are considered zero by this predicate,
     * regardless of the encoded exponent or sign.
     */
    fun isZero(): Boolean = stealIsZER(steal)

    /** Returns `true` if this value is **not** ±0. */
    fun isNotZero() = !isZero()

    /**
     * isCanonicalZero(x) is true if x is ±0 and the coefficient
     * digit length <= 34.
     *
     * IEEE rules state that nonCanonical coefficients must be treated
     * as zero. Therefore, more than 34 digits == 0
     */
    fun isCanonicalZero(): Boolean = stealIsZER(steal)

    /**
     * Returns `true` if this value is ±∞.
     */
    fun isInfinite(): Boolean = stealIsINF(steal)

    /**
     * Returns `true` if this value is a NaN (quiet or signaling).
     */
    fun isNaN(): Boolean = stealIsNAN(steal)

    /**
     * Returns `true` if this value is a *signaling* NaN.
     *
     * Signaling NaNs trigger an [DecException.INVALID_OPERATION] exception
     * (or set the corresponding flag) when they appear as operands to most
     * arithmetic operations.
     */
    fun isSignaling(): Boolean = stealIsSNAN(steal)

    /**
     * Returns `true` if this value is in its canonical encoding.
     *
     * Non-canonical encodings can arise from raw bit patterns (e.g., from
     * interop with Intel BID or DPD formats) and are treated as equivalent
     * to their canonical form by all arithmetic. The rules are:
     * - **Finite**: coefficient must have at most 34 digits and `qExp` in −6176..6111
     * - **Infinity**: coefficient/payload must be zero
     * - **NaN**: payload must have at most 33 digits
     */
    fun isCanonical(): Boolean {
        val steal = steal
        val qExp = stealQExp(steal)
        val digitLen = stealDigitLen(steal)
        verify { digitLen == calcDigitLen128(dw1, dw0) }
        return when {
            stealIsFinite(steal) -> qExp in -6176..6111 && digitLen <= 34
            stealIsINF(steal) -> digitLen == 0
            else -> // NAN
                digitLen <= 33
        }
    }

    // ── Quantum and Exponent ──────────────────────────────────────────────────


    // 5.7.3 Decimal operation
    /**
     * Returns `true` if this value and [other] share the same *quantum* (same
     * encoded exponent `qExp`), per IEEE 754-2019 §5.7.3.
     *
     * Two values have the same quantum when their exponents are identical after
     * encoding, regardless of whether their numerical values are equal.
     * For non-finite values: two infinities share a quantum; a NaN shares a
     * quantum only with another NaN.
     *
     * ```kotlin
     * Decimal.from("1.0").isSameQuantum(Decimal.from("2.0"))   // true  (both qExp = -1)
     * Decimal.from("1.0").isSameQuantum(Decimal.from("1.00"))  // false (qExp -1 vs -2)
     * ```
     */
    fun isSameQuantum(other: Decimal): Boolean {
        val stealX = steal
        val stealY = other.steal
        return when {
            stealIsFinite(stealX) -> stealIsFinite(stealY) && stealQExp(stealX) == stealQExp(stealY)
            stealIsINF(stealX) -> stealIsINF(stealY)
            else -> stealIsNAN(stealY)
        }
    }

    /**
     * Returns the quantum exponent `qExp` of this finite value, i.e., the power
     * of ten by which the integer coefficient is scaled.
     *
     * Returns [Int.MIN_VALUE] and signals [DecException.INVALID_OPERATION] if
     * called on a non-finite value.
     */
    fun quantumExponent(ctx: DecContext): Int {
        if (isFinite())
            return qExp
        ctx.signalInvalid(InvalidOperationReason.QEXP_OF_NON_FINITE, this)
        return Int.MIN_VALUE
    }

    /**
     * Returns the adjusted (scientific) exponent `eExp`, defined as
     * `qExp + digitLen − 1`, or [Int.MIN_VALUE] for non-finite values.
     *
     * This is the exponent used by [logB] and [isNormal]/[isSubnormal] checks.
     */
    fun eExponent(): Int = if (isFinite()) stealSciExp(steal) else Int.MIN_VALUE

    /**
     * Returns the number of significant decimal digits in the coefficient, or
     * [Int.MIN_VALUE] for non-finite values.
     */
    fun precision(): Int = if (isFinite()) stealDigitLen(steal) else Int.MIN_VALUE


    // ── Unary Operations ──────────────────────────────────────────────────────

    // ── 5.5 Quiet-computational operations ────────────────────────────────────
    // -- 5.5.1 Sign bit operations ---------------------------------------------
    /**
     * Returns the absolute value of this decimal.
     *
     * The sign bit is cleared. For NaNs, the sign is cleared without signaling.
     */
    fun abs() = if (steal < 0) negate() else this

    /**
     * Returns this value with its sign bit flipped.
     *
     * Negate is exact: no rounding, no exceptions. `−(−x) == x` always.
     * For NaNs (including sNaNs), the sign bit is toggled without signaling.
     */
    fun negate(): Decimal {
        val steal = steal
        val newSign = !stealSignFlag(steal)
        when {
            stealIsZER(steal) && stealQExp(steal) == 0 -> return zero(newSign)
            stealIsINF(steal) -> return infinity(newSign)
            stealIsNAN(steal) && stealBitLen(steal) == 0 ->
                    return NaN(newSign, signaling = stealIsSNAN(steal))
        }
        return Decimal(steal xor BIT31, dw1, dw0)
    }

    /**
     * Returns this value with the sign bit copied from [signDonor].
     *
     * This is the IEEE 754-2019 `copySign` operation (§5.5.1). The magnitude
     * and type of `this` are unchanged; only the sign bit is replaced.
     * No exceptions are signaled.
     */
    fun copySign(signDonor: Decimal): Decimal =
        if (this.signBit == signDonor.signBit) this else this.negate()

    /**
     * Returns a copy of this value. Because `Decimal` is immutable, this
     * always returns `this`. Provided for IEEE 754-2019 completeness (§5.5.1).
     */
    fun copy(): Decimal = this

    // ── Scaling and Quantum Adjustment ───────────────────────────────────────

    /**
     * Returns `logB(this)` — the adjusted (scientific) exponent as a `Decimal`,
     * following IEEE 754-2019 §5.3.3.
     *
     * | Input | Result |
     * |---|---|
     * | finite non-zero | `Decimal.from(eExp)` |
     * | ±0 | −∞, signals [DecException.DIVISION_BY_ZERO] |
     * | ±∞ | +∞ |
     * | NaN | NaN, signals [DecException.INVALID_OPERATION] |
     */
    fun logB(ctx: DecContext): Decimal {
        val steal = steal
        return when {
            stealIsZER(steal) -> ctx.signalDivByZero(NEG_INFINITY)
            stealIsFNZ(steal) -> from(stealSciExp(steal))
            stealIsINF(steal) -> POS_INFINITY
            else -> nanOperandFound(this, ctx)
        }

    }

    /**
     * Returns `this × 10^pow10Delta`, rounded as needed within [ctx].
     *
     * Equivalent to IEEE 754-2019 `scaleB`. The exponent delta is clamped to
     * ±100 000 internally to prevent intermediate overflow.
     * Infinities pass through unchanged; NaNs signal [DecException.INVALID_OPERATION].
     */
    fun scaleB(pow10Delta: Int, ctx: DecContext): Decimal {
        val steal = steal
        return when {
            stealIsFinite(steal) -> {
                val pow10DeltaCapped = max(min(pow10Delta, 100_000), -100_000)
                val qNew = qExp + pow10DeltaCapped
                decFinalizeFinite(signFlag, dw1, dw0, qNew, ctx)
            }

            stealIsINF(steal) -> this
            else -> nanOperandFound(this, ctx)
        }
    }

    /**
     * Returns this value with trailing fractional zeros removed, reducing the
     * quantum exponent as far as possible without changing the numerical value.
     *
     * ```kotlin
     * Decimal.from("1.200").stripTrailingZeros(ctx)  // → "1.2"
     * Decimal.from("120").stripTrailingZeros(ctx)    // → "1.2E+2" (or "120" if no trailing zeros)
     * ```
     */
    fun stripTrailingZeros(ctx: DecContext): Decimal = stripTrailingZerosImpl(this, ctx)

    /**
     * Returns this value rescaled so that its quantum exponent equals
     * `−decimalScale`, rounding if necessary under [ctx].
     *
     * `decimalScale` is the number of digits to the right of the decimal point.
     * For example, `withScale(2, ctx)` produces a value like `1.23`.
     *
     * This is equivalent to [quantize] with an implicit quantum of `10^(-decimalScale)`.
     */
    fun withScale(decimalScale: Int, ctx: DecContext): Decimal = withScale(this, decimalScale, ctx)

    /**
     * Returns the smallest representable value greater than this one,
     * IEEE 754-2019 §5.3.1 `nextUp`.
     *
     * - For finite values, increments the last digit of the coefficient.
     * - +∞ returns +∞.
     * - NaN signals [DecException.INVALID_OPERATION].
     */
    fun nextUp(ctx: DecContext): Decimal = nextUpOrDown(isUp = true, this, ctx)

    /**
     * Returns the largest representable value smaller than this one,
     * IEEE 754-2019 §5.3.1 `nextDown`.
     *
     * - For finite values, decrements the last digit of the coefficient.
     * - −∞ returns −∞.
     * - NaN signals [DecException.INVALID_OPERATION].
     */
    fun nextDown(ctx: DecContext): Decimal = nextUpOrDown(isUp = false, this, ctx)

    // ── Comparison ────────────────────────────────────────────────────────────

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
     * Java-style numeric equality.
     *
     * Returns `true` when both values represent the same number, ignoring the
     * quantum. Signed zeros are equal. All NaNs are equal to each other.
     * Equivalent to `compareTo(other) == 0`.
     */
    override fun equals(other: Any?): Boolean =
        other is Decimal && eqJavaStyle(other)

    /**
     * Returns a hash code consistent with `compareTo(other) == 0`.
     * Values in the same numerical cohort produce the same hash code,
     * all NaNs hash identically, and ±0 hash to distinct constants.
     * This is unlike [BigDecimal.equals] which also requires the same scale.
     */
    override fun hashCode(): Int {
        val steal = steal
        when {
            stealIsFNZ(steal) -> {
                val qExp = qExp
                var r1 = dw1
                var r0 = dw0
                var rQ = qExp
                if (qExp < Q_MAX) {
                    val maxNtzdClamp = Q_MAX - qExp
                    val tmps = DecContext.current().tmps
                    val t = tmps.mdecArg1
                    t.c256Set128(dw1, dw0)
                    val ntzdActual = c256CountTrailingZeroDigitsDestructive(t)
                    val ntzdNormalized = min(maxNtzdClamp, ntzdActual)
                    if (ntzdNormalized > 0) {
                        t.c256Set128(dw1, dw0)
                        c256SetDivPow10(t, t, ntzdNormalized, tmps.pentad1)
                        r1 = t.dw1
                        r0 = t.dw0
                        rQ = qExp + ntzdNormalized
                    }
                }
                val hcSign = if (signFlag) HASH_CODE_SIGN_TRUE else HASH_CODE_SIGN_FALSE
                val hcQExp = rQ * 31 * 31
                val hcDw1 = (r1 xor (r1 shr 32)).toInt() * 31
                val hcDw0 = (r0 xor (r0 shr 32)).toInt()
                return hcSign + hcQExp + hcDw1 + hcDw0
            }

            stealIsZER(steal) -> return if (steal < 0) HASH_CODE_NEG_ZERO else HASH_CODE_POS_ZERO
            stealIsINF(steal) -> return if (steal < 0) HASH_CODE_NEG_INFINITY else HASH_CODE_POS_INFINITY
            else -> return HASH_CODE_NAN
        }
    }

    /**
     * Returns `true` if and only if this `Decimal` value has the exact same
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
        ((this.steal - other.steal).toLong() or
                (this.dw1 - other.dw1) or
                (this.dw0 - other.dw0)) == 0L

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

    fun compareTotalOrderTo(other: Decimal) = d128CompareTotalOrder(this, other)

    /** Returns `true` if `totalOrder(this, other)` — i.e., `this ≤ other` in total order. */
    fun isTotalOrder(other: Decimal) = compareTotalOrderTo(other) <= 0

    /**
     * Like [compareTotalOrderTo] but compares magnitudes only (ignoring sign).
     * IEEE 754-2019 `totalOrderMag`.
     *
     * @return −1, 0, or +1.
     */
    fun compareTotalOrderMagTo(other: Decimal) = d128CompareTotalOrderMag(this, other)

    /** Returns `true` if `totalOrderMag(this, other)` — i.e., `|this| ≤ |other|` in total order. */
    fun isTotalOrderMag(other: Decimal) = compareTotalOrderMagTo(other) <= 0

    /**
     * Compares magnitudes numerically (ignoring sign), without the total-order
     * tie-breaking on quantum
     *
     * @return −1, 0, or +1.
     */
    fun compareNumericMagnitudeTo(other: Decimal): Int = d128CompareNumericMagnitude(this, other)

    // ── IEEE 754-2019 Comparison Predicates (§5.6.1) ─────────────────────────

    /**
     * IEEE 754-2019 `compareQuietEqual`: returns `true` if `this == other`
     * numerically. Does **not** signal on quiet NaN operands.
     */
    fun compareQuietEqual(other: Decimal, ctx: DecContext): Boolean =
        d128CompareQuiet754(this, other, ctx) == IEEE754_EQ

    /**
     * IEEE 754-2019 `compareQuietNotEqual`: returns `true` if `this ≠ other`.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietNotEqual(other: Decimal, ctx: DecContext): Boolean =
        d128CompareQuiet754(this, other, ctx) != IEEE754_EQ

    /**
     * IEEE 754-2019 `compareQuietGreater`: returns `true` if `this > other`.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietGreater(other: Decimal, ctx: DecContext): Boolean =
        d128CompareQuiet754(this, other, ctx) == IEEE754_GT

    /**
     * IEEE 754-2019 `compareQuietNotGreater`: returns `true` if `this` is not greater
     * than `other` (i.e., less, equal, or unordered).
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietNotGreater(other: Decimal, ctx: DecContext): Boolean =
        d128CompareQuiet754(this, other, ctx) != IEEE754_GT

    fun compareQuietGreaterEqual(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = d128CompareQuiet754(this, other, ctx)
        return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_EQ)
    }

    fun compareQuietLess(other: Decimal, ctx: DecContext): Boolean =
        d128CompareQuiet754(this, other, ctx) == IEEE754_LT

    fun compareQuietLessEqual(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = d128CompareQuiet754(this, other, ctx)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_EQ)
    }

    fun compareQuietUnordered(other: Decimal, ctx: DecContext): Boolean =
        d128CompareQuiet754(this, other, ctx) == IEEE754_UNORDERED

    /**
     * IEEE 754-2019 `compareQuietLessUnordered`: returns `true` if `this < other`
     * **or** either operand is NaN.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietLessUnordered(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = d128CompareQuiet754(this, other, ctx)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_UNORDERED)
    }

    /**
     * IEEE 754-2019 `compareQuietGreaterUnordered`: returns `true` if `this > other`
     * **or** either operand is NaN.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietGreaterUnordered(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = d128CompareQuiet754(this, other, ctx)
        return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_UNORDERED)
    }

    /**
     * IEEE 754-2019 `compareQuietNotLess`: returns `true` if `this` is not less
     * than `other`.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietNotLess(other: Decimal, ctx: DecContext): Boolean =
        d128CompareQuiet754(this, other, ctx) != IEEE754_LT

    /**
     * Returns the full four-valued [Compare754Result] for a quiet comparison.
     * The result is one of [IEEE754_LT], [IEEE754_EQ], [IEEE754_GT], or [IEEE754_UNORDERED].
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuiet(other: Decimal, ctx: DecContext): Compare754Result =
        d128CompareQuiet754(this, other, ctx)

    /**
     * IEEE 754-2019 `compareQuietUnordered`: returns `true` if either operand is NaN.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietOrdered(other: Decimal, ctx: DecContext): Boolean =
        d128CompareQuiet754(this, other, ctx) != IEEE754_UNORDERED

    /**
     * IEEE 754-2019 `compareSignalingEqual`: returns `true` if `this == other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN
     * (quiet or signaling).
     */
    fun compareSignalingEqual(other: Decimal, ctx: DecContext): Boolean =
        d128CompareSignaling754(this, other, ctx) == IEEE754_EQ

    /**
     * IEEE 754-2019 `compareSignalingGreater`: returns `true` if `this > other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingGreater(other: Decimal, ctx: DecContext): Boolean =
        d128CompareSignaling754(this, other, ctx) == IEEE754_GT

    /**
     * IEEE 754-2019 `compareSignalingGreaterEqual`: returns `true` if `this >= other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingGreaterEqual(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = d128CompareSignaling754(this, other, ctx)
        return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_EQ)
    }

    /**
     * IEEE 754-2019 `compareSignalingLess`: returns `true` if `this < other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingLess(other: Decimal, ctx: DecContext): Boolean =
        d128CompareSignaling754(this, other, ctx) == IEEE754_LT

    /**
     * IEEE 754-2019 `compareSignalingLessEqual`: returns `true` if `this <= other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingLessEqual(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = d128CompareSignaling754(this, other, ctx)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_EQ)
    }

    /**
     * IEEE 754-2019 `compareSignalingNotEqual`: returns `true` if `this ≠ other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingNotEqual(other: Decimal, ctx: DecContext): Boolean =
        d128CompareSignaling754(this, other, ctx) != IEEE754_EQ

    /**
     * IEEE 754-2019 `compareSignalingNotGreater`: returns `true` if `this` is
     * not greater than `other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingNotGreater(other: Decimal, ctx: DecContext): Boolean =
        d128CompareSignaling754(this, other, ctx) != IEEE754_GT

    /**
     * IEEE 754-2019 `compareSignalingLessUnordered`: returns `true` if
     * `this < other` **or** either operand is NaN.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */

    fun compareSignalingLessUnordered(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = d128CompareSignaling754(this, other, ctx)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_UNORDERED)
    }

    /**
     * IEEE 754-2019 `compareSignalingNotLess`: returns `true` if `this` is
     * not less than `other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingNotLess(other: Decimal, ctx: DecContext): Boolean =
        d128CompareSignaling754(this, other, ctx) != IEEE754_LT


    /**
     * IEEE 754-2019 `compareSignalingGreaterUnordered`: returns `true` if
     * `this > other` **or** either operand is NaN.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingGreaterUnordered(other: Decimal, ctx: DecContext): Boolean {
        val cmp754 = d128CompareSignaling754(this, other, ctx)
        return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_UNORDERED)
    }

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
    fun compareJavaStyleTo(other: Decimal): Int = d128CompareJavaStyle(this, other)

    fun eqJavaStyle(other: Decimal): Boolean = d128EqJavaStyle(this, other)

    // ── String Representation ─────────────────────────────────────────────────

    /**
     * Returns the canonical decimal text representation of this value.
     *
     * Formatting follows the same conventions as Java's `BigDecimal.toString()`:
     * - Values are rendered with an uppercase `E` exponent when necessary.
     * - The coefficient is never padded or truncated; all significant digits appear.
     * - Special values render as `"Infinity"`, `"-Infinity"`, and `"NaN"`.
     *
     * The round-trip property holds: `Decimal.from(d.toString()) bitwiseEQ d`
     * for all canonical values.
     */
    override fun toString(): String = D128Print.toString(this, DecContext.current())

    fun toString(ctx: DecContext): String = D128Print.toString(this, ctx)

    operator fun plus(other: Decimal): Decimal = d128AddImpl(this, other)

    context(decContext: DecContext)
    operator fun plus(other: Decimal): Decimal = d128AddSubImpl(this, other.steal, other, decContext)

    operator fun minus(other: Decimal): Decimal = d128SubImpl(this, other)

    context(decContext: DecContext)
    operator fun minus(other: Decimal): Decimal = d128AddSubImpl(this, other.steal xor Int.MIN_VALUE, other, decContext)

    operator fun times(other: Decimal): Decimal = d128MulImpl(this, other)

    context(decContext: DecContext)
    operator fun times(other: Decimal): Decimal = d128MulImpl(this, other, decContext)

    operator fun div(other: Decimal): Decimal = d128DivImpl(this, other)

    context(decContext: DecContext)
    operator fun div(other: Decimal): Decimal = d128DivImpl(this, other, decContext)

    operator fun rem(other: Decimal): Decimal = d128RemTruncImpl(this, other)

    context(decContext: DecContext)
    operator fun rem(other: Decimal): Decimal = d128RemTruncImpl(this, other, decContext)

    fun remainderTruncate(other: Decimal): Decimal = rem(other)

    /**
     * Returns the IEEE 754-2019 *near-remainder* of `this ÷ other` (§5.3.1).
     *
     * The result `r = this − (other × n)`, where `n` is the integer nearest
     * to the exact quotient, ties broken to even. The magnitude of `r` is
     * always `≤ |other| / 2`, so unlike `%`, the result can be negative
     * even when both operands are positive.
     *
     * Special cases:
     * | `this`  | `other` | result |
     * |---------|---------|--------|
     * | finite  | ±0      | NaN, signals [DecException.INVALID_OPERATION] |
     * | ±∞      | any     | NaN, signals [DecException.INVALID_OPERATION] |
     * | finite  | ±∞      | `this` |
     * | NaN (either) | — | NaN, signals [DecException.INVALID_OPERATION] |
     *
     * The sign of an exact zero result matches the sign of `this`.
     *
     * @see remainderTruncate for the `%` operator behavior
     */
    fun remainderNear(other: Decimal): Decimal = d128RemNearImpl(this, other)

    // ── Rounding ──────────────────────────────────────────────────────────────

    /**
     * Rounds to the nearest integer, ties resolved to even (banker's rounding).
     * Does not signal [DecException.INEXACT].
     * IEEE 754-2019 §5.3.1 `roundToIntegralTiesToEven`.
     */
    fun roundToIntegralTiesToEven(ctx: DecContext) =
        d128RoundToIntegral(this, DecRounding.ROUND_TIES_TO_EVEN, ctx, beQuiet = true)

    /**
     * Rounds to the nearest integer, ties resolved away from zero.
     * Does not signal [DecException.INEXACT].
     * IEEE 754-2019 §5.3.1 `roundToIntegralTiesToAway`.
     */
    fun roundToIntegralTiesToAway(ctx: DecContext) =
        d128RoundToIntegral(this, DecRounding.ROUND_TIES_TO_AWAY, ctx, beQuiet = true)

    /**
     * Rounds toward zero (truncation).
     * Does not signal [DecException.INEXACT].
     * IEEE 754-2019 §5.3.1 `roundToIntegralTowardZero`.
     */
    fun roundToIntegralTowardZero(ctx: DecContext) =
        d128RoundToIntegral(this, DecRounding.ROUND_TOWARD_ZERO, ctx, beQuiet = true)

    /**
     * Rounds toward positive infinity (ceiling).
     * Does not signal [DecException.INEXACT].
     * IEEE 754-2019 §5.3.1 `roundToIntegralTowardPositive`.
     */
    fun roundToIntegralTowardPositive(ctx: DecContext) =
        d128RoundToIntegral(this, DecRounding.ROUND_TOWARD_POSITIVE, ctx, beQuiet = true)

    /**
     * Rounds toward negative infinity (floor).
     * Does not signal [DecException.INEXACT].
     * IEEE 754-2019 §5.3.1 `roundToIntegralTowardNegative`.
     */
    fun roundToIntegralTowardNegative(ctx: DecContext) =
        d128RoundToIntegral(this, DecRounding.ROUND_TOWARD_NEGATIVE, ctx, beQuiet = true)

    /**
     * Rounds to an integer using the rounding mode from [ctx], and **does** signal
     * [DecException.INEXACT] if the value is not already integral.
     * IEEE 754-2019 §5.3.1 `roundToIntegralExact`.
     */
    fun roundToIntegralExact(ctx: DecContext) =
        d128RoundToIntegral(this, ctx.decRounding, ctx, beQuiet = false)

    // ── Conversion to Kotlin Integer Types ───────────────────────────────────

    /**
     * Converts this decimal to [Long] using round-half-to-even.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toLongTiesToEven(ctx: DecContext) =
        d128ConvertToLong(this, DecRounding.ROUND_TIES_TO_EVEN, ctx, suppressInexact = true)

    /**
     * Converts this decimal to [Long] using round-half-away-from-zero.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toLongTiesToAway(ctx: DecContext) =
        d128ConvertToLong(this, DecRounding.ROUND_TIES_TO_AWAY, ctx, suppressInexact = true)

    /**
     * Converts this decimal to [Long] by truncating toward zero.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toLongTowardZero(ctx: DecContext) =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_ZERO, ctx, suppressInexact = true)

    /**
     * Converts this decimal to [Long] by rounding toward positive infinity (ceiling).
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toLongTowardPositive(ctx: DecContext) =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_POSITIVE, ctx, suppressInexact = true)

    /**
     * Converts this decimal to [Long] by rounding toward negative infinity (floor).
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toLongTowardNegative(ctx: DecContext) =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_NEGATIVE, ctx, suppressInexact = true)

    /**
     * Converts this decimal to [Long] using round-half-to-even,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toLongExactTiesToEven(ctx: DecContext) =
        d128ConvertToLong(this, DecRounding.ROUND_TIES_TO_EVEN, ctx, suppressInexact = false)

    /**
     * Converts this decimal to [Long] using round-half-away-from-zero.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toLongExactTiesToAway(ctx: DecContext) =
        d128ConvertToLong(this, DecRounding.ROUND_TIES_TO_AWAY, ctx, suppressInexact = false)

    /**
     * Converts this decimal to [Long] by truncating toward zero,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toLongExactTowardZero(ctx: DecContext) =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_ZERO, ctx, suppressInexact = false)

    /**
     * Converts this decimal to [Long] by rounding toward positive infinity,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toLongExactTowardPositive(ctx: DecContext) =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_POSITIVE, ctx, suppressInexact = false)

    /**
     * Converts this decimal to [Long] by rounding toward negative infinity,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toLongExactTowardNegative(ctx: DecContext) =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_NEGATIVE, ctx, suppressInexact = false)

    /**
     * Converts this decimal to [Int] using round-half-to-even.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toIntTiesToEven(ctx: DecContext) =
        d128ConvertToInt(this, DecRounding.ROUND_TIES_TO_EVEN, ctx, suppressInexact = true)

    /**
     * Converts this decimal to [Int] using round-half-away-from-zero.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toIntTiesToAway(ctx: DecContext) =
        d128ConvertToInt(this, DecRounding.ROUND_TIES_TO_AWAY, ctx, suppressInexact = true)

    /**
     * Converts this decimal to [Int] by truncating toward zero.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toIntTowardZero(ctx: DecContext) =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_ZERO, ctx, suppressInexact = true)

    /**
     * Converts this decimal to [Int] by rounding toward positive infinity (ceiling).
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toIntTowardPositive(ctx: DecContext) =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_POSITIVE, ctx, suppressInexact = true)

    /**
     * Converts this decimal to [Int] by rounding toward negative infinity (floor).
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toIntTowardNegative(ctx: DecContext) =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_NEGATIVE, ctx, suppressInexact = true)

    /**
     * Converts this decimal to [Int] using round-half-to-even,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toIntExactTiesToEven(ctx: DecContext) =
        d128ConvertToInt(this, DecRounding.ROUND_TIES_TO_EVEN, ctx, suppressInexact = false)

    /**
     * Converts this decimal to [Int] using round-half-away-from-zero.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toIntExactTiesToAway(ctx: DecContext) =
        d128ConvertToInt(this, DecRounding.ROUND_TIES_TO_AWAY, ctx, suppressInexact = false)

    /**
     * Converts this decimal to [Int] by truncating toward zero,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toIntExactTowardZero(ctx: DecContext) =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_ZERO, ctx, suppressInexact = false)

    /**
     * Converts this decimal to [Int] by rounding toward positive infinity,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toIntExactTowardPositive(ctx: DecContext) =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_POSITIVE, ctx, suppressInexact = false)

    /**
     * Converts this decimal to [Int] by rounding toward negative infinity,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     */
    fun toIntExactTowardNegative(ctx: DecContext) =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_NEGATIVE, ctx, suppressInexact = false)

}

fun String.toDecimal(): Decimal = Decimal.from(this, DecContext.current())

//context(ctx: DecContext)
//fun String.toDecimal(): Decimal = Decimal.from(this, ctx)

fun Int.toDecimal(): Decimal = Decimal.from(this)

fun Long.toDecimal(): Decimal = Decimal.from(this)
