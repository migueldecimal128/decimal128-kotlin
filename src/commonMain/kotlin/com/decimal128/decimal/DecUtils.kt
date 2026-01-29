@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal
import com.decimal128.decimal.Residue.Companion.EXACT
import kotlin.math.max
import kotlin.math.min

internal inline fun packLengths(digitLen: Int, bitLen: Int) =
    ((digitLen shl 9) or bitLen).toShort()

internal inline fun packSignExp(sign: Boolean, qExp: Int): Short = ((if (sign) 0x8000 else 0) or (qExp and 0x7FFF)).toShort()

internal inline fun unpackBitLen(packedLengths: Short) = packedLengths.toInt() and 0x1FF

internal inline fun unpackDigitLen(packedLengths: Short) = (packedLengths.toInt() shr 9) and 0x7F

internal inline fun unpackSign(signExp: Short) = signExp < 0

internal inline fun unpackExp(signExp: Short) = (signExp.toInt() shl 1) shr 1

internal inline fun calcPackedLengths(dw0: Long): Short {
    val bitLen = calcBitLen64(dw0)
    val digitLen = calcDigitLen64(bitLen, dw0)
    val packed = packLengths(digitLen, bitLen)
    return packed
}

internal inline fun calcPackedLengths(dw1: Long, dw0: Long): Short {
    val bitLen = calcBitLen128(dw1, dw0)
    val digitLen = calcDigitLen128(bitLen, dw1, dw0)
    val packed = packLengths(digitLen, bitLen)
    return packed
}

internal inline fun capExponentRange(e: Int): Int {
    return min(max(e, CAPPED_EXP_MIN), CAPPED_EXP_MAX)
}

internal fun roundAndFinalize(sign: Boolean, dw1: Long, dw0: Long, qExp: Int, inboundResidue: Residue, decRounding: DecRounding, env: DecEnv): Decimal {
    return when {
        qExp < MIN_SPECIAL_VALUE && (dw1 or dw0) != 0L ->
            roundAndFinalizeFnz(sign, dw1, dw0, qExp, inboundResidue, decRounding, env)
        qExp < MIN_SPECIAL_VALUE ->
            roundAndFinalizeZero(sign, qExp, inboundResidue, decRounding, env)
        qExp == NON_FINITE_INF && sign -> Decimal.NEG_INFINITY
        qExp == NON_FINITE_INF         -> Decimal.POS_INFINITY
        qExp == NON_FINITE_QNAN        -> Decimal.qNaN(sign, dw1, dw0)
        else                           -> Decimal.sNaN(sign, dw1, dw0)
    }
}

private fun roundAndFinalizeZero(sign: Boolean, qExp: Int, residue: Residue,
                                 decRounding: DecRounding, env: DecEnv): Decimal {
    // if the coefficient is zero, then it must be the case that
    // residue == EXACT
    require (residue == EXACT) { "cannot have zero coefficient with inEXACT residue" }
    return Decimal.newZero(sign, qExp, env)
}

private fun roundAndFinalizeFnz(sign: Boolean, dw1: Long, dw0: Long, qExp: Int, inboundResidue: Residue,
                                decRounding: DecRounding, env: DecEnv): Decimal {
    val eMax = env.eMax
    val precision = env.precision
    val bitLen = calcBitLen128(dw1, dw0)
    val digitLen = calcDigitLen128(bitLen, dw1, dw0)
    val eExp = qExp + (digitLen - 1)
    val qMax = env.qMax
    // IEEE754-2008 7.5: detect tininess on the unrounded result
    val isTiny = (eExp < env.eMin)

    val excess = max(0, digitLen - precision)
    val myQTiny = env.qTiny - excess      // threshold for normalized

    // 2) Normal result: round only if bd has >precision digits
    if (eExp <= eMax && qExp >= myQTiny) {
        return when {
            qExp > qMax ->
                finalizeFnzClampHighExp(sign, dw1, dw0, qExp, inboundResidue, env)

            excess == 0 && inboundResidue == EXACT ->
                Decimal.from(dw1, dw0, packLengths(digitLen, bitLen), packSignExp(sign, qExp))

            excess == 0 ->
                finalizeFnzInexactNoExcess(sign, dw1, dw0, qExp, inboundResidue, decRounding, isTiny, env)

            else ->
                finalizeFnzWithExcess(sign, dw1, dw0, qExp, inboundResidue, decRounding, isTiny, excess, env)
        }
    }

    // 1) Overflow => +/- Infinity
    if (eExp > eMax) {
        verify { !isTiny }
        // overflow IEEE754-2008 7.4 Overflow page 37
        return finalizeOverflow(sign, decRounding, env)
    }

    // 7.5.1: subnormal rounding (tiny result stays nonzero)
    verify { isTiny }
    val myQMin = env.qTiny - digitLen           // threshold for subnormal cohort
    val overlap = qExp - myQMin
    if (overlap >= 0) {
        val excessTail = digitLen - overlap
        return finalizeFnzSubnormalExcessTail(
            sign, dw1, dw0, qExp,
            inboundResidue, decRounding, excessTail, env
        )
    }
    return finalizeUnderflow(sign, decRounding, env)
}

