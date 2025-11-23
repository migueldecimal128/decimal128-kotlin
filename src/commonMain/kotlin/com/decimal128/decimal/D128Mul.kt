package com.decimal128.decimal

import com.decimal128.decimal.DecException.INVALID_OPERATION
import com.decimal128.decimal.DecExceptionReason.SIGNALING_NAN_OPERAND
import com.decimal128.decimal.DecOld.Companion.NEG_INFINITY
import com.decimal128.decimal.DecOld.Companion.POS_INFINITY
import kotlin.math.max

object D128Mul {

    fun mulImpl(x: DecOld, y: DecOld, env: DecEnv): DecOld {
        val qMax = max(x.qExp, y.qExp)
        return when {
            qMax < MIN_SPECIAL_VALUE ->
                finiteMul(x, y, env)
            qMax == NON_FINITE_INF ->
                infiniteMul(x, y)
            qMax == NON_FINITE_SNAN ->
                env.signal(
                    INVALID_OPERATION,
                    SIGNALING_NAN_OPERAND,
                    "mul",
                    DecOld.NaN)
            x.qExp == NON_FINITE_QNAN -> x
            y.qExp == NON_FINITE_QNAN -> y
            else -> throw IllegalStateException()
        }

    }

    // fast-path iff ...
    //  product bitLen strictly less than decFormat.maxBitLen
    //  (equal bitLen could overflow coefficient decimal limit)
    //
    //  exponent on the upper end is easy, must be < qMax
    //  exponent on the low end must be >= eMin, not qTiny
    //  anything in the range [qTiny, eMin) is subnormal
    //  and must be scaled, so not on the fast-path
    private fun finiteMul(x: DecOld, y: DecOld, env: DecEnv): DecOld {
        val prodBitLen = x.bitLen + y.bitLen
        val prodExp = x.qExp + y.qExp
        if (prodBitLen < env.decFormat.maxBitLen &&
            prodExp <= env.qMax && prodExp >= env.eMin) {
            val p0 = x.dw0 * y.dw0
            val p1 = unsignedMulHi(x.dw0, y.dw0) + (x.dw1 * y.dw0) + (y.dw1 * x.dw0)
            val prodSign = x.sign xor y.sign
            val d = DecOld(prodSign, p1, p0, prodExp)
            return d
        }
        return finiteMul256(x, y, env)
    }

    private fun finiteMul256(x: DecOld, y: DecOld, env: DecEnv): DecOld {
        val p = env.decTemps.mdecArg1.set(x)
        val n = env.decTemps.mdecArg2.set(y)
        p.setMul(p, n, env)
        val d = DecOld.from(p)
        return d
    }

    private fun infiniteMul(x: DecOld, y: DecOld) =
        if (x.sign xor y.sign) NEG_INFINITY else POS_INFINITY

}