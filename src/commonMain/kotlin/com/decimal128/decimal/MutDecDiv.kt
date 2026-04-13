package com.decimal128.decimal

import com.decimal128.decimal.Compare754Result.*
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO
import kotlin.math.min

internal fun mutDecDivImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    val quotientSign = stealSignFlag(xSteal) xor stealSignFlag(ySteal)
    val binopSignature = binopSignatureOf(xSteal, ySteal)
    if (binopSignature == FNZ_FNZ)
        return mutDecDivFnzFnz(z, quotientSign, x, y, ctx)
    else when (binopSignature) {
        ZER_ZER -> ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.DIV_ZERO_BY_ZERO)
        ZER_FNZ -> z.setZero(quotientSign, stealQExp(xSteal) - stealQExp(ySteal))
        ZER_INF,
        FNZ_INF -> z.setZero(quotientSign, Q_TINY)
        FNZ_ZER -> ctx.signalDivByZero(z.setInfinite(quotientSign))
        INF_ZER,
        INF_FNZ -> z.setInfinite(quotientSign)
        INF_INF -> ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.DIV_INF_BY_INF)
        else -> z.setNanOperandFound(x, y, ctx)
    }
    return z
}

internal fun mutDecDivIntImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    val quotientSign = stealSignFlag(xSteal) xor stealSignFlag(ySteal)
    val binopSignature = binopSignatureOf(xSteal, ySteal)
    if (binopSignature == FNZ_FNZ) {
        setDivIntFnzFnz(z, x, y, ctx)
    } else {
        val quotientSign = x.sign xor y.sign
        when (binopSignature) {
            ZER_ZER -> ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.DIV_ZERO_BY_ZERO)
            INF_INF -> ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.DIV_INF_BY_INF)
            FNZ_INF,
            ZER_FNZ,
            ZER_INF -> z.setZero(quotientSign)
            INF_ZER,
            INF_FNZ -> z.setInfinite(quotientSign)
            FNZ_ZER -> ctx.signalDivByZero(z.setInfinite(quotientSign))
            else -> z.setNanOperandFound(x, y, ctx)
        }
    }
    return z
}

internal fun setDivIntFnzFnz(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val truncCtx = ctx.withRoundingAndNewFlags(ROUND_TOWARD_ZERO)
    z.setDiv(x, y, truncCtx)
    z.setRoundToIntegralExact(z, truncCtx)
    // Normalize integer toward qExp = 0 using available precision
    val zQ = z.qExp
    verify { zQ >= 0 }
    if (zQ > 0) {
        if (!z.isZero()) { // truncation could generate z == 0
            val headroom = ctx.precision - z.digitLen
            if (headroom < zQ) // 1234567890123456789012345678901234 / .000000001
                return ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.DIV_INT_OVERFLOWS_COEFFICIENT)
            c256SetScaleUpPow10(z, z, zQ, ctx.tmps.pentad)
        }
        z.qExp = 0
    }
    return z
}

internal fun mutDecReciprocalImpl(z: MutDec, x: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val quotientSign = stealSignFlag(xSteal)
    when (stealTyp(xSteal)) {
        STEAL_TYP_FNZ -> {
            return mutDecInv(z, quotientSign, x, ctx)
        }
        STEAL_TYP_ZER -> ctx.signalDivByZero(z.setInfinite(quotientSign))
        STEAL_TYP_INF -> z.setZero(quotientSign)
        else -> z.setNanOperandFound(x, ctx)
    }
    return z
}

fun mutDecSetRemTruncImpl(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): Boolean {
    val xSteal = x.steal
    val ySteal = y.steal
    val binopSignature = binopSignatureOf(x.steal, y.steal)
    if (binopSignature == FNZ_FNZ) {
        return setRemTruncFnzFnz(z, x, y, ctx)
    } else {
        when (binopSignature) {
            ZER_FNZ -> z.setZero(stealSignFlag(xSteal), min(stealQExp(xSteal), stealQExp(ySteal)))

            FNZ_ZER -> ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.DIV_BY_ZERO_IN_REMAINDER_OP)
            ZER_ZER -> ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.DIV_ZERO_BY_ZERO)

            INF_ZER,
            INF_FNZ,
            INF_INF -> ctx.setNanSignalInvalidOperation(z, InvalidOperationReason.INF_NUMERATOR_IN_REMAINDER_OP)
            ZER_INF,
            FNZ_INF -> z.set(x)
            else -> z.setNanOperandFound(x, y, ctx)
        }
    }
    return false
}

