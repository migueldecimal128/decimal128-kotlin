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
                scaledFiniteAddImpl(x, ySign, y, decEnv)
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
        val shiftLeft = min(headroom, delta)
        val qAlign = x.qExp - shiftLeft
        throw RuntimeException("not impl")
    }

    private fun unscaledFiniteAddMagnitudes(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal {
        if (decEnv.decFormat.isC128AddSafe(x.bitLen, y.bitLen))
            return C128AddSub.c128UnscaledAdd(x, y)
        else {
            val arg1 = decEnv.decTemps.mutDecArg1.set(x)
            val arg2 = decEnv.decTemps.mutDecArg2.set(y)
            throw RuntimeException("not impl")
            //val sum = decEnv.decTemps.mutDecResult.setAdd(arg1, arg2, decEnv)
            //val d = Decimal.from(sum)
            //return d
        }
    }


    private fun scaledFiniteAddImpl(x: Decimal, ySign: Boolean, y: Decimal, decEnv: DecEnv): Decimal {
        val qMax = max(x.qExp, y.qExp)
        check(qMax < MIN_SPECIAL_VALUE)
        if (x.isZero() or y.isZero())
            return scaledFiniteAddZero(x, ySign, y, decEnv)
        throw RuntimeException("not impl")
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