// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.Decimal.Companion.NEG_ONEe0
import com.decimal128.decimal.Decimal.Companion.NaN
import com.decimal128.decimal.Decimal.Companion.POS_ONEe0
import com.decimal128.decimal.Decimal.Companion.ZERO
import com.decimal128.decimal.Decimal.Companion.hasNaN
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun d128CompareMagnitude(x: Decimal, y: Decimal): Int {
    val qMax = max(x.qExp, y.qExp)
    return when {
        qMax < NON_FINITE_INF -> magnitudeCompareFinite(x, y)
        qMax == NON_FINITE_INF -> magnitudeCompareInfinite(x, y)
        else -> magnitudeCompareNaN(x, y)
    }
}

private fun magnitudeCompareFinite(x: Decimal, y: Decimal): Int {
    if (x.qExp == y.qExp)
        return ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
    val cmpSci = x.eExp.compareTo(y.eExp)
    if (cmpSci != 0)
        return cmpSci
    val qDelta = x.qExp - y.qExp
    val qDeltaAbs = abs(qDelta)
    val pow10BitLen = pow10BitLen(qDeltaAbs)
    val (dw1Pow10, dw0Pow10) = pow10_128(qDeltaAbs)
    if (qDelta > 0) {
        // x.qExp is larger
        // scale up x.coefficient
        if (pow10BitLen <= 64)
            return -ucmp128_128x64(y.dw1, y.dw0, x.dw1, x.dw0, dw0Pow10)
        return -ucmp128_128x64(y.dw1, y.dw0, dw1Pow10, dw0Pow10, x.dw0)
    } else {
        // scale up y
        if (pow10BitLen <= 64)
            return ucmp128_128x64(x.dw1, x.dw0, y.dw1, y.dw0, dw0Pow10)
        return ucmp128_128x64(x.dw1, x.dw0, dw1Pow10, dw0Pow10, y.dw0)
    }
}

private fun magnitudeCompareInfinite(x: Decimal, y: Decimal): Int {
    verify { max(x.qExp, y.qExp) == NON_FINITE_INF }
    val minExp = min(x.qExp, y.qExp)
    return when {
        minExp == NON_FINITE_INF -> 0
        x.qExp == NON_FINITE_INF -> 1
        else -> -1
    }
}

private fun magnitudeCompareNaN(x: Decimal, y: Decimal): Int {
    val minExp = min(x.qExp, y.qExp)
    return when {
        minExp >= NON_FINITE_QNAN -> 0
        x.qExp >= NON_FINITE_QNAN -> 1
        y.qExp >= NON_FINITE_QNAN -> -1
        else -> throw IllegalStateException()
    }
}


/**
 * Compares two decimal128 values using the IEEE-754 *totalOrder* relation.
 *
 * This implements the ordering referenced in
 * IEEE-754-2019 **§5.7.2 General operations** and defined in
 * **§5.10 Details of totalOrder predicate**, which specifies
 * a *total* ordering over all floating-point values, including NaNs,
 * signed zeros, subnormals, infinities, and normal numbers.
 *
 * Unlike the usual <, ≤, >, and ≥ comparison predicates, the
 * `totalOrder` relation imposes a complete ordering on all values,
 * including values in the same cohort that are numerically equal and
 * NaNs that are normally considered *unordered*. The total-order rules
 * determine an ordering based on:
 *
 *  • **sign**
 *  • **magnitude**, using a canonical comparison of exponents and
 *    coefficients
 *  • **NaN category**, where signaling NaNs precede quiet NaNs and
 *    NaNs are ordered by their payloads
 *
 * This function performs only the *sign-ordering* and delegates the
 * magnitude ordering of finite non-zero values to
 * `cmpTotalOrderMag(x, y)`. Once the magnitude relation is known,
 * the result is adjusted according to the sign of the operands, as
 * required by the total-order specification.
 *
 * The return value uses the standard Kotlin/Java convention:
 *
 *  • −1 → `x` is less than `y`
 *  • 0  → `x` and `y` are equal in totalOrder
 *  • +1 → `x` is greater than `y`
 *
 * No rounding, exceptions, or signaling behavior are produced.
 * This is a pure comparison consistent with IEEE-754-2019 totalOrder.
 *
 * @return −1, 0, or +1 indicating the total-order relationship of `x` and `y`.
 */