fun setRemTruncFnzFnz(z: MutDec, x: MutDec, y: MutDec, ctx: DecContext): Boolean {
    val xSteal = x.steal
    val ySteal = y.steal
    verify { stealBitLen(xSteal) != 0 && stealBitLen(ySteal) != 0 }
    // Compute n = nearest integer to x/y (ties to even)
    // setRemainder is an EXACT operation, so we will use a temp
    // environment so that INEXACT flag/trap does not get signaled.
    // use INTERNAL_TMP_ENV so that flag-setting
    val truncCtx = ctx.withRoundingAndNewFlags(ROUND_TOWARD_ZERO)
    val n = ctx.tmps.mdecDivRemPowCtzd.setDiv(x, y, truncCtx)
    if (n.qExp < 0)
        n.setRoundToIntegralExact(n, truncCtx)

    // Compute r = x - n*y
    // (-n) * y + x
    n.sign = !n.sign // negate n
    val quotientIsOdd = (n.dw0.toInt() and 1) != 0
    z.setFma(n, y, x, truncCtx)
    if (z.isZero())
        z.setZero(stealSignFlag(xSteal), min(stealQExp(xSteal), stealQExp(ySteal)))

    return quotientIsOdd
}

fun mutDecCompare754Impl(x: MutDec, y: MutDec, isSignaling: Boolean, ctx: DecContext): Compare754Result {
    val xSteal = x.steal
    val ySteal = y.steal
    val binopSignature = binopSignatureOf(xSteal, ySteal)
    if (binopSignature == FNZ_FNZ) {
        return Compare754Result(x.compareNumericMagnitudeTo(y))
    }
    val xSign = stealSignFlag(xSteal)
    val ySign = stealSignFlag(ySteal)
    return when (binopSignature) {
        ZER_ZER -> IEEE754_EQ

        ZER_FNZ -> if (ySign) IEEE754_LT else IEEE754_GT
        FNZ_ZER -> if (xSign) IEEE754_GT else IEEE754_LT

        INF_INF -> when {
            xSign == ySign -> IEEE754_EQ
            xSign -> IEEE754_LT
            else -> IEEE754_GT
        }

        INF_FNZ,
        INF_ZER -> if (xSign) IEEE754_LT else IEEE754_GT
        FNZ_INF,
        ZER_INF -> if (ySign) IEEE754_GT else IEEE754_LT

        else -> { // NAN_FOUND
            if (isSignaling) {
                val guiltyParty = when {
                    stealIsSNAN(xSteal) -> x
                    stealIsSNAN(ySteal) or !stealIsNAN(xSteal)-> y
                    else -> x
                }
                ctx.operandIsSignalingNaN(guiltyParty)
            }
            IEEE754_UNORDERED
        }
    }
}

fun mutDecDivFnzFnz(z: MutDec, sign: Boolean, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val xSteal = x.steal
    val ySteal = y.steal
    val numeratorScale = ctx.precision + 1 - (stealDigitLen(xSteal) - stealDigitLen(ySteal))
    val tmps = ctx.tmps
    val pentad = tmps.pentad
    val scaledNumerator = tmps.mdecDivRemPowCtzd
    c256SetScaleUpPow10(scaledNumerator, x, numeratorScale, pentad)
    val residue = when {
        (stealBitLen(ySteal) <= 64) -> c256SetDivX64(z, scaledNumerator, y.dw0, tmps.knuthD)
        else -> c256SetDiv(z, scaledNumerator, y, tmps)
    }
    val qPreferred = stealQExp(xSteal) - stealQExp(ySteal)
    var qZ = qPreferred - numeratorScale
    var ntz = z.dw0.countTrailingZeroBits()
    if (residue == Residue.EXACT && qZ < qPreferred && ntz > 0) {
        if (qZ + 1 < qPreferred) {
            val quot = C256()
            do {
                val deltaQ = qPreferred - qZ
                val chunk = min(min(9, deltaQ), ntz)
                val chunkRemainder = barrettDivModPow10(quot, z, chunk)
                // FIXME -- the stripTrailingZeros code uses a faster way to
                //  countTrailingZeroDigits
                if (chunkRemainder > 0) {
                    var pow10Count = 0
                    var t = chunkRemainder
                    val M = 0xCCCCCCCCCCCCCCCDuL.toLong()
                    val S = 3
                    while (true) {
                        // val q = t / 10
                        // val r = t % 10
                        val q = unsignedMulHi(t, M) ushr S
                        val r = t - (q * 10)
                        if (r != 0L)
                            break
                        ++pow10Count
                        t = q
                    }
                    if (pow10Count > 0) {
                        c256SetScaleDownPow10(z, z, pow10Count, pentad)
                        qZ += pow10Count
                    }
                    break
                } else {
                    z.c256Set(quot)
                    ntz -= chunk
                    qZ += chunk
                }
            } while (qZ < qPreferred && ntz > 0)
        } else if (c256IsMultipleOf10(z)) {
            c256SetScaleDownPow10(z, z, 1, pentad)
            ++qZ
        }
    }
    return z.roundAndFinalizeFnz(sign, qZ, residue, ctx)
}
