package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.Residue.Companion.LT_HALF

internal fun d128IsExactIntegral(x: Decimal): Boolean {
    val steal = x.steal
    when {
        stealIsFNZ(steal) -> {
            val qExp = stealQExp(steal)
            if (qExp >= 0)
                return true
            val q = -qExp
            if (stealDigitLen(steal) > q) {
                val t = DecContext.current().tmps.mdecDivRemPowCtzd.set(x)
                val ctzd = c256CountTrailingZeroDigitsAndIsOddDestructive(t) shr 1
                if (ctzd >= q)
                    return true
            }
        }
        stealIsZER(steal) -> return true
    }
    return false
}

internal fun d128IsOddIntegral(x: Decimal): Boolean {
    val xSteal = x.steal
    if (!stealIsFNZ(xSteal)) return false
    val qExp = stealQExp(xSteal)
    return when {
        qExp > 0 -> false  // coefficient * 10^qExp — always even since 10^qExp is even
        qExp == 0 -> (x.dw0 and 1L) != 0L  // check last digit of coefficient
        else -> {  // qExp < 0 — may have fractional part
            val q = -qExp
            val digitLen = stealDigitLen(xSteal)
            if (digitLen <= q) return false  // purely fractional, not an integer
            val ctx = DecContext.current()
            val t = ctx.tmps.mdecBridge1.set(x)
            val encoded = c256CountTrailingZeroDigitsAndIsOddDestructive(t)
            if (encoded == -1) return false  // zero
            val ctzd = encoded ushr 1
            val isOdd = (encoded and 1) != 0
            ctzd == q && isOdd  // is integer and odd
        }
    }
}

internal fun d128RoundToIntegral(x: Decimal, rounding: RoundingDirection, suppressInexact: Boolean = false): Decimal {
    val stealX = x.steal
    if (!stealIsFinite(stealX)) {
        if (stealIsSNAN(stealX))
            return nanOperandFound(x)
        return x
    }
    val qExp = stealQExp(stealX)
    if (qExp >= 0)
        return x
    val sign = stealSignFlag(stealX)
    if (x.isZero())
        return Decimal.zero(sign)
    val dw1 = x.dw1;
    val dw0 = x.dw0
    val pow10 = -qExp
    val ctx = DecContext.current()
    val pentadResult = ctx.tmps.pentad
    pentadResult.dw0 = 0L
    pentadResult.dw1 = 0L
    val digitLen = stealDigitLen(stealX)
    val residue: Residue = when {
        pow10 > digitLen -> LT_HALF
        pow10 == digitLen -> Residue.fromValuePow10(dw1, dw0, digitLen)
        else -> c128ScaleDownPow10(pentadResult, dw1, dw0, pow10)
    }
    return decRoundAndFinalizeFinite(
        sign, pentadResult.dw1, pentadResult.dw0,
        residue, 0, rounding, ctx, suppressInexact
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
internal fun d128ConvertToLong(x: Decimal, rounding: RoundingDirection,
                               suppressInexact: Boolean,
                               suppressInvalid: Boolean): Long {
    val steal = x.steal
    if (stealIsFinite(steal)) {
        val signMaskLong = stealSignMask(steal).toLong()
        val sign = stealSignFlag(steal)
        val qExp = stealQExp(steal)
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
                    val roundUp = residue.ulpRoundUp(rounding.forMagnitude(sign), 0L)
                    val ret = if (!roundUp) 0L else (signMaskLong shl 1) or 1L
                    if (!suppressInexact)
                        signalInexact(x)
                    return ret
                }
                // both integral and fractional digits
                val intDigitLen = digitLen - fracDigitLen
                if (intDigitLen <= 19) {
                    val dwPair = DecContext.current().tmps.pentad
                    val residue = c128ScaleDownPow10(dwPair, x.dw1, dw0, fracDigitLen)
                    // DANGER! CAUTION! r0 might roll over to ZEEERO with this roundUp
                    var r0 = dwPair.dw0
                    val roundUp01 = residue.ulpRoundUp01L(rounding.forMagnitude(sign), r0)
                    r0 += roundUp01
                    verify { dwPair.dw1 == 0L }
                    if (r0 > 0L || r0 == Long.MIN_VALUE && sign) {
                        val ret = (r0 xor signMaskLong) - signMaskLong
                        if (!suppressInexact && residue != EXACT)
                            signalInexact(ret)
                        return ret
                    }
                }
                // return signalInvalid
            }
        }
    }
    // return signalInvalid
    if (! suppressInvalid)
        signalInvalidOperation(InvalidCause.CONVERT_NON_FINITE_TO_INTEGER)
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
fun d128ConvertToInt(x: Decimal, rounding: RoundingDirection, suppressInexact: Boolean): Int {
    val steal = x.steal
    if (stealIsFinite(steal)) {
        val signMask = stealSignMask(steal)
        val sign = stealSignFlag(steal)
        val qExp = stealQExp(steal)
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
                    val roundUp = residue.ulpRoundUp(rounding.forMagnitude(sign), 0L)
                    val ret = if (!roundUp) 0 else (signMask shl 1) or 1
                    if (!suppressInexact)
                        signalInexact(x)
                    return ret
                }
                // both integral and fractional digits
                val intDigitLen = digitLen - fracDigitLen
                if (intDigitLen <= 10) {
                    val dwPair = DecContext.current().tmps.pentad
                    val residue = c128ScaleDownPow10(dwPair, x.dw1, dw0, fracDigitLen)
                    var r0 = dwPair.dw0
                    val roundUp01 = residue.ulpRoundUp01L(rounding.forMagnitude(sign), r0)
                    verify { dwPair.dw1 == 0L }
                    // r0 cannot roll over
                    // worse case is 10 9s 99999_99999 which rolls up to 11 digits
                    r0 += roundUp01
                    if (r0 <= Int.MAX_VALUE.toLong() ||
                        r0 == -(Int.MIN_VALUE.toLong()) && sign
                    ) {
                        val ret = (r0.toInt() xor signMask) - signMask
                        if (!suppressInexact && residue != EXACT)
                            signalInexact(x)
                        return ret
                    }
                }
                // return signalInvalid
            }
        }
    }
    // return signalInvalid
    val d = DecContext.current().signalInvalidOperation(InvalidCause.CONVERT_NON_FINITE_TO_INTEGER, x)
    // FIXME - do simple check of d and if it is an Int then return it
    return Int.MIN_VALUE
}
