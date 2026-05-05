// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.IntegerParsePrint.int32ToASCII
import kotlin.math.abs
import kotlin.math.max

/**
 * Formats [x] as a string according to [ctx] preferences.
 *
 * For finite values, the format is selected by [DecPrefs.printStyle]:
 * - [FormatStyle.RAW] — integer coefficient plus quantum exponent
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
 * - [FormatStyle.RAW] — integer coefficient plus quantum exponent
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
    val utf8 = ctx.tmps.asciiBuffer
    // a minus sign is always written
    // individual routines will overwrite it for non-negative values
    utf8[0] = '-'.code.toByte()
    if (stealIsFinite(steal)) {
        val formatStyle = printPrefs.formatStyle
        val exponentEUtf8Byte = (if (printPrefs.exponentLowercaseE) 'e' else 'E').code.toByte()
        val printExponentPlusSign = printPrefs.exponentPlusSign
        return when (formatStyle) {
            FormatStyle.AUTO -> {
                val qExp = stealQExp(steal)
                when {
                    qExp == 0 -> toIntegerString(steal, dw1, dw0, utf8)
                    qExp < 0 && stealSciExp(steal) >= printPrefs.minPlainExponent ->
                        toDecimalPointString(steal, dw1, dw0, utf8)
                    else ->
                        toNormalizedScientificString(
                            steal, dw1, dw0,
                            exponentEUtf8Byte, printExponentPlusSign, utf8
                        )
                }
            }

            FormatStyle.EXPONENTIAL ->
                toNormalizedScientificString(
                    steal, dw1, dw0,
                    exponentEUtf8Byte, printExponentPlusSign, utf8
                )

            FormatStyle.ENGINEERING -> {
                val qExp = stealQExp(steal)
                when {
                    qExp == 0 -> toIntegerString(steal, dw1, dw0, utf8)
                    qExp < 0 && stealSciExp(steal) >= printPrefs.minPlainExponent ->
                        toDecimalPointString(steal, dw1, dw0, utf8)
                    else ->
                        toEngineeringString(
                            steal, dw1, dw0,
                            exponentEUtf8Byte, printExponentPlusSign, utf8
                        )
                }
            }

            FormatStyle.RAW ->
                toCoefficientQExponentString(
                    steal, dw1, dw0,
                    exponentEUtf8Byte, printExponentPlusSign, utf8
                )
        }
    }
    // ... non-finite handling unchanged
    var signBit = stealSignBit(steal)
    val caseOffset = printPrefs.specialsCase.ordinal shl 3
    if (stealIsINF(steal)) {
        val infShortOffset = if (printPrefs.infinityShort) 2 else 0
        return SPECIAL_VALUE_STRINGS[caseOffset + infShortOffset + signBit]
    }
    if (!printPrefs.nanMinusSign)
        signBit = 0
    val nanIndex = if (stealIsQNAN(steal) || printPrefs.collapseSNAN) 4 else 6
    val nanStr = SPECIAL_VALUE_STRINGS[caseOffset + nanIndex + signBit]
    val digitLen = stealDigitLen(steal)
    if (digitLen == 0 || !printPrefs.nanPayload)
        return nanStr
    val payloadNanLen = nanStr.length + digitLen
    for (i in nanStr.indices)
        utf8[i] = nanStr[i].code.toByte()
    IntegerParsePrint.u128ToASCII(digitLen, dw1, dw0, utf8, nanStr.length)
    return utf8.decodeToString(0, payloadNanLen)
}

private val SPECIAL_VALUE_STRINGS = arrayOf(
    "infinity", "-infinity", "inf", "-inf", "nan", "-nan", "snan", "-snan",
    "Infinity", "-Infinity", "Inf", "-Inf", "NaN", "-NaN", "sNaN", "-sNaN",
    "INFINITY", "-INFINITY", "INF", "-INF", "NAN", "-NAN", "SNAN", "-SNAN",
)

private val SMALL_INTEGER_STRINGS = arrayOf(
    "0", "1", "2", "3", "4", "5", "6", "7",
    "8", "9", "10", "11", "12", "13", "14", "15",

    "-0", "-1", "-2", "-3", "-4", "-5", "-6", "-7",
    "-8", "-9", "-10", "-11", "-12", "-13", "-14", "-15"
)

/**
 * Formats a decimal with quantum exponent zero as a plain integer string
 * (e.g. `123`), writing into [utf8] and returning the result.
 *
 * Small values are returned directly from a lookup table without touching [utf8].
 *
 * @param steal packed sign, bit length, and digit length
 * @param dw1 high 64 bits of the coefficient
 * @param dw0 low 64 bits of the coefficient
 * @param utf8 scratch buffer; index 0 must contain `'-'`
 */
