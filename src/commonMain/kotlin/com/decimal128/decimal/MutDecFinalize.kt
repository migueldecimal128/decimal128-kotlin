// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

internal fun MutDec.finalizeFnz(sign: Boolean, inboundQExp: Int, ctx: DecContext): MutDec {
    val steal = steal
    val digitLen = stealDigitLen(steal)
    verify { digitLen != 0 }
    val precision = ctx.precision
    // Step 1: Fast path: already in valid decimal128 range
    if (digitLen <= precision &&
        inboundQExp >= Q_TINY && inboundQExp <= Q_MAX) {
        this.steal = stealEncodeFNZ(sign, inboundQExp, stealPackedLengths(steal))
        return this
    }
    return roundAndFinalizeFnz(sign, inboundQExp, EXACT, ctx.decRounding, ctx)
}


internal inline fun MutDec.roundAndFinalizeFnz(sign: Boolean, inboundQExp: Int,
                                               inboundResidue: Residue, ctx: DecContext): MutDec =
    roundAndFinalizeFnz(sign, inboundQExp, inboundResidue, ctx.decRounding, ctx)


internal fun MutDec.roundAndFinalizeFnz(inboundResidue: Residue, ctx: DecContext): MutDec{
    val steal = steal
    val sign = stealSignFlag(steal)
    val qExp = stealQExp(steal)
    verify { stealBitLen(steal) != 0 }
    verify { qExp >= -8000 && qExp <= 8000 }
    return roundAndFinalizeFnz(sign, qExp, inboundResidue, ctx.decRounding, ctx)
}

internal fun MutDec.roundAndFinalizeFinite(sign: Boolean, inboundQExp: Int,
                                           inboundResidue: Residue, rounding: DecRounding,
                                           ctx: DecContext): MutDec {
    return if (stealBitLen(steal) != 0)
        roundAndFinalizeFnz(sign, inboundQExp, inboundResidue, rounding, ctx)
    else
        roundAndFinalizeZero(sign, inboundQExp, inboundResidue, rounding, ctx)

}
/**
 * Main entry point - implements the DAG structure:
 * 1. Normalize coefficient length (accumulate residue)
 * 2. Apply rounding (may increment coefficient)
 * 3. Handle rollover if needed
 * 4. Check final bounds
 * 5. Signal once with all flags
 */
internal fun MutDec.roundAndFinalizeFnz(sign: Boolean, inboundQExp: Int,
                                        inboundResidue: Residue, rounding: DecRounding,
                                        ctx: DecContext): MutDec {
    verify { stealPackedLengths(steal) != 0 }
    this.steal =
        stealEncodeFNZ(sign,
            clampQExponentRange(inboundQExp), stealPackedLengths(steal))
    val precision = ctx.precision
    // Step 1: Fast path: already in valid decimal128 range
    if (inboundResidue == EXACT &&
        stealDigitLen(steal) <= precision &&
        inboundQExp >= Q_TINY && inboundQExp <= Q_MAX) {
        return this
    }

    // Step 4: Handle underflow
    // Only divert if range truncation exceeds precision truncation,
    // meaning the normal path can't bring qExp into range on its own
    val rangeTruncationNeeded = Q_TINY - inboundQExp
    val precisionTruncationNeeded = max(0, stealDigitLen(this.steal) - precision)
    if (rangeTruncationNeeded > precisionTruncationNeeded) {
        return finalizeUnderflowRegion(sign, inboundQExp, inboundResidue, rounding, ctx)
    }

    // Step 5: Normalize coefficient length to <= precision, accumulating residue
    var adjustedQExp = inboundQExp
    var totalResidue = inboundResidue
    if (precisionTruncationNeeded > 0) {
        val truncationResidue =
            c256SetScaleDownPow10(this, this, precisionTruncationNeeded, ctx.tmps.pentad1)
        adjustedQExp += precisionTruncationNeeded
        totalResidue = truncationResidue.merge(inboundResidue)
        verify { stealDigitLen(this.steal) == precision }
    }

    // Step 6: Apply rounding ... might increment coefficient
    val applyRounding = totalResidue != EXACT
    if (applyRounding && totalResidue.ulpRoundUp(rounding.negate(sign), dw0)) {
        // Step 6.1: increment
        c256MutateIncrement()

        // Step 6.2: Handle rollover if increment caused overflow to new digit
        if (stealDigitLen(this.steal) > precision) {
            verify { stealDigitLen(this.steal) == precision + 1 }
            // Rolling over means the result is divisible by 10, so no residue
            val rolloverResidue = c256SetScaleDownPow10(this, this, 1, ctx.tmps.pentad1)
            verify { rolloverResidue == EXACT }
            ++adjustedQExp
            verify { stealDigitLen(this.steal) == precision }
        }
    }

    // Step 7: Check final bounds
    // qExp >= qTiny is guaranteed by steps 3/4/5
    // digitLen <= precision is guaranteed by steps 4/5
    verify { adjustedQExp >= Q_TINY }
    verify { stealDigitLen(this.steal) <= precision }
    this.qExp = adjustedQExp
    return when {
        adjustedQExp > Q_MAX -> {
            val qExcess = adjustedQExp - Q_MAX
            if (stealDigitLen(this.steal) + qExcess <= precision)
                finalizeClamping(sign, adjustedQExp, ctx)
            else
                finalizeOverflow(sign, rounding, ctx)
        }
        applyRounding -> ctx.signalInexact(this)
        else -> this
    }
}

