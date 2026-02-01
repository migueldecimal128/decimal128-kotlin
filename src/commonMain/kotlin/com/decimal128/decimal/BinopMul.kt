package com.decimal128.decimal

import com.decimal128.decimal.BinopSignature.*
import com.decimal128.decimal.Decimal.Companion.bothFnz
import kotlin.math.min

internal fun mulImpl(x: Decimal, y: Decimal): Decimal =
    mulImpl(x, y, DecContext.current())

internal fun mulImpl(x: Decimal, y: Decimal, env: DecContext): Decimal {
    return if (bothFnz(x, y)) {
        mulFnzFnz(x, y, env)
    } else when (BinopSignature.of(x, y)) {
        ZER_ZER -> mulZero(x, y, env)
        ZER_FNZ -> mulZero(x, y, env)
        ZER_INF -> mulInfZero(x, y, env)

        FNZ_ZER -> mulZero(x, y, env)
        FNZ_FNZ -> mulFnzFnz(x, y, env)
        FNZ_INF -> mulInfNonzero(x, y, env)

        INF_ZER -> mulInfZero(x, y, env)
        INF_FNZ -> mulInfNonzero(x, y, env)
        INF_INF -> mulInfNonzero(x, y, env)

        NAN_FOUND -> nanFound(x, y, env)
    }
}

private fun mulZero(x: Decimal, y: Decimal, env: DecContext): Decimal =
    Decimal.newZero(false, min(x.qExp, y.qExp), env)

private fun mulInfZero(x: Decimal, y: Decimal, env: DecContext): Decimal =
    env.signal(DecExceptionReason.MULTIPLICATION_OF_ZERO_BY_INFINITY)

private fun mulInfNonzero(x: Decimal, y: Decimal, env: DecContext): Decimal =
    if (x.sign xor y.sign) Decimal.POS_INFINITY else Decimal.NEG_INFINITY

// fast-path iff ...
//  product bitLen strictly less than decFormat.maxBitLen
//  (equal bitLen could overflow coefficient decimal limit)
//
//  exponent on the upper end is easy, must be < qMax
//  exponent on the low end must be >= eMin, not qTiny
//  anything in the range [qTiny, eMin) is subnormal
//  and must be scaled, so not on the fast-path
private fun mulFnzFnz(x: Decimal, y: Decimal, env: DecContext): Decimal {
    val prodBitLen = x.bitLen + y.bitLen
    val prodExp = x.qExp + y.qExp
    if (prodBitLen < env.maxBitLen && prodExp >= env.eMin && prodExp <= env.qMax) {
        val p0 = x.dw0 * y.dw0
        val p1 = unsignedMulHi(x.dw0, y.dw0) + (x.dw1 * y.dw0) + (y.dw1 * x.dw0)
        val prodSign = x.sign xor y.sign
        val d = Decimal(prodSign, p1, p0, prodExp)
        return d
    }
    return mulFnzFnz256(x, y, env)
}

private fun mulFnzFnz256(x: Decimal, y: Decimal, env: DecContext): Decimal {
    val m = env.decTemps.mdecArg1.set(x)
    val n = env.decTemps.mdecArg2.set(y)
    val p = env.decTemps.mdecResult.setMul(m, n, env)
    val d = Decimal.from(p)
    return d
}