private fun toIntegerString(steal: Int, dw1: Long, dw0: Long, utf8: ByteArray): String {
    val signBit = stealSignBit(steal)
    if (stealBitLen(steal) < 4) {
        val i = ((signBit shl 4) + dw0.toInt()) and 0x1F // bounds-check-elimination
        return SMALL_INTEGER_STRINGS[i]
    }
    val signLen = signBit
    val digitLen = stealDigitLen(steal)
    verify { utf8[0] == '-'.code.toByte() }
    IntegerParsePrint.u128ToASCII(digitLen, dw1, dw0, utf8, signLen)
    return utf8.decodeToString(0, signLen + digitLen)
}

/**
 * Formats a decimal as a plain decimal-point string (e.g. `0.00123`),
 * writing into [utf8] and returning the result.
 *
 * No exponent is written; leading zeros are inserted as needed to position
 * the radix point correctly.
 *
 * @param steal packed sign, digit length, and quantum exponent
 * @param dw1 high 64 bits of the coefficient
 * @param dw0 low 64 bits of the coefficient
 * @param utf8 scratch buffer; index 0 must contain `'-'`
 */
private fun toDecimalPointString(steal: Int, dw1: Long, dw0: Long, utf8: ByteArray): String {
    val qExp = stealQExp(steal)
    val digitLen = stealDigitLen(steal)
    val digitsRightOfDecimal = -qExp
    val leadingZeroCount = max(1 + digitsRightOfDecimal - digitLen, 0)
    val signLen = stealSignBit(steal)
    val decimalPointLen = 1
    val totalLen = signLen + leadingZeroCount + decimalPointLen + digitLen
    verify { utf8[0] == '-'.code.toByte() }
    for (i in signLen..leadingZeroCount) // there is one extra here
        utf8[i] = '0'.code.toByte()
    IntegerParsePrint.u128ToASCII(digitLen, dw1, dw0, utf8, signLen + leadingZeroCount)
    for (i in totalLen - 1 downTo totalLen - digitsRightOfDecimal)
        utf8[i] = utf8[i - 1]
    utf8[totalLen - digitsRightOfDecimal - 1] = '.'.code.toByte()
    return utf8.decodeToString(0, totalLen)
}

/**
 * Formats a decimal as a normalized scientific string (e.g. `1.23E+4`),
 * writing into [utf8] and returning the result.
 *
 * The coefficient is formatted with exactly one digit left of the radix point.
 *
 * @param steal packed sign, digit length, and scientific exponent
 * @param dw1 high 64 bits of the coefficient
 * @param dw0 low 64 bits of the coefficient
 * @param exponentEUtf8Byte the exponent separator byte, e.g. `'E'` or `'e'`
 * @param printExponentPlusSign if `true`, a `+` is written before non-negative exponents
 * @param utf8 scratch buffer; index 0 must contain `'-'`
 */
