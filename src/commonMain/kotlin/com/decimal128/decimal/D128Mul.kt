package com.decimal128.decimal

import com.decimal128.decimal.DecException.INVALID_OPERATION
import com.decimal128.decimal.DecExceptionReason.SIGNALING_NAN_OPERAND
import com.decimal128.decimal.Decimal.Companion.NEG_INFINITY
import com.decimal128.decimal.Decimal.Companion.POS_INFINITY
import kotlin.math.max

object D128Mul {

    fun mulImpl(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal {
        val qMax = max(x.qExp, y.qExp)
        return when {
            qMax < MIN_SPECIAL_VALUE ->
                finiteMul(x, y, decEnv)
            qMax == NON_FINITE_INF ->
                infiniteMul(x, y)
            qMax == NON_FINITE_SNAN ->
                decEnv.signal(
                    INVALID_OPERATION,
                    SIGNALING_NAN_OPERAND,
                    "mul",
                    Decimal.NaN)
            x.qExp == NON_FINITE_QNAN -> x
            y.qExp == NON_FINITE_QNAN -> y
            else -> throw IllegalStateException()
        }

    }

    private fun finiteMul(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal {
        val prodBitLen = x.bitLen + y.bitLen
        if (prodBitLen < decEnv.decFormat.maxBitLen) {
            val maxOperandBitLen = max(x.bitLen, y.bitLen)
            val (p1, p0) = when {
                maxOperandBitLen <= 64 ->
                    umul64x64to128(x.dw0, y.dw0)
                (maxOperandBitLen <= 128) and (x.bitLen <= 64) ->
                    umul128x64to128(y.dw1, y.dw0, x.dw0)
                (maxOperandBitLen <= 128) ->
                    umul128x64to128(x.dw1, x.dw0, y.dw0)
                else -> throw IllegalStateException()
            }
            val prodSign = x.sign xor y.sign
            val prodExp = x.qExp + y.qExp
            val d = Decimal(prodSign, p1, p0, prodExp)
            return d
        }
        return finiteMul256Impl(x, y, decEnv)
    }

    private fun finiteMul256Impl(x: Decimal, y: Decimal, decEnv: DecEnv): Decimal {
        val p = decEnv.decTemps.mutDecArg1.set(x)
        val n = decEnv.decTemps.mutDecArg2.set(y)
        p.setMul(p, n, decEnv)
        val d = Decimal.from(p)
        return d
    }

    private fun infiniteMul(x: Decimal, y: Decimal) =
        if (x.sign xor y.sign) NEG_INFINITY else POS_INFINITY

}