private fun finalizeFnzClampHighExp(sign: Boolean, dw1: Long, dw0: Long, qExp: Int,
                                    residue: Residue, env: DecEnv): Decimal {
    // clamp/fold-over
    val qExcess = qExp - env.qMax
    val (dw1Scaled, dw0Scaled) = C128ScalePow10.c128ScaleUpPow10(dw1, dw0, qExcess)
    val bitLen = calcBitLen128(dw1Scaled, dw0Scaled)
    val digitLen = calcDigitLen128(bitLen, dw1Scaled, dw0Scaled)
    verify { digitLen <= env.precision }
    val qExpScaled = qExp - qExcess
    verify { residue == EXACT }
    val ret = Decimal.from(dw1Scaled, dw0Scaled, packLengths(digitLen, bitLen), packSignExp(sign, qExpScaled))
    verify { ret.digitLen <= env.precision }
    verify { ret.qExp == env.qMax }
    return ret
}

private fun finalizeFnzInexactNoExcess(sign: Boolean, dw1: Long, dw0: Long, qExp: Int,
                                       residue: Residue, decRounding: DecRounding, isTiny: Boolean, env: DecEnv): Decimal {
    verify { residue != EXACT } // inexact
    verify { calcDigitLen128(dw1, dw0) <= env.precision } // no excess

    val roundUp = residue.ulpRoundUp(decRounding.negate(sign), dw0)
    val dw0Rounded = dw0 + if (roundUp) 1 else 0
    val dw1Rounded = dw1 + if (roundUp && dw0Rounded == 0L) 1 else 0
    if (!roundUp || dw0Rounded != env.decFormat.dw0Maxx || dw1Rounded != env.decFormat.dw1Maxx) {
        val ret = Decimal.from(sign, dw1Rounded, dw0Rounded, qExp)
        return if (isTiny) env.signalInexactUnderflow(ret) else env.signalInexact(ret)
    }
    verify { calcDigitLen128(dw1Rounded, dw0Rounded) == env.precision + 1 }
    // we rolled over into another digit because of roundup
    // this is 10**precision ... so of course it is exactly divisible by 10
    // we have pre-calculated dw1 and dw0 for this case in decFormat.dw*AfterOverflow
    val qExpAfterRollover = qExp + 1
    val eExpAfterRollover = qExpAfterRollover + env.precision - 1
    if (eExpAfterRollover <= env.eMax) {
        val ret = Decimal.from(
            env.decFormat.dw1AfterRollover,
            env.decFormat.dw0AfterRollover,
            env.decFormat.packedLengthsAfterOverflow,
            packSignExp(sign, qExpAfterRollover)
        )
        return env.signalInexact(ret)
    }
    // rounding caused overflow
    return finalizeOverflow(sign, decRounding, env)
}

private fun finalizeOverflow(sign: Boolean, decRounding: DecRounding, env: DecEnv): Decimal {
    val ret = if (decRounding.overflowsToInfinity(sign)) {
        Decimal.infinity(sign)
    } else {
        val dw1MaxFinite = env.decFormat.dw1Maxx
        val dw0MaxFinite = env.decFormat.dw0Maxx - 1 // won't ever have a borrow
        val bitLen = env.decFormat.maxBitLen
        val digitLen = env.decFormat.precision
        val packedLengths = packLengths(digitLen, bitLen)
        val signExp = packSignExp(sign, env.qMax)
        Decimal.from(dw1MaxFinite, dw0MaxFinite, packedLengths, signExp)
    }
    return env.signalInexactOverflow(ret)
}

private fun finalizeUnderflow(sign: Boolean, decRounding: DecRounding, env: DecEnv): Decimal {
    // underflow ... swamped non-zero value
    val ret = if (decRounding.underflowsToZero(sign)) {
        Decimal.newZero(sign, env.qTiny, env)
    } else {
        val dw1MinFinite = 0L
        val dw0MinFinite = 1L
        val packedLengths = packLengths(1, 1)
        val signExp = packSignExp(sign, env.qTiny)
        Decimal.from(dw1MinFinite, dw0MinFinite, packedLengths, signExp)
    }
    return env.signalInexactUnderflow(ret)
}

private fun finalizeFnzWithExcess(sign: Boolean, dw1: Long, dw0: Long, qExp: Int, inboundResidue: Residue, decRounding: DecRounding, tiny: Boolean, excess: Int, env: DecEnv): Decimal {
    TODO()
}