private fun toNormalizedScientificString(
    steal: Int,
    dw1: Long,
    dw0: Long,
    exponentEUtf8Byte: Byte,
    printExponentPlusSign: Boolean,
    utf8: ByteArray
): String {
    val eExp = stealSciExp(steal)
    val eExpAbs = (eExp xor (eExp shr 31)) - (eExp shr 31)
    val signLen = stealSignBit(steal)
    val digitLen = stealDigitLen(steal)
    val decimalPointLen = if (digitLen > 1) 1 else 0
    val printedDigitLen = digitLen + 1 - (-digitLen ushr 31)
    val expELen = 1
    val expSignLen = if (eExp < 0 || printExponentPlusSign) 1 else 0
    val expSignByte = (if (eExp < 0) '-' else '+').code.toByte()
    val expDigitLen = max(calcDigitLen64(eExpAbs.toLong()), 1)
    val totalLen = signLen + decimalPointLen + printedDigitLen + expELen + expSignLen + expDigitLen
    verify { utf8[0] == '-'.code.toByte() }
    IntegerParsePrint.u128ToASCII(printedDigitLen, dw1, dw0, utf8, signLen + decimalPointLen)
    if (decimalPointLen > 0) {
        utf8[signLen] = utf8[signLen + 1]
        utf8[signLen + 1] = '.'.code.toByte()
    }
    val iE = signLen + decimalPointLen + printedDigitLen
    utf8[iE] = exponentEUtf8Byte
    utf8[iE + 1] = expSignByte // will get overwritten when expSignLen == 0
    IntegerParsePrint.renderTailDigitsBeforeIndex(expDigitLen, eExpAbs.toLong(), utf8, totalLen)
    return utf8.decodeToString(0, totalLen)
}

/**
 * Formats a decimal as an engineering string per the General Decimal Arithmetic
 * specification, writing into [utf8] and returning the result.
 *
 * Dispatches to [toEngineeringStringZero] or [toEngineeringStringNonZero]
 * depending on whether the coefficient is zero.
 *
 * @param steal packed sign, digit length, and scientific exponent
 * @param dw1 high 64 bits of the coefficient
 * @param dw0 low 64 bits of the coefficient
 * @param exponentEUtf8Byte the exponent separator byte, e.g. `'E'` or `'e'`
 * @param printExponentPlusSign if `true`, a `+` is written before non-negative exponents
 * @param utf8 scratch buffer; index 0 must contain `'-'`
 */
private fun toEngineeringString(
    steal: Int,
    dw1: Long,
    dw0: Long,
    exponentEUtf8Byte: Byte,
    printExponentPlusSign: Boolean,
    utf8: ByteArray
): String {
    val digitLen = stealDigitLen(steal)
    val eExp = stealSciExp(steal)
    val signLen = stealSignBit(steal)

    return if (digitLen == 0)
        toEngineeringStringZero(eExp, signLen, exponentEUtf8Byte, printExponentPlusSign, utf8)
    else
        toEngineeringStringNonZero(
            steal,
            dw1,
            dw0,
            digitLen,
            eExp,
            signLen,
            exponentEUtf8Byte,
            printExponentPlusSign,
            utf8
        )
}

/**
 * Formats a zero decimal as an engineering string per
 * General Decimal Arithmetic Specification,
 * writing into [utf8] and returning the result.
 *
 * The exponent is rounded up to the next multiple of three, and the
 * coefficient is written as `0`, `0.0`, or `0.00` accordingly. If the
 * adjusted exponent is zero, no exponent part is written.
 *
 * @param eExp the full (non-adjusted) exponent
 * @param signLen 1 if negative, 0 otherwise
 * @param exponentEUtf8Byte the exponent separator byte, e.g. `'E'` or `'e'`
 * @param printExponentPlusSign if `true`, a `+` is written before non-negative exponents
 * @param utf8 scratch buffer; index 0 must contain `'-'`
 */
private fun toEngineeringStringZero(
    eExp: Int,
    signLen: Int,
    exponentEUtf8Byte: Byte,
    printExponentPlusSign: Boolean,
    utf8: ByteArray
): String {
    // zero path — round exponent up to next multiple of 3
    val expAdjustment = (3 - ((eExp % 3) + 3) % 3) % 3
    val adjustedExp = eExp + expAdjustment

    // write '0.00' always ... truncate as needed
    utf8[signLen] = '0'.code.toByte()
    utf8[signLen + 1] = '.'.code.toByte()
    utf8[signLen + 2] = '0'.code.toByte()
    utf8[signLen + 3] = '0'.code.toByte()

    // truncate based on expAdjustment
    // expAdjustment == 0 → "0"     (just the '0')
    // expAdjustment == 1 → "0.0"   (one trailing zero)
    // expAdjustment == 2 → "0.00"  (two trailing zeros)
    var i = signLen + 1 + if (expAdjustment == 0) 0 else 1 + expAdjustment

    if (adjustedExp == 0)
        return utf8.decodeToString(0, i)

    utf8[i++] = exponentEUtf8Byte
    if (printExponentPlusSign && adjustedExp >= 0)
        utf8[i++] = '+'.code.toByte()
    val expLen = int32ToASCII(adjustedExp, utf8, i)
    return utf8.decodeToString(0, i + expLen)
}

