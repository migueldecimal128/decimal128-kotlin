// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT

internal inline fun decFinalizeFinite(sign: Boolean,
                                      dw1: Long, dw0: Long,
                                      qExp: Int,
                                      ctx: DecContext): Decimal =
    decRoundAndFinalizeFinite(sign, dw1, dw0, EXACT, qExp, ctx.decRounding, ctx)

internal fun decRoundAndFinalizeFinite(sign: Boolean,
                                       dw1: Long, dw0: Long, residue: Residue,
                                       qExp: Int,
                                       ctx: DecContext): Decimal =
    decRoundAndFinalizeFinite(sign, dw1, dw0, residue, qExp, ctx.decRounding, ctx)

internal fun decRoundAndFinalizeFinite(sign: Boolean,
                                       dw1In: Long, dw0In: Long, inboundResidue: Residue,
                                       qExpIn: Int,
                                       rounding: DecRounding, ctx: DecContext): Decimal {
    // Step 1: Fast path: already in valid decimal128 range
    val decFormat = ctx.decFormat
    if (inboundResidue == EXACT && decFormat.coeffQexpFit(dw1In, dw0In, qExpIn))
        return Decimal(sign, dw1In, dw0In, qExpIn)

    // Step 2: special values
    /* NOT APPLICABLE because we are only dealing with Finite
    if (qExp >= MIN_SPECIAL_VALUE) {
        return if (qExp ==NON_FINITE_INF)
            Decimal.infinity(sign)
        else
            Decimal.NaN(sign, qExp == NON_FINITE_SNAN, dw1, dw0)
    }
     */

    // Step 3: zero coefficient
    if ((dw1In or dw0In) == 0L)
        return decFinalizeZero(sign, inboundResidue, qExpIn, rounding, ctx)

    val qTiny = decFormat.qTiny
    val qMax = decFormat.qMax
    val precision = decFormat.precision

    // Step 4: underflow
    // divert iff range truncation exceeds precision truncation
    val rangeTruncationNeeded = qTiny - qExpIn
    val precisionTruncationNeeded =
        if (decFormat.coeffFits(dw1In, dw0In)) 0
        else calcDigitLen128(dw1In, dw0In) - precision
    if (rangeTruncationNeeded > precisionTruncationNeeded)
        return decFinalizeUnderflowRegion(sign, dw1In, dw0In,inboundResidue, qExpIn, rounding, ctx)

    // Step 5: normalize to <= precision, accumulating residue
    var totalResidue = inboundResidue
    var dw1 = dw1In
    var dw0 = dw0In
    var qExp = qExpIn
    if (precisionTruncationNeeded > 0) {
        val tmpPair = ctx.decTmps.dwPair1
        val truncationResidue =
            C128ScalePow10.c128ScaleDownPow10(tmpPair, dw1, dw0, precisionTruncationNeeded)
        dw1 = tmpPair.dw1
        dw0 = tmpPair.dw0
        totalResidue = truncationResidue.merge(totalResidue)
        qExp += precisionTruncationNeeded
        verify { calcDigitLen128(dw1, dw0) == precision }
    }

    // step 6: rounding
    val applyRounding = totalResidue != EXACT
    if (applyRounding && totalResidue.ulpRoundUp(rounding.negate(sign), dw0)) {
        // step 6.1: increment
        ++dw0
        dw1 += if (dw0 == 0L) 1L else 0L

        // step 6.2: rollover
        if (decFormat.coeffIsMaxx(dw1, dw0)) {
            dw1 = decFormat.dw1MinFullPrecisionCoeff
            dw0 = decFormat.dw0MinFullPrecisionCoeff
            ++qExp
        }
    }

    // step 7: check final bounds
    verify { qExpIn >= qTiny }
    verify {calcDigitLen128(dw1, dw0) <= precision }
    if (qExp > qMax) {
        val qExcess = qExp - qMax
        return if (calcDigitLen128(dw1, dw0) + qExcess <= precision)
            decFinalizeClamping(sign, dw1, dw0, qExp, ctx)
        else
            decFinalizeOverflow(sign, rounding, ctx)
    }
    val ret = Decimal(sign, dw1, dw0, qExp)
    return if (applyRounding) ctx.signalInexact(ret) else ret
}

private fun decFinalizeZero(sign: Boolean, residue: Residue, qExp: Int, rounding: DecRounding, ctx: DecContext): Decimal {
    if (residue != EXACT && residue.ulpRoundUp(rounding.negate(sign), lsdwIsOdd = 0L)) {
        return when {
            qExp > ctx.qMax -> decFinalizeOverflow(sign, rounding, ctx)
            qExp < ctx.qTiny -> decFinalizeUnderflowRegion(sign, dw1 = 0L, dw0 = 1L, residue, qExp, rounding, ctx)
            else -> ctx.signalInexact(Decimal.from(sign, dw1 = 0L, dw0 = 1L, qExp))
        }
    }
    val z = Decimal.newZero(sign, qExp, ctx)
    return if (residue == EXACT) z else ctx.signalInexact(z)
}

