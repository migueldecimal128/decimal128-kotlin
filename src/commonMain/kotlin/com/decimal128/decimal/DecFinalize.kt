package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

internal fun MutDec.roundAndFinalize(inboundResidue: Residue, ctx: DecContext) =
    roundAndFinalize(inboundResidue, ctx.decRounding, ctx)

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
    // Handle special values immediately
    if (qExp >= MIN_SPECIAL_VALUE)
        return this

    // Handle zero coefficient
    if (digitLen == 0)
        return finalizeZero(inboundResidue, rounding, ctx)

    // Step 1: Normalize coefficient length to <= precision
    // This accumulates any residue from truncation
    var totalResidue = inboundResidue
    if (digitLen > ctx.precision) {
        val excessDigitCount = digitLen - ctx.precision
        val truncationResidue = c256SetScaleDownPow10(this, this, excessDigitCount)
        qExp += excessDigitCount
        totalResidue = truncationResidue.merge(inboundResidue)
        verify { digitLen == ctx.precision }
    }

    // Step 1.5: Check if we're in the underflow region BEFORE applying rounding
    // This matches the old code's behavior in roundAndFinalizeFnzValidCoeff
    if (digitLen == ctx.precision && qExp < ctx.qTiny) {
        return finalizeUnderflow(totalResidue, rounding, ctx)
    }

    // Step 2: Apply rounding (may increment coefficient)
    val shouldRound = totalResidue != EXACT
    if (shouldRound) {
        val roundUp = totalResidue.ulpRoundUp(rounding.negate(sign), dw0)
        if (roundUp) {
            c256MutateIncrement()

            // Step 3: Handle rollover if increment caused overflow to new digit
            if (digitLen > ctx.precision) {
                verify { digitLen == ctx.precision + 1 }
                // Rolling over means the result is divisible by 10, so no residue
                val rolloverResidue = c256SetScaleDownPow10(this, this, 1)
                verify { rolloverResidue == EXACT }
                ++qExp
                verify { digitLen == ctx.precision }
            }
        }
    }

    // Step 4: Check final bounds and determine which flags to signal
    // Note: For short coefficients (digitLen < precision), we still might overflow/underflow
    // after rounding changes the exponent
    return when {
        eExp > ctx.eMax -> finalizeOverflow(rounding, ctx)
        eExp < ctx.eMin -> {
            // In the tiny range - distinguish valid subnormal from true underflow
            if (qExp >= ctx.qTiny) {
                // Valid subnormal at the requested scale - just signal inexact if needed
                if (shouldRound) ctx.signalInexact(this) else this
            } else {
                // Truly underflowed - needs special handling
                finalizeUnderflow(totalResidue, rounding, ctx)
            }
        }
        qExp > ctx.qMax -> finalizeClamping(ctx)
        shouldRound -> ctx.signalInexact(this)
        else -> this
    }
}

private fun MutDec.finalizeZero(inboundResidue: Residue, rounding: DecRounding, ctx: DecContext): MutDec {
    verify { digitLen == 0 }

    // If we had a non-zero residue, the result is inexact
    // This can happen in quantize operations where a non-zero value rounds to zero
    if (inboundResidue != EXACT) {
        // Check if rounding would produce a non-zero result
        val roundUp = inboundResidue.ulpRoundUp(rounding.negate(sign), 0L)
        if (roundUp) {
            // Rounding says we should round up from zero to 1 at the current exponent
            // Don't clamp the exponent yet - respect the requested scale
            dw0 = 1L
            dw1 = 0L
            dw2 = 0L
            dw3 = 0L
            updateDigitLenBitLen()
            // Now check if this result fits in the valid range
            return when {
                eExp > ctx.eMax -> finalizeOverflow(rounding, ctx)
                eExp < ctx.eMin -> {
                    // In subnormal/underflow range - need to handle carefully
                    // If qExp >= qTiny, this is a valid subnormal, just signal inexact
                    if (qExp >= ctx.qTiny) {
                        ctx.signalInexact(this)
                    } else {
                        // Truly underflowed - need subnormal handling
                        finalizeUnderflow(inboundResidue, rounding, ctx)
                    }
                }
                else -> ctx.signalInexact(this)
            }
        }
        // Rounding confirms zero, but it's still inexact
        // Now clamp the exponent
        qExp = max(min(qExp, ctx.qMax), ctx.qTiny)
        return ctx.signalInexact(this)
    }

    // Exact zero - clamp exponent
    qExp = max(min(qExp, ctx.qMax), ctx.qTiny)
    return this
}

private fun MutDec.finalizeOverflow(rounding: DecRounding, ctx: DecContext): MutDec {
    // IEEE 754 7.4 Overflow - always inexact
    verify { eExp > ctx.eMax }
    verify { digitLen <= ctx.precision }
    if (rounding.overflowsToInfinity(sign)) {
        setInfinite(sign)
    } else {
        setMaxFiniteMagnitude(ctx)
    }
    return ctx.signalInexactOverflow(this)
}

private fun MutDec.finalizeUnderflow(
    residue: Residue,
    rounding: DecRounding,
    ctx: DecContext
): MutDec {
    // IEEE 754 7.5 Underflow - handle subnormal region
    verify { eExp < ctx.eMin }
    verify { digitLen <= ctx.precision }

    val truncationNeeded = ctx.qTiny - qExp

    return when {
        truncationNeeded <= 0 -> {
            // Already in valid subnormal range
            // IEEE 754 7.5: If the rounded result is exact, no underflow flag is raised
            if (residue != EXACT) ctx.signalInexactUnderflow(this) else this
        }
        truncationNeeded == digitLen -> {
            // Exactly on the underflow boundary - round to decide
            finalizeUnderflowBoundary(residue, rounding, ctx)
        }
        truncationNeeded > digitLen -> {
            // Result is swamped - becomes zero or min finite
            // This is always inexact
            if (rounding.underflowsToZero(sign)) {
                ctx.signalInexactUnderflow(setMinZeroMagnitude(ctx))
            } else {
                ctx.signalInexactUnderflow(setMinFiniteMagnitude(ctx))
            }
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
    // Clamp/fold-over for high exponent
    verify { eExp <= ctx.eMax && qExp > ctx.qMax }

    val qExcess = qExp - ctx.qMax
    c256SetScaleUpPow10(this, this, qExcess)
    verify { digitLen <= ctx.precision }
    qExp -= qExcess
    verify { qExp == ctx.qMax }

    return this
}

// Old functions retained for compatibility during transition
internal fun MutDec.finalize(env: DecContext) = finalize(env.decRounding, env)

internal fun MutDec.finalize(rounding: DecRounding, env: DecContext): MutDec {
    val qExp = this.qExp
    val eExp = this.eExp
    val digitLen = this.digitLen
    return when {
        qExp >= MIN_SPECIAL_VALUE -> this
        digitLen == 0 -> finalizeZero(Residue.EXACT, rounding, env)
        digitLen > env.precision -> finalizeFnzLongCoeff(rounding, env)
        eExp > env.eMax -> finalizeOverflow(rounding, env)
        eExp < env.eMin -> finalizeUnderflow(Residue.EXACT, rounding, env)
        qExp > env.qMax -> finalizeClamping(env)
        else -> this
    }
}

private fun MutDec.finalizeFnzLongCoeff(rounding: DecRounding, env: DecContext): MutDec {
    val excessDigitCount = digitLen - env.precision
    verify { excessDigitCount > 0 }
    val residue = c256SetScaleDownPow10(this, this, excessDigitCount)
    qExp += excessDigitCount
    return roundAndFinalizeDAG(residue, rounding, env)
}
