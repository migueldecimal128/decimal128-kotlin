package com.decimal128.decimal

import com.decimal128.decimal.U256Bits.calcBitLen128
import com.decimal128.decimal.U256Pow10.calcDigitLen128
import com.decimal128.decimal.U256Pow10.calcDigitLen64
import com.decimal128.hugeint.Magia
import kotlin.math.max

object Dec2ParsePrint {
    private val SPECIAL_VALUE_STRINGS = arrayOf(
        "Infinity", "-Infinity", "NaN", "-NaN", "sNaN", "-sNaN"
    )

    private val SMALL_INTEGER_STRINGS = arrayOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15",
        "-0", "-1", "-2", "-3", "-4", "-5", "-6", "-7",
        "-8", "-9", "-10", "-11", "-12", "-13", "-14", "-15"
    )

    fun parseInfinityText(str: String): Dec2? {
        if (str.length < 3)
            return null
        var ch = str[0].code
        var ich = 1
        val sign = ch == '-'.code
        if (ch == '-'.code || ch == '+'.code)
            ch = str[ich++].code
        ch = ch or 0x20
        for (target in "infinity") {
            if (ch != target.code)
                return null
            if (ich == str.length)
                break
            ch = str[ich++].code or 0x20
        }
        if (ich != str.length || (ch != 'f'.code && ch != 'y'.code))
            return null
        return Dec2.infinity(sign)
    }

    inline fun tenPow(n: Int): ULong {
        var exp = n
        var pow = 1uL
        var base = 10uL
        while (exp != 0) {
            if ((exp and 1) != 0)
                pow *= base
            exp = exp ushr 1
            if (exp != 0)
                base *= base
        }
        return pow
    }

    fun parseNanText(str: String): Dec2? {
        if (str.length < 3)
            return null
        var ch = str[0].code
        var ich = 1
        val sign = ch == '-'.code
        if (ch == '-'.code || ch == '+'.code)
            ch = str[ich++].code
        val hasS = (ch or 0x20) == 's'.code
        if (hasS)
            ch = str[ich++].code
        if ((str.length - ich) < 2)
            return null
        if (((ch or 0x20) != 'n'.code) or
            ((str[ich].code or 0x20) != 'a'.code) or
            ((str[ich+1].code or 0x20) != 'n'.code))
            return null
        ich += 2
        var payloadDigitCount = 0
        var accumulator19 = 0uL
        var accumulator34 = 0uL
        while (ich < str.length) {
            val chDigit = str[ich++]
            if (chDigit !in '0'..'9')
                return null
            val d = (chDigit - '0').toULong()
            if (payloadDigitCount > 0 || d != 0uL) {
                when {
                    payloadDigitCount < 19 -> accumulator19 = (accumulator19 * 10uL) + d
                    payloadDigitCount < 34 -> accumulator34 = (accumulator34 * 10uL) + d
                    else -> return null
                }
                ++payloadDigitCount
            }
        }
        var payloadDw1 = 0uL
        var payloadDw0 = accumulator19
        if (payloadDigitCount > 19) {
            val m = tenPow(payloadDigitCount - 19)
            payloadDw0 = accumulator19 * m
            payloadDw1 = unsignedMulHi(accumulator19, m)
            payloadDw0 += accumulator34
            payloadDw1 += if (payloadDw0 < accumulator34) 1uL else 0uL
        }
        return Dec2.NaN(sign, hasS, payloadDw1, payloadDw0)
    }

    fun parseFiniteValueText(str: String): Dec2? {
        var hasCoefficientDigit = false
        var significantDigitCount = 0 // does not count leading zeros
        var hasDot = false
        var hasExp = false
        var hasExpDigit = false
        var expSign = false
        var expSignificantDigitCount = 0

        if (str.length == 0)
            return null
        var ch = str[0]
        var ich = 1
        var chLast = '\u0000'

        val sign = ch == '-'
        if (ch == '-' || ch == '+') {
            if (ich == str.length)
                return null
            ch = str[ich++]
        }
        var fractionalDigitCount = 0
        var coeff19 = 0uL
        var coeff34 = 0uL
        var exp = 0

        while (ch in '0'..'9' || ch == '.' || ch == '_') {
            when {
                ch in '0'..'9' -> {
                    val n = ch - '0'
                    hasCoefficientDigit = true
                    // increment if we have seen other digits or n != 0
                    significantDigitCount += (-(significantDigitCount or n)) ushr 31
                    if (significantDigitCount <= 19) {
                        coeff19 = coeff19 * 10uL + n.toULong()
                    } else if (significantDigitCount <= 34) {
                        coeff34 = coeff34 * 10uL + n.toULong()
                    } else {
                        return null
                    }
                    if (hasDot)
                        ++fractionalDigitCount
                }
                ch == '.' -> {
                    if (hasDot || chLast == '_')
                        return null
                    hasDot = true
                }
                ch == '_' -> {
                    if (! hasCoefficientDigit)
                        return null
                    if (hasDot && fractionalDigitCount == 0)
                        return null
                }
            }
            chLast = ch
            ch = if (ich < str.length) str[ich++] else '\u0000'
        }
        if (ch == 'E' || ch == 'e') {
            if (chLast == '_')
                return null
            hasExp = true
            ch = if (ich < str.length) str[ich++] else '\u0000'
            if (ch == '_')
                return null
            if (ch == '+' || ch == '-') {
                expSign = ch == '-'
                ch = if (ich < str.length) str[ich++] else '\u0000'
            }
            while (ch in '0'..'9' || ch == '_') {
                if (ch != '_') {
                    hasExpDigit = true
                    val eDigit = ch - '0'
                    expSignificantDigitCount +=
                        (eDigit or -expSignificantDigitCount) ushr 31
                    exp = exp * 10 + (ch - '0')
                } else {
                    if (! hasExpDigit)
                        return null
                }
                chLast = ch
                ch = if (ich < str.length) str[ich++] else '\u0000'
            }
        }
        if (ch != '\u0000' ||
            ich != str.length ||
            chLast == '_' ||
            ! hasCoefficientDigit ||
            hasExp && !hasExpDigit ||
            expSignificantDigitCount > 4)
            return null
        // we have at least one digit
        var dw0T = coeff19
        var dw1T = 0uL
        if (significantDigitCount > 19) {
            val m = tenPow(significantDigitCount - 19)
            dw0T = coeff19 * m
            dw1T = unsignedMulHi(coeff19, m)
            dw0T += coeff34
            dw1T += if (dw0T < coeff34) 1uL else 0uL
        }
        val signedExp = if (expSign) -exp else exp
        // fractionalDigitCount can exceed significantDigitCount ... 0.00000123
        val integerDigitCount = significantDigitCount - fractionalDigitCount
        val qExp = signedExp - fractionalDigitCount
        val bitLen = calcBitLen128(dw1T, dw0T)
        val digitLen = calcDigitLen128(bitLen, dw1T, dw0T)
        // FIXME ... make sure that this is clamping properly
        // or just document that it won't accept subnormals
        if (qExp < -6176 || qExp > 6111)
            return null
        return Dec2(sign, qExp, digitLen, bitLen, dw1T, dw0T)
    }

    fun toString(dec: Dec2): String {
        return when {
            dec.qExp == 0 -> toIntegerString(dec)
            dec.qExp >= MIN_SPECIAL_VALUE -> toSpecialValueString(dec)
            dec.qExp < 0 && dec.eExp >= -6 -> toDecimalPointString(dec)
            else -> toNormalizedScientificString(dec)
        }
    }

    private fun toSpecialValueString(dec: Dec2): String {
        if (dec.qExp < NON_FINITE_QNAN)
            return SPECIAL_VALUE_STRINGS[dec.sign01]
        val nanIndex = (if (dec.qExp == NON_FINITE_QNAN) 2 else 4) + dec.sign01
        val nanStr = SPECIAL_VALUE_STRINGS[nanIndex]
        if ((dec.dw1 or dec.dw0) == 0uL)
            return nanStr
        val utf8 = ByteArray(nanStr.length + dec.digitLen)
        for (i in nanStr.indices)
            utf8[i] = nanStr[i].code.toByte()
        IntegerParsePrint.u128ToUtf8(dec.digitLen, dec.dw1, dec.dw0, utf8, nanStr.length, )
        return String(utf8)
    }

    private fun toIntegerString(dec: Dec2): String {
        if (dec.bitLen < 4) {
            val i = ((16 and dec.sign0Neg1) + dec.dw0.toInt()) and 0x1F // bounds-check-elimination
            return SMALL_INTEGER_STRINGS[i]
        }
        val utf8 = ByteArray(dec.sign01 + dec.digitLen)
        utf8[0] = '-'.code.toByte() // will be overwritten if positive
        IntegerParsePrint.u128ToUtf8(dec.digitLen, dec.dw1, dec.dw0, utf8, dec.sign01)
        return utf8.decodeToString()
    }

    private fun toDecimalPointString(dec: Dec2): String {
        val digitsRightOfDecimal = -dec.qExp
        val leadingZeroCount = max(1 + digitsRightOfDecimal - dec.digitLen, 0)
        val signLen = dec.sign01
        val decimalPointLen = 1
        val totalLen = signLen + leadingZeroCount + decimalPointLen + dec.digitLen
        val utf8 = ByteArray(totalLen)
        utf8[0] = '-'.code.toByte() // overwritten when positive
        for (i in signLen..leadingZeroCount) // there is one extra here
            utf8[i] = '0'.code.toByte()
        IntegerParsePrint.u128ToUtf8(dec.digitLen, dec.dw1, dec.dw0, utf8, signLen + leadingZeroCount)
        for (i in totalLen-1 downTo totalLen-digitsRightOfDecimal)
            utf8[i] = utf8[i - 1]
        utf8[totalLen - digitsRightOfDecimal - 1] = '.'.code.toByte()
        return utf8.decodeToString()
    }

    private fun toNormalizedScientificString(dec: Dec2): String {
        val eExp = dec.eExp
        val eExpAbs = (eExp xor (eExp shr 31)) - (eExp shr 31)
        val signLen = dec.sign01
        val decimalPointLen = if (dec.digitLen > 1) 1 else 0
        val printedDigitLen = dec.digitLen + 1 - (-dec.digitLen ushr 31)
        val expELen = 1
        val expSignLen = 1
        val expSignByte = (if (eExp < 0) '-' else '+').code.toByte()
        val expDigitLen = max(calcDigitLen64(eExpAbs.toULong()), 1)
        val totalLen = signLen + decimalPointLen + printedDigitLen + expELen + expSignLen + expDigitLen
        val utf8 = ByteArray(totalLen)
        IntegerParsePrint.u128ToUtf8(printedDigitLen, dec.dw1, dec.dw0, utf8, signLen + decimalPointLen)
        if (decimalPointLen > 0) {
            utf8[signLen] = utf8[signLen + 1]
            utf8[signLen + 1] = '.'.code.toByte()
        }
        val iE = signLen + decimalPointLen + printedDigitLen
        utf8[iE] = 'E'.code.toByte()
        utf8[iE + 1] = expSignByte
        val j = Magia.renderTailDigitsBeforeIndex(eExpAbs.toUInt(), utf8, utf8.size)
        check (j == expDigitLen)
        return utf8.decodeToString()
    }



}