internal fun d128CompareTotalOrder(x: Decimal, y: Decimal): Int {
    val xSignMask = x.signMask // 0 or -1 (0xFFFF_FFFF)
    if ((xSignMask xor y.signMask) != 0)
        return (xSignMask shl 1) + 1 // return -1 or 1
    val cmpMag = d128CompareTotalOrderMag(x, y)
    return negateForSign(cmpMag, xSignMask)
}

/**
 * Compares the *magnitudes* of two decimal128 values according to the
 * IEEE-754-2019 totalOrder rules (see §§5.10 and 5.7.2).
 *
 * This handles ordering among zeros, finite non-zero values, infinities,
 * and NaNs (via `cmpTotalOrderMagnitudeNanFound`). Sign is *not* considered
 * here; callers must apply sign ordering separately.
 *
 * For finite non-zero operands this dispatches to
 * `cmpTotalOrderMagFnzFnz(x, y)`. Other operand-class combinations
 * are ordered according to the totalOrder magnitude rules.
 *
 * @return −1, 0, or +1 describing the total-order magnitude relation.
 */
fun d128CompareTotalOrderMag(x: Decimal, y: Decimal): Int {
    val signature = signatureOf(x.steal, y.steal)
    val cmp =
        if (signature == xFNZ_FNZ) {
            cmpTotalOrderMagFnzFnz(x, y)
        } else when (signature) {
            xZER_ZER -> cmp32(x.qExp, y.qExp)
            xZER_FNZ,
            xZER_INF,
            xFNZ_INF -> -1

            xFNZ_ZER,
            xINF_ZER,
            xINF_FNZ -> 1
            xINF_INF -> 0
            else -> cmpTotalOrderMagnitudeNanFound(x, y)
        }
    return cmp
}

/**
 * Compares the magnitudes of two **finite, non-zero** decimal128 values
 * using the IEEE-754-2019 *totalOrder* rules (see §5.10 and §5.7.2).
 *
 * This function assumes both operands are finite and non-zero; all other
 * operand classes (zeros, infinities, NaNs) are handled by
 * `cmpTotalOrderMag`.
 *
 * Magnitude ordering proceeds by:
 *
 *  • comparing the **adjusted exponents** (`eExp`)
 *  • comparing the **binary-exponent bounds** (`bExpMin`/`bExpMax`)
 *  • comparing coefficients directly when `qExp` matches
 *  • scaling the coefficient of the operand with the smaller quantum
 *    exponent (`qExp`) when needed, and comparing the scaled integers
 *
 * After magnitude comparison, if `x` and `y` represent the **same numeric
 * value**, IEEE-754 totalOrder requires ordering based on the quantum
 * exponent (`qExp`): for positive operands, the one with
 * the **smaller qExp** compares *earlier*.
 * This ensures a strict ordering among all members of the same
 * numerical *cohort*, as required by §5.10.
 *
 * @return −1, 0, or +1 describing the total-order magnitude relation
 *         between the two finite, non-zero values.
 */
private inline fun cmpTotalOrderMagFnzFnz(x: Decimal, y: Decimal): Int {
    val cmpMag = cmpMagFnzFnz(x, y)

    // If x and y represent the same floating-point datum:
    //  i) If x and y have negative sign,
    //    totalOrder(x, y) is true if and only if the exponent of x ≥ the exponent of y
    //  ii) otherwise,
    //    totalOrder(x, y) is true if and only if the exponent of x ≤ the exponent of y.

    // of course, we are comparing magnitude so assume signs are equal
    // and that return value may be negated because of sign.
    val cmpExp = cmp32(x.qExp, y.qExp)
    val cmpMagIsZeroMask = (cmpMag or -cmpMag).inv()
    val cmp = cmpMag or (cmpExp and cmpMagIsZeroMask)
    return cmp
}

private fun cmpMagFnzFnz(x: Decimal, y: Decimal): Int {
    val cmpMag = when {
        x.eExp > y.eExp -> 1
        x.eExp < y.eExp -> -1
        x.bExpMin > y.bExpMax -> 1
        x.bExpMax < y.bExpMin -> -1
        x.qExp == y.qExp -> ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
        x.qExp > y.qExp -> -ucmp128ScalePow10(y.dw1, y.dw0, x.dw1, x.dw0, x.qExp - y.qExp)
        // x.qExp < y.qExp
        else -> ucmp128ScalePow10(x.dw1, x.dw0, y.dw1, y.dw0, y.qExp - x.qExp)
    }
    return cmpMag
}

