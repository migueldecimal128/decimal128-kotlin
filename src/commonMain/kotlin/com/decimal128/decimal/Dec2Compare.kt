@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.FNZ_FNZ
import com.decimal128.decimal.BinopSignature.FNZ_INF
import com.decimal128.decimal.BinopSignature.FNZ_ZER
import com.decimal128.decimal.BinopSignature.INF_FNZ
import com.decimal128.decimal.BinopSignature.INF_INF
import com.decimal128.decimal.BinopSignature.INF_ZER
import com.decimal128.decimal.BinopSignature.NAN_FOUND
import com.decimal128.decimal.BinopSignature.ZER_FNZ
import com.decimal128.decimal.BinopSignature.ZER_INF
import com.decimal128.decimal.BinopSignature.ZER_ZER
import com.decimal128.decimal.Dec2.Companion.bothFnz

object Dec2Compare {


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
    fun cmpTotalOrder(x: Dec2, y: Dec2): Int {
        val xSign = x.sign0Neg1 // 0 or -1 (0xFFFF_FFFF)
        if ((xSign xor y.sign0Neg1) != 0)
            return (xSign shl 1) + 1 // return -1 or 1
        val cmpMag = cmpTotalOrderMag(x, y)
        return negateForSign(cmpMag, xSign)
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
    fun cmpTotalOrderMag(x: Dec2, y: Dec2): Int {
        val cmp =
            if (bothFnz(x, y))
                cmpTotalOrderMagFnzFnz(x, y)
            else when (BinopSignature.of(x, y)) {
                ZER_ZER -> cmp32(x.qExp, y.qExp)
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
    private inline fun cmpTotalOrderMagFnzFnz(x: Dec2, y: Dec2): Int {
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

    private fun cmpMagFnzFnz(x: Dec2, y: Dec2): Int {
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

    private fun cmpTotalOrderMagnitudeNanFound(x: Dec2, y: Dec2): Int {
        return when {
            x.qExp < NON_FINITE_QNAN -> -1
            y.qExp < NON_FINITE_QNAN -> 1
            // if both are the same NaN, then compare payloads
            x.qExp == y.qExp -> ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
            // strange ... qNaN sorts higher than sNaN
            x.qExp == NON_FINITE_QNAN -> 1
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
    fun cmpJavaStyle(x: Dec2, y: Dec2): Int {
        if (Dec2.neitherIsNaN(x, y)) {
            val xSign = x.sign0Neg1 // 0 or -1 (0xFFFF_FFFF)
            if ((xSign xor y.sign0Neg1) != 0)
                return (xSign shl 1) + 1 // return -1 or 1
            val cmpMag = cmpJavaStyleMagnitude(x, y)
            return negateForSign(cmpMag, xSign)
        }
        return when {
            x.qExp < NON_FINITE_QNAN -> -1
            y.qExp < NON_FINITE_QNAN -> 1
            else -> 0
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
    fun eqJavaStyle(x: Dec2, y: Dec2): Boolean {
        return when (BinopSignature.of(x, y)) {
            ZER_ZER -> true
            ZER_FNZ -> false
            ZER_INF -> false

            FNZ_ZER -> false
            FNZ_FNZ -> x.sign == y.sign && cmpMagFnzFnz(x, y) == 0
            FNZ_INF -> false

            INF_ZER -> false
            INF_FNZ -> false
            INF_INF -> x.sign == y.sign
            NAN_FOUND -> x.isNaN() && y.isNaN()
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
    private fun cmpJavaStyleMagnitude(x: Dec2, y: Dec2): Int {
        val cmpMag =
            if (bothFnz(x, y))
                cmpMagFnzFnz(x, y)
            else when (BinopSignature.of(x, y)) {
                ZER_ZER -> 0
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
     * @param sign0Neg1 0 for non-negative values, or −1 for negative values
     */
    private inline fun negateForSign(cmp: Int, sign0Neg1: Int) =
        (cmp xor sign0Neg1) - sign0Neg1

}