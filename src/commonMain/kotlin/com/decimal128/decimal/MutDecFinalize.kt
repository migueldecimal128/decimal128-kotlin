package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

internal fun MutDec.finalize(ctx: DecContext) = roundAndFinalize(Residue.EXACT, ctx.decRounding, ctx)

internal fun MutDec.roundAndFinalize(inboundResidue: Residue, ctx: DecContext) =
    roundAndFinalizeDAG(inboundResidue, ctx.decRounding, ctx)

internal fun MutDec.roundAndFinalize(inboundResidue: Residue, rounding: DecRounding, ctx: DecContext) =
    roundAndFinalizeDAG(inboundResidue, rounding, ctx)

/**
 * Main entry point - implements the DAG structure:
 * 1. Normalize coefficient length (accumulate residue)
 * 2. Apply rounding (may increment coefficient)
 * 3. Handle rollover if needed
 * 4. Check final bounds
 * 5. Signal once with all flags
 */
private fun MutDec.roundAndFinalizeDAG(inboundResidue: Residue, rounding: DecRounding, ctx: DecContext): MutDec {

    // Step 1: Fast path: already in valid decimal128 range
    if (digitLen <= ctx.precision &&
        qExp >= ctx.qTiny && qExp <= ctx.qMax &&
        inboundResidue == EXACT) {
        return this
    }

    // Step 2: special values
    if (qExp >= MIN_SPECIAL_VALUE)
        return this

    // Step 3: Zero coefficient
    if (digitLen == 0)
        return finalizeZero(inboundResidue, rounding, ctx)

    // Step 4: Handle underflow
    // Only divert if range truncation exceeds precision truncation,
    // meaning the normal path can't bring qExp into range on its own
    val rangeTruncationNeeded = ctx.qTiny - qExp
    val precisionTruncationNeeded = max(0, digitLen - ctx.precision)
    if (rangeTruncationNeeded > precisionTruncationNeeded) {
        return finalizeUnderflowRegion(inboundResidue, rounding, ctx)
    }

    // Step 5: Normalize coefficient length to <= precision, accumulating residue
    var totalResidue = inboundResidue
    if (precisionTruncationNeeded > 0) {
        val truncationResidue = c256SetScaleDownPow10(this, this, precisionTruncationNeeded)
        qExp += precisionTruncationNeeded
        totalResidue = truncationResidue.merge(inboundResidue)
        verify { digitLen == ctx.precision }
    }

    // Step 6: Apply rounding ... might increment coefficient
    val applyRounding = totalResidue != EXACT
    if (applyRounding && totalResidue.ulpRoundUp(rounding.negate(sign), dw0)) {
        // Step 6.1: increment
        c256MutateIncrement()

        // Step 6.2: Handle rollover if increment caused overflow to new digit
        if (digitLen > ctx.precision) {
            verify { digitLen == ctx.precision + 1 }
            // Rolling over means the result is divisible by 10, so no residue
            val rolloverResidue = c256SetScaleDownPow10(this, this, 1)
            verify { rolloverResidue == EXACT }
            ++qExp
            verify { digitLen == ctx.precision }
        }
    }

    // Step 7: Check final bounds
    // qExp >= qTiny is guaranteed by steps 3/4/5
    // digitLen <= precision is guaranteed by steps 4/5
    verify { qExp >= ctx.qTiny }
    verify { digitLen <= ctx.precision }
    return when {
        qExp > ctx.qMax -> {
            val qExcess = qExp - ctx.qMax
            if (digitLen + qExcess <= ctx.precision)
                finalizeClamping(ctx)
            else
                finalizeOverflow(rounding, ctx)
        }
        applyRounding -> ctx.signalInexact(this)
        else -> this
    }
}

