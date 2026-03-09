package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.Residue.Companion.LT_HALF

internal fun d128RoundToIntegral(x: Decimal, rounding: DecRounding, ctx: DecContext, beQuiet: Boolean = false): Decimal {
    val sign = x.sign;
    val qExp = x.qExp;
    val dw1 = x.dw1;
    val dw0 = x.dw0
    if (qExp >= 0) {
        if (qExp < NON_FINITE_SNAN)
            return x
        return nanOperandFound(x, ctx)
    }
    if (x.isZero())
        return Decimal.zero(sign)
    val pow10 = -qExp
    val tmpPair = ctx.tmps.dwQuad1
    tmpPair.dw0 = 0L
    tmpPair.dw1 = 0L
    val digitLen = x.digitLen
    val residue: Residue = when {
        pow10 > digitLen -> LT_HALF
        pow10 == digitLen -> Residue.fromValuePow10(dw1, dw0, digitLen)
        else -> c128ScaleDownPow10(tmpPair, dw1, dw0, pow10)
    }
    return decRoundAndFinalizeFinite(
        sign, tmpPair.dw1, tmpPair.dw0,
        residue, 0, rounding, ctx, beQuiet
    )
}

/**
 * Core implementation for all decimal-to-[Long] conversions.
 *
 * Implements the IEEE 754-2019 `convertToInteger` family of operations for signed 64-bit integers.
 * The invalid sentinel [Long.MIN_VALUE] matches Intel's decimal library (bid128_to_int64_*).
 *
 * @param rounding the rounding mode to apply when the value is not exactly representable as a [Long]
 * @param ctx the decimal context for signaling flags
 * @param suppressInexact if true, suppresses [DecException.INEXACT] (used by the non-exact IEEE 754 variants).
 *   [DecException.INVALID_OPERATION] is always signaled regardless of this flag.
 * @return the converted [Long], or [Long.MIN_VALUE] if the value is NaN, infinite, or out of range
 */
fun d128ConvertToLong(x: Decimal, rounding: DecRounding, ctx: DecContext, suppressInexact: Boolean = false): Long {
    val steal = x.steal
    if (stealIsFinite(steal)) {
        val signMaskLong = stealSignMask(steal).toLong()
        val sign = stealSignFlag(steal)
        val qExp = stealQexp(steal)
        val bitLen = stealBitLen(steal)
        val digitLen = stealDigitLen(steal)
        val dw0 = x.dw0
        when {
            qExp == 0 -> {
                if (bitLen < 64)
                    return (dw0 xor signMaskLong) - signMaskLong
                if (bitLen == 64 && dw0 == Long.MIN_VALUE && sign)
                    return Long.MIN_VALUE
                // return signalInvalid
            }

            stealIsZER(steal) -> return 0L
            qExp > 0 -> {
                // if there is headroom then scale it up
                if (digitLen + qExp <= 19) {
                    val result = dw0 * pow10_64(qExp)
                    if (result > 0)
                        return (result xor signMaskLong) - signMaskLong
                    // Long.MIN_VALUE && sign is not possible ...
                    // ... because we just multiplied by 10**qExp
                    // ... so the value ends in 0
                    // ... but Long.MIN_VALUE ends in 8
                }
                // return signalInvalid
            }

            else -> { // qExp < 0
                // at least some fractional digits, perhaps 0 digits
                val fracDigitLen = -qExp
                if (fracDigitLen >= digitLen) {
                    // all fractional digits
                    val residue: Residue
                    if (fracDigitLen > digitLen)
                        residue = LT_HALF
                    else {
                        residue = Residue.fromValueDecade(x)
                        verify { residue != EXACT }
                    }
                    val roundUp = residue.ulpRoundUp(rounding.negate(sign), 0L)
                    val ret = if (!roundUp) 0L else (signMaskLong shl 1) or 1L
                    if (!suppressInexact)
                        ctx.signalInexact(x)
                    return ret
                }
                // both integral and fractional digits
                val intDigitLen = digitLen - fracDigitLen
                if (intDigitLen <= 19) {
                    val dwPair = ctx.tmps.dwQuad1
                    val residue = c128ScaleDownPow10(dwPair, x.dw1, dw0, fracDigitLen)
                    // DANGER! CAUTION! r0 might roll over to ZEEERO with this roundUp
                    var r0 = dwPair.dw0
                    val roundUp01 = residue.ulpRoundUp01L(rounding.negate(sign), r0)
                    r0 += roundUp01
                    verify { dwPair.dw1 == 0L }
                    if (r0 > 0L || r0 == Long.MIN_VALUE && sign) {
                        val ret = (r0 xor signMaskLong) - signMaskLong
                        if (!suppressInexact && residue != EXACT)
                            ctx.signalInexact(ret)
                        return ret
                    }
                }
                // return signalInvalid
            }
        }
    }
    // return signalInvalid
    ctx.signalInvalid(x)
    return Long.MIN_VALUE
}

