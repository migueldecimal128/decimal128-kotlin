package com.decimal128.decimal

internal fun d128MulImpl(x: Decimal, y: Decimal): Decimal =
    d128MulImpl(x, y, DecContext.current())

internal fun d128MulImpl(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val signature = binopSignatureOf(x.steal, y.steal)
    return if (signature == FNZ_FNZ) {
        mulFnzFnz(x, y, ctx)
    } else when (signature) {
        ZER_ZER,
        ZER_FNZ,
        FNZ_ZER -> mulZero(x, y, ctx)

        ZER_INF,
        INF_ZER -> mulInfZero(x, y, ctx)

        FNZ_INF,
        INF_FNZ,
        INF_INF -> mulInfNonzero(x, y)

        else -> nanOperandFound(x, y, ctx)
    }
}

private fun mulZero(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    Decimal.zero(x.sign xor y.sign, x.qExp + y.qExp, ctx)

private fun mulInfZero(x: Decimal, y: Decimal, ctx: DecContext): Decimal =
    ctx.signalInvalid(Decimal.NaN)

private fun mulInfNonzero(x: Decimal, y: Decimal): Decimal =
    if (x.sign xor y.sign) Decimal.NEG_INFINITY else Decimal.POS_INFINITY

// fast-path iff ...
//  product bitLen strictly less than decFormat.maxBitLen
//  (equal bitLen could overflow coefficient decimal limit)
//
//  exponent on the upper end is easy, must be < qMax
//  exponent on the low end must be >= eMin, not qTiny
//  anything in the range [qTiny, eMin) is subnormal
//  and must be scaled, so not on the fast-path
private fun mulFnzFnz(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val prodBitLen = x.bitLen + y.bitLen
    val prodExp = x.qExp + y.qExp
    if (prodBitLen <= 128) {
        val p0 = x.dw0 * y.dw0
        val p1 = unsignedMulHi(x.dw0, y.dw0) + (x.dw1 * y.dw0) + (y.dw1 * x.dw0)
        val prodSign = x.sign xor y.sign
        return decFinalizeFinite(prodSign, p1, p0, prodExp, ctx)
    }
    return mulFnzFnz256(x, y, ctx)
}

private fun mulFnzFnz256(x: Decimal, y: Decimal, ctx: DecContext): Decimal {
    val tmps = ctx.tmps
    val m = tmps.mdecBridge1.set(x)
    val n = tmps.mdecBridge2.set(y)
    val p = tmps.mdecResult
    c256SetMul(p, m, n, tmps.dwQuad1)
    p.type = STEAL_TYPE_FNZ
    p.qExp = x.qExp + y.qExp
    p.sign = x.sign xor y.sign
    p.finalize(ctx)
    val d = Decimal.from(p)
    return d
}

