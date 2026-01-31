package com.decimal128.decimal

import com.decimal128.decimal.C128Compare.c128UnscaledCompare
import com.decimal128.decimal.DecException.INVALID_OPERATION
import com.decimal128.decimal.DecExceptionReason.*
import kotlin.math.max
import kotlin.math.min

object D128AddSub {

    fun addImpl(x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal {
        val qMax = max(x.qExp, y.qExp)
        return when {
            (qMax < MIN_SPECIAL_VALUE) and (x.qExp == y.qExp) ->
                unscaledFiniteAddImpl(x, ySign, y, env)
            qMax < MIN_SPECIAL_VALUE ->
                scaledFiniteAdd(x, ySign, y, env)
            qMax == NON_FINITE_INF ->
                infiniteAddImpl(x, ySign, y, env)
            qMax == NON_FINITE_SNAN ->
                env.signal(
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

    private fun unscaledFiniteAddImpl(x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal {
        val xIsZero = x.isZero()
        val yIsZero = y.isZero()

        if (xIsZero or yIsZero)
            return unscaledFiniteAddZero(x, ySign, y, env)

        if (x.sign == ySign)
            return unscaledFiniteAddMagnitudes(x, y, env)

        val cmp = c128UnscaledCompare(x, y)
        return when {
            (cmp > 0) -> C128AddSub.c128UnscaledSub(x.sign, x, y)
            (cmp < 0) -> C128AddSub.c128UnscaledSub(ySign, y, x)
            else -> Decimal.newZero(env.isRoundTowardNegative(), x.qExp, env)
        }
    }

    private fun unscaledFiniteAddZero(x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal {
        verify { x.isZero() || y.isZero() }
        return when {
            // Case 1: Only one operand is zero.
            !x.isZero() -> x
            !y.isZero() -> y

            // Case 2: Both operands are zero. This is where the special rules apply.
            // Rule: x + x = x. Preserves the sign of zero. (-0) + (-0) = -0.
            x.sign == ySign -> x

            // Rule: (+0) + (-0). The signs are different.
            // Result is +0 unless rounding is roundTowardNegative.
            else -> Decimal.newZero(env.isRoundTowardNegative(), x.qExp, env)
        }
    }

    private fun scaledFiniteAddZero(x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal {
        verify { x.isZero() || y.isZero() }
        val minExp = min(x.qExp, y.qExp)
        return when {
            !x.isZero() -> scaleToMinExp(x, minExp, env.decFormat.precision)
            !y.isZero() -> scaleToMinExp(y, minExp, env.decFormat.precision)
            x.sign == ySign -> if (x.qExp == minExp) x else y
            else -> Decimal.newZero(env.isRoundTowardNegative(), minExp, env)
        }
    }

    private fun scaleToMinExp(x: Decimal, minExp: Int, precision: Int): Decimal {
        verify { x.qExp >= minExp }
        if (x.qExp == minExp)
            return x
        val delta = x.qExp - minExp
        verify { delta > 0 }
        val headroom = precision - x.digitLen
        if (headroom == 0)
            return x
        val shiftLeft = min(headroom, delta)
        return D128Pow10.scaleCoeffUpPow10(x, shiftLeft)
    }

    private fun unscaledFiniteAddMagnitudes(x: Decimal, y: Decimal, env: DecContext): Decimal {
        val sumBitLen = x.bitLen + y.bitLen + 1
        val sum = if (sumBitLen < env.decFormat.maxBitLen) {
            val x0 = x.dw0
            val y0 = y.dw0
            val s0 = x0 + y0
            val carry0 = if (unsignedLT(s0, x0)) 1L else 0L
            val x1 = x.dw1
            val y1 = y.dw1
            val s1 = x1 + y1 + carry0
            Decimal.from(s1, s0, x.signExp)
        } else {
            val arg1 = env.decTemps.mdecArg1.set(x)
            val arg2 = env.decTemps.mdecArg2.set(y)
            val mdecSum = env.decTemps.mutDecResult.setAdd(arg1, arg2, env)
            Decimal.from(mdecSum)
        }
        return sum
    }


    private fun scaledFiniteAdd(x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal {
        verify { max(x.qExp, y.qExp) < MIN_SPECIAL_VALUE }
        when {
            x.isZero() or y.isZero() ->
                return scaledFiniteAddZero(x, ySign, y, env)

            x.sign == ySign ->
                return scaledFiniteAddMagnitudes(x, y, env)
        }
        val cmpMag = x.magnitudeCompareTo(y)
        return when {
            cmpMag > 0 -> scaledFiniteSubMagnitudes(x.sign, x, y, env)
            cmpMag < 0 -> scaledFiniteSubMagnitudes(ySign, y, x, env)
            else -> Decimal.newZero(x.sign && ySign, min(x.qExp, y.qExp), env)
        }
    }

    private fun scaledFiniteSubMagnitudes(resultSign: Boolean, m: Decimal, n: Decimal, env: DecContext): Decimal {
        // non-zero with different signs ... subtract magnitudes
        verify { m.magnitudeCompareTo(n) > 0 }
        verify { n.isNotZero() }
        verify { m.qExp != n.qExp }
        if (m.qExp < n.qExp) {
            // TC("22E1", "-2E2"),
            // signs opposite, |m| > |n|, but m.qExp < n.qExp
            // scale n before subtraction
            val qDelta = n.qExp - m.qExp
            verify { qDelta < PRECISION_34 }
            return D128Pow10.fusedSubtractMulPow10(resultSign, m, n, qDelta)
        } else {
            // |m| > |n| && m.qExp > n.qExp
            val headroom = env.precision - m.digitLen
            val qDelta = m.qExp - n.qExp
            if (headroom >= qDelta) {
                // 12E3, -4
                // m has enough headroom to scale and align with n.qExp
                return D128Pow10.fusedMulPow10Subtract(resultSign, m, qDelta, n)
            }
            val qAlign = m.qExp - headroom
            val shiftRight = qAlign - n.qExp
            if (shiftRight >= n.digitLen) {
                // n is fully swamped
                // this becomes a rounding/residue problem
                // FIXME
                //  This requires residue and rounding in the D128 world
                //  I'm not ready to tackle that yet
            }

        }
        return fullWidthAdd(resultSign, m, !resultSign, n, env)
    }

    private fun scaledFiniteAddMagnitudes(x: Decimal, y: Decimal, env: DecContext): Decimal {
        val flip = x.qExp > y.qExp
        val m = if (flip) x else y
        val n = if (flip) y else x
        val qDelta = m.qExp - n.qExp
        verify { qDelta >= 0 }
        val headroom = env.precision - m.digitLen
        return if (qDelta <= headroom) {
            // we can resolve in our D128 world
            val shiftLeft = min(qDelta, headroom)
            D128Pow10.fmaCoeffPow10(m, shiftLeft, n)
        } else {
            fullWidthAdd(x.sign, x, y.sign, y, env)
        }
    }

    private fun fullWidthAdd(xSign: Boolean, x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal {
        val arg1 = env.decTemps.mdecArg1.set(x)
        arg1.sign = xSign
        val arg2 = env.decTemps.mdecArg2.set(y)
        arg2.sign = ySign
        val mdecSum = env.decTemps.mutDecResult.setAdd(arg1, arg2, env)
        val sum = Decimal.from(mdecSum)
        return sum
    }

    private fun infiniteAddImpl(x: Decimal, ySign: Boolean, y: Decimal, env: DecContext): Decimal {
        val qX = x.qExp
        val qY = y.qExp
        verify { qX == NON_FINITE_INF || qY == NON_FINITE_INF }
        return when {
            qX == qY && x.sign != ySign -> {
                if (env.hasTrapHandler(INVALID_OPERATION)) {
                    env.signal(
                        DecExceptionContext(
                            INVALID_OPERATION,
                            MAGNITUDE_SUBTRACTION_OF_INFINITIES, "add/sub", env
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