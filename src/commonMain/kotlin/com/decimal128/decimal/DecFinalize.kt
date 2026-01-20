package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

internal fun MutDec.roundAndFinalize(inboundResidue: Residue, env: DecEnv) =
    roundAndFinalize(inboundResidue, env.decRounding, env)

internal fun MutDec.roundAndFinalize(inboundResidue: Residue, rounding: DecRounding, env: DecEnv) =
    roundAndFinalize2(inboundResidue, rounding, env)

private fun MutDec.roundAndFinalizeSubnormalUnderflowBoundary(inboundResidue: Residue, rounding: DecRounding, env: DecEnv): MutDec {
    val scaleResidue = Residue.residueFrom(this)
    val totalResidue = scaleResidue.merge(inboundResidue)
    val roundUp = totalResidue.ulpRoundUp(rounding.negate(sign), 0L)
    if (! roundUp)
        return finalizeUnderflow(rounding, env)
    setMinFiniteMagnitude(env)
    return env.signalInexactUnderflow(this)
}

private fun MutDec.roundAndFinalizeSubnormal(inboundResidue: Residue, rounding: DecRounding, env: DecEnv): MutDec {
    // non-zero subnormal
    val truncationNeeded = env.qTiny - qExp
    verify { truncationNeeded < digitLen }
    var totalResidue = inboundResidue
    if (truncationNeeded > 0) {
        val scaleResidue = c256SetScaleDownPow10(this, this, truncationNeeded)
        qExp += truncationNeeded
        verify { digitLen > 0 }
        verify { digitLen < env.precision }
        verify { qExp == env.qTiny }
        totalResidue = scaleResidue.merge(inboundResidue)
    }
    if (totalResidue == EXACT) {
        // 7.5 Underflow
        // If the rounded result is exact, no flag is raised
        // and no inexact exception is signaled.
        return this
    }
    val roundUp = totalResidue.ulpRoundUp(rounding.negate(sign), dw0)
    if (roundUp) {
        c256MutateIncrement()
        if (digitLen > env.precision) {
            verify { digitLen == env.precision + 1 }
            // if we rolled into another digit because of roundup
            // then the result is definitely divisible by 10
            val residueExact = c256SetScaleDownPow10(this, this, 1)
            verify { residueExact == EXACT }
            ++qExp
        }
    }
    return env.signalInexactUnderflow(this)
}

private fun MutDec.roundAndFinalizeZero(inboundResidue: Residue, rounding: DecRounding, env: DecEnv): MutDec {
    // since the coefficient == 0 the inboundResidue must be EXACT
    verify { inboundResidue == EXACT }
    qExp = max(min(qExp, env.qMax), env.qTiny)
    return this
}

// a new beginning ... searching for a different/better way to finalize

internal fun MutDec.finalize(env: DecEnv) = finalize(env.decRounding, env)

internal fun MutDec.finalize(rounding: DecRounding, env: DecEnv): MutDec {
    val qExp = this.qExp
    val eExp = this.eExp
    val digitLen = this.digitLen
    return when {
        qExp >= MIN_SPECIAL_VALUE -> this
        digitLen == 0 -> finalizeZero(env)
        digitLen > env.precision -> finalizeFnzLongCoeff(rounding, env)
        eExp > env.eMax -> finalizeOverflow(rounding, env)
        eExp < env.eMin -> roundAndFinalizeTiny(EXACT, rounding, env)
        qExp > env.qMax -> finalizeFnzClampHighExp(env)
        else -> finalizeFnzNormal(env)
    }
}

private fun MutDec.finalizeZero(env: DecEnv): MutDec {
    qExp = max(min(qExp, env.qMax), env.qTiny)
    return this
}

private fun MutDec.finalizeFnzLongCoeff(rounding: DecRounding, env: DecEnv): MutDec {
    val excessDigitCount = digitLen - env.precision
    verify { excessDigitCount > 0 }
    val residue = c256SetScaleDownPow10(this, this, excessDigitCount)
    qExp += excessDigitCount
    return roundAndFinalize(residue, rounding, env)
}

private fun MutDec.finalizeOverflow(rounding: DecRounding, env: DecEnv): MutDec {
    // overflow IEEE754-2008 7.4 Overflow page 37
    if (rounding.overflowsToInfinity(sign)) {
        setInfinite(sign)
    } else {
        setMaxFiniteMagnitude(env)
    }
    return env.signalInexactOverflow(this)
}