/**
 * Formats a non-zero finite decimal as an engineering string per
 * General Decimal Arithmetic Specification,
 * writing into [utf8] and returning the result.
 *
 * Engineering notation adjusts the exponent to a multiple of three, placing
 * one to three digits left of the radix point (e.g. `123E+6`, `12.3E+6`,
 * `1.23E+6`). If the adjusted exponent is zero, no exponent part is written.
 *
 * @param steal packed sign, digit length, and quantum exponent — no, wait...
 * @param dw1 high 64 bits of the coefficient
 * @param dw0 low 64 bits of the coefficient
 * @param digitLen number of significant digits in the coefficient
 * @param eExp the full (non-adjusted) exponent
 * @param signLen 1 if negative, 0 otherwise
 * @param exponentEUtf8Byte the exponent separator byte, e.g. `'E'` or `'e'`
 * @param printExponentPlusSign if `true`, a `+` is written before non-negative exponents
 * @param utf8 scratch buffer; index 0 must contain `'-'`
 */
private fun toEngineeringStringNonZero(
    steal: Int,
    dw1: Long,
    dw0: Long,
    digitLen: Int,
    eExp: Int,
    signLen: Int,
    exponentEUtf8Byte: Byte,
    printExponentPlusSign: Boolean,
    utf8: ByteArray
): String {
    val expAdjustment = (if (eExp >= 0) eExp else ((eExp % 3) + 3)) % 3
    val leftOfRadixPointCount = 1 + expAdjustment
    val adjustedExp = eExp - expAdjustment
    val expAlignZeroCount = max(0, 1 + expAdjustment - digitLen)
    val decimalPointLen = if (digitLen > leftOfRadixPointCount) 1 else 0
    val printedDigitLen = digitLen
    val expSignLen = if (adjustedExp < 0 || printExponentPlusSign) 1 else 0
    val expDigitLen = max(calcDigitLen64(abs(adjustedExp).toLong()), 1)
    val totalLen = signLen + decimalPointLen + expAlignZeroCount +
            printedDigitLen + (if (adjustedExp != 0) 1 + expSignLen + expDigitLen else 0)

    IntegerParsePrint.u128ToASCII(printedDigitLen, dw1, dw0, utf8, signLen + decimalPointLen)
    var i = signLen + decimalPointLen + printedDigitLen

    when {
        expAlignZeroCount > 0 -> {
            repeat(expAlignZeroCount) { utf8[i++] = '0'.code.toByte() }
        }

        digitLen > leftOfRadixPointCount -> {
            for (j in 0..<leftOfRadixPointCount)
                utf8[signLen + j] = utf8[signLen + j + 1]
            utf8[signLen + leftOfRadixPointCount] = '.'.code.toByte()
        }
    }

    if (adjustedExp == 0)
        return utf8.decodeToString(0, i)

    utf8[i++] = exponentEUtf8Byte
    if (printExponentPlusSign && adjustedExp >= 0)
        utf8[i++] = '+'.code.toByte()
    val j = int32ToASCII(adjustedExp, utf8, i)
    verify { i + j == totalLen }
    return utf8.decodeToString(0, totalLen)
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
private fun toCoefficientQExponentString(
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
    val printedDigitLen = max(digitLen, 1)
    val expELen = 1
    val expSignLen = if (qExp < 0 || printExponentPlusSign) 1 else 0
    val expDigitLen = max(calcDigitLen64(abs(qExp).toLong()), 1)
    val totalLen = signLen + printedDigitLen + expELen + expSignLen + expDigitLen
    verify { utf8[0] == '-'.code.toByte() }
    var i = signLen
    i += IntegerParsePrint.u128ToASCII(printedDigitLen, dw1, dw0, utf8, signLen)
    utf8[i++] = exponentEUtf8Byte
    if (printExponentPlusSign && qExp >= 0)
        utf8[i++] = '+'.code.toByte()
    val j = int32ToASCII(qExp, utf8, i)
    verify { i + j == totalLen }
    return utf8.decodeToString(0, totalLen)
}

