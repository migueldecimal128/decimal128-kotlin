package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.Decimal.Companion.bothFnz
import com.decimal128.decimal.Decimal.Companion.newZero
import kotlin.math.max
import kotlin.math.min

internal fun maxImpl(x: Decimal, y: Decimal): Decimal =
    maxImpl(x, y, DecContext.current())

internal fun maxImpl(x: Decimal, y: Decimal, env: DecContext): Decimal {
    if (Decimal.neitherIsNaN(x, y))
        return if (cmpTotalOrderImpl(x, y, env) >= 0) x else y
    return nanOperandFound(x, y, env)
}

private fun magnitudeCompareFinite(x: Decimal, y: Decimal) : Int {
    if (x.qExp == y.qExp)
        return ucmp128(x.dw1, x.dw0, y.dw1, y.dw0)
    val cmpSci = x.eExp.compareTo(y.eExp)
    if (cmpSci != 0)
        return cmpSci
    val qDelta = x.qExp - y.qExp
    val qDeltaAbs = kotlin.math.abs(qDelta)
    val pow10BitLen = pow10BitLen(qDeltaAbs)
    val pow10Offset = pow10Offset(qDeltaAbs)
    val dw0Pow10 = POW10[pow10Offset]
    val dw1Pow10 = POW10[pow10Offset + 1]
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

private fun magnitudeCompareInfinite(x: Decimal, y: Decimal) : Int {
    verify { max(x.qExp, y.qExp) == NON_FINITE_INF }
    val minExp = min(x.qExp, y.qExp)
    return when {
        minExp == NON_FINITE_INF -> 0
        x.qExp == NON_FINITE_INF -> 1
        else -> -1
    }
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
private fun cmpTotalOrderMagFnzFnz(x: Decimal, y: Decimal): Int {
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




