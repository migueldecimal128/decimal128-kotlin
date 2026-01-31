package com.decimal128.decimal

import com.decimal128.decimal.DecException.INVALID_OPERATION
import com.decimal128.decimal.DecExceptionReason.*
import com.decimal128.decimal.Decimal.Companion.NEG_INFINITY
import com.decimal128.decimal.Decimal.Companion.NEG_ZEROe0
import com.decimal128.decimal.Decimal.Companion.POS_INFINITY
import com.decimal128.decimal.Decimal.Companion.ZERO
import kotlin.math.max
import kotlin.math.min

object D128Div {

    fun divImpl(x: Decimal, y: Decimal, env: DecContext): Decimal {
        val qMax = max(x.qExp, y.qExp)
        return when {
            qMax < MIN_SPECIAL_VALUE ->
                finiteDivImpl(x, y, env)
            qMax == NON_FINITE_INF ->
                infiniteDivImpl(x, y, env)
            qMax == NON_FINITE_SNAN ->
                env.signal(
                    INVALID_OPERATION,
                    SIGNALING_NAN_OPERAND,
                    "div",
                    Decimal.NaN)
            x.qExp == NON_FINITE_QNAN -> x
            y.qExp == NON_FINITE_QNAN -> y
            else -> throw IllegalStateException()
        }

    }

    private fun finiteDivImpl(x: Decimal, y: Decimal, env: DecContext): Decimal {
        verify { x.isFinite() && y.isFinite() }
        return when {
            y.packedLengths > 0 -> {
                if (x.packedLengths.toInt() == 0)
                    Decimal.newZero(false, x.qExp - y.qExp, env)
                else
                    finiteDivNonZero(x, y, env)
            }

            x.packedLengths > 0 -> {
                // finite division by zero
                val sign = x.sign xor y.sign
                env.signalDivByZero(sign)
            }

            else -> {
                // zero divided by zero
                val inf = if (x.sign xor y.sign) NEG_INFINITY else POS_INFINITY
                env.signal(INVALID_OPERATION, DIVISION_OF_ZERO_BY_ZERO, "div", inf)
            }
        }
    }

    private fun finiteDivNonZero(x: Decimal, y: Decimal, env: DecContext): Decimal {
        val dividend = env.decTemps.mdecArg1.set(x)
        val divisor = env.decTemps.mdecArg2.set(y)
        val quotient = env.decTemps.mutDecResult.setDiv(dividend, divisor, env)
        return Decimal.from(quotient)
    }

    private fun infiniteDivImpl(x: Decimal, y: Decimal, env: DecContext): Decimal {
        val minExp = min(x.qExp, y.qExp)
        val quotSign = x.sign xor y.sign
        return when {
            minExp == NON_FINITE_INF ->
                env.signal(INVALID_OPERATION, DIVISION_OF_INFINITY_BY_INFINITY, "x", Decimal.NaN)
            x.qExp == NON_FINITE_INF ->
                if (quotSign) NEG_INFINITY else POS_INFINITY
            y.qExp == NON_FINITE_INF ->
                if (quotSign) NEG_ZEROe0 else ZERO
            else -> throw IllegalArgumentException()
        }
    }

}