/**
 * Core implementation for all decimal-to-[Int] conversions.
 *
 * Implements the IEEE 754-2019 `convertToInteger` family of operations for signed 32-bit integers.
 * All internal arithmetic is performed in [Long] to avoid overflow during intermediate calculations.
 * The invalid sentinel [Int.MIN_VALUE] matches Intel's decimal library (bid128_to_int32_*).
 *
 * @param rounding the rounding mode to apply when the value is not exactly representable as an [Int]
 * @param ctx the decimal context for signaling flags
 * @param suppressInexact if true, suppresses [DecException.INEXACT] (used by the non-exact IEEE 754 variants).
 *   [DecException.INVALID_OPERATION] is always signaled regardless of this flag.
 * @return the converted [Int], or [Int.MIN_VALUE] if the value is NaN, infinite, or out of range
 */
fun d128ConvertToInt(x: Decimal, rounding: DecRounding, ctx: DecContext, suppressInexact: Boolean = false): Int {
    val steal = x.steal
    if (stealIsFinite(steal)) {
        val signMask = stealSignMask(steal)
        val sign = stealSignFlag(steal)
        val qExp = stealQexp(steal)
        val bitLen = stealBitLen(steal)
        val digitLen = stealDigitLen(steal)
        val dw0 = x.dw0
        val w0 = dw0.toInt()
        when {
            qExp == 0 -> {
                if (bitLen < 32)
                    return (w0 xor signMask) - signMask
                if (bitLen == 32 && w0 == Int.MIN_VALUE && sign)
                    return Int.MIN_VALUE
                // return signalInvalid
            }

            stealIsZER(steal) -> return 0
            qExp > 0 -> {
                // if there is headroom then scale it up
                if (digitLen + qExp <= 10) {
                    val result = x.dw0 * pow10_64(qExp)
                    if (result <= Int.MAX_VALUE.toLong())
                        return (result.toInt() xor signMask) - signMask
                    // Long.MIN_VALUE && sign is not possible ...
                    // ... because we just multiplied by 10**qExp
                    // ... so the value ends in 0
                    // ... but Long.MIN_VALUE ends in 8
                }
                // return signalInvalid
            }

            else -> { // qExp < 0
                // at least some fractional digits, perhaps 0 digits
                val fracDigitLen = -qExp
                if (fracDigitLen >= digitLen) {
                    // all fractional digits
                    val residue: Residue
                    if (fracDigitLen > digitLen)
                        residue = LT_HALF
                    else {
                        residue = Residue.fromValueDecade(x)
                        verify { residue != Residue.EXACT }
                    }
                    val roundUp = residue.ulpRoundUp(rounding.negate(sign), 0L)
                    val ret = if (!roundUp) 0 else (signMask shl 1) or 1
                    if (!suppressInexact)
                        ctx.signalInexact(x)
                    return ret
                }
                // both integral and fractional digits
                val intDigitLen = digitLen - fracDigitLen
                if (intDigitLen <= 10) {
                    val dwPair = ctx.tmps.dwQuad1
                    val residue = c128ScaleDownPow10(dwPair, x.dw1, dw0, fracDigitLen)
                    var r0 = dwPair.dw0
                    val roundUp01 = residue.ulpRoundUp01L(rounding.negate(sign), r0)
                    verify { dwPair.dw1 == 0L }
                    // r0 cannot roll over
                    // worse case is 10 9s 99999_99999 which rolls up to 11 digits
                    r0 += roundUp01
                    if (r0 <= Int.MAX_VALUE.toLong() ||
                        r0 == -(Int.MIN_VALUE.toLong()) && sign
                    ) {
                        val ret = (r0.toInt() xor signMask) - signMask
                        if (!suppressInexact && residue != EXACT)
                            ctx.signalInexact(x)
                        return ret
                    }
                }
                // return signalInvalid
            }
        }
    }
    // return signalInvalid
    ctx.signalInvalid(x)
    return Int.MIN_VALUE
}
