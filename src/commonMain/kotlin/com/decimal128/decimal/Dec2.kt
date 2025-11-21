@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.U256Bits.calcBitLen128
import com.decimal128.decimal.U256Bits.calcBitLen64
import com.decimal128.decimal.U256Pow10.calcDigitLen128
import com.decimal128.decimal.U256Pow10.calcDigitLen64

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

// These are scaled by 2**32
private const val LOG2_10_FLOOR: Long = 14_267_572_564L
private const val LOG2_10_CEIL: Long = 14_267_572_565L

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
    // the normalized scientific base 10 exponent
    internal val eExp: Int
        get() = qExp + (digitLen - (-digitLen ushr 31))
    // the lower bound of the normalized binary exponent interval
    internal val bExpMin: Int
        get() = calcBExpMin(bitLen, qExp)
    internal val bExpMax: Int
        get() = calcBExpMax(bitLen, qExp)

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
        internal inline fun calcBExpMin(bitLen: Int, qExp: Int): Int {
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
        internal inline fun calcBExpMax(bitLen: Int, qExp: Int): Int {
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