private fun cmpTotalOrderMagnitudeNanFound(x: Decimal, y: Decimal): Int {
    val stealX = x.steal
    val stealY = y.steal
    verify { stealHasNAN(stealX, stealY) }
    return when {
        !stealIsNAN(stealX) -> -1
        !stealIsNAN(stealY) -> 1
        // if both are the same NaN, then compare payloads
        (stealX and STEAL_NAN_MASK) == (stealY and STEAL_NAN_MASK) ->
            ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
        // qNaN sorts higher than sNaN
        stealIsQNAN(stealX) -> 1
        else -> -1
    }
}

/**
 * Compares two decimal128 values using **Java-style numeric comparison**
 * semantics. This is a context-free comparison similar to Java’s
 * `Double.compare`, where NaN payloads, NaN sign, and encoding
 * differences among cohort members are ignored.
 *
 * Ordering rules implemented here:
 *
 *  • **NaNs**:
 *      – Any non-NaN compares less than any NaN.
 *      – All NaNs compare *equal* to each other, ignoring payload,
 *        signaling/quiet distinction, and sign.
 *
 *  • **Sign ordering**:
 *      – Negative values compare less than positive values.
 *      – **−0 < +0**, matching Java’s distinction between signed zeros.
 *
 *  • **Finite non-zero values**:
 *      – Compared purely by numerical magnitude.
 *      – All cohort members (different encodings of the same value)
 *        compare as **equal**.
 *
 * The comparison result uses the standard Kotlin/Java convention:
 *
 *  • **−1** → `x` is less than `y`
 *  • **0**  → `x` and `y` are numerically equal under Java-style rules
 *  • **+1** → `x` is greater than `y`
 *
 * This function does **not** raise IEEE-754 flags or interpret signaling
 * NaNs. It provides a deterministic, environment-free ordering suitable
 * for use in `compareTo`, sorting, sets, and maps.
 *
 * @return −1, 0, or +1 describing the Java-style ordering between `x` and `y`.
 */
internal fun d128CompareJavaStyle(x: Decimal, y: Decimal): Int {
    val stealX = x.steal
    val stealY = y.steal
    when {
        !stealHasNAN(stealX, stealY) -> {
            val xSignMask = stealSignMask(stealX) // 0 or -1 (0xFFFF_FFFF)
            if (xSignMask != stealSignMask(stealY))
                return (xSignMask shl 1) + 1 // return -1 or 1
            val cmpMag = cmpNumericMagnitude(x, y)
            return negateForSign(cmpMag, xSignMask)
        }

        !stealIsNAN(stealX) -> return -1
        !stealIsNAN(stealY) -> return 1
        else -> return 0
    }
}

/**
 * Tests two decimal128 values for **Java-style numeric equality**.
 *
 * This implements the same notion of equality used by Java’s
 * `Double.equals` / `Double.compare` and `BigDecimal.compareTo`:
 *
 *  • **Finite non-zero values** are equal if they have the same sign
 *    and equal magnitude (`cmpMagFnzFnz(x, y) == 0`), regardless of
 *    exponent or encoding differences. All cohort members compare equal.
 *
 *  • **Zeros:** −0 and +0 compare equal.
 *
 *  • **Infinities:** +∞ equals +∞, and −∞ equals −∞; cross-sign
 *    infinities are not equal.
 *
 *  • **NaNs:** Any NaN is considered equal to any other NaN, ignoring
 *    payload, sign bit, and signaling/quiet distinction.
 *
 *  • All other cross-category combinations (zero vs finite, finite vs
 *    infinite, etc.) compare as not equal.
 *
 * This function performs no IEEE-754 signaling and does not examine
 * NaN payloads or NaN sign bits. It provides a deterministic,
 * context-free equality suitable for Java-style comparison semantics.
 *
 * @return `true` if `x` and `y` are equal under Java-style rules;
 *         `false` otherwise.
 */
internal fun d128EqJavaStyle(x: Decimal, y: Decimal): Boolean {
    val signature = signatureOf(x.steal, y.steal)
    return if (signature == xFNZ_FNZ) {
        x.sign == y.sign && cmpMagFnzFnz(x, y) == 0
    } else when (signature) {
        xFNZ_ZER,
        xFNZ_INF,
        xINF_ZER,
        xZER_FNZ,
        xZER_INF,
        xINF_FNZ -> false
        xZER_ZER -> true
        xINF_INF -> x.sign == y.sign
        else -> x.isNaN() && y.isNaN()
    }
}

