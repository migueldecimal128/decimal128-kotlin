@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal
import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

internal inline fun packLengths(digitLen: Int, bitLen: Int) =
    ((digitLen shl 9) or bitLen).toShort()

/*
internal inline fun packSignExp(sign: Boolean, qExp: Int): Short = ((if (sign) 0x8000 else 0) or (qExp and 0x7FFF)).toShort()

internal inline fun unpackBitLen(packedLengths: Short) = packedLengths.toInt() and 0x1FF

internal inline fun unpackDigitLen(packedLengths: Short) = (packedLengths.toInt() shr 9) and 0x7F

internal inline fun unpackSign(signExp: Short) = signExp < 0

internal inline fun unpackExp(signExp: Short) = (signExp.toInt() shl 1) shr 1

internal inline fun calcPackedLengths(dw0: Long): Short {
    val bitLen = calcBitLen64(dw0)
    val digitLen = calcDigitLen64(bitLen, dw0)
    val packed = packLengths(digitLen, bitLen)
    return packed
}

internal inline fun calcPackedLengths(dw1: Long, dw0: Long): Short {
    val bitLen = calcBitLen128(dw1, dw0)
    val digitLen = calcDigitLen128(bitLen, dw1, dw0)
    val packed = packLengths(digitLen, bitLen)
    return packed
}

internal inline fun capExponentRange(e: Int): Int {
    return min(max(e, CAPPED_EXP_MIN), CAPPED_EXP_MAX)
}

 */

// These have an implied binary decimal point at 2**32
private const val LOG2_10_FLOOR: Long = 14_267_572_564L
private const val LOG2_10_CEIL: Long = 14_267_572_565L


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

