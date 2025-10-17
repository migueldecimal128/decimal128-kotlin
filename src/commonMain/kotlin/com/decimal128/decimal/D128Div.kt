package com.decimal128.decimal

import com.decimal128.decimal.DecException.INVALID_OPERATION
import com.decimal128.decimal.DecExceptionReason.*
import com.decimal128.decimal.Decimal.Companion.NEG_INFINITY
import com.decimal128.decimal.Decimal.Companion.NEG_ZERO
import com.decimal128.decimal.Decimal.Companion.POS_INFINITY
import com.decimal128.decimal.Decimal.Companion.ZERO
import kotlin.math.max
import kotlin.math.min

object D128Div {

    fun divImpl(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal {
        val qMax = max(x.qExp, y.qExp)
        return when {
            qMax < MIN_SPECIAL_VALUE ->
                finiteDivImpl(x, y, decEnv)
            qMax == NON_FINITE_INF ->
                infiniteDivImpl(x, y, decEnv)
            qMax == NON_FINITE_SNAN ->
                decEnv.signal(
                    INVALID_OPERATION,
                    SIGNALING_NAN_OPERAND,
                    "div",
                    Decimal.NaN)
            x.qExp == NON_FINITE_QNAN -> x
            y.qExp == NON_FINITE_QNAN -> y
            else -> throw IllegalStateException()
        }

    }

    private fun finiteDivImpl(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal {
        check (x.isFinite() && y.isFinite())
        return when {
            y.packedLengths > 0 -> {
                if (x.packedLengths.toInt() == 0)
                    Decimal.newZero(false, x.qExp - y.qExp, decEnv)
                else
                    finiteDivNonZero(x, y, decEnv)
            }
            x.packedLengths > 0 -> {
                // finite division by zero
                val sign = x.sign xor y.sign
                decEnv.signalDivByZero(sign)
            }
            else -> {
                // zero divided by zero
                val inf = if (x.sign xor y.sign) NEG_INFINITY else POS_INFINITY
                decEnv.signal(INVALID_OPERATION, DIVISION_OF_ZERO_BY_ZERO, "div", inf)
            }
        }
    }

    private fun finiteDivNonZero(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal {
        val dividend = decEnv.decTemps.mdecArg1.set(x)
        val divisor = decEnv.decTemps.mdecArg2.set(y)
        val quotient = decEnv.decTemps.mutDecResult.setDiv(dividend, divisor, decEnv)
        return Decimal.from(quotient)
    }

    private fun infiniteDivImpl(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal {
        val minExp = min(x.qExp, y.qExp)
        val quotSign = x.sign xor y.sign
        return when {
            minExp == NON_FINITE_INF ->
                decEnv.signal(INVALID_OPERATION, DIVISION_OF_INFINITY_BY_INFINITY, "x", Decimal.NaN)
            x.qExp == NON_FINITE_INF ->
                if (quotSign) NEG_INFINITY else POS_INFINITY
            y.qExp == NON_FINITE_INF ->
                if (quotSign) NEG_ZERO else ZERO
            else -> throw IllegalArgumentException()
        }
    }

}