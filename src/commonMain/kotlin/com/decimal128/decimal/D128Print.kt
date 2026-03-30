// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.DecPrefs.PrintStyle
import com.decimal128.decimal.IntegerParsePrint.int32ToUtf8
import com.decimal128.decimal.InvalidOperationReason.PARSE_INVALID_UNDERSCORE_LOCATION
import com.decimal128.decimal.InvalidOperationReason.PARSE_DOUBLE_DOT
import com.decimal128.decimal.InvalidOperationReason.PARSE_EMPTY_STRING
import com.decimal128.decimal.InvalidOperationReason.PARSE_MALFORMED
import com.decimal128.decimal.InvalidOperationReason.PARSE_NO_EXPONENT_DIGIT
import com.decimal128.decimal.InvalidOperationReason.PARSE_UNEXPECTED_CHAR
import com.decimal128.decimal.InvalidOperationReason.PARSE_VALUE_OUT_OF_RANGE
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object D128Print {
    private val SPECIAL_VALUE_STRINGS = arrayOf(
        "Infinity", "-Infinity", "Inf", "-Inf", "NaN", "-NaN", "sNaN", "-sNaN",
        "INFINITY", "-INFINITY", "INF", "-INF", "NAN", "-NAN", "SNAN", "-SNAN",
    )
    private const val SVS_BCE = 0x0F // Special Value Strings Bounds Check Elimination

    private val SMALL_INTEGER_STRINGS = arrayOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15",
        "-0", "-1", "-2", "-3", "-4", "-5", "-6", "-7",
        "-8", "-9", "-10", "-11", "-12", "-13", "-14", "-15"
    )

    fun toString(x: Decimal, ctx: DecContext): String {
        return d128ToString(x.steal, x.dw1, x.dw0, ctx)
    }

    fun d128ToString(steal: Int, dw1: Long, dw0: Long, ctx: DecContext): String {
        val prefs = ctx.decPrefs
        val utf8 = ctx.tmps.utf8BytesPrintOnly
        // a minus sign is always written
        // individual routines will overwrite it for non-negative values
        utf8[0] = '-'.code.toByte()
        if (stealIsFinite(steal)) {
            val printStyle = prefs.printStyle
            val exponentEUtf8Byte = (if (prefs.printExponentLowercaseE) 'e' else 'E').code.toByte()
            val printExponentPlusSign = prefs.printExponentPlusSign
            if (printStyle != PrintStyle.COEFFICIENT_QEXPONENT) {
                val qExp = stealQExp(steal)
                return when {
                    qExp == 0 -> toIntegerString(steal, dw1, dw0, utf8)
                    qExp < 0 && stealSciExp(steal) >= prefs.printMinPlainExponent ->
                        toDecimalPointString(steal, dw1, dw0, utf8)
                    printStyle != PrintStyle.ENGINEERING ->
                        toNormalizedScientificString(
                            steal,
                            dw1,
                            dw0,
                            exponentEUtf8Byte,
                            printExponentPlusSign,
                            utf8
                        )
                    else ->
                        toEngineeringString(steal, dw1, dw0, exponentEUtf8Byte, printExponentPlusSign, utf8)
                }
            } else {
                return toCoefficientQExponentString(steal, dw1, dw0,
                    exponentEUtf8Byte, printExponentPlusSign, utf8)
            }
        }
        var signBit = stealSignBit(steal)
        val caseOffset = if (prefs.printSpecialValueAllCaps) 8 else 0
        if (stealIsINF(steal)) {
            val infShortOffset = if (prefs.printInfinityShort3Char) 2 else 0
            return SPECIAL_VALUE_STRINGS[(caseOffset + infShortOffset + signBit) and SVS_BCE]
        }
        if (! prefs.printNaNMinusSign)
            signBit = 0
        val nanIndex = (if (stealIsQNAN(steal) || prefs.printCollapseSNaN) 4 else 6) + signBit
        val nanStr = SPECIAL_VALUE_STRINGS[(caseOffset + nanIndex) and SVS_BCE]
        val digitLen = stealDigitLen(steal)
        if (digitLen == 0 || !prefs.printNaNPayload)
            return nanStr
        val payloadNanLen = nanStr.length + digitLen
        for (i in nanStr.indices)
            utf8[i] = nanStr[i].code.toByte()
        IntegerParsePrint.u128ToUtf8(digitLen, dw1, dw0, utf8, nanStr.length)
        return utf8.decodeToString(0, payloadNanLen)
    }

    private fun toIntegerString(steal: Int, dw1: Long, dw0: Long, utf8: ByteArray): String {
        val signBit = stealSignBit(steal)
        if (stealBitLen(steal) < 4) {
            val i = ((signBit shl 4) + dw0.toInt()) and 0x1F // bounds-check-elimination
            return SMALL_INTEGER_STRINGS[i]
        }
        val signLen = signBit
        val digitLen = stealDigitLen(steal)
        verify { utf8[0] == '-'.code.toByte() }
        IntegerParsePrint.u128ToUtf8(digitLen, dw1, dw0, utf8, signLen)
        return utf8.decodeToString(0, signLen + digitLen)
    }

    private fun toDecimalPointString(steal: Int, dw1: Long, dw0: Long, utf8: ByteArray): String {
        val xQ = stealQExp(steal)
        val xDigitLen = stealDigitLen(steal)
        val digitsRightOfDecimal = -xQ
        val leadingZeroCount = max(1 + digitsRightOfDecimal - xDigitLen, 0)
        val signLen = stealSignBit(steal)
        val decimalPointLen = 1
        val totalLen = signLen + leadingZeroCount + decimalPointLen + xDigitLen
        verify { utf8[0] == '-'.code.toByte() }
        for (i in signLen..leadingZeroCount) // there is one extra here
            utf8[i] = '0'.code.toByte()
        IntegerParsePrint.u128ToUtf8(xDigitLen, dw1, dw0, utf8, signLen + leadingZeroCount)
        for (i in totalLen-1 downTo totalLen-digitsRightOfDecimal)
            utf8[i] = utf8[i - 1]
        utf8[totalLen - digitsRightOfDecimal - 1] = '.'.code.toByte()
        return utf8.decodeToString(0, totalLen)
    }

    private fun toNormalizedScientificString(steal: Int,
                                             dw1: Long,
                                             dw0: Long,
                                             exponentEUtf8Byte: Byte,
                                             printExponentPlusSign: Boolean,
                                             utf8: ByteArray): String {
        val eExp = stealSciExp(steal)
        val eExpAbs = (eExp xor (eExp shr 31)) - (eExp shr 31)
        val signLen = stealSignBit(steal)
        val xDigitLen = stealDigitLen(steal)
        val decimalPointLen = if (xDigitLen > 1) 1 else 0
        val printedDigitLen = xDigitLen + 1 - (-xDigitLen ushr 31)
        val expELen = 1
        val expSignLen = if (eExp < 0 || printExponentPlusSign) 1 else 0
        val expSignByte = (if (eExp < 0) '-' else '+').code.toByte()
        val expDigitLen = max(calcDigitLen64(eExpAbs.toLong()), 1)
        val totalLen = signLen + decimalPointLen + printedDigitLen + expELen + expSignLen + expDigitLen
        verify { utf8[0] == '-'.code.toByte() }
        IntegerParsePrint.u128ToUtf8(printedDigitLen, dw1, dw0, utf8, signLen + decimalPointLen)
        if (decimalPointLen > 0) {
            utf8[signLen] = utf8[signLen + 1]
            utf8[signLen + 1] = '.'.code.toByte()
        }
        val iE = signLen + decimalPointLen + printedDigitLen
        utf8[iE] = exponentEUtf8Byte
        utf8[iE + 1] = expSignByte // will get overwritten when expSignLen == 0
        val j = IntegerParsePrint.renderTailDigitsBeforeIndex(eExpAbs.toLong(), utf8, totalLen)
        verify { j == expDigitLen }
        return utf8.decodeToString(0, totalLen)
    }

    private fun toEngineeringString(steal: Int,
                                    dw1: Long,
                                    dw0: Long,
                                    exponentEUtf8Byte: Byte,
                                    printExponentPlusSign: Boolean,
                                    utf8: ByteArray): String {
        val digitLen = stealDigitLen(steal)
        val eExp = stealSciExp(steal)
        val signLen = stealSignBit(steal)

        return if (digitLen == 0)
            toEngineeringStringZero(eExp, signLen, exponentEUtf8Byte, printExponentPlusSign, utf8)
        else
            toEngineeringStringNonZero(steal, dw1, dw0, digitLen, eExp, signLen, exponentEUtf8Byte, printExponentPlusSign, utf8)
    }

    private fun toEngineeringStringZero(eExp: Int,
                                        signLen: Int,
                                        exponentEUtf8Byte: Byte,
                                        printExponentPlusSign: Boolean,
                                        utf8: ByteArray): String {
        // zero path — round exponent up to next multiple of 3
        val expAdjustment = (3 - ((eExp % 3) + 3) % 3) % 3
        val adjustedExp = eExp + expAdjustment

        // write '0.00' always ... truncate as needed
        utf8[signLen]     = '0'.code.toByte()
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
        val expLen = int32ToUtf8(adjustedExp, utf8, i)
        return utf8.decodeToString(0, i + expLen)
    }

    private fun toEngineeringStringNonZero(steal: Int,
                                           dw1: Long,
                                           dw0: Long,
                                           digitLen: Int,
                                           eExp: Int,
                                           signLen: Int,
                                           exponentEUtf8Byte: Byte,
                                           printExponentPlusSign: Boolean,
                                           utf8: ByteArray): String {
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

        IntegerParsePrint.u128ToUtf8(printedDigitLen, dw1, dw0, utf8, signLen + decimalPointLen)
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
        val j = int32ToUtf8(adjustedExp, utf8, i)
        verify { i + j == totalLen }
        return utf8.decodeToString(0, totalLen)
    }

    private fun toCoefficientQExponentString(steal: Int,
                                             dw1: Long,
                                             dw0: Long,
                                             exponentEUtf8Byte: Byte,
                                             printExponentPlusSign: Boolean,
                                             utf8: ByteArray): String {
        val digitLen = stealDigitLen(steal)
        val signLen = stealSignBit(steal)
        val qExp = stealQExp(steal)
        val printedDigitLen = max(digitLen, 1)
        val expELen = 1
        val expSignLen = if (qExp < 0 || printExponentPlusSign) 1 else 0
        val expDigitLen = max(calcDigitLen64(abs(qExp).toLong()), 1)
        val totalLen = signLen + printedDigitLen + expELen + expSignLen + expDigitLen
        verify { utf8[0] == '-'.code.toByte() }
        var i = IntegerParsePrint.u128ToUtf8(printedDigitLen, dw1, dw0, utf8, signLen)
        utf8[i++] = exponentEUtf8Byte
        if (printExponentPlusSign && qExp >= 0)
            utf8[i++] = '+'.code.toByte()
        val j = int32ToUtf8(qExp, utf8, i)
        verify { i + j == totalLen }
        return utf8.decodeToString(0, totalLen)
    }

}