internal fun MutDec.roundAndFinalizeZero(sign: Boolean, qExp: Int,
                                         inboundResidue: Residue, rounding: DecRounding,
                                         ctx: DecContext): MutDec {
    verify { digitLen == 0 }
    val qExpCapped = max(min(qExp, Q_MAX), Q_TINY)
    this.steal = stealEncodeZER(sign, qExpCapped)

    // If we had a non-zero residue, the result is inexact
    // This can happen in quantize operations where a non-zero value rounds to zero
    if (inboundResidue != EXACT) {
        // Check if rounding would produce a non-zero result
        val roundUp = inboundResidue.ulpRoundUp(rounding.negate(sign), 0L)
        if (roundUp) {
            c256SetOne()
            this.steal = stealEncodeFNZ(sign, qExpCapped, PACKED_LENGTHS_1_1)
            when {
                qExp > Q_MAX -> return finalizeOverflow(sign, rounding, ctx)
                qExp < Q_TINY -> return finalizeUnderflowRegion(sign, qExp, inboundResidue, rounding, ctx)
            }
        }
        // Rounding confirms zero, but it's still inexact
        return ctx.signalInexact(this)
    }

    // Exact zero
    return this
}

private fun MutDec.finalizeOverflow(sign: Boolean, rounding: DecRounding, ctx: DecContext): MutDec {
    // IEEE 754 7.4 Overflow - always inexact
    verify { digitLen <= ctx.precision }
    return ctx.signalInexactOverflow(
        if (rounding.overflowsToInfinity(sign)) setInfinite(sign)
        else setMaxFiniteMagnitude(sign, ctx))
}

private fun MutDec.finalizeUnderflowRegion(
    sign: Boolean,
    qExp: Int,
    residue: Residue,
    rounding: DecRounding,
    ctx: DecContext
): MutDec {
    // IEEE 754 7.5 Underflow - handle subnormal region
    verify { qExp < Q_TINY }

    val truncationNeeded = Q_TINY - qExp
    val digitLen = stealDigitLen(this.steal)

    return when {
        truncationNeeded > digitLen -> {
            // Result is swamped - becomes zero or min finite
            // This is always inexact
            ctx.signalInexactUnderflow(
                if (rounding.underflowsToZero(sign)) setZeroWithQTiny(sign)
                else setMinFiniteMagnitude(sign, ctx))
        }
        truncationNeeded == digitLen -> {
            // Exactly on the underflow boundary - round to decide
            finalizeUnderflowBoundary(sign, residue, rounding, ctx)
        }
        else -> {
            // truncationNeeded < digitLen (and > 0)
            // Truncate to subnormal and round
            finalizeSubnormal(sign, residue, rounding, ctx, truncationNeeded)
        }
    }
}

private fun MutDec.finalizeUnderflowBoundary(
    sign: Boolean,
    inboundResidue: Residue,
    rounding: DecRounding,
    ctx: DecContext
): MutDec {
    // no value params ... nothing to verify
    val scaleResidue = Residue.fromValueDecade(this)
    val totalResidue = scaleResidue.merge(inboundResidue)
    val roundUp = totalResidue.ulpRoundUp(rounding.negate(sign), 0L)

    return ctx.signalInexactUnderflow(
        if (roundUp) setMinFiniteMagnitude(sign, ctx)
        else setZeroWithQTiny(sign))
}

private fun MutDec.finalizeSubnormal(
    sign: Boolean,
    inboundResidue: Residue,
    rounding: DecRounding,
    ctx: DecContext,
    truncationNeeded: Int
): MutDec {
    verify { truncationNeeded > 0 && truncationNeeded < digitLen }

    // Scale down to fit in subnormal range
    val scaleResidue = c256SetScaleDownPow10(this, this, truncationNeeded, ctx.tmps.pentad1)
    this.steal = stealEncodeFNZ(sign, Q_TINY, stealPackedLengths(this.steal))
    val precision = ctx.precision
    verify { digitLen > 0 && digitLen < precision }

    val totalResidue = scaleResidue.merge(inboundResidue)

    // IEEE 754 7.5: If the rounded result is exact, no underflow flag

    if (totalResidue == EXACT)
        return this

    // Apply rounding
    val roundUp = totalResidue.ulpRoundUp(rounding.negate(sign), dw0)
    if (roundUp) {
        c256MutateIncrement()

        // Check if rounding caused rollover into precision range
        if (digitLen > precision) {
            verify { digitLen == precision + 1 }
            val rolloverResidue = c256SetScaleDownPow10(this, this, 1, ctx.tmps.pentad1)
            verify { rolloverResidue == EXACT }
            this.qExp = Q_TINY + 1
        }
    }

    return ctx.signalInexactUnderflow(this)
}

private fun MutDec.finalizeClamping(sign: Boolean, qExp: Int, ctx: DecContext): MutDec {
    val qExcess = qExp - Q_MAX
    verify { qExcess > 0 && qExcess <= ctx.precision - digitLen }
    c256SetScaleUpPow10(this, this, qExcess, ctx.tmps.pentad1)
    this.steal = stealEncodeFNZ(sign, Q_MAX, stealPackedLengths(this.steal))
    return this
}