private fun MutDec.roundAndFinalizeTiny(inboundResidue: Residue, rounding: DecRounding, env: DecEnv): MutDec {
    // 7.5.1: subnormal rounding (tiny result stays nonzero)
    verify { eExp < env.eMin }
    verify { digitLen <= env.precision }
    val truncationNeeded = env.qTiny - qExp
    return when {
        truncationNeeded < 0 && inboundResidue == EXACT -> finalizeFnzSubnormal(env)
        truncationNeeded < 0 -> throw IllegalStateException()
        truncationNeeded > digitLen -> finalizeUnderflow(rounding, env)
        truncationNeeded == digitLen -> roundAndFinalizeSubnormalUnderflowBoundary(inboundResidue, rounding, env)
        else -> roundAndFinalizeSubnormal(inboundResidue, rounding, env)
    }
}

private fun MutDec.finalizeFnzClampHighExp(env: DecEnv): MutDec {
    // clamp/fold-over
    verify { eExp <= env.eMax && qExp >= env.qMax }
    val qExcess = qExp - env.qMax
    c256SetScaleUpPow10(this, this, qExcess)
    verify { digitLen <= env.precision }
    qExp -= qExcess
    verify { qExp == env.qMax }
    return this
}

private fun MutDec.finalizeFnzNormal(env: DecEnv) = this

private fun MutDec.finalizeFnzSubnormal(env: DecEnv) = this

private fun MutDec.finalizeUnderflow(rounding: DecRounding, env: DecEnv): MutDec {
    // underflow ... swamped non-zero value
    return env.signalInexactUnderflow(
        if (rounding.underflowsToZero(sign)) {
            setMinZeroMagnitude(env)
        } else {
            setMinFiniteMagnitude(env)
        })
}

internal fun MutDec.roundAndFinalize2(residue: Residue, rounding: DecRounding, env: DecEnv): MutDec {
    return when {
        residue == EXACT -> finalize(rounding, env)
        digitLen > env.precision -> roundAndFinalizeFnzLongCoeff(residue, rounding, env)
        eExp < env.eMin -> roundAndFinalizeTiny(residue, rounding, env)
        digitLen == env.precision -> roundAndFinalizeFnzValidCoeff(residue, rounding, env)
        digitLen < env.precision -> roundAndFinalizeShortCoeff(residue, rounding, env)
        else -> throw IllegalStateException()
    }
}

private fun MutDec.roundAndFinalizeFnzLongCoeff(inboundResidue: Residue, rounding: DecRounding, env: DecEnv): MutDec {
    verify { inboundResidue != EXACT }
    val excessDigitCount = digitLen - env.precision
    verify { excessDigitCount > 0 }
    val newResidue = c256SetScaleDownPow10(this, this, excessDigitCount)
    val totalResidue = newResidue.merge(inboundResidue)
    verify { totalResidue != EXACT }
    verify { digitLen == env.precision }
    qExp += excessDigitCount
    return roundAndFinalizeFnzValidCoeff(totalResidue, rounding, env)
}

private fun MutDec.roundAndFinalizeFnzValidCoeff(residue: Residue, rounding: DecRounding, env: DecEnv): MutDec {
    verify { residue != EXACT }
    verify { digitLen == env.precision }
    if (qExp < env.qTiny)
        return roundAndFinalizeTiny(residue, rounding, env)
    val roundUp = residue.ulpRoundUp(rounding.negate(sign), dw0)
    if (roundUp) {
        dw1 += if (++dw0 == 0L) 1L else 0L
        if (dw0 == env.decFormat.dw0Maxx && dw1 == env.decFormat.dw1Maxx) {
            // we rolled over to a new decimal digit 10000...0000
            dw1 = env.decFormat.dw1AfterRollover
            dw0 = env.decFormat.dw0AfterRollover
            // FIXME
            //   packedLengths = env.decFormat.packedLengthsAfterOverflow
            updateDigitLenBitLen()
            ++qExp
        }
    }
    verify { digitLen == env.precision }
    return when {
        // these checks can be done with qExp instead of eExp because digitLen == env.precision
        qExp > env.qMax -> finalizeOverflow(rounding, env)
        qExp < env.qTiny -> roundAndFinalizeTiny(residue, rounding, env)
        else -> env.signalInexact(this)
    }
}

internal fun MutDec.roundAndFinalizeShortCoeff(residue: Residue, rounding: DecRounding, env: DecEnv): MutDec {
    verify { residue != EXACT }
    verify { digitLen < env.precision }
    verify { qExp <= env.qMax && qExp >= env.qTiny }
    val roundUp = residue.ulpRoundUp(rounding.negate(sign), dw0)
    if (roundUp)
        c256MutateIncrement()
    return env.signalInexact(this)
}
