package com.decimal128.decimal

import com.decimal128.decimal.C128Compare.c128UnscaledCompare
import com.decimal128.decimal.DecException.INVALID_OPERATION
import com.decimal128.decimal.DecExceptionReason.*
import kotlin.math.max
import kotlin.math.min

object D128Add {

    fun addImpl(x: Decimal, ySign: Boolean, y: Decimal, decEnv: DecEnv): Decimal {
        val qMax = max(x.qExp, y.qExp)
        return when {
            (qMax < MIN_SPECIAL_VALUE) and (x.qExp == y.qExp) ->
                unscaledFiniteAddImpl(x, ySign, y, decEnv)
            qMax < MIN_SPECIAL_VALUE ->
                scaledFiniteAdd(x, ySign, y, decEnv)
            qMax == NON_FINITE_INF ->
                infiniteAddImpl(x, ySign, y, decEnv)
            qMax == NON_FINITE_SNAN ->
                decEnv.signal(
                    INVALID_OPERATION,
                    SIGNALING_NAN_OPERAND,
                    "add/sub",
                    Decimal.NaN)
            x.qExp == NON_FINITE_QNAN -> x
            y.qExp == NON_FINITE_QNAN -> y
            else -> throw IllegalStateException()
        }
    }

    // IEEE754-2019 6.3 The sign bit
    // When the sum of two operands with opposite signs
    // (or the difference of two operands with like signs)
    // is exactly zero, the sign of that sum (or difference)
    // shall be +0 under all rounding-direction attributes
    // except roundTowardNegative; under that attribute, the
    // sign of an exact zero sum (or difference) shall be −0.
    // However, under all rounding-direction attributes,
    // when x is zero, x + x and x − (−x) have the sign of x.

    private fun unscaledFiniteAddImpl(x: Decimal, ySign: Boolean, y: Decimal, decEnv: DecEnv): Decimal {
        val xIsZero = x.isZero()
        val yIsZero = y.isZero()

        if (xIsZero or yIsZero)
            return unscaledFiniteAddZero(x, ySign, y, decEnv)

        if (x.sign == ySign)
            return unscaledFiniteAddMagnitudes(x, y, decEnv)

        val cmp = c128UnscaledCompare(x, y)
        return when {
            (cmp > 0) -> C128AddSub.c128UnscaledSub(x, y)
            (cmp < 0) -> C128AddSub.c128UnscaledSub(y, x)
            else -> Decimal.newZero(decEnv.isRoundTowardNegative(), x.qExp)
        }
    }

    private fun unscaledFiniteAddZero(x: Decimal, ySign: Boolean, y: Decimal, decEnv: DecEnv): Decimal {
        check(x.isZero() || y.isZero())
        return when {
            // Case 1: Only one operand is zero.
            !x.isZero() -> x
            !y.isZero() -> y

            // Case 2: Both operands are zero. This is where the special rules apply.
            // Rule: x + x = x. Preserves the sign of zero. (-0) + (-0) = -0.
            x.sign == ySign -> x

            // Rule: (+0) + (-0). The signs are different.
            // Result is +0 unless rounding is roundTowardNegative.
            else -> Decimal.newZero(decEnv.isRoundTowardNegative(), x.qExp)
        }
    }

    private fun scaledFiniteAddZero(x: Decimal, ySign: Boolean, y: Decimal, decEnv: DecEnv): Decimal {
        check(x.isZero() || y.isZero())
        val minExp = min(x.qExp, y.qExp)
        return when {
            !x.isZero() -> scaleToMinExp(x, minExp, decEnv.decFormat.precision)
            !y.isZero() -> scaleToMinExp(y, minExp, decEnv.decFormat.precision)
            x.sign == ySign -> if (x.qExp == minExp) x else y
            else -> Decimal.newZero(decEnv.isRoundTowardNegative(), minExp)
        }
    }

    private fun scaleToMinExp(x: Decimal, minExp: Int, precision: Int): Decimal {
        check(x.qExp >= minExp)
        if (x.qExp == minExp)
            return x
        val delta = x.qExp - minExp
        check(delta > 0)
        val headroom = precision - x.digitLen
        if (headroom == 0)
            return x
        val shiftLeft = min(headroom, delta)
        return D128Pow10.scaleCoeffUpPow10(x, shiftLeft)
    }

    private fun unscaledFiniteAddMagnitudes(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal {
        val sumBitLen = x.bitLen + y.bitLen + 1
        val sum = if (sumBitLen < decEnv.decFormat.maxBitLen) {
            val x0 = x.dw0
            val y0 = y.dw0
            val s0 = x0 + y0
            val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
            val x1 = x.dw1
            val y1 = y.dw1
            val s1 = x1 + y1 + carry0
            Decimal.from(s1, s0, x.signExp)
        } else {
            val arg1 = decEnv.decTemps.mutDecArg1.set(x)
            val arg2 = decEnv.decTemps.mutDecArg2.set(y)
            val mdecSum = decEnv.decTemps.mutDecResult.setAdd(arg1, arg2, decEnv)
            Decimal.from(mdecSum)
        }
        return sum
    }


    private fun scaledFiniteAdd(x: Decimal, ySign: Boolean, y: Decimal, decEnv: DecEnv): Decimal {
        check(max(x.qExp, y.qExp) < MIN_SPECIAL_VALUE)
        return when {
            x.isZero() or y.isZero() ->
                scaledFiniteAddZero(x, ySign, y, decEnv)
            x.sign == ySign ->
                scaledFiniteAddMagnitudes(x, y, decEnv)
            else ->
                // FIXME
                //  add fast-path for exact subtractions
                fullWidthAdd(x, ySign, y, decEnv)
        }
    }

    private fun scaledFiniteAddMagnitudes(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal {
        val flip = x.qExp > y.qExp
        val m = if (flip) x else y
        val n = if (flip) y else x
        val qDelta = m.qExp - n.qExp
        check (qDelta >= 0)
        val headroom = decEnv.precision - m.digitLen
        if (qDelta > headroom)
            return fullWidthAdd(x, y.sign, y, decEnv)
        // we can resolve in the D128 world
        val shiftLeft = min(qDelta, headroom)
        return D128Pow10.fmaCoeffPow10(m, shiftLeft, n)
    }

    private fun fullWidthAdd(x: Decimal, ySign: Boolean, y: Decimal, decEnv: DecEnv): Decimal {
        val arg1 = decEnv.decTemps.mutDecArg1.set(x)
        val arg2 = decEnv.decTemps.mutDecArg2.set(y)
        arg2.sign = ySign
        val mdecSum = decEnv.decTemps.mutDecResult.setAdd(arg1, arg2, decEnv)
        val sum = Decimal.from(mdecSum)
        return sum
    }

    private fun infiniteAddImpl(x: Decimal, ySign: Boolean, y: Decimal, decEnv: DecEnv): Decimal {
        val qX = x.qExp
        val qY = y.qExp
        check (qX == NON_FINITE_INF || qY == NON_FINITE_INF)
        return when {
            qX == qY && x.sign != ySign -> {
                if (decEnv.hasTrapHandler(INVALID_OPERATION)) {
                    decEnv.signal(
                        DecExceptionContext(
                            INVALID_OPERATION,
                            MAGNITUDE_SUBTRACTION_OF_INFINITIES, "add/sub", decEnv
                        )
                    )
                } else {
                    Decimal.NaN
                }
            }
            qX == NON_FINITE_INF -> x
            ySign -> Decimal.NEG_INFINITY
            else -> Decimal.POS_INFINITY
        }
    }
}