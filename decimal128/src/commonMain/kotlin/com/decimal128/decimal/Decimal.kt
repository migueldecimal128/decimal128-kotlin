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
import com.decimal128.decimal.InvalidCause.*
import com.decimal128.decimal.Ieee754Class.*
import kotlin.math.max
import kotlin.math.min

// commonMain
expect abstract class DecimalRep(steal: Int, dw1: Long, dw0: Long) {
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
 * - **Context-aware** (e.g., `ctx.eval { a + b }`): uses the specified ctx
 *   [DecContext] for rounding mode, precision, exponent limits, and exception
 *   handling.
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

        /**
         * internal constructor for finite values
         */
        internal fun decimalFinite(sign: Boolean, qExp: Int, dw1: Long, dw0: Long): Decimal {
            verify { qExp >= Q_TINY && qExp <= Q_MAX }
            val signBit = if (sign) 1 else 0
            val steal = stealEncodeFinite(signBit, qExp, dw1, dw0)
            return Decimal(steal, dw1, dw0)
        }

        /**
         * internal constructor for FNZ Finite Non-Zero values
         */
        internal inline fun decimalFNZ(signFlag: Boolean, qExp: Int, dw1: Long, dw0: Long): Decimal =
            decimalFNZ(if (signFlag) 1 else 0, qExp, dw1, dw0)

        /**
         * internal constructor for FNZ Finite Non-Zero values
         */
        internal fun decimalFNZ(signBit: Int, qExp: Int, dw1: Long, dw0: Long): Decimal {
            verify { qExp >= Q_TINY && qExp <= Q_MAX }
            verify { dw0 or dw1 != 0L }
            val steal = stealEncodeFNZ(signBit, qExp, dw1, dw0)
            return Decimal(steal, dw1, dw0)
        }

        // this is a cache of positive zeros with 0 and negative exponents
        // 0, 0.0, 0.00, 0.000, 0.0000,
        private const val ZEROS_CACHE_SIZE = 16 // must be a power of 2
        private const val ZEROS_CACHE_BCE = ZEROS_CACHE_SIZE - 1 // BCE Bounds Check Elimination
        private val ZEROS_CACHE = Array(ZEROS_CACHE_SIZE) { i ->
            Decimal(stealEncodeZER(0, -i), 0L, 0L) }

        val POS_ZEROe0 = ZEROS_CACHE[0]
        val NEG_ZEROe0 = Decimal(stealEncodeZER(1, 0), 0L, 0L)
        val ZERO = POS_ZEROe0
        val POS_ZEROeQ_TINY = Decimal(stealEncodeZER(0, Q_TINY), 0L, 0L)
        val POS_ZEROeQ_MAX = Decimal(stealEncodeZER(0, Q_MAX), 0L, 0L)
        val NEG_ZEROeQ_TINY = Decimal(stealEncodeZER(1, Q_TINY), 0L, 0L)
        val NEG_ZEROeQ_MAX = Decimal(stealEncodeZER(1, Q_MAX), 0L, 0L)

        // Unclear to me how big this cache should be.
        // Needs instrumentation in a real-world application.
        // For now, we will go thru 12, to pick up 12 months in a year.
        private val SMALL_POS_INT_CACHE: Array<Decimal> = Array<Decimal>(13) { i ->
            if (i == 0) ZERO else decimalFNZ(0, 0, 0L, i.toLong()) }


        val POS_ONEe0 = SMALL_POS_INT_CACHE[1]
        val NEG_ONEe0 = Decimal(stealEncodeFNZ(1, 0, PACKED_LENGTHS_1_1), 0L, 1L)
        val ONE = POS_ONEe0
        val TWO = SMALL_POS_INT_CACHE[2]
        val FOUR = SMALL_POS_INT_CACHE[4]
        val POS_TENe0 = SMALL_POS_INT_CACHE[10]
        val NEG_TENe0 = decimalFNZ(1, 0, 0L, 10L)
        val TEN = POS_TENe0
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
            if (qExp <= 0 && qExp > -ZEROS_CACHE_SIZE && !sign)
                return ZEROS_CACHE[-qExp and ZEROS_CACHE_BCE]
            if (qExp == 0) {
                verify { sign }
                return NEG_ZEROe0
            }
            if (qExp >= Q_MAX)
                return if (sign) NEG_ZEROeQ_MAX else POS_ZEROeQ_MAX
            if (qExp <= Q_TINY)
                return if (sign) NEG_ZEROeQ_TINY else POS_ZEROeQ_TINY
            val signBit = if (sign) 1 else 0
            val steal = stealEncodeZER(signBit, qExp)
            val zero = Decimal(steal, 0L, 0L)
            return zero
        }

        fun one(sign: Boolean, qExp: Int): Decimal {
            require (qExp >= -33 && qExp <= 0)
            if (qExp == 0)
                return if (sign) NEG_ONEe0 else POS_ONEe0
            val pow10Offset = (-qExp shl 1) and POW10_BCE
            val dw1 = POW10[pow10Offset + 1]
            val dw0 = POW10[pow10Offset]
            return decimalFinite(sign, qExp, dw1, dw0)
        }

        /**
         * Returns a `Decimal` with value [n].
         */
        fun from(n: Int) = from(n.toLong())

        /**
         * Returns a `Decimal` with value [l].
         */
        fun from(l: Long): Decimal {
            if (l >= 0L && l < SMALL_POS_INT_CACHE.size.toLong()) {
                val i = l.toInt()
                return SMALL_POS_INT_CACHE[i]
            }
            val signMask = (l shr 63)
            return decimalFNZ((l ushr 63).toInt(), 0, 0L, (l xor signMask) - signMask)
        }

        /**
         * Returns a `Decimal` with value [dw] where the
         * Long param is interpreted as unsigned.
         */
        fun fromUnsigned(dw: Long): Decimal {
            if (dw >= 0L && dw < SMALL_POS_INT_CACHE.size.toLong()) {
                val i = dw.toInt()
                return SMALL_POS_INT_CACHE[i]
            }
            return decimalFNZ(0, 0, 0L, dw)
        }

        /** Decodes a BID-encoded decimal128 from two 64-bit words. */
        fun decodeBid128(bid128Hi: Long, bid128Lo: Long): Decimal =
            bid128Decode(bid128Hi, bid128Lo)

        /**
         * Decodes a BID-encoded decimal128 from two consecutive elements of [longs] starting at [offset].
         * [isLittleEndian] controls whether the least significant long is at [offset] or [offset] + 1.
         */
        fun decodeBid128(longs: LongArray, offset: Int = 0, isLittleEndian: Boolean = false) =
            d128Decode(isDpd = false, longs, offset, isLittleEndian)

        /**
         * Decodes a BID-encoded decimal128 from 16 consecutive bytes of [bytes] starting at [offset].
         * [isLittleEndian] controls whether the least significant byte is at [offset] or [offset] + 15.
         */
        fun decodeBid128(bytes: ByteArray, offset: Int = 0, isLittleEndian: Boolean = false) =
            d128Decode(isDpd = false, bytes, offset, isLittleEndian)

        /** Decodes a DPD-encoded decimal128 from two 64-bit words. */
        fun decodeDpd128(dpd128Hi: Long, dpd128Lo: Long): Decimal =
            dpd128Decode(dpd128Hi, dpd128Lo)

        /**
         * Decodes a DPD-encoded decimal128 from two consecutive elements of [longs] starting at [offset].
         * [isLittleEndian] controls whether the least significant long is at [offset] or [offset] + 1.
         */
        fun decodeDpd128(longs: LongArray, offset: Int = 0, isLittleEndian: Boolean = false) =
            d128Decode(isDpd = true, longs, offset, isLittleEndian)

        /**
         * Decodes a DPD-encoded decimal128 from 16 consecutive bytes of [bytes] starting at [offset].
         * [isLittleEndian] controls whether the least significant byte is at [offset] or [offset] + 15.
         */
        fun decodeDpd128(bytes: ByteArray, offset: Int = 0, isLittleEndian: Boolean = false) =
            d128Decode(isDpd = true, bytes, offset, isLittleEndian)

        /**
         * Parses a `Decimal` from its textual representation using the
         * strict decimal128 interchange format.
         *
         * The parser accepts:
         * - an optional leading sign (`+` or `-`)
         * - a decimal coefficient
         * - optional `_` underscore digit separators
         * - an optional exponent (`E` or `e`) within the decimal128 range
         * - `"Infinity"`, `"+Infinity"`, `"-Infinity"`
         * - `"NaN"`, `"sNaN"`, with an optional numeric payload
         *
         * Leading insignificant zeros are stripped from coefficient and exponent
         * digit sequences.
         *
         * Coefficients with more than 34 significant digits are rounded to 34 digits
         * using the current [DecContext].
         *
         * Underscores are allowed between digits as visual separators;
         * they must not appear adjacent to a sign, decimal point, or exponent marker.
         *
         * ### Invalid input
         * If [str] does not represent a valid decimal128 value, behavior depends on
         * the current [DecContext]:
         * - `DecContext.decimal128Kotlin()` (default) throws [IllegalArgumentException],
         *   consistent with Kotlin's [Double] and `java.math.BigDecimal` parsing behavior.
         * - `DecContext.decimal128IEEE()` signals [DecException.INVALID_OPERATION]
         *   and returns `NaN`, following IEEE 754-2019 rules.
         *
         * @param str the string to parse
         */
        fun from(str: String) = parseToDecimal(str)

        /**
         * Creates a [Decimal] from a [MutDec] value.
         * Common special values (zero, infinity, NaN) are mapped to canonical instances.
         *
         * @param mutDec the source value; must have at most 38 significant digits
         */
        fun from(mutDec: MutDec): Decimal {
            val steal = mutDec.steal
            val digitLen = stealDigitLen(steal)
            require(digitLen <= 38)
            when (stealTyp(steal)) {
                STEAL_TYP_FNZ -> {
                    if (stealSignBit(steal) == 0 &&
                        stealQExp(steal) == 0 &&
                        digitLen <= 3 &&
                        mutDec.dw0 < SMALL_POS_INT_CACHE.size) {
                        val n = mutDec.dw0.toInt()
                        return SMALL_POS_INT_CACHE[n]
                    }
                }
                STEAL_TYP_ZER -> return zero(stealSignFlag(steal), stealQExp(steal))
                STEAL_TYP_INF -> return if (steal < 0) NEG_INFINITY else POS_INFINITY
                STEAL_TYP_NAN -> if (digitLen == 0) {
                    return if (stealIsQNAN(steal))
                        if (steal < 0) NEG_QNAN else POS_QNAN
                    else
                        if (steal < 0) NEG_SNAN else POS_SNAN
                }
            }
            return Decimal(steal, mutDec.dw1, mutDec.dw0)
        }

        fun qNaN(sign: Boolean): Decimal =
            if (sign) NEG_QNAN else POS_QNAN

        /**
         * Returns a quiet NaN with a 128-bit diagnostic payload.
         * If the payload exceeds 33 digits, the canonical no-payload NaN is returned.
         *
         * IEEE 754-2019 3.5.2 c) 2)
         * If the value exceeds the maximum, the significand c is non-
         * canonical and the value used for c is zero.
         */
        fun qNaN(sign: Boolean, dw1: Long, dw0: Long): Decimal {
            val payloadIsZero =
                (dw1 or dw0) == 0L || calcDigitLen128(dw1, dw0) > NAN_PAYLOAD_PRECISION_33
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
            val payloadIsZero =
                (dw1 or dw0) == 0L || calcDigitLen128(dw1, dw0) > NAN_PAYLOAD_PRECISION_33
            return when {
                payloadIsZero && sign -> NEG_SNAN
                payloadIsZero -> POS_SNAN
                else -> Decimal(
                    stealEncodeSNAN(if (sign) 1 else 0, dw1, dw0), dw1, dw0)
            }
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

    // ── Encoding ────────────────────────────────────────────────────────
    // IEEE754-2019 3.5 Decimal interchange format encodings p. 20

    fun encodeBid128(longs: LongArray, offset: Int = 0, isLittleEndian: Boolean = false) =
        d128Encode(this, isDpd = false, longs, offset, isLittleEndian, DecContext.current().tmps.pentad)

    fun encodeBid128(bytes: ByteArray, offset: Int = 0, isLittleEndian: Boolean = false) =
        d128Encode(this, isDpd = false, bytes, offset, isLittleEndian, DecContext.current().tmps.pentad)

    fun encodeDpd128(longs: LongArray, offset: Int = 0, isLittleEndian: Boolean = false) =
        d128Encode(this, isDpd = true, longs, offset, isLittleEndian, DecContext.current().tmps.pentad)

    fun encodeDpd128(bytes: ByteArray, offset: Int = 0, isLittleEndian: Boolean = false) =
        d128Encode(this, isDpd = true, bytes, offset, isLittleEndian, DecContext.current().tmps.pentad)

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
     *         with IEEE-754-2019 §5.7.2.
     */
    fun ieeeClass(): Ieee754Class {
        val steal = steal
        val sign = stealSignFlag(steal)
        val type = stealTyp(steal)
        return when (type) {
            STEAL_TYP_FNZ -> {
                if (stealSciExp(steal) < E_MIN) {
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
     *
     * IEEE754-2019 5.7.2
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
     *
     * IEEE754-2019 5.7.2
     */
    fun isSubnormal(): Boolean = stealIsFNZ(steal) && stealSciExp(steal) < -6143

    /**
     * Returns `true` if this value is normal, subnormal, or zero.
     * Returns `false` for infinity, and NaN.
     *
     * IEEE754-2019 5.7.2
     */
    fun isFinite(): Boolean = stealIsFinite(steal)

    /**
     * Returns `true` if this value is normal or subnormal and non-zero.
     * Returns `false` for zero, infinity, and NaN.
     */
    fun isFiniteNonZero(): Boolean = stealIsFNZ(steal)

    /**
     * Returns `true` if this value is ±0.
     *
     * In decimal floating-point, zero has an entire cohort of representations
     * (one per valid exponent). All are considered zero by this predicate,
     * regardless of the encoded exponent or sign.
     *
     * IEEE754-2019 5.7.2
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
     *
     * // FIXME should is this actually needed?
     */
    fun isCanonicalZero(): Boolean = stealIsZER(steal)

    /**
     * Returns `true` if this value is ±∞.
     *
     * IEEE754-2019 5.7.2
     */
    fun isInfinite(): Boolean = stealIsINF(steal)

    /**
     * Returns `true` if this value is a NaN (quiet or signaling).
     *
     * IEEE754-2019 5.7.2
     */
    fun isNaN(): Boolean = stealIsNAN(steal)

    /**
     * Returns `true` if this value is a *signaling* NaN.
     *
     * Signaling NaNs trigger an [DecException.INVALID_OPERATION] exception
     * (or set the corresponding flag) when they appear as operands to most
     * arithmetic operations.
     *
     * IEEE754-2019 5.7.2
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
     *
     * IEEE754-2019 5.7.2
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

    fun isExactIntegral(): Boolean = d128IsExactIntegral(this)

    fun isOddIntegral(): Boolean = d128IsOddIntegral(this)

    fun isExactPowerOfTen(): Boolean = d128IsExactPowerOfTen(this)

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
     *
     * IEEE754-2019 5.7.3
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
     * Returns the quantum of this value as a [Decimal], per IEEE 754-2019 §3.5.2.
     *
     * For a finite value with exponent `q`, returns `10^q` — the value of one
     * unit in the last place. For example, `1.23.quantum()` returns `0.01`.
     *
     * IEEE754-2019 5.3.2
     *
     * Signals invalidOperation and returns NaN if called on a non-finite value.
     *
     * @see quantumInt
     */
    fun quantum(): Decimal {
        val steal = this.steal
        if (stealIsFinite(steal))
            return Decimal.from(stealQExp(steal))
        return DecContext.current().signalInvalidOperation(QUANTUM_OF_NON_FINITE, this)
    }

    /**
     * Returns the quantum exponent `qExp` of this finite value as an [Int],
     * i.e., the power of ten by which the integer coefficient is scaled.
     * For example, `1.23.quantumInt()` returns `-2`.
     *
     * Signals invalidOperation and returns [Int.MIN_VALUE] if called on a
     * non-finite value.
     *
     * @see quantum
     */
    fun quantumInt(): Int {
        val steal = this.steal
        if (stealIsFinite(steal))
            return stealQExp(steal)
        DecContext.current().signalInvalidOperation(QUANTUM_OF_NON_FINITE, this)
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
     *
     * IEEE754-2019 5.5.1
     */
    fun abs() = if (steal < 0) negate() else this

    /**
     * Returns this value with its sign bit flipped.
     *
     * Negate is exact: no rounding, no exceptions. `−(−x) == x` always.
     * For NaNs (including sNaNs), the sign bit is toggled without signaling.
     *
     * IEEE754-2019 5.5.1
     */
    fun negate(): Decimal {
        val steal = steal
        val newSign = !stealSignFlag(steal)
        when {
            stealIsZER(steal) && stealQExp(steal) == 0 -> return zero(newSign)
            stealIsINF(steal) -> return infinity(newSign)
            stealIsNAN(steal) && stealBitLen(steal) == 0 ->
                    return if (stealIsSNAN(steal)) Decimal.sNaN(newSign) else Decimal.qNaN(newSign)
        }
        return Decimal(steal xor BIT31, dw1, dw0)
    }

    /**
     * Returns this value with the sign bit copied from [signDonor].
     *
     * The magnitude and type of `this` are unchanged; only the sign
     * bit is replaced. No exceptions are signaled.
     *
     * IEEE 754-2019 §5.5.1
     */
    fun copySign(signDonor: Decimal): Decimal =
        if (this.signBit == signDonor.signBit) this else this.negate()

    /**
     * Returns a copy of this value. Because `Decimal` is immutable, this
     * always returns `this`.
     *
     * Provided for IEEE 754-2019 completeness (§5.5.1).
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
    fun logB(): Decimal {
        val steal = steal
        return when {
            stealIsZER(steal) -> DecContext.current().signalDivByZero(NEG_INFINITY)
            stealIsFNZ(steal) -> from(stealSciExp(steal))
            stealIsINF(steal) -> POS_INFINITY
            else -> nanOperandFound(this)
        }

    }

    /**
     * Returns `this × 10^pow10Delta`, rounded as needed within [ctx].
     *
     * Equivalent to IEEE 754-2019 5.3.3 `scaleB`.
     *
     * Infinities pass through unchanged; NaNs signal [DecException.INVALID_OPERATION].
     */
    fun scaleB(pow10Delta: Int): Decimal {
        val steal = steal
        return when {
            stealIsFinite(steal) -> {
                val pow10DeltaCapped = max(min(pow10Delta, 100_000), -100_000)
                val qNew = qExp + pow10DeltaCapped
                decFinalizeFinite(signFlag, dw1, dw0, qNew)
            }

            stealIsINF(steal) -> this
            else -> nanOperandFound(this)
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
    fun stripTrailingZeros(): Decimal = stripTrailingZerosImpl(this, DecContext.current())

    /**
     * Returns this value rescaled so that its quantum exponent equals
     * `-decimalScale`, rounding if necessary under the current decimal context.
     *
     * `decimalScale` is the number of digits to the right of the decimal point.
     * For example, `withScale(2)` produces a value like `1.23`.
     *
     * This is equivalent to [quantize] with a quantum of `10^(-decimalScale)`.
     */
    fun withScale(decimalScale: Int): Decimal =
        withScaleImpl(this, decimalScale)

    /**
     * Returns this value rescaled to the same quantum as [reference], rounding
     * if necessary under the current decimal context.
     *
     * If both operands are infinite, returns infinity with the sign of this value.
     * If exactly one operand is infinite, signals invalidOperation and returns NaN.
     * If either operand is NaN, propagates NaN according to the usual rules.
     *
     * IEEE754-2019 5.3.2
     *
     * @param reference The value whose quantum (exponent) is used as the target.
     * @return This value rescaled to the quantum of [reference].
     * @see withScale
     */
    fun quantize(reference: Decimal): Decimal =
        quantizeImpl(this, reference)

    /**
     * Returns the smallest representable value greater than this one,
     * IEEE 754-2019 §5.3.1 `nextUp`.
     *
     * - For finite values, increments the last digit of the coefficient.
     * - +∞ returns +∞.
     * - NaN signals [DecException.INVALID_OPERATION].
     */
    fun nextUp(): Decimal = nextUpOrDown(this, isUp = true)

    /**
     * Returns the largest representable value smaller than this one,
     * IEEE 754-2019 §5.3.1 `nextDown`.
     *
     * - For finite values, decrements the last digit of the coefficient.
     * - −∞ returns −∞.
     * - NaN signals [DecException.INVALID_OPERATION].
     */
    fun nextDown(): Decimal = nextUpOrDown(this, isUp = false)

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
    override fun compareTo(other: Decimal): Int = d128CompareJavaStyle(this, other)

    /**
     * Compares this value to the integer [n] using Java-style numeric comparison.
     *
     * Equivalent to `compareTo(Decimal(n))`. See [compareTo] for full semantics.
     *
     * @return −1, 0, or +1 according to Java-style numeric ordering.
     */
    operator fun compareTo(n: Int): Int = d128CompareJavaStyle(this, n)

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
     * Always throws [UnsupportedOperationException].
     *
     * `Decimal` does not support use as a hash-based collection key because
     * [equals] uses numeric equality (ignoring quantum), which is incompatible
     * with a stable hash contract across cohort members.
     *
     * @throws UnsupportedOperationException always
     */
    override fun hashCode(): Int {
        throw UnsupportedOperationException("hashCode() not supported")
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
        ((this.steal xor other.steal).toLong() or
                (this.dw1 xor other.dw1) or
                (this.dw0 xor other.dw0)) == 0L

    /**
     * Tests numeric equality between this and [other].
     *
     * Equivalent to `==` for two `Decimal` values — [equals] is overridden
     * to compare numerically, so cohort members such as `1.0` and `1.00`
     * compare equal. Provided for consistency with the mixed-type `Int EQ Decimal`
     * form, where `==` cannot be used.
     *
     *  @see compareJavaStyleTo for the full semantics.
     */
    infix fun EQ(other: Decimal): Boolean = eqJavaStyle(other)

    /** Returns `true` if this value and integer [n] are numerically equal
     *  under Java-style semantics.
     *
     * Prefer this over `==` when comparing mixed `Int`/`Decimal` values — Kotlin's
     * `==` compiles to `equals()`, which requires matching types and will silently
     * return `false` for `Int == Decimal`.
     *
     *  @see compareJavaStyleTo for the full semantics.
     */
    infix fun EQ(n: Int): Boolean = d128EqJavaStyle(this, n)

    /**
     * Returns `true` if this value and [other] are not numerically equal
     * under Java-style semantics.
     *
     * Equivalent to `!=` for two `Decimal` values — [equals] is overridden
     * to compare numerically, so cohort members such as `1.0` and `1.00`
     * compare equal. Provided for consistency with the mixed-type `Int EQ Decimal`
     * form, where `!=` cannot be used.
     *
     *  @see compareJavaStyleTo for the full semantics.
     */
    infix fun NE(other: Decimal): Boolean = !d128EqJavaStyle(this, other)

    /**
     * Returns `true` if this value and integer [n] are numerically not equal
     * under Java-style semantics.
     *
     * Prefer this over `!=` when comparing mixed `Int`/`Decimal` values — Kotlin's
     * `!=` compiles to `!equals()`, which requires matching types and will silently
     * return `true` for `Int != Decimal`.
     *
     * @see compareJavaStyleTo for the full semantics.
     */
    infix fun NE(n: Int): Boolean = !d128EqJavaStyle(this, n)

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

    /**
     * Returns `true` if `totalOrder(this, other)` — i.e., `this ≤ other` in total order.
     *
     * IEEE754-2019 5.7.2 & 5.10
     */
    fun isTotalOrder(other: Decimal) = compareTotalOrderTo(other) <= 0

    /**
     * Like [compareTotalOrderTo] but compares magnitudes only (ignoring sign).
     * IEEE 754-2019 `totalOrderMag`.
     *
     * @return −1, 0, or +1.
     */
    fun compareTotalOrderMagTo(other: Decimal) = d128CompareTotalOrderMag(this, other)

    /**
     * Returns `true` if `totalOrderMag(this, other)` — i.e., `|this| ≤ |other|` in total order.
     *
     * IEEE754-2019 5.7.2
     */
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
     * IEEE 754-2019 5.6.1 `compareQuietEqual`: returns `true` if `this == other`
     * numerically. Does **not** signal on quiet NaN operands.
     */
    fun compareQuietEqual(other: Decimal): Boolean =
        d128CompareQuiet754(this, other) == IEEE754_EQ

    /**
     * IEEE 754-2019 5.6.1 `compareQuietNotEqual`: returns `true` if `this ≠ other`.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietNotEqual(other: Decimal): Boolean =
        d128CompareQuiet754(this, other) != IEEE754_EQ

    /**
     * IEEE 754-2019 5.6.1 `compareQuietGreater`: returns `true` if `this > other`.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietGreater(other: Decimal): Boolean =
        d128CompareQuiet754(this, other) == IEEE754_GT

    /**
     * IEEE 754-2019 5.6.1 `compareQuietNotGreater`: returns `true` if `this` is not greater
     * than `other` (i.e., less, equal, or unordered).
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietNotGreater(other: Decimal): Boolean =
        d128CompareQuiet754(this, other) != IEEE754_GT

    fun compareQuietGreaterEqual(other: Decimal): Boolean {
        val cmp754 = d128CompareQuiet754(this, other)
        return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_EQ)
    }

    fun compareQuietLess(other: Decimal): Boolean =
        d128CompareQuiet754(this, other) == IEEE754_LT

    fun compareQuietLessEqual(other: Decimal): Boolean {
        val cmp754 = d128CompareQuiet754(this, other)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_EQ)
    }

    fun compareQuietUnordered(other: Decimal): Boolean =
        d128CompareQuiet754(this, other) == IEEE754_UNORDERED

    /**
     * IEEE 754-2019 5.6.1 `compareQuietLessUnordered`: returns `true` if `this < other`
     * **or** either operand is NaN.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietLessUnordered(other: Decimal): Boolean {
        val cmp754 = d128CompareQuiet754(this, other)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_UNORDERED)
    }

    /**
     * IEEE 754-2019 5.6.1 `compareQuietGreaterUnordered`: returns `true` if `this > other`
     * **or** either operand is NaN.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietGreaterUnordered(other: Decimal): Boolean {
        val cmp754 = d128CompareQuiet754(this, other)
        return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_UNORDERED)
    }

    /**
     * IEEE 754-2019 5.6.1 `compareQuietNotLess`: returns `true` if `this` is not less
     * than `other`.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietNotLess(other: Decimal): Boolean =
        d128CompareQuiet754(this, other) != IEEE754_LT

    /**
     * Returns the full four-valued [Compare754Result] for a quiet comparison.
     * The result is one of [IEEE754_LT], [IEEE754_EQ], [IEEE754_GT], or [IEEE754_UNORDERED].
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuiet(other: Decimal): Compare754Result =
        d128CompareQuiet754(this, other)

    /**
     * IEEE 754-2019 5.6.1 `compareQuietOrdered`: returns `true` if **neither** operand is NaN.
     * Does **not** signal on quiet NaN operands.
     */
    fun compareQuietOrdered(other: Decimal): Boolean =
        d128CompareQuiet754(this, other) != IEEE754_UNORDERED

    /**
     * IEEE 754-2019 5.6.1 `compareSignalingEqual`: returns `true` if `this == other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN
     * (quiet or signaling).
     */
    fun compareSignalingEqual(other: Decimal): Boolean =
        d128CompareSignaling754(this, other) == IEEE754_EQ

    /**
     * IEEE 754-2019 5.6.1 `compareSignalingGreater`: returns `true` if `this > other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingGreater(other: Decimal): Boolean =
        d128CompareSignaling754(this, other) == IEEE754_GT

    /**
     * IEEE 754-2019 5.6.1 `compareSignalingGreaterEqual`: returns `true` if `this >= other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingGreaterEqual(other: Decimal): Boolean {
        val cmp754 = d128CompareSignaling754(this, other)
        return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_EQ)
    }

    /**
     * IEEE 754-2019 5.6.1 `compareSignalingLess`: returns `true` if `this < other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingLess(other: Decimal): Boolean =
        d128CompareSignaling754(this, other) == IEEE754_LT

    /**
     * IEEE 754-2019 5.6.1 `compareSignalingLessEqual`: returns `true` if `this <= other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingLessEqual(other: Decimal): Boolean {
        val cmp754 = d128CompareSignaling754(this, other)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_EQ)
    }

    /**
     * IEEE 754-2019 5.6.1 `compareSignalingNotEqual`: returns `true` if `this ≠ other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingNotEqual(other: Decimal): Boolean =
        d128CompareSignaling754(this, other) != IEEE754_EQ

    /**
     * IEEE 754-2019 5.6.1 `compareSignalingNotGreater`: returns `true` if `this` is
     * not greater than `other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingNotGreater(other: Decimal): Boolean =
        d128CompareSignaling754(this, other) != IEEE754_GT

    /**
     * IEEE 754-2019 5.6.1 `compareSignalingLessUnordered`: returns `true` if
     * `this < other` **or** either operand is NaN.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */

    fun compareSignalingLessUnordered(other: Decimal): Boolean {
        val cmp754 = d128CompareSignaling754(this, other)
        return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_UNORDERED)
    }

    /**
     * IEEE 754-2019 5.6.1 `compareSignalingNotLess`: returns `true` if `this` is
     * not less than `other`.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingNotLess(other: Decimal): Boolean =
        d128CompareSignaling754(this, other) != IEEE754_LT


    /**
     * IEEE 754-2019 5.6.1 `compareSignalingGreaterUnordered`: returns `true` if
     * `this > other` **or** either operand is NaN.
     * **Signals** [DecException.INVALID_OPERATION] if either operand is a NaN.
     */
    fun compareSignalingGreaterUnordered(other: Decimal): Boolean {
        val cmp754 = d128CompareSignaling754(this, other)
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

    /**
     * Returns `true` if this value and [other] are equal under Java-style
     * semantics — equivalent to `compareJavaStyleTo(other) == 0`.
     *
     * This is the implementation backing the [EQ] and [NE] infix operators.
     * Prefer those at call sites, either a functions or as infix operators.
     *
     * @see compareJavaStyleTo for the full semantics.
     */
    fun eqJavaStyle(other: Decimal): Boolean = d128EqJavaStyle(this, other)

    // ── String Representation ─────────────────────────────────────────────────

    /**
     * Returns a string representation of this value formatted according to
     * [DecPrefs] in [DecContext.current].
     *
     * Default [DecPrefs] behaviour:
     * - Finite values: plain decimal for exponents in `[-6, 0]`, normalized
     *   scientific notation (uppercase `E`, no `+` sign) outside that range.
     * - Special values: `Infinity`, `-Infinity`, `NaN`, `-NaN`; NaN payload
     *   included; sNaN not collapsed to qNaN.
     */
    override fun toString(): String = d128ToString(steal, dw1, dw0, DecContext.current())

    // ── operators and arithmetic ─────────────────────────────────────────────────

    /**
     * Returns the negation of this value.
     */
    operator fun unaryMinus(): Decimal = negate()

    /**
     * Returns the sum of this value and [other], rounded according to [DecContext.current].
     */
    operator fun plus(other: Decimal): Decimal = d128AddImpl(this, other)

    /**
     * Returns the sum of this value and integer [n], rounded according to [DecContext.current].
     */
    operator fun plus(n: Int): Decimal = d128AddImpl(this, from(n))

    /**
     * Returns the difference of this value and [other], rounded according to [DecContext.current].
     */
    operator fun minus(other: Decimal): Decimal = d128SubImpl(this, other)

    /**
     * Returns the difference of this value and integer [n], rounded according to [DecContext.current].
     */
    operator fun minus(n: Int): Decimal = d128SubImpl(this, from(n))

    /**
     * Returns the product of this value and [other], rounded according to [DecContext.current].
     */
    operator fun times(other: Decimal): Decimal = d128MulImpl(this, other)

    /**
     * Returns the product of this value and int [n], rounded according to [DecContext.current].
     */
    operator fun times(n: Int): Decimal = d128MulImpl(this, Decimal.from(n))

    /**
     * Returns the quotient of this value and [other], rounded according to [DecContext.current].
     */
    operator fun div(other: Decimal): Decimal = d128DivImpl(this, other)

    /**
     * Returns the quotient of this value and Int [n], rounded according to [DecContext.current].
     */
    operator fun div(n: Int): Decimal = d128DivImpl(this, n.toDecimal())

    /**
     * Returns the remainder of this value divided by [other], truncated toward zero.
     */
    operator fun rem(other: Decimal): Decimal = d128RemTruncImpl(this, other)

    /**
     * Returns the remainder of this value divided by [other], truncated toward zero.
     */
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
     *
     * | `this`       | `other` | result |
     * |--------------|---------|--------|
     * | finite       | ±0      | NaN, signals [DecException.INVALID_OPERATION] |
     * | ±∞           | any     | NaN, signals [DecException.INVALID_OPERATION] |
     * | finite       | ±∞      | `this` |
     * | NaN (either) | —       | NaN, signals [DecException.INVALID_OPERATION] |
     *
     * The sign of an exact zero result matches the sign of `this`.
     *
     * @see remainderTruncate for the `%` operator behavior
     */
    fun remainderNear(other: Decimal): Decimal = d128RemNearImpl(this, other)

    /**
     * Returns the square of this value (`this × this`), rounded according to [DecContext.current].
     *
     * Special cases follow IEEE 754-2019 multiplication rules:
     * - `(±∞)² = +∞`
     * - `(±0)² = +0`
     * - qNaN returns NaN without signaling
     * - sNaN signals [DecException.INVALID_OPERATION] and returns NaN
     */
    fun square(): Decimal = d128SqrImpl(this, DecContext.current())

    /**
     * Returns the square root of this value, rounded according to [DecContext.current].
     *
     * IEEE754-2019 5.4.1
     *
     * Special cases:
     * - `√(+∞) = +∞`
     * - `√(±0) = ±0`
     * - `√(negative)` signals [DecException.INVALID_OPERATION] and returns NaN
     * - qNaN returns NaN without signaling
     * - sNaN signals [DecException.INVALID_OPERATION] and returns NaN
     */
    fun squareRoot(): Decimal = d128SqrtImpl(this)

    /**
     * Returns `(this * multiplier) + addend` as a single fused operation,
     * with only one rounding step applied to the final result.
     *
     * This avoids the double rounding that would occur if multiplication
     * and addition were performed separately, producing a more accurate result.
     * The operation is performed under the current decimal context.
     *
     * IEEE754-2019 §5.4.1 `fusedMultiplyAdd`
     *
     * @param multiplier The value to multiply with this.
     * @param addend The value to add to the product.
     * @return The fused multiply-add result.
     */
    fun fma(multiplier: Decimal, addend: Decimal): Decimal =
        d128FmaImpl(this, multiplier, addend, DecContext.current())

// ── Rounding ──────────────────────────────────────────────────────────────

    /**
     * Rounds to the nearest integer, ties resolved to even (banker's rounding).
     * Does not signal [DecException.INEXACT].
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralTiesToEven`.
     */
    fun roundToIntegralTiesToEven(): Decimal =
        d128RoundToIntegral(this, DecRounding.ROUND_TIES_TO_EVEN, suppressInexact = true)

    /**
     * Rounds to the nearest integer, ties resolved away from zero.
     * Does not signal [DecException.INEXACT].
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralTiesToAway`.
     */
    fun roundToIntegralTiesToAway(): Decimal =
        d128RoundToIntegral(this, DecRounding.ROUND_TIES_TO_AWAY, suppressInexact = true)

    /**
     * Rounds toward zero (truncation).
     * Does not signal [DecException.INEXACT].
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralTowardZero`.
     */
    fun roundToIntegralTowardZero(): Decimal =
        d128RoundToIntegral(this, DecRounding.ROUND_TOWARD_ZERO, suppressInexact = true)

    /**
     * Rounds toward positive infinity (ceiling).
     * Does not signal [DecException.INEXACT].
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralTowardPositive`.
     */
    fun roundToIntegralTowardPositive(): Decimal =
        d128RoundToIntegral(this, DecRounding.ROUND_TOWARD_POSITIVE, suppressInexact = true)

    /**
     * Rounds toward negative infinity (floor).
     * Does not signal [DecException.INEXACT].
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralTowardNegative`.
     */
    fun roundToIntegralTowardNegative(): Decimal =
        d128RoundToIntegral(this, DecRounding.ROUND_TOWARD_NEGATIVE, suppressInexact = true)

    /**
     * Rounds to an integer using the rounding mode from [DecContext.current],
     * signaling [DecException.INEXACT] if the result is not already integral.
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralExact`.
     */
    fun roundToIntegralSignalInexact(): Decimal =
        d128RoundToIntegral(this, DecContext.current().decRounding, suppressInexact = false)

    /**
     * Rounds to an integer using the rounding mode from [DecContext.current].
     * Does not signal [DecException.INEXACT].
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralValue`.
     */
    fun roundToIntegral(): Decimal =
        d128RoundToIntegral(this, DecContext.current().decRounding, suppressInexact = true)

    /**
     * Rounds to the nearest integer, ties resolved to even (banker's rounding),
     * signaling [DecException.INEXACT] if the result is not already integral.
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralExact` with `roundTiesToEven`.
     */
    fun roundToIntegralTiesToEvenSignalInexact(): Decimal =
        d128RoundToIntegral(this, DecRounding.ROUND_TIES_TO_EVEN, suppressInexact = false)

    /**
     * Rounds to the nearest integer, ties resolved away from zero,
     * signaling [DecException.INEXACT] if the result is not already integral.
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralExact` with `roundTiesToAway`.
     */
    fun roundToIntegralTiesToAwaySignalInexact(): Decimal =
        d128RoundToIntegral(this, DecRounding.ROUND_TIES_TO_AWAY, suppressInexact = false)

    /**
     * Rounds toward zero (truncation),
     * signaling [DecException.INEXACT] if the result is not already integral.
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralExact` with `roundTowardZero`.
     */
    fun roundToIntegralTowardZeroSignalInexact(): Decimal =
        d128RoundToIntegral(this, DecRounding.ROUND_TOWARD_ZERO, suppressInexact = false)

    /**
     * Rounds toward positive infinity (ceiling),
     * signaling [DecException.INEXACT] if the result is not already integral.
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralExact` with `roundTowardPositive`.
     */
    fun roundToIntegralTowardPositiveSignalInexact(): Decimal =
        d128RoundToIntegral(this, DecRounding.ROUND_TOWARD_POSITIVE, suppressInexact = false)

    /**
     * Rounds toward negative infinity (floor),
     * signaling [DecException.INEXACT] if the result is not already integral.
     *
     * IEEE 754-2019 §5.3.1 `roundToIntegralExact` with `roundTowardNegative`.
     */
    fun roundToIntegralTowardNegativeSignalInexact(): Decimal =
        d128RoundToIntegral(this, DecRounding.ROUND_TOWARD_NEGATIVE, suppressInexact = false)

// ── Conversion to Kotlin Integer Types ───────────────────────────────────

    /**
t     * Converts this decimal to [Long] using round-half-to-even (banker's rounding).
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toLongTiesToEven(): Long =
        d128ConvertToLong(this, DecRounding.ROUND_TIES_TO_EVEN, suppressInexact = true, suppressInvalid = false)

    /**
     * Converts this decimal to [Long] using round-half-to-even (banker's rounding),
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toLongTiesToEvenSignalInexact(): Long =
        d128ConvertToLong(this, DecRounding.ROUND_TIES_TO_EVEN, suppressInexact = false, suppressInvalid = false)

    /**
     * Converts this decimal to [Long] using round-half-away-from-zero.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toLongTiesToAway(): Long =
        d128ConvertToLong(this, DecRounding.ROUND_TIES_TO_AWAY, suppressInexact = true, suppressInvalid = false)

    /**
     * Converts this decimal to [Long] using round-half-away-from-zero,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toLongTiesToAwaySignalInexact(): Long =
        d128ConvertToLong(this, DecRounding.ROUND_TIES_TO_AWAY, suppressInexact = false, suppressInvalid = false)

    /**
     * Converts this decimal to [Long] by truncating toward zero.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toLongTowardZero(): Long =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_ZERO, suppressInexact = true, suppressInvalid = false)

    /**
     * Converts this decimal to [Long] by truncating toward zero.
     * Does not signal or raise any flags.
     * Returns [Long.MIN_VALUE] on overflow, NaN, or infinity.
     */
    fun toLongTowardZeroNoFlags(): Long =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_ZERO, suppressInexact = true, suppressInvalid = true)

    /**
     * Converts this decimal to [Long] by truncating toward zero,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toLongTowardZeroSignalInexact(): Long =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_ZERO, suppressInexact = false, suppressInvalid = false)

    /**
     * Converts this decimal to [Long] by rounding toward positive infinity (ceiling).
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toLongTowardPositive(): Long =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_POSITIVE, suppressInexact = true, suppressInvalid = false)

    /**
     * Converts this decimal to [Long] by rounding toward positive infinity (ceiling),
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toLongTowardPositiveSignalInexact(): Long =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_POSITIVE, suppressInexact = false, suppressInvalid = false)

    /**
     * Converts this decimal to [Long] by rounding toward negative infinity (floor).
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toLongTowardNegative(): Long =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_NEGATIVE, suppressInexact = true, suppressInvalid = false)

    /**
     * Converts this decimal to [Long] by rounding toward negative infinity (floor),
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Long.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toLongTowardNegativeSignalInexact(): Long =
        d128ConvertToLong(this, DecRounding.ROUND_TOWARD_NEGATIVE, suppressInexact = false, suppressInvalid = false)

    /**
     * Converts this decimal to [Int] using round-half-to-even (banker's rounding).
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toIntTiesToEven(): Int =
        d128ConvertToInt(this, DecRounding.ROUND_TIES_TO_EVEN, suppressInexact = true)

    /**
     * Converts this decimal to [Int] using round-half-to-even (banker's rounding),
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toIntTiesToEvenSignalInexact(): Int =
        d128ConvertToInt(this, DecRounding.ROUND_TIES_TO_EVEN, suppressInexact = false)

    /**
     * Converts this decimal to [Int] using round-half-away-from-zero.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toIntTiesToAway(): Int =
        d128ConvertToInt(this, DecRounding.ROUND_TIES_TO_AWAY, suppressInexact = true)

    /**
     * Converts this decimal to [Int] using round-half-away-from-zero,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toIntTiesToAwaySignalInexact(): Int =
        d128ConvertToInt(this, DecRounding.ROUND_TIES_TO_AWAY, suppressInexact = false)

    /**
     * Converts this decimal to [Int] by truncating toward zero.
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toIntTowardZero(): Int =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_ZERO, suppressInexact = true)

    /**
     * Converts this decimal to [Int] by truncating toward zero,
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toIntTowardZeroSignalInexact(): Int =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_ZERO, suppressInexact = false)

    /**
     * Converts this decimal to [Int] by rounding toward positive infinity (ceiling).
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toIntTowardPositive(): Int =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_POSITIVE, suppressInexact = true)

    /**
     * Converts this decimal to [Int] by rounding toward positive infinity (ceiling),
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toIntTowardPositiveSignalInexact(): Int =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_POSITIVE, suppressInexact = false)

    /**
     * Converts this decimal to [Int] by rounding toward negative infinity (floor).
     * Does not signal [DecException.INEXACT].
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toIntTowardNegative(): Int =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_NEGATIVE, suppressInexact = true)

    /**
     * Converts this decimal to [Int] by rounding toward negative infinity (floor),
     * signaling [DecException.INEXACT] if the result is not exact.
     * Signals [DecException.INVALID_OPERATION] and returns [Int.MIN_VALUE]
     * on overflow, NaN, or infinity.
     *
     * IEEE754-2019 5.8
     */
    fun toIntTowardNegativeSignalInexact(): Int =
        d128ConvertToInt(this, DecRounding.ROUND_TOWARD_NEGATIVE, suppressInexact = false)

    // ── Elementary Functions ──────────────────────────────────────────────────────────────

    /**
     * Returns the reciprocal (1/this) of this value, following IEEE 754-2019 division rules.
     *
     * Special cases:
     * - `1/0` signals [DecException.DIVIDE_BY_ZERO] and returns ±infinity
     * - `1/±∞` returns ±zero
     * - `1/NaN` returns NaN
     */
    fun reciprocal(): Decimal = d128DivImpl(ONE, this, DecContext.current())

    /**
     * Returns this value raised to the integer power [n].
     *
     * Special cases follow IEEE 754-2019 `pown` rules:
     * - `x^0 = 1` for all finite x, including zero and infinity, but NaN^0 = NaN
     * - `x^1 = x`
     * - `0^n` for negative n signals [DecException.DIVIDE_BY_ZERO] and returns ±infinity
     * - `(±∞)^n` for negative n returns ±zero
     *
     * IEEE754-2019 9.2 `pown(integer)`
     */
    fun pow(n: Int): Decimal = d128PownImpl(this, n)

    /**
     * Returns this value raised to the decimal power [x].
     *
     * Special cases follow IEEE 754-2019 `pow` rules:
     * - `x^0 = 1` for all finite x, including zero and infinity, but NaN^0 = NaN
     * - `0^x` for negative x signals [DecException.DIVIDE_BY_ZERO] and returns ±infinity
     * - `(±∞)^x` for negative x returns ±zero
     * - `pow` of a negative base with a non-integer exponent signals [DecException.INVALID_OPERATION] and returns NaN
     *
     * IEEE754-2019 9.2
     */
    fun pow(x: Decimal): Decimal = d128PowImpl(this, x)

    /**
     * Computes compound interest: (1 + x)^n where x is this decimal and n is an integer exponent.
     *
     * Special cases:
     * - x ≥ −1 or qNaN, n = 0: returns 1
     * - x = −1, n < 0: returns +∞ and signals divideByZero
     * - x = −1, n > 0: returns +0
     * - x = ±0: returns 1
     * - x = +∞, n > 0: returns +∞
     * - x = +∞, n < 0: returns +0
     * - x < −1: returns qNaN and signals invalidOperation
     * - x = qNaN, n ≠ 0: returns qNaN
     *
     * Domain: x ∈ [−1, +∞], n ∈ Z
     *
     * @param n the integer exponent
     * @return (1 + this)^n
     */
     fun compound(n: Int): Decimal = d128CompoundImpl(this, n)

    /**
     * Computes the nth root of x: x^(1/n) where x is this decimal and n is an integer.
     *
     * Special cases:
     * - n = 0: returns qNaN and signals invalidOperation
     * - x < 0 and n even: returns qNaN and signals invalidOperation
     * - n = −1: may overflow or underflow
     * - x = 0 and n < 0: returns ±∞ and signals divideByZero
     * - x = ±0, n odd < 0: returns ±∞ and signals divideByZero
     * - x = ±0, n even < 0: returns +∞ and signals divideByZero
     * - x = ±0, n even > 0: returns +0
     * - x = ±0, n odd > 0: returns ±0
     * - x = +∞, n > 0: returns +∞
     * - x = +∞, n < 0: returns +0
     * - x = −∞, n odd > 0: returns −∞
     * - x = −∞, n even > 0: returns qNaN and signals invalidOperation
     * - x = −∞, n odd < 0: returns −0
     * - x = −∞, n even < 0: returns qNaN and signals invalidOperation
     *
     * Note: rootn(−0, 2) == +0 differs from squareRoot(−0) == -0.
     *
     * Domain: x ∈ [−∞, +∞], n ∈ Z
     *
     * @param n the integer root
     * @return this^(1/n)
     */
    fun rootn(n: Int): Decimal = d128RootnImpl(this, n)

    /**
     * Computes the natural logarithm of this value.
     *
     * Special cases:
     * - `ln(0)` signals [DecException.DIVIDE_BY_ZERO] and returns `-∞`
     * - `ln(negative)` signals [DecException.INVALID_OPERATION] and returns NaN
     * - `ln(+∞)` returns `+∞`
     *
     * @return `ln(this)`
     */
    fun ln(): Decimal = d128LnImpl(this)

    /**
     * Computes `e` raised to the power of this value.
     *
     * Special cases:
     * - `exp(-∞)` returns `0`
     * - `exp(+∞)` returns `+∞`
     *
     * IEEE754-2019 9.2
     *
     * @return `e^this`
     */
    fun exp(): Decimal = d128ExpImpl(this, isExp10 = false)

    /**
     * Computes the base-10 logarithm of this value.
     *
     * Special cases:
     * - `log10(0)` signals [DecException.DIVIDE_BY_ZERO] and returns `-∞`
     * - `log10(negative)` signals [DecException.INVALID_OPERATION] and returns NaN
     * - `log10(+∞)` returns `+∞`
     *
     * IEEE754-2019 9.2
     *
     * @return `log10(this)`
     */
    fun log10(): Decimal = d128Log10Impl(this)

    /**
     * Computes `10` raised to the power of this value.
     *
     * Special cases:
     * - `exp10(-∞)` returns `0`
     * - `exp10(+∞)` returns `+∞`
     *
     * IEEE754-2019 9.2
     *
     * @return `10^this`
     */
    fun exp10(): Decimal = d128ExpImpl(this, isExp10 = true)


}

/**
 * Parses this string into a [Decimal] according to [DecContext.current].
 *
 * @see Decimal.from
 */
fun String.toDecimal(): Decimal = Decimal.from(this)

/**
 * Converts this [Int] to a [Decimal].
 */
fun Int.toDecimal(): Decimal = Decimal.from(this)

/**
 * Converts this [Long] to a [Decimal].
 */
fun Long.toDecimal(): Decimal = Decimal.from(this)

/**
 * Returns the sum of all elements, or [ZERO] if the collection is empty.
 */
fun Iterable<Decimal>.sum(): Decimal = fold(ZERO) { acc, d -> acc + d }

/**
 * Adds a [Decimal] to this integer, delegating to [Decimal.plus].
 *
 * Enables natural left-hand syntax:
 * ```
 * val result = 1 + interestRate  // equivalent to interestRate + 1
 * ```
 *
 * @receiver The integer addend.
 * @param d The [Decimal] to add to.
 * @return The sum as a [Decimal].
 */
operator fun Int.plus(d: Decimal) = d.plus(this)

/**
 * Subtracts a [Decimal] from this integer, delegating to [Decimal.minus].
 *
 * Enables natural left-hand syntax:
 * ```
 * val result = 1 - interestRate  // equivalent to 1.toDecimal() - interestRate
 * ```
 *
 * @receiver The integer minuend.
 * @param d The [Decimal] subtrahend.
 * @return The difference as a [Decimal].
 */
operator fun Int.minus(d: Decimal) = -(d.minus(this))

/**
 * Multiplies this integer by a [Decimal], delegating to [Decimal.times].
 *
 * Enables natural left-hand syntax:
 * ```
 * val result = 3 * myDecimal  // equivalent to myDecimal * 3
 * ```
 *
 * @receiver The integer multiplier.
 * @param d The [Decimal] to multiply by.
 * @return The product as a [Decimal].
 */
operator fun Int.times(d: Decimal) = d.times(this)

/**
 * Divides this integer by a [Decimal], delegating to [Decimal.div].
 *
 * A common use case is computing the reciprocal of a [Decimal]:
 * ```
 * val reciprocal = 1 / d
 * ```
 *
 * @receiver The integer dividend.
 * @param d The [Decimal] divisor.
 * @return The quotient as a [Decimal].
 */
operator fun Int.div(d: Decimal) = Decimal.from(this).div(d)

/**
 * Tests numeric equality between this integer and [d], delegating to [Decimal.EQ].
 *
 * Prefer this over `==` when comparing mixed `Int`/`Decimal` values — Kotlin's
 * `==` compiles to `equals()`, which requires matching types and will silently
 * return `false` for `Int == Decimal`.
 *
 * Enables natural left-hand syntax: `3 EQ myDecimal`
 *
 * @see Decimal.EQ
 */
infix fun Int.EQ(d: Decimal) = d.EQ(this)

/**
 * Tests inequality between this integer and [d], delegating to [Decimal.NE].
 *
 * Prefer this over `!=` when comparing mixed `Int`/`Decimal` values — Kotlin's
 * `!=` compiles to `!equals()`, which requires matching types and will silently
 * return `true` for `Int != Decimal`.
 *
 * Enables natural left-hand syntax: `3 NE myDecimal`
 *
 * @see Decimal.NE
 */
infix fun Int.NE(d: Decimal) = d.NE(this)

/**
 * Compares this integer to [d] using Java-style numeric comparison,
 * delegating to [Decimal.compareTo].
 *
 * Enables natural left-hand syntax: `3 > myDecimal`, `3 <= myDecimal`, etc.
 *
 * @return −1, 0, or +1 according to Java-style numeric ordering.
 * @see Decimal.compareTo
 */
operator fun Int.compareTo(d: Decimal) = -(d.compareTo(this))
