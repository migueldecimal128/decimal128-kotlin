package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

internal fun MutDec.finalizeFnz(ctx: DecContext) = roundAndFinalizeFnz(Residue.EXACT, ctx.decRounding, ctx)

internal fun MutDec.roundAndFinalizeFnz(inboundResidue: Residue, ctx: DecContext) =
    roundAndFinalizeFnz(inboundResidue, ctx.decRounding, ctx)

/**
 * Main entry point - implements the DAG structure:
 * 1. Normalize coefficient length (accumulate residue)
 * 2. Apply rounding (may increment coefficient)
 * 3. Handle rollover if needed
 * 4. Check final bounds
 * 5. Signal once with all flags
 */
internal fun MutDec.roundAndFinalizeFnz(inboundResidue: Residue, rounding: DecRounding, ctx: DecContext): MutDec {
    val type = type
    verify { type == STEAL_TYP_FNZ }
    var localQExp = qExp
    val precision = ctx.precision
    // Step 1: Fast path: already in valid decimal128 range
    if (stealIsFNZ(type) && inboundResidue == EXACT &&
        digitLen <= precision &&
        localQExp >= Q_TINY && localQExp <= Q_MAX) {
        return this
    }

    // Step 4: Handle underflow
    // Only divert if range truncation exceeds precision truncation,
    // meaning the normal path can't bring qExp into range on its own
    val rangeTruncationNeeded = Q_TINY - localQExp
    val precisionTruncationNeeded = max(0, digitLen - precision)
    if (rangeTruncationNeeded > precisionTruncationNeeded) {
        return finalizeUnderflowRegion(sign, qExp, inboundResidue, rounding, ctx)
    }

    // Step 5: Normalize coefficient length to <= precision, accumulating residue
    var totalResidue = inboundResidue
    if (precisionTruncationNeeded > 0) {
        val truncationResidue =
            c256SetScaleDownPow10(this, this, precisionTruncationNeeded, ctx.tmps.pentad1)
        localQExp += precisionTruncationNeeded
        totalResidue = truncationResidue.merge(inboundResidue)
        verify { digitLen == precision }
    }

    // Step 6: Apply rounding ... might increment coefficient
    val applyRounding = totalResidue != EXACT
    if (applyRounding && totalResidue.ulpRoundUp(rounding.negate(sign), dw0)) {
        // Step 6.1: increment
        c256MutateIncrement()

        // Step 6.2: Handle rollover if increment caused overflow to new digit
        if (digitLen > precision) {
            verify { digitLen == precision + 1 }
            // Rolling over means the result is divisible by 10, so no residue
            val rolloverResidue = c256SetScaleDownPow10(this, this, 1, ctx.tmps.pentad1)
            verify { rolloverResidue == EXACT }
            ++localQExp
            verify { digitLen == precision }
        }
    }

    // Step 7: Check final bounds
    // qExp >= qTiny is guaranteed by steps 3/4/5
    // digitLen <= precision is guaranteed by steps 4/5
    verify { localQExp >= Q_TINY }
    verify { digitLen <= precision }
    this.qExp = localQExp
    return when {
        localQExp > Q_MAX -> {
            val qExcess = localQExp - Q_MAX
            if (digitLen + qExcess <= precision)
                finalizeClamping(sign, localQExp, ctx)
            else
                finalizeOverflow(sign, rounding, ctx)
        }
        applyRounding -> ctx.signalInexact(this)
        else -> this
    }
}

/**
 * Main entry point - implements the DAG structure:
 * 1. Normalize coefficient length (accumulate residue)
 * 2. Apply rounding (may increment coefficient)
 * 3. Handle rollover if needed
 * 4. Check final bounds
 * 5. Signal once with all flags
 */
internal fun MutDec.roundAndFinalizeFinite(sign: Boolean, inboundQExp: Int,
                                           inboundResidue: Residue, rounding: DecRounding,
                                           ctx: DecContext): MutDec {
    if (bitLen == 0)
        return roundAndFinalizeZero(sign, inboundQExp, inboundResidue, rounding, ctx)
    this.sign = sign
    this.type = STEAL_TYP_FNZ
    val precision = ctx.precision
    // Step 1: Fast path: already in valid decimal128 range
    if (inboundResidue == EXACT &&
        digitLen <= precision &&
        inboundQExp >= Q_TINY && inboundQExp <= Q_MAX) {
        this.qExp = inboundQExp
        return this
    }

    // Step 4: Handle underflow
    // Only divert if range truncation exceeds precision truncation,
    // meaning the normal path can't bring qExp into range on its own
    val rangeTruncationNeeded = Q_TINY - inboundQExp
    val precisionTruncationNeeded = max(0, digitLen - precision)
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
        verify { digitLen == precision }
    }

    // Step 6: Apply rounding ... might increment coefficient
    val applyRounding = totalResidue != EXACT
    if (applyRounding && totalResidue.ulpRoundUp(rounding.negate(sign), dw0)) {
        // Step 6.1: increment
        c256MutateIncrement()

        // Step 6.2: Handle rollover if increment caused overflow to new digit
        if (digitLen > precision) {
            verify { digitLen == precision + 1 }
            // Rolling over means the result is divisible by 10, so no residue
            val rolloverResidue = c256SetScaleDownPow10(this, this, 1, ctx.tmps.pentad1)
            verify { rolloverResidue == EXACT }
            ++adjustedQExp
            verify { digitLen == precision }
        }
    }

    // Step 7: Check final bounds
    // qExp >= qTiny is guaranteed by steps 3/4/5
    // digitLen <= precision is guaranteed by steps 4/5
    verify { adjustedQExp >= Q_TINY }
    verify { digitLen <= precision }
    this.qExp = adjustedQExp
    return when {
        adjustedQExp > Q_MAX -> {
            val qExcess = adjustedQExp - Q_MAX
            if (digitLen + qExcess <= precision)
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
    this.sign = sign
    this.type = STEAL_TYP_ZER
    this.qExp = max(min(qExp, Q_MAX), Q_TINY) // clamped

    // If we had a non-zero residue, the result is inexact
    // This can happen in quantize operations where a non-zero value rounds to zero
    if (inboundResidue != EXACT) {
        // Check if rounding would produce a non-zero result
        val roundUp = inboundResidue.ulpRoundUp(rounding.negate(sign), 0L)
        if (roundUp) {
            c256SetOne()
            this.type = STEAL_TYP_FNZ
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

    return when {
        truncationNeeded > digitLen -> {
            // Result is swamped - becomes zero or min finite
            // This is always inexact
            ctx.signalInexactUnderflow(
                if (rounding.underflowsToZero(sign)) setMinZeroMagnitude(sign, ctx)
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
        else setMinZeroMagnitude(sign, ctx))
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
    this.type = STEAL_TYP_FNZ
    this.sign = sign
    this.qExp = Q_TINY
    verify { digitLen > 0 && digitLen < ctx.precision }

    val totalResidue = scaleResidue.merge(inboundResidue)

    // IEEE 754 7.5: If the rounded result is exact, no underflow flag

    if (totalResidue == EXACT)
        return this

    // Apply rounding
    val roundUp = totalResidue.ulpRoundUp(rounding.negate(sign), dw0)
    if (roundUp) {
        c256MutateIncrement()

        // Check if rounding caused rollover into precision range
        if (digitLen > ctx.precision) {
            verify { digitLen == ctx.precision + 1 }
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
    this.sign = sign
    this.type = STEAL_TYP_FNZ
    this.qExp = Q_MAX
    return this
}
