package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT

internal fun decFinalizeFinite(sign: Boolean,
                              dw1: Long, dw0: Long, residue: Residue,
                              qExp: Int,
                              rounding: DecRounding, ctx: DecContext): Decimal {
    if ((dw1 or dw0) == 0L)
        return decFinalizeZero(sign, residue, qExp, rounding, ctx)

    val decFormat = ctx.decFormat
    if (residue == EXACT && decFormat.coeffQexpFit(dw1, dw0, qExp))
        return Decimal.from(sign, dw1, dw0, qExp)
    var qExpT = qExp
    var dw1T = dw1
    var dw0T = dw0
    var totalResidue = EXACT
    var bitLen = calcBitLen128(dw1T, dw0T)
    var digitLen = calcDigitLen128(bitLen, dw1T, dw0T)
    if (digitLen > ctx.precision) {
        val excessDigitCount = digitLen - ctx.precision
        val (dw1Scaled, dw0Scaled, residueScaled) =
            C128ScalePow10.c128ScaleDownPow10(dw1T, dw0T, excessDigitCount)
        dw1T = dw1Scaled
        dw0T = dw0Scaled
        qExpT += excessDigitCount
        totalResidue = residueScaled.merge(totalResidue)
        bitLen = calcBitLen128(dw1T, dw0T)
        verify { calcDigitLen128(bitLen, dw1T, dw0T) == ctx.precision }
        digitLen = ctx.precision
        }
    if (digitLen == ctx.precision && qExpT < ctx.qTiny)
        return decFinalizeUnderflow(sign, totalResidue, rounding, ctx)
    val roundingNeeded = totalResidue != EXACT
    if (roundingNeeded) {
        val roundUp = totalResidue.ulpRoundUp(rounding.negate(sign), dw0T)
        if (roundUp) {
            ++dw0T
            dw1T += if (dw0T == 0L) 1L else 0L
            if (decFormat.coeffIsMaxx(dw1T, dw0T)) {
                dw1T = decFormat.dw1MinFullPrecisionCoeff
                dw0T = decFormat.dw0MinFullPrecisionCoeff
                ++qExpT
            }
            bitLen = calcBitLen128(dw1T, dw0T)
            digitLen = calcDigitLen128(bitLen, dw1T, dw0T)
        }
    }
    TODO()
}

private fun decFinalizeZero(sign: Boolean, residue: Residue, qExpIn: Int, rounding: DecRounding, ctx: DecContext): Decimal {
    if (residue == EXACT)
        return Decimal.newZero(sign, qExpIn, ctx)
    TODO()
}

private fun decFinalizeUnderflow(sign: Boolean, residue: Residue, rounding: DecRounding, ctx: DecContext): Decimal {
    TODO()
}