/**
 * Compares the **magnitudes** of two decimal128 values using
 * Java-style numeric comparison rules.
 *
 * This function assumes that:
 *  • NaN handling has already been performed by the caller, and
 *    neither operand is a NaN here.
 *  • Sign ordering has already been handled; this function compares
 *    **absolute magnitudes only**.
 *
 * Java-style magnitude ordering follows the standard numeric hierarchy:
 *
 *  • `0` compares less than any finite non-zero
 *  • finite non-zero values are ordered by their numerical magnitude
 *  • any finite value compares less than infinity
 *  • `+∞` and `−∞` compare as equal in magnitude
 *
 * For two finite non-zero values, magnitude comparison is delegated to
 * `cmpMagFnzFnz(x, y)`. For mixed categories (zero / finite / infinity),
 * ordering is determined by the pairwise class signature encoded in
 * `BinopSignature`.
 *
 * This function never inspects NaN payloads, sign bits, or exponent
 * encodings. It returns a pure magnitude ordering suitable for use in
 * Java-style comparison where sign and NaN rules are applied outside.
 *
 * @return −1 if `|x| < |y|`, 0 if `|x| == |y|`, or +1 if `|x| > |y|`.
 */
private fun cmpNumericMagnitude(x: Decimal, y: Decimal): Int {
    val signature = signatureOf(x.steal, y.steal)
    val cmpMag =
        if (signature == xFNZ_FNZ) {
            cmpMagFnzFnz(x, y)
        } else when (signature) {
            xZER_ZER -> 0
            xZER_FNZ,
            xZER_INF,
            xFNZ_INF -> -1

            xFNZ_ZER,
            xINF_ZER,
            xINF_FNZ -> 1
            xINF_INF -> 0
            else -> throw IllegalStateException()
        }
    return cmpMag
}


/**
 * Returns `cmp` when `sign0Neg1 == 0` and `-cmp` when `sign0Neg1 == -1`,
 * using the branch-free transform `(cmp xor sign0Neg1) - sign0Neg1`.
 *
 * Used in comparison routines to reverse magnitude comparison results
 * for negative values.
 *
 * @param cmp the magnitude comparison result (−1, 0, or +1)
 * @param signMask 0 for non-negative values, or −1 for negative values
 */
private inline fun negateForSign(cmp: Int, signMask: Int) =
    (cmp xor signMask) - signMask

internal fun d128CompareQuiet754(x: Decimal, y: Decimal, ctx: DecContext): Compare754Result =
    d128Compare754(x, y, isSignaling = false, ctx)

internal fun d128CompareSignaling754(x: Decimal, y: Decimal, ctx: DecContext): Compare754Result =
    d128Compare754(x, y, isSignaling = true, ctx)

internal fun d128Compare754(x: Decimal, y: Decimal, isSignaling: Boolean, ctx: DecContext): Compare754Result {
    if (Decimal.neitherIsNaN(x, y)) {
        // IEEE754-2019
        // 5.11 Details of comparison predicates
        // Comparisons shall ignore the sign of zero (so +0 = −0).
        if (x.isZero() && y.isZero())
            return IEEE754_EQ
        val xSignMask = x.signMask // 0 or -1 (0xFFFF_FFFF)
        val cmp =
            if (xSignMask != y.signMask) (xSignMask shl 1) + 1 // -1 or 1
            else negateForSign(cmpNumericMagnitude(x, y), xSignMask)
        return Compare754Result(cmp)
    }
    if (isSignaling || x.isSignaling() || y.isSignaling())
        ctx.signalInvalid()
    return IEEE754_UNORDERED
}

// IEEE754-2019 5.6.1 Comparisons

internal fun d128CompareQuietEqual(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareQuiet754(x, y, ctx) == IEEE754_EQ

internal fun d128CompareQuietNotEqual(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareQuiet754(x, y, ctx) != IEEE754_EQ

internal fun d128CompareSignalingEqual(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareSignaling754(x, y, ctx) == IEEE754_EQ

internal fun d128CompareSignalingGreater(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareSignaling754(x, y, ctx) == IEEE754_GT

internal fun d128CompareSignalingGreaterEqual(x: Decimal, y: Decimal, ctx: DecContext): Boolean {
    val cmp754 = d128CompareSignaling754(x, y, ctx)
    return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_EQ)
}

internal fun d128CompareSignalingLess(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareSignaling754(x, y, ctx) == IEEE754_LT

internal fun d128CompareSignalingLessEqual(x: Decimal, y: Decimal, ctx: DecContext): Boolean {
    val cmp754 = d128CompareSignaling754(x, y, ctx)
    return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_EQ)
}

internal fun d128CompareSignalingNotEqual(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareSignaling754(x, y, ctx) != IEEE754_EQ

internal fun d128CompareSignalingNotGreater(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareSignaling754(x, y, ctx) != IEEE754_GT

internal fun d128CompareSignalingLessUnordered(x: Decimal, y: Decimal, ctx: DecContext): Boolean {
    val cmp754 = d128CompareSignaling754(x, y, ctx)
    return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_UNORDERED)
}

internal fun d128CompareSignalingNotLess(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareSignaling754(x, y, ctx) != IEEE754_LT

internal fun d128CompareSignalingGreaterUnordered(x: Decimal, y: Decimal, ctx: DecContext): Boolean {
    val cmp754 = d128CompareSignaling754(x, y, ctx)
    return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_UNORDERED)
}

internal fun d128CompareQuietGreater(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareQuiet754(x, y, ctx) == IEEE754_GT

internal fun d128CompareQuietGreaterEqual(x: Decimal, y: Decimal, ctx: DecContext): Boolean {
    val cmp754 = d128CompareQuiet754(x, y, ctx)
    return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_EQ)
}

internal fun d128CompareQuietLess(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareQuiet754(x, y, ctx) == IEEE754_LT

internal fun d128CompareQuietLessEqual(x: Decimal, y: Decimal, ctx: DecContext): Boolean {
    val cmp754 = d128CompareQuiet754(x, y, ctx)
    return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_EQ)
}

internal fun d128CompareQuietUnordered(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareQuiet754(x, y, ctx) == IEEE754_UNORDERED

internal fun d128CompareQuietNotGreater(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareQuiet754(x, y, ctx) != IEEE754_GT

internal fun d128CompareQuietLessUnordered(x: Decimal, y: Decimal, ctx: DecContext): Boolean {
    val cmp754 = d128CompareQuiet754(x, y, ctx)
    return (cmp754 == IEEE754_LT) or (cmp754 == IEEE754_UNORDERED)
}

internal fun d128CompareQuietNotLess(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareQuiet754(x, y, ctx) != IEEE754_LT

internal fun d128CompareQuietGreaterUnordered(x: Decimal, y: Decimal, ctx: DecContext): Boolean {
    val cmp754 = d128CompareQuiet754(x, y, ctx)
    return (cmp754 == IEEE754_GT) or (cmp754 == IEEE754_UNORDERED)
}

internal fun d128CompareQuietOrdered(x: Decimal, y: Decimal, ctx: DecContext): Boolean =
    d128CompareQuiet754(x, y, ctx) != IEEE754_UNORDERED

private val mapToDecimal: Array<Decimal> =
    arrayOf(NEG_ONEe0, ZERO, POS_ONEe0, NaN)

// TODO - in MutDec I combined the different types of comparisons
//  into a common routine. Consider doing that for Decimal in order
//  to reduce code size and redundancy.

internal fun cmpImpl(x: Decimal, y: Decimal): Decimal =
    cmpImpl(x, y, DecContext.current())

internal fun cmpImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    if (hasNaN(x, y))
        return nanOperandFound(x, y, ctx)
    if (x.isZero() && y.isZero())
        return ZERO
    if (x.sign != y.sign)
        return if (x.sign) NEG_ONEe0 else POS_ONEe0
    val cmpMag =
        if (bothFnz(x, y)) {
            cmpMagnitudeFnzFnz(x, y)
        } else when (binopSignatureOf(x, y)) {
            ZER_ZER -> throw IllegalStateException()
            ZER_FNZ -> -1
            ZER_INF -> -1

            FNZ_ZER -> 1
            FNZ_FNZ -> throw IllegalStateException()
            FNZ_INF -> -1

            INF_ZER -> 1
            INF_FNZ -> 1
            INF_INF -> 0

            NAN_FOUND -> throw IllegalStateException()
        }
    val negateMask = x.signMask
    val t = (cmpMag xor negateMask) - negateMask
    return mapToDecimal[t + 1]
}

private fun cmpFnzFnz(x: Decimal, y: Decimal): Int {
    if (x.sign != y.sign)
        return if (x.sign) -1 else 1
    val negateMask = x.signMask // 0 or -1
    return (cmpMagnitudeFnzFnz(x, y) xor negateMask) - negateMask
}

private fun cmpMagnitudeFnzFnz(x: Decimal, y: Decimal): Int {
    val x1 = x.dw1; val x0 = x.dw0
    val xQ = x.qExp
    val y1 = y.dw1; val y0 = y.dw0
    val yQ = y.qExp
    if (xQ == yQ)
        return ucmp128(x1, x0, y1, y0)
    val cmpSci = x.eExp.compareTo(y.eExp)
    if (cmpSci != 0)
        return cmpSci
    val qDelta = xQ - yQ
    val qDeltaAbs = abs(qDelta)
    val pow10BitLen = pow10BitLen(qDeltaAbs)
    val (dw1Pow10, dw0Pow10) = pow10_128(qDeltaAbs)
    if (qDelta > 0) {
        // x.qExp is larger
        // scale up x.coefficient
        if (pow10BitLen <= 64)
            return -ucmp128_128x64(y1, y0, x1, x0, dw0Pow10)
        return -ucmp128_128x64(y1, y0, dw1Pow10, dw0Pow10, x0)
    } else {
        // scale up y
        if (pow10BitLen <= 64)
            return ucmp128_128x64(x1, x0, y1, y0, dw0Pow10)
        return ucmp128_128x64(x1, x0, dw1Pow10, dw0Pow10, y0)
    }
}

internal fun cmpSignalingImpl(x: Decimal, y: Decimal): Decimal =
    cmpSignalingImpl(x, y, DecContext.current())

internal fun cmpSignalingImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    if (hasNaN(x, y))
        return nanOperandFound(x, y, ctx, alwaysSignal = true)
    return cmpImpl(x, y, ctx)
}

internal fun cmpMagnitudeImpl(x: Decimal, y: Decimal): Decimal =
    cmpMagnitudeImpl(x, y, DecContext.current())

fun cmpMagnitudeImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val cmp = if (bothFnz(x, y)) {
        cmpMagnitudeFnzFnz(x, y)
    } else when (binopSignatureOf(x, y)) {
        ZER_ZER -> 0
        ZER_FNZ -> -1
        ZER_INF -> -1

        FNZ_ZER -> 1
        FNZ_FNZ -> throw IllegalStateException()
        FNZ_INF -> -1

        INF_ZER -> 1
        INF_FNZ -> 1
        INF_INF -> 0

        NAN_FOUND -> return nanOperandFound(x, y, ctx)
    }
    return mapToDecimal[(cmp + 1) and 0x03]
}

internal fun cmpTotalOrderImpl(x: Decimal, y: Decimal, ctx: DecContext): Int {
    if (x.sign != y.sign)
        return if (x.sign) -1 else 1
    val negateMask = -x.signBit // 0 or -1
    return (cmpTotalOrderMagnitudeImpl(x, y, ctx) xor negateMask) - negateMask
}

fun cmpTotalOrderMagnitudeImpl(x: Decimal, y: Decimal, ctx: DecContext): Int {
    return if (bothFnz(x, y)) {
        cmpTotalOrderMagnitudeFnzFnz(x, y)
    } else when (binopSignatureOf(x, y)) {
        ZER_ZER -> x.qExp.compareTo(y.qExp)
        ZER_FNZ -> -1
        ZER_INF -> -1

        FNZ_ZER -> 1
        FNZ_FNZ -> throw IllegalStateException()
        FNZ_INF -> -1

        INF_ZER -> 1
        INF_FNZ -> 1
        INF_INF -> 0
        NAN_FOUND -> cmpTotalOrderMagnitudeNanFound(x, y)
    }
}

private fun cmpTotalOrderMagnitudeFnzFnz(x: Decimal, y: Decimal): Int {
    val cmp = cmpMagnitudeFnzFnz(x, y)
    if (cmp != 0)
        return cmp
    // If x and y represent the same floating-point datum:
    //  i) If x and y have negative sign,
    //    totalOrder(x, y) is true if and only if the exponent of x ≥ the exponent of y
    //  ii) otherwise,
    //    totalOrder(x, y) is true if and only if the exponent of x ≤ the exponent of y.
    return x.qExp.compareTo(y.qExp)
}

