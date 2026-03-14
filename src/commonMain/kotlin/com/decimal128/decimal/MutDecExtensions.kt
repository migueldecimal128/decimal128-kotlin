package com.decimal128.decimal

fun MutDec.toLong(rounding: DecRounding, ctx: DecContext): Long {
    val signMask = signMask.toLong()
    val sign = sign
    val qExp = qExp
    val bitLen = bitLen
    val digitLen = digitLen
    val dw0 = dw0
    when {
        qExp == 0 -> {
            if (bitLen < 64)
                return (dw0 xor signMask) - signMask
            if (dw0 == Long.MIN_VALUE && sign)
                return Long.MIN_VALUE
            ctx.signalInvalid(this)
            return Long.MAX_VALUE - signMask
        }
        qExp >= NON_FINITE_INF -> {
            val ret =
                if (qExp == NON_FINITE_INF && !sign) Long.MAX_VALUE
                else Long.MIN_VALUE
            ctx.signalInvalid(this)
            return ret
        }
        bitLen == 0 -> return 0L
        qExp < 0 -> {
            val fracDigitLen = -qExp
            if (fracDigitLen >= digitLen) {
                // all fractional digits
                val residue: Residue
                if (fracDigitLen > digitLen)
                    residue = Residue.LT_HALF
                else {
                    residue = Residue.fromValueDecade(this)
                    verify { residue != Residue.EXACT }
                }
                val roundUp = residue.ulpRoundUp(rounding.negate(sign), 0L)
                return ctx.signalInexact(
                    if (!roundUp)
                        0L
                    else
                        -signMask
                )
            }
            // both integral and fractional digits
            val tmps = ctx.tmps
            val t = tmps.mdecArg1
            val residue = c256SetScaleDownPow10(t, this, fracDigitLen, tmps.pentad1)
            val roundUp = residue.ulpRoundUp(rounding.negate(sign), 0L)
            if (roundUp)
                c256MutateIncrement(t)
            t.qExp = 0
            t.sign = sign
            // recursive call now that t.qExp == 0
            // where tail recursion when I need it?
            // even better ... GOTO
            return t.toLong(rounding, ctx)
        }
        else -> {
            if (digitLen + qExp <= 19) {
                val result = dw0 * pow10_64(qExp)
                if (result > 0)
                    return (result xor signMask) - signMask
                // Long.MIN_VALUE && sign is not possible ...
                // ... because we just multiplied by 10**qExp
                // ... so the value ends in 0
                // ... but Long.MIN_VALUE ends in 8
            }
            return ctx.signalInvalid(Long.MAX_VALUE - signMask)
        }
    }
}