private fun MutDec.finalizeZero(inboundResidue: Residue, rounding: DecRounding, ctx: DecContext): MutDec {
    verify { digitLen == 0 }
    val qMax = ctx.qMax
    val qTiny = ctx.qTiny

    // If we had a non-zero residue, the result is inexact
    // This can happen in quantize operations where a non-zero value rounds to zero
    if (inboundResidue != EXACT) {
        // Check if rounding would produce a non-zero result
        val roundUp = inboundResidue.ulpRoundUp(rounding.negate(sign), 0L)
        if (roundUp) {
            c256SetOne()
            return when {
                qExp > qMax -> finalizeOverflow(rounding, ctx)
                qExp < qTiny -> finalizeUnderflowRegion(inboundResidue, rounding, ctx)
                else -> ctx.signalInexact(this)
            }
        }
        // Rounding confirms zero, but it's still inexact
        // Now clamp the exponent
        qExp = max(min(qExp, qMax), qTiny)
        return ctx.signalInexact(this)
    }

    // Exact zero - clamp exponent
    qExp = max(min(qExp, qMax), qTiny)
    return this
}

private fun MutDec.finalizeOverflow(rounding: DecRounding, ctx: DecContext): MutDec {
    // IEEE 754 7.4 Overflow - always inexact
    verify { qExp > ctx.qMax }
    verify { digitLen <= ctx.precision }
    if (rounding.overflowsToInfinity(sign)) {
        setInfinite(sign)
    } else {
        setMaxFiniteMagnitude(ctx)
    }
    return ctx.signalInexactOverflow(this)
}

private fun MutDec.finalizeUnderflowRegion(
    residue: Residue,
    rounding: DecRounding,
    ctx: DecContext
): MutDec {
    // IEEE 754 7.5 Underflow - handle subnormal region
    verify { qExp < ctx.qTiny }

    val truncationNeeded = ctx.qTiny - qExp

    return when {
        truncationNeeded > digitLen -> {
            // Result is swamped - becomes zero or min finite
            // This is always inexact
            ctx.signalInexactUnderflow(
                if (rounding.underflowsToZero(sign)) setMinZeroMagnitude(ctx)
                else setMinFiniteMagnitude(ctx))
        }
        truncationNeeded == digitLen -> {
            // Exactly on the underflow boundary - round to decide
            finalizeUnderflowBoundary(residue, rounding, ctx)
        }
        else -> {
            // truncationNeeded < digitLen (and > 0)
            // Truncate to subnormal and round
            finalizeSubnormal(residue, rounding, ctx, truncationNeeded)
        }
    }
}

private fun MutDec.finalizeUnderflowBoundary(
    inboundResidue: Residue,
    rounding: DecRounding,
    ctx: DecContext
): MutDec {
    // no value params ... nothing to verify
    val scaleResidue = Residue.fromValueDecade(this)
    val totalResidue = scaleResidue.merge(inboundResidue)
    val roundUp = totalResidue.ulpRoundUp(rounding.negate(sign), 0L)

    return if (roundUp) {
        setMinFiniteMagnitude(ctx)
        ctx.signalInexactUnderflow(this)
    } else {
        setMinZeroMagnitude(ctx)
        ctx.signalInexactUnderflow(this)
    }
}

private fun MutDec.finalizeSubnormal(
    inboundResidue: Residue,
    rounding: DecRounding,
    ctx: DecContext,
    truncationNeeded: Int
): MutDec {
    verify { truncationNeeded > 0 && truncationNeeded < digitLen }

    // Scale down to fit in subnormal range
    val scaleResidue = c256SetScaleDownPow10(this, this, truncationNeeded)
    qExp += truncationNeeded
    verify { digitLen > 0 && digitLen < ctx.precision }
    verify { qExp == ctx.qTiny }

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
            val rolloverResidue = c256SetScaleDownPow10(this, this, 1)
            verify { rolloverResidue == EXACT }
            ++qExp
        }
    }

    return ctx.signalInexactUnderflow(this)
}

private fun MutDec.finalizeClamping(ctx: DecContext): MutDec {
    val qExcess = qExp - ctx.qMax
    verify { qExcess > 0 && qExcess <= ctx.precision - digitLen }

    c256SetScaleUpPow10(this, this, qExcess, ctx.tmps.dwQuad1)
    qExp -= qExcess
    verify { qExp == ctx.qMax }

    return this
}