private fun finalizeFnzSubnormalExcessTail(sign: Boolean, dw1: Long, dw0: Long, qExp: Int,
                                           inboundResidue: Residue, decRounding: DecRounding,
                                           excessTail: Int, env: DecEnv): Decimal {
    val (dw1Scaled, dw0Scaled, residueScaled) = C128ScalePow10.c128ScaleDownPow10(dw1, dw0, excessTail)
    verify { qExp + excessTail == env.qTiny }
    verify { calcDigitLen128(dw1, dw0) <= env.precision }

    val totalResidue = residueScaled.merge(inboundResidue)
    if (totalResidue == EXACT) {
        // 7.5 Underflow
        // If the rounded result is exact, no flag is raised
        // and no inexact exception is signaled.
        return Decimal.from(sign, dw1Scaled, dw0Scaled, env.qTiny)
    }
    return finalizeFnzInexactNoExcess(sign, dw1Scaled, dw0Scaled, env.qTiny, totalResidue, decRounding, true, env)
}


internal fun finalize(sign: Boolean, dw1: Long, dw0: Long, qExp: Int,
                      decRounding: DecRounding, env: DecEnv): Decimal {
    // decRounding is needed because we might overflow/underflow
    // to infinity or min/max value, depending upon decRounding
    return when {
        qExp < MIN_SPECIAL_VALUE && (dw1 or dw0) != 0L ->
            finalizeExactFnz(sign, dw1, dw0, qExp, decRounding, env)
        qExp < MIN_SPECIAL_VALUE ->
            Decimal.newZero(sign, qExp, env)
        qExp == NON_FINITE_INF && sign -> Decimal.NEG_INFINITY
        qExp == NON_FINITE_INF         -> Decimal.POS_INFINITY
        qExp == NON_FINITE_QNAN        -> Decimal.qNaN(sign, dw1, dw0)
        else                           -> Decimal.sNaN(sign, dw1, dw0)
    }
}

private fun finalizeExactFnz(sign: Boolean, dw1: Long, dw0: Long, qExp: Int,
                             decRounding: DecRounding, env: DecEnv): Decimal {
    val bitLen = calcBitLen128(dw1, dw0)
    val digitLen = calcDigitLen128(bitLen, dw1, dw0)
    val eExp = qExp + (digitLen - 1)
    // IEEE754-2008 7.5: detect tininess on the unrounded result
    val isTiny = (eExp < env.eMin)

    val excess = max(0, digitLen - env.precision)
    val myQTiny = env.qTiny - excess      // threshold for normalized

    // 2) Normal result: round only if bd has >precision digits
    if (eExp <= env.eMax && qExp >= myQTiny) {
        return when {
            qExp > env.qMax ->
                finalizeExactFnzClampExp(sign, dw1, dw0, qExp, env)

            excess == 0 ->
                Decimal.from(dw1, dw0, packLengths(digitLen, bitLen), packSignExp(sign, qExp))

            else ->
                finalizeFnzWithExcess(
                    sign, dw1, dw0, qExp,
                    EXACT, decRounding, isTiny, excess, env
                )
        }
    }

    // 1) Overflow => +/- Infinity
    if (eExp > env.eMax) {
        verify { !isTiny }
        // overflow IEEE754-2008 7.4 Overflow page 37
        return finalizeOverflow(sign, decRounding, env)
    }

    // 7.5.1: subnormal rounding (tiny result stays nonzero)
    verify { isTiny }
    val myQMin = env.qTiny - digitLen           // threshold for subnormal cohort
    val overlap = qExp - myQMin
    if (overlap >= 0) {
        val excessTail = digitLen - overlap
        return finalizeFnzSubnormalExcessTail(
            sign, dw1, dw0, qExp,
            EXACT, decRounding, excessTail, env
        )
    }
    return finalizeUnderflow(sign, decRounding, env)
}

private fun finalizeExactFnzClampExp(sign: Boolean, dw1: Long, dw0: Long, qExp: Int, env: DecEnv): Decimal {
    // clamp/fold-over
    val qExcess = qExp - env.qMax
    val (dw1Scaled, dw0Scaled) = C128ScalePow10.c128ScaleUpPow10(dw1, dw0, qExcess)
    val bitLen = calcBitLen128(dw1Scaled, dw0Scaled)
    val digitLen = calcDigitLen128(bitLen, dw1Scaled, dw0Scaled)
    verify { digitLen <= env.precision }
    val qExpScaled = qExp - qExcess
    val ret = Decimal.from(dw1Scaled, dw0Scaled, packLengths(digitLen, bitLen), packSignExp(sign, qExpScaled))
    verify { ret.digitLen <= env.precision }
    verify { ret.qExp == env.qMax }
    return ret
}