private fun decFinalizeUnderflowRegion(sign: Boolean,
                                       dw1: Long, dw0: Long, residue: Residue,
                                       qExp: Int, rounding: DecRounding, ctx: DecContext): Decimal {
    // IEEE 754 7.5 Underflow - handle subnormal region
    verify { qExp < ctx.qTiny }
    val truncationNeeded = ctx.qTiny - qExp
    val digitLen = calcDigitLen128(dw1, dw0)
    val z = when {
        truncationNeeded > digitLen -> {
            // Result is swamped - becomes zero or min finite
            // This is always inexact
            ctx.signalInexactUnderflow(
                if (rounding.underflowsToZero(sign)) Decimal.newZero(sign, -9999, ctx)
                else minFiniteMagnitude(sign, ctx))
        }
        truncationNeeded == digitLen -> decFinalizeUnderflowBoundary(sign, dw1, dw0, residue, qExp, rounding, ctx)
        else -> decFinalizeSubnormal(sign, dw1, dw0, residue, qExp, rounding, ctx)
    }
    return z
}

/**
 * Called when the entire coefficient is shifted exactly away
 * completely so that the most significant coefficient digit
 * becomes the rounding digit.
 *
 * Compare against (10**digitLen)/2
 *
 */
private fun decFinalizeUnderflowBoundary(sign: Boolean,
                                         dw1: Long, dw0: Long, residue: Residue,
                                         qExp: Int,
                                         rounding: DecRounding, ctx: DecContext): Decimal {
    // no value params ... nothing to verify
    val digitLen = calcDigitLen128(dw1, dw0)
    val scaleResidue = Residue.fromValuePow10(dw1, dw0, digitLen)
    val totalResidue = scaleResidue.merge(residue)
    return ctx.signalInexactUnderflow(
        if (totalResidue.ulpRoundUp(rounding.negate(sign), 0L)) minFiniteMagnitude(sign, ctx)
        else Decimal.newZero(sign, -9999, ctx)
    )
}

private fun decFinalizeSubnormal(sign: Boolean,
                                 dw1: Long, dw0: Long, residue: Residue,
                                 qExp: Int,
                                 rounding: DecRounding, ctx: DecContext): Decimal {
    val decFormat = ctx.decFormat
    val qTiny = decFormat.qTiny
    val truncationNeeded = qTiny - qExp
    verify { truncationNeeded > 0 && truncationNeeded < calcDigitLen128(dw1, dw0) }

    val tmpPair = ctx.decTmps.dwPair1
    val scaleResidue = C128ScalePow10.c128ScaleDownPow10(tmpPair, dw1, dw0, truncationNeeded)
    val totalResidue = scaleResidue.merge(residue)
    var dw1T = tmpPair.dw1
    var dw0T = tmpPair.dw0
    var qExpT = qTiny

    if (totalResidue != EXACT && totalResidue.ulpRoundUp(rounding.negate(sign), dw0T)) {
        // apply rounding
        ++dw0T
        dw1T += if (dw0T == 0L) 1L else 0L
        if (!decFormat.coeffFits(dw1T, dw0T)) {
            dw1T = decFormat.dw1MinFullPrecisionCoeff
            dw0T = decFormat.dw0MinFullPrecisionCoeff
            ++qExpT
        }
    }
    val z = Decimal.from(sign, dw1T, dw0T, qExpT)
    // IEEE 754 7.5: If the rounded result is exact, no underflow flag
    return if (totalResidue == EXACT) z else ctx.signalInexactUnderflow(z)
}

private fun decFinalizeClamping(sign: Boolean,
                                dw1: Long, dw0: Long, qExp: Int, ctx: DecContext): Decimal {
    val qMax = ctx.qMax
    val qExcess = qExp - qMax
    verify { qExcess > 0 && qExcess <= ctx.precision - calcDigitLen128(dw1, dw0) }
    val (dw1S, dw0S) = umul128xPow10to128(dw1, dw0, qExcess)
    verify { ctx.decFormat.coeffFits(dw1S, dw0S) }
    // successful clamping does not signal because
    // the returned value is numerically equal
    return Decimal.from(sign, dw1S, dw0S, qMax)
}

private fun decFinalizeOverflow(sign: Boolean, rounding: DecRounding, ctx: DecContext): Decimal {
    // IEEE 754 7.4 Overflow - always inexact
    val z =
        if (rounding.overflowsToInfinity(sign)) Decimal.infinity(sign)
        else maxFiniteMagnitude(sign, ctx)
    return ctx.signalInexactOverflow(z)
}
