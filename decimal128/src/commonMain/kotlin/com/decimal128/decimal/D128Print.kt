// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.IntegerParsePrint.int32ToASCII
import com.decimal128.decimal.IntegerParsePrint.u64ToASCII
import com.decimal128.decimal.IntegerParsePrint.uIntToASCII
import kotlin.math.abs
import kotlin.math.max

/**
 * Formats [x] as a string according to [ctx] preferences.
 *
 * For finite values, the format is selected by [DecPrefs.printStyle]:
 * - [FormatStyle.COEFFICIENT_QEXP] — integer coefficient plus quantum exponent
 * - [FormatStyle.ENGINEERING] — engineering notation per the General Decimal Arithmetic Spec
 * - Otherwise — plain decimal-point or normalized scientific notation, depending on
 *   the exponent and [DecPrefs.printMinPlainExponent]
 *
 * For special values, casing, sign, and payload rendering are controlled by
 * the relevant [DecPrefs] flags.
 *
 *
 * @param x [Decimal] value
 * @param ctx formatting preferences and scratch buffers
 * @return the formatted string
 */
internal fun d128ToString(x: Decimal, ctx: DecContext): String {
    return d128ToString(x.steal, x.dw1, x.dw0, ctx)
}

/**
 * Formats a Decimal128 value as a string according to [ctx] preferences.
 *
 * For finite values, the format is selected by [DecPrefs.printStyle]:
 * - [FormatStyle.COEFFICIENT_QEXP] — integer coefficient plus quantum exponent
 * - [FormatStyle.ENGINEERING] — engineering notation per the General Decimal Arithmetic specification
 * - Otherwise — plain decimal-point or normalized scientific notation, depending on
 *   the exponent and [DecPrefs.printMinPlainExponent]
 *
 * For special values, casing, sign, and payload rendering are controlled by
 * the relevant [DecPrefs] flags.
 *
 * @param steal packed sign, finite/special flag, digit length, and exponent
 * @param dw1 high 64 bits of the coefficient or NaN payload
 * @param dw0 low 64 bits of the coefficient or NaN payload
 * @param ctx formatting preferences and scratch buffers
 * @return the formatted string
 */
internal fun d128ToString(steal: Int, dw1: Long, dw0: Long, ctx: DecContext): String {
    val printPrefs = ctx.printPrefs
    val ascii = ctx.tmps.asciiBuffer
    check(ascii.size >= ASCII_SIZE)
    // a minus sign is always written
    // individual routines will overwrite it for non-negative values
    ascii[0] = '-'.code.toByte()
    if (stealIsFIN(steal)) {
        return toFiniteString(steal, dw1, dw0, printPrefs, ascii)
    } else {
        return toNonFiniteString(steal, dw1, dw0, printPrefs, ascii)
    }
}

private val SPECIAL_VALUE_STRINGS = arrayOf(
    "infinity", "-infinity", "inf", "-inf", "nan", "-nan", "snan", "-snan",
    "Infinity", "-Infinity", "Inf", "-Inf", "NaN", "-NaN", "sNaN", "-sNaN",
    "INFINITY", "-INFINITY", "INF", "-INF", "NAN", "-NAN", "SNAN", "-SNAN",
)

private fun toNonFiniteString(steal: Int, dw1: Long, dw0: Long, printPrefs: PrintPrefs, ascii: AsciiBuffer): String {
    // non-finite
    val signBit = stealSignBit(steal)
    val caseOffset = printPrefs.specialsCase.ordinal shl 3
    if (stealIsINF(steal)) {
        val infShortOffset = if (printPrefs.infinityShort) 2 else 0
        return SPECIAL_VALUE_STRINGS[caseOffset + infShortOffset + signBit]
    }
    val effectiveSignBit = if (printPrefs.nanMinusSign) signBit else 0
    val nanIndex = if (stealIsQNAN(steal) || printPrefs.collapseSNAN) 4 else 6
    val nanStr = SPECIAL_VALUE_STRINGS[caseOffset + nanIndex + effectiveSignBit]
    val digitLen = stealDigitLen(steal)
    if (digitLen == 0 || !printPrefs.nanPayload)
        return nanStr
    val payloadNanLen = nanStr.length + digitLen
    for (i in nanStr.indices)
        ascii[i] = nanStr[i].code.toByte()
    IntegerParsePrint.u128ToASCII(digitLen, dw1, dw0, ascii, nanStr.length)
    return ascii.decodeToString(0, payloadNanLen)
}

