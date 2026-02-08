package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT

internal fun decRoundAndFinalizeFinite(sign: Boolean,
                                       dw1: Long, dw0: Long, inboundResidue: Residue,
                                       qExp: Int,
                                       rounding: DecRounding, ctx: DecContext): Decimal {
    // Step 1: Fast path: already in valid decimal128 range
    val decFormat = ctx.decFormat
    if (inboundResidue == EXACT && decFormat.coeffQexpFit(dw1, dw0, qExp))
        return Decimal(sign, dw1, dw0, qExp)

    // Step 2: special values
    if (qExp >= MIN_SPECIAL_VALUE) {
        return if (qExp ==NON_FINITE_INF)
            Decimal.infinity(sign)
        else
            Decimal.NaN(sign, qExp == NON_FINITE_SNAN, dw1, dw0)
    }

    // Step 3: zero coefficient
    if ((dw1 or dw0) == 0L)
        return decFinalizeZero(sign, inboundResidue, qExp, rounding, ctx)

    // Step 4: underflow
    // divert iff range truncation exceeds precision truncation
    val rangeTruncationNeeded = ctx.qTiny - qExp
    val precisionTruncationNeeded =
        if (decFormat.coeffFits(dw1, dw0)) 0
        else calcDigitLen128(dw1, dw0) - ctx.precision
    if (rangeTruncationNeeded > precisionTruncationNeeded)
        return decFinalizeUnderflow(sign, dw1, dw0,inboundResidue, qExp, rounding, ctx)

    // Step 5: normalize to <= precision, accumulating residue
    var totalResidue = inboundResidue
    var dw1T = dw1
    var dw0T = dw0
    var qExpT = qExp
    if (precisionTruncationNeeded > 0) {
        val (dw1S, dw0S, truncationResidue) =
            C128ScalePow10.c128ScaleDownPow10(dw1T, dw0T, precisionTruncationNeeded)
        dw1T = dw1S
        dw0T = dw0S
        totalResidue = truncationResidue.merge(totalResidue)
        qExpT += precisionTruncationNeeded
        verify { calcDigitLen128(dw1T, dw0T) == ctx.precision }
    }

    // step 6: rounding
    val applyRounding = totalResidue != EXACT
    if (applyRounding && totalResidue.ulpRoundUp(rounding.negate(sign), dw0)) {
        // step 6.1: increment
        ++dw0T
        dw1T += if (dw0T == 0L) 1L else 0L

        // step 6.2: rollover
        if (decFormat.coeffIsMaxx(dw1T, dw1T)) {
            dw1T = decFormat.dw1MinFullPrecisionCoeff
            dw0T = decFormat.dw0MinFullPrecisionCoeff
            ++qExpT
        }
    }

    // step 7: check final bounds
    verify { qExp >= ctx.qTiny }
    verify {calcDigitLen128(dw1T, dw0T) <= ctx.precision }
    if (qExp > ctx.qMax) {
        val qExcess = qExp - ctx.qMax
        return if (calcDigitLen128(dw1T, dw0T) + qExcess <= ctx.precision)
            decFinalizeClamping(sign, dw1T, dw0T, qExpT, ctx)
        else
            decFinalizeOverflow(rounding, ctx)
    }
    val ret = Decimal(sign, dw1T, dw0T, qExpT)
    return if (applyRounding) ctx.signalInexact(ret) else ret
}

private fun decFinalizeZero(sign: Boolean, residue: Residue, qExpIn: Int, rounding: DecRounding, ctx: DecContext): Decimal {
    if (residue != EXACT && residue.ulpRoundUp(rounding.negate(sign), 0L)) {
        TODO()

    }
    val z = Decimal.newZero(sign, qExpIn, ctx)
    return if (residue == EXACT) z else ctx.signalInexact(z)
}

private fun decFinalizeUnderflow(sign: Boolean, dw1: Long, dw0: Long, residue: Residue, qExp: Int, rounding: DecRounding, ctx: DecContext): Decimal {
    TODO()
}

private fun decFinalizeClamping(sign: Boolean, dw1: Long, dw0: Long, qExpIn: Int, ctx: DecContext): Decimal {
    TODO()
}

private fun decFinalizeOverflow(rounding: DecRounding, ctx: DecContext): Decimal {
    TODO()
}