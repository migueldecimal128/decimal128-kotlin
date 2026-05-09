// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.decimalFNZ
import com.decimal128.decimal.Decimal.Companion.decimalFinite
import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max

internal inline fun decFinalizeFinite(
    sign: Boolean,
    qExp: Int,
    dw1: Long, dw0: Long,
    ctx: DecContext = DecContext.current(),
    beQuiet: Boolean = false
): Decimal =
    decRoundAndFinalizeFinite(sign, qExp, dw1, dw0, EXACT, ctx.roundingDirection, ctx, beQuiet)

internal fun decRoundAndFinalizeFinite(
    sign: Boolean,
    qExp: Int,
    dw1: Long, dw0: Long,
    residue: Residue,
    ctx: DecContext, beQuiet: Boolean = false
): Decimal =
    decRoundAndFinalizeFinite(sign, qExp, dw1, dw0, residue, ctx.roundingDirection, ctx, beQuiet)

internal fun decRoundAndFinalizeFinite(
    sign: Boolean,
    qExpIn: Int,
    dw1In: Long, dw0In: Long,
    inboundResidue: Residue,
    rounding: RoundingDirection, ctx: DecContext,
    beQuiet: Boolean = false
): Decimal {
    // Step 1: Fast path: already in valid decimal128 range
    if (inboundResidue == EXACT && ctx.isCanonical(qExpIn, dw1In, dw0In)) {
        // allocate zero thru this path to reuse cached zeros
        if ((dw1In or dw0In) == 0L)
            return Decimal.zero(sign, qExpIn)
        // pull small positive integers from the cache
        if ((dw1In == 0L) and (qExpIn == 0) && !sign)
            return Decimal.fromUnsigned(dw0In)
        return decimalFinite(sign, qExpIn, dw1In, dw0In)
    }

    // Step 2: special values ... not applicable here ... only Finite

    // Step 3: zero coefficient
    if ((dw1In or dw0In) == 0L)
        return decFinalizeZero(sign, qExpIn, inboundResidue, rounding, ctx, beQuiet)

    val precision = ctx.precision

    // Step 4: underflow
    // divert iff range truncation exceeds precision truncation
    val rangeTruncationNeeded = -6176 - qExpIn
    val precisionTruncationNeeded = max(calcDigitLen128(dw1In, dw0In) - precision, 0)
    if (rangeTruncationNeeded > precisionTruncationNeeded)
        return decFinalizeUnderflowRegion(sign, qExpIn, dw1In, dw0In, inboundResidue, rounding, ctx, beQuiet)

    // Step 5: normalize to <= precision, accumulating residue
    var totalResidue = inboundResidue
    var dw1 = dw1In
    var dw0 = dw0In
    var qExp = qExpIn
    if (precisionTruncationNeeded > 0) {
        val tmpPair = ctx.tmps.pentad
        val truncationResidue =
            c128ScaleDownPow10(tmpPair, dw1, dw0, precisionTruncationNeeded)
        dw1 = tmpPair.dw1
        dw0 = tmpPair.dw0
        totalResidue = truncationResidue.merge(totalResidue)
        qExp += precisionTruncationNeeded
        verify { calcDigitLen128(dw1, dw0) == precision }
    }

    // step 6: rounding
    val applyRounding = totalResidue != EXACT
    if (applyRounding && totalResidue.ulpRoundUp(rounding.forMagnitude(sign), dw0)) {
        // step 6.1: increment
        ++dw0
        dw1 += if (dw0 == 0L) 1L else 0L

        // step 6.2: rollover
        if (ctx.coeffIsMaxx(dw1, dw0)) {
            dw1 = ctx.dw1MinFullPrecisionCoeff
            dw0 = ctx.dw0MinFullPrecisionCoeff
            ++qExp
        }
    }

    // step 7: check final bounds
    verify { qExp >= -6176 }
    verify {calcDigitLen128(dw1, dw0) <= precision }
    if (qExp > 6111) {
        val qExcess = qExp - 6111
        if (calcDigitLen128(dw1, dw0) + qExcess <= precision) {
            return decFinalizeClamping(sign, dw1, dw0, qExp, ctx)
        } else {
            return decFinalizeOverflow(sign, rounding, ctx, beQuiet)
        }
    }
    val ret = decimalFNZ(sign, qExp, dw1, dw0)
    return if (!applyRounding || beQuiet) ret else ctx.signalInexact(ret)
}

private fun decFinalizeZero(
    sign: Boolean,
    qExp: Int, residue: Residue,
    rounding: RoundingDirection, ctx: DecContext,
    beQuiet: Boolean
): Decimal {
    val z: Decimal
    if (residue != EXACT && residue.ulpRoundUp(rounding.forMagnitude(sign), isOdd = 0L)) {
        when {
            qExp > 6111 -> return decFinalizeOverflow(sign, rounding, ctx, beQuiet)
            qExp < -6176 ->
                return decFinalizeUnderflowRegion(
                    sign, qExp, dw1 = 0L,
                    dw0 = 1L, residue, rounding, ctx, beQuiet
                )
            else -> z = decimalFNZ(sign, qExp, dw1 = 0L, dw0 = 1L)
        }
    } else {
        z = Decimal.zero(sign, qExp)
    }
    return if (residue == EXACT || beQuiet) z else ctx.signalInexact(z)
}

private fun decFinalizeUnderflowRegion(
    sign: Boolean,
    qExp: Int, dw1: Long, dw0: Long,
    residue: Residue, rounding: RoundingDirection,
    ctx: DecContext, beQuiet: Boolean
): Decimal {
    // IEEE 754 7.5 Underflow - handle subnormal region
    verify { qExp < -6176 }
    val truncationNeeded = -6176 - qExp
    val digitLen = calcDigitLen128(dw1, dw0)
    when {
        truncationNeeded > digitLen -> {
            // Result is swamped - becomes zero or min finite
            // This is always inexact
            val z =
                if (rounding.underflowsToZero(sign)) Decimal.zero(sign, -6176)
                else minFiniteMagnitude(sign, ctx)
            return if (beQuiet) z else ctx.signalInexactUnderflow(z)
        }
        truncationNeeded == digitLen ->
            return decFinalizeUnderflowBoundary(sign, dw1, dw0, residue, rounding, ctx, beQuiet)
        else ->
            return decFinalizeSubnormal(sign, dw1, dw0, residue, qExp, rounding, ctx, beQuiet)
    }
}

/**
 * Called when the entire coefficient is shifted exactly away
 * completely so that the most significant coefficient digit
 * becomes the rounding digit.
 *
 * We compare our coefficient against (10**digitLen)/2
 *
 */
private fun decFinalizeUnderflowBoundary(sign: Boolean,
                                         dw1: Long, dw0: Long, residue: Residue,
                                         rounding: RoundingDirection, ctx: DecContext,
                                         beQuiet: Boolean): Decimal {
    // no value params ... nothing to verify
    val digitLen = calcDigitLen128(dw1, dw0)
    val scaleResidue = Residue.fromValuePow10(dw1, dw0, digitLen)
    val totalResidue = scaleResidue.merge(residue)
    val z =
        if (totalResidue.ulpRoundUp(rounding.forMagnitude(sign), 0L)) minFiniteMagnitude(sign, ctx)
        else Decimal.zero(sign, -6176)
    return if (beQuiet) z else ctx.signalInexactUnderflow(z)
}

private fun decFinalizeSubnormal(sign: Boolean,
                                 dw1: Long, dw0: Long, residue: Residue,
                                 qExp: Int,
                                 rounding: RoundingDirection, ctx: DecContext,
                                 beQuiet: Boolean): Decimal {
    val truncationNeeded = -6176 - qExp
    verify { truncationNeeded > 0 && truncationNeeded < calcDigitLen128(dw1, dw0) }

    val tmpPair = ctx.tmps.pentad
    val scaleResidue = c128ScaleDownPow10(tmpPair, dw1, dw0, truncationNeeded)
    val totalResidue = scaleResidue.merge(residue)
    var dw1T = tmpPair.dw1
    var dw0T = tmpPair.dw0
    var qExpT = -6176

    if (totalResidue != EXACT && totalResidue.ulpRoundUp(rounding.forMagnitude(sign), dw0T)) {
        // apply rounding
        ++dw0T
        dw1T += if (dw0T == 0L) 1L else 0L
        // the inbound coefficient may have had > 34 digits
        // even though we just scaled down, the roundUp might overflow
        if (!ctx.isCanonical(dw1T, dw0T)) {
            dw1T = ctx.dw1MinFullPrecisionCoeff
            dw0T = ctx.dw0MinFullPrecisionCoeff
            ++qExpT
        }
    }
    val z = decimalFNZ(sign, qExpT, dw1T, dw0T)
    // IEEE 754 7.5: If the rounded result is exact, no underflow flag
    return if (totalResidue == EXACT || beQuiet) z else ctx.signalInexactUnderflow(z)
}

private fun decFinalizeClamping(sign: Boolean,
                                dw1: Long, dw0: Long, qExp: Int, ctx: DecContext): Decimal {
    val qExcess = qExp - 6111
    verify { qExcess > 0 && qExcess <= ctx.precision - calcDigitLen128(dw1, dw0) }
    val pentad = ctx.tmps.pentad
    umul128xPow10to128(pentad, dw1, dw0, qExcess)
    val dw1S = pentad.dw1
    val dw0S = pentad.dw0
    verify { ctx.isCanonical(dw1S, dw0S) }
    // successful clamping does not signal because
    // the returned value is numerically equal
    return decimalFNZ(sign, 6111, dw1S, dw0S)
}

private fun decFinalizeOverflow(sign: Boolean, rounding: RoundingDirection,
                                ctx: DecContext, beQuiet: Boolean): Decimal {
    // IEEE 754 7.4 Overflow - always inexact
    val z =
        if (rounding.overflowsToInfinity(sign)) Decimal.infinity(sign)
        else maxFiniteMagnitude(sign, ctx)
    return if (beQuiet) z else ctx.signalInexactOverflow(z)
}