private inline fun toFiniteString(steal: Int, dw1: Long, dw0: Long, printPrefs: PrintPrefs, ascii: AsciiBuffer): String {
    val formatStyle = printPrefs.formatStyle
    val expEByte = (if (printPrefs.exponentLowercaseE) 'e' else 'E').code.toByte()
    val expPlusSign = printPrefs.exponentPlusSign
    if (formatStyle == FormatStyle.COEFFICIENT_QEXP) {
        return toCoefficientQExpString(steal, dw1, dw0, expEByte, expPlusSign, ascii)
    }
    if (formatStyle == FormatStyle.EXPONENTIAL) {
        return toExponentialString(steal, dw1, dw0, expEByte, expPlusSign, ascii)
    }
    val qExp = stealQExp(steal)
    if (qExp == 0) {
        return toIntegerString(steal, dw1, dw0, ascii)
    }
    val sciExp = stealSciExp(steal)
    if (qExp < 0 && sciExp >= printPrefs.minPlainExponent) {
        return toDecimalPointString(steal, dw1, dw0, ascii)
    }
    if (formatStyle == FormatStyle.AUTO) {
        return toExponentialString(steal, dw1, dw0, expEByte, expPlusSign, ascii)
    }
    verify { formatStyle == FormatStyle.ENGINEERING }
    return toEngineeringString(steal, dw1, dw0, expEByte, expPlusSign, ascii)
}

private val SMALL_INTEGER_STRINGS = arrayOf(
    "0", "1", "2", "3", "4", "5", "6", "7",
    "8", "9", "10", "11", "12", "13", "14", "15",

    "-0", "-1", "-2", "-3", "-4", "-5", "-6", "-7",
    "-8", "-9", "-10", "-11", "-12", "-13", "-14", "-15"
)

/**
 * Formats a decimal with quantum exponent zero as a plain integer string
 * (e.g. `123`), writing into [ascii] and returning the result.
 *
 * Small values are returned directly from a lookup table without touching [ascii].
 *
 * @param steal packed sign, bit length, and digit length
 * @param dw1 high 64 bits of the coefficient
 * @param dw0 low 64 bits of the coefficient
 * @param ascii scratch buffer; index 0 must contain `'-'`
 */
private fun toIntegerString(steal: Int, dw1: Long, dw0: Long, ascii: AsciiBuffer): String {
    val signBit = stealSignBit(steal)
    if (stealBitLen(steal) < 4) {
        val i = ((signBit shl 4) + dw0.toInt()) and 0x1F // bounds-check-elimination
        return SMALL_INTEGER_STRINGS[i]
    }
    val signLen = signBit
    val digitLen = stealDigitLen(steal)
    verify { ascii[0] == '-'.code.toByte() }
    IntegerParsePrint.u128ToASCII(digitLen, dw1, dw0, ascii, signLen)
    return ascii.decodeToString(0, signLen + digitLen)
}

/**
 * Formats a decimal as a plain decimal-point string (e.g. `0.00123`),
 * writing into [ascii] and returning the result.
 *
 * No exponent is written; leading zeros are inserted as needed to position
 * the radix point correctly.
 *
 * @param steal packed sign, digit length, and quantum exponent
 * @param dw1 high 64 bits of the coefficient
 * @param dw0 low 64 bits of the coefficient
 * @param ascii scratch buffer; index 0 must contain `'-'`
 */
private fun toDecimalPointString(steal: Int, dw1: Long, dw0: Long, ascii: ByteArray): String {
    val qExp = stealQExp(steal)
    val digitLen = stealDigitLen(steal)
    val digitsRightOfDecimal = -qExp
    val leadingZeroCount = max(1 + digitsRightOfDecimal - digitLen, 0)
    val signLen = stealSignBit(steal)
    val decimalPointLen = 1
    val totalLen = signLen + leadingZeroCount + decimalPointLen + digitLen
    verify { ascii[0] == '-'.code.toByte() }
    var i = signLen
    while (i <= leadingZeroCount) { // there is one extra here
        ascii[i] = '0'.code.toByte()
        i += 1
    }
    IntegerParsePrint.u128ToASCII(digitLen, dw1, dw0, ascii, signLen + leadingZeroCount)
    val iDot = totalLen - digitsRightOfDecimal - 1
    i = totalLen - 1
    while (i > iDot) {
        ascii[i] = ascii[i - 1]
        i -= 1
    }
    ascii[iDot] = '.'.code.toByte()
    return ascii.decodeToString(0, totalLen)
}

/**
 * Formats a decimal as a normalized scientific string (e.g. `1.23E+4`),
 * writing into [ascii] and returning the result.
 *
 * The coefficient is formatted with exactly one digit left of the radix point.
 *
 * @param steal packed sign, digit length, and scientific exponent
 * @param dw1 high 64 bits of the coefficient
 * @param dw0 low 64 bits of the coefficient
 * @param expEByte the exponent separator byte, e.g. `'E'` or `'e'`
 * @param expPlusSign if `true`, a `+` is written before non-negative exponents
 * @param ascii scratch buffer; index 0 must contain `'-'`
 */
private fun toExponentialString(
    steal: Int,
    dw1: Long,
    dw0: Long,
    expEByte: Byte,
    expPlusSign: Boolean,
    ascii: ByteArray
): String {
    val eExp = stealSciExp(steal)
    val eExpAbs = (eExp xor (eExp shr 31)) - (eExp shr 31)
    val signLen = stealSignBit(steal)
    val digitLen = stealDigitLen(steal)
    val decimalPointLen = if (digitLen > 1) 1 else 0
    val printedDigitLen = digitLen + 1 + (-digitLen shr 31)
    val expELen = 1
    val expSignLen = if (eExp < 0 || expPlusSign) 1 else 0
    val expSignByte = (if (eExp < 0) '-' else '+').code.toByte()
    val expDigitLen = max(calcDigitLenInt(eExpAbs), 1)
    val totalLen = signLen + decimalPointLen + printedDigitLen + expELen + expSignLen + expDigitLen
    verify { ascii[0] == '-'.code.toByte() }
    IntegerParsePrint.u128ToASCII(printedDigitLen, dw1, dw0, ascii, signLen + decimalPointLen)
    if (decimalPointLen > 0) {
        ascii[signLen] = ascii[signLen + 1]
        ascii[signLen + 1] = '.'.code.toByte()
    }
    val iE = signLen + decimalPointLen + printedDigitLen
    ascii[iE] = expEByte
    ascii[iE + 1] = expSignByte // will get overwritten when expSignLen == 0
    IntegerParsePrint.renderTailDigitsBeforeIndex(expDigitLen, eExpAbs.toLong(), ascii, totalLen)
    return ascii.decodeToString(0, totalLen)
}

/**
 * Formats a decimal as an engineering string per the General Decimal Arithmetic
 * specification, writing into [ascii] and returning the result.
 *
 * Be advised that dealing with ZER coefficient is quite different from
 * a FNZ coefficient.
 *
 * For a ZER coefficient ...
 * The exponent is rounded up to the next multiple of three, and the
 * coefficient is written as `0`, `0.0`, or `0.00` accordingly. If the
 * adjusted exponent is zero, no exponent part is written.
 *
 * for a FNZ coefficient ...
 * Engineering notation adjusts the exponent to a multiple of three, placing
 * one to three digits left of the radix point (e.g. `123E+6`, `12.3E+6`,
 * `1.23E+6`). If the adjusted exponent is zero, no exponent part is written.
 *
 * @param steal packed sign, digit length, and scientific exponent
 * @param dw1 high 64 bits of the coefficient
 * @param dw0 low 64 bits of the coefficient
 * @param expEByte the exponent separator byte, e.g. `'E'` or `'e'`
 * @param expPlusSign if `true`, a `+` is written before non-negative exponents
 * @param ascii scratch buffer; index 0 must contain `'-'`
 */
private fun toEngineeringString(
    steal: Int,
     dw1: Long,
     dw0: Long,
     expEByte: Byte,
     expPlusSign: Boolean,
     ascii: AsciiBuffer
): String {
    val digitLen = stealDigitLen(steal)
    val eExp = stealSciExp(steal)
    val signLen = stealSignBit(steal)

    val isZero = digitLen == 0
    var zerExpAdjustment = 0
    var fnzExpAdjustment = 0
    // expAdjustment differs for a zero/fnz coefficient
    if (isZero) {
        zerExpAdjustment = (3 - ((eExp % 3) + 3) % 3) % 3
    } else {
        fnzExpAdjustment = (if (eExp >= 0) eExp else ((eExp % 3) + 3)) % 3
    }
    val adjustedExp = eExp + zerExpAdjustment - fnzExpAdjustment

    val adjustedExpMask = adjustedExp shr 31
    val adjustedExpAbs = (adjustedExp xor adjustedExpMask) - adjustedExpMask
    val expSign = (if (adjustedExp < 0) '-' else '+').code.toByte()
    val expSignLen = if (eExp < 0 || expPlusSign) 1 else 0
    val expDigitLen = max(calcDigitLenInt(adjustedExpAbs), 1)
    var i: Int
    if (isZero) {
        // write '0.00' always ... truncate as needed
        ascii[signLen] = '0'.code.toByte()
        ascii[signLen + 1] = '.'.code.toByte()
        ascii[signLen + 2] = '0'.code.toByte()
        ascii[signLen + 3] = '0'.code.toByte()

        // truncate based on expAdjustment
        // expAdjustment == 0 → "0"     (just the '0')
        // expAdjustment == 1 → "0.0"   (one trailing zero)
        // expAdjustment == 2 → "0.00"  (two trailing zeros)
        i = signLen + 1 + if (zerExpAdjustment == 0) 0 else 1 + zerExpAdjustment
    } else {
        // non-zero coefficient ... 1 or more digits to left of decimal point
        val expAlignZeroCount = max(0, 1 + fnzExpAdjustment - digitLen)
        val leftOfRadixPointCount = 1 + fnzExpAdjustment
        val decimalPointLen = if (digitLen > leftOfRadixPointCount) 1 else 0
        val printedDigitLen = digitLen

        IntegerParsePrint.u128ToASCII(printedDigitLen, dw1, dw0, ascii, signLen + decimalPointLen)
        i = signLen + decimalPointLen + printedDigitLen

        if (expAlignZeroCount > 0) {
                repeat(expAlignZeroCount) { ascii[i++] = '0'.code.toByte() }
        } else if (digitLen > leftOfRadixPointCount) {
            for (j in 0..<leftOfRadixPointCount)
                ascii[signLen + j] = ascii[signLen + j + 1]
            ascii[signLen + leftOfRadixPointCount] = '.'.code.toByte()
        }
    }
    if (adjustedExp == 0)
        return ascii.decodeToString(0, i)
    ascii[i] = expEByte; i += 1
    ascii[i] = expSign; i += expSignLen
    u64ToASCII(expDigitLen, adjustedExpAbs.toLong(), ascii, i)
    val totalLen = i + expDigitLen
    return ascii.decodeToString(0, totalLen)
}

/**
 * Formats a decimal as an integer coefficient plus quantum exponent
 * (e.g. `-123E+4`) into [utf8] and returns the result.
 *
 * [steal] encodes the sign, digit length, and quantum exponent via [stealSignBit],
 * [stealDigitLen], and [stealQExp]. [utf8] must be large enough to hold the
 * result and pre-populated with a `'-'` at index 0.
 *
 * @param steal packed sign, digit length, and quantum exponent
 * @param dw1 high 64 bits of the coefficient
 * @param dw0 low 64 bits of the coefficient
 * @param exponentEUtf8Byte the exponent separator byte, e.g. `'E'` or `'e'`
 * @param printExponentPlusSign if `true`, a `+` is written before non-negative exponents
 * @param utf8 scratch buffer; index 0 must contain `'-'`
 */
private fun toCoefficientQExpString(
    steal: Int,
    dw1: Long,
    dw0: Long,
    exponentEUtf8Byte: Byte,
    printExponentPlusSign: Boolean,
    utf8: ByteArray
): String {
    val digitLen = stealDigitLen(steal)
    val signLen = stealSignBit(steal)
    val qExp = stealQExp(steal)
    val qExpAbs = abs(qExp)
    val printedDigitLen = max(digitLen, 1)
    val expELen = 1
    val expSignLen = if (qExp < 0 || printExponentPlusSign) 1 else 0
    val expSignByte = if (qExp < 0) '-'.code.toByte() else '+'.code.toByte()
    val expDigitLen = max(calcDigitLen64(qExpAbs.toLong()), 1)
    val totalLen = signLen + printedDigitLen + expELen + expSignLen + expDigitLen
    verify { utf8[0] == '-'.code.toByte() }
    var i = signLen
    i += IntegerParsePrint.u128ToASCII(printedDigitLen, dw1, dw0, utf8, signLen)
    utf8[i++] = exponentEUtf8Byte
    utf8[i] = expSignByte
    i += expSignLen
    val j = int32ToASCII(qExpAbs, utf8, i)
    verify { i + j == totalLen }
    return utf8.decodeToString(0, totalLen)
}

