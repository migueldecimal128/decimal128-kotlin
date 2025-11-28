@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.U256Bits.calcBitLen128
import com.decimal128.decimal.U256Pow10.calcDigitLen128
import com.decimal128.decimal.U256Pow10.calcDigitLen64
import com.decimal128.hugeint.Magia
import kotlin.math.max
import kotlin.math.min

object DecParsePrint {
    private val SPECIAL_VALUE_STRINGS = arrayOf(
        "Infinity", "-Infinity", "NaN", "-NaN", "sNaN", "-sNaN"
    )

    private val SMALL_INTEGER_STRINGS = arrayOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15",
        "-0", "-1", "-2", "-3", "-4", "-5", "-6", "-7",
        "-8", "-9", "-10", "-11", "-12", "-13", "-14", "-15"
    )

    private const val DECIMAL128_QTINY = -6176
    private const val DECIMAL128_QMAX_6111 = 6111

    /**
     * Parses a decimal string into a `Decimal` (decimal128).
     *
     * Accepts the same finite-value syntax as `parseFiniteValueText`:
     * optional sign, digits with optional decimal point, optional `e`/`E`
     * exponent (with optional sign), and only ASCII/Basic-Latin characters.
     *
     * If the input is valid, a `Decimal` is returned. Otherwise an
     * `IllegalArgumentException` is thrown containing the diagnostic text
     * produced by `parseDecimalOrErrorString`.
     *
     * @throws IllegalArgumentException if the input is not a valid finite
     *         decimal128 text form or would require rounding.
     */
    fun parseDecimal(str: String): Decimal {
        val decOrErrStr = parseDecimalOrErrorString(str)
        if (decOrErrStr is Decimal)
            return decOrErrStr
        val msg = decOrErrStr ?: ""
        throw IllegalArgumentException("invalid decimal format:$msg:'$str'")
    }

    /**
     * Attempts to parse the input as a Decimal128 value.
     *
     * Parsing is attempted in order:
     *  1. `parseFiniteValueText`
     *  2. `parseInfinityText`
     *  3. `parseNanText`
     *
     * Returns the first non-null result, which may be:
     *  * a `Decimal` for a successful parse, or
     *  * a `String` describing an error.
     *
     * Error messages are returned as `String` constant values
     * to reduce any performance overhead for failed parse attempts.
     */
    fun parseDecimalOrErrorString(str: String): Any? {
        var d: Any? = parseFiniteValueText(str)
        if (d is Decimal || d is String)
            return d
        d = parseInfinityText(str)
        if (d is Decimal)
            return d
        d = parseNanText(str)
        return d
    }

    /**
     * Parses a textual infinity representation into a [`Decimal`] value.
     *
     * Accepted forms are case-insensitive:
     *
     *   - `INF`, `Infinity`
     *   - Optional leading sign: `+inf`, `-infinity`
     *
     * The entire string must match one of these forms exactly (no trailing
     * characters). Returns `null` if the text is not a valid infinity form.
     *
     * @return the parsed positive or negative infinity, or `null` if not matched.
     */
    fun parseInfinityText(str: String): Decimal? {
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
        return Decimal.infinity(sign)
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

    /**
     * Parses a textual NaN representation into a [`Decimal`] value.
     *
     * Accepted forms (case-insensitive):
     *
     *   - `nan`, `snan`
     *   - Optional leading sign: `+nan`, `-snan`
     *   - Any characters may follow the `nan`/`snan` prefix.
     *
     * After the prefix, all decimal digits found anywhere in the remainder of
     * the string are collected (in order) up to a maximum of **33 digits**.
     * These digits form the NaN payload; excess digits are ignored. If no digits
     * are present, the payload is zero.
     *
     * Returns a quiet NaN or signaling NaN depending on whether the prefix began
     * with `s`. If the string does not begin with a valid NaN prefix, returns
     * `null`.
     *
     * @return the parsed NaN value, or `null` if not a NaN text form.
     */
    fun parseNanText(str: String): Decimal? {
        if (str.length < 3)
            return null
        var ch = str[0].code
        var ich = 1
        val sign = ch == '-'.code
        if (ch == '-'.code || ch == '+'.code)
            ch = str[ich++].code
        val hasS = (ch or 0x20) == 's'.code
        val hasQ = (ch or 0x20) == 'q'.code
        if (hasQ || hasS)
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
        var accumulator33 = 0uL
        while (ich < str.length) {
            val chDigit = str[ich++]
            // be very lenient when parsing NaN payload
            // IEEE754 says nothing about external text representation of NaN payload
            if (chDigit !in '0'..'9' || payloadDigitCount == 33)
                continue
            val d = (chDigit - '0').toULong()
            if (payloadDigitCount > 0 || d != 0uL) {
                if (payloadDigitCount < 19)
                    accumulator19 = (accumulator19 * 10uL) + d
                else
                    accumulator33 = (accumulator33 * 10uL) + d
                ++payloadDigitCount
            }
        }
        var payloadDw1 = 0uL
        var payloadDw0 = accumulator19
        if (payloadDigitCount > 19) {
            val m = tenPow(payloadDigitCount - 19)
            payloadDw0 = accumulator19 * m
            payloadDw1 = unsignedMulHi(accumulator19, m)
            payloadDw0 += accumulator33
            payloadDw1 += if (payloadDw0 < accumulator33) 1uL else 0uL
        }
        return Decimal.NaN(sign, hasS, payloadDw1, payloadDw0)
    }

    /**
     * Parses a finite decimal value into a [`Decimal`] (decimal128) with **no rounding**.
     *
     * This is a simple numeric parser that accepts only standard finite forms:
     *
     *   - Optional leading sign (`+` or `-`)
     *   - Digits with optional decimal point `.`
     *   - Optional exponent using `e` or `E`
     *   - Optional exponent sign
     *   - All characters must be ASCII/Basic-Latin
     *
     * If the input requires rounding, contains invalid syntax, or exceeds the
     * exact range of decimal128 finite values, this method returns `null`.
     *
     * @return the parsed finite `Decimal` value, or `null` if not a valid finite
     *         decimal128 text form without rounding.
     */
    fun parseFiniteValueText(str: String): Any? {
        var hasCoefficientDigit = false
        var significantDigitCount = 0 // does not count leading zeros
        var hasDot = false
        var hasE = false
        var hasExpDigit = false
        var expSign = false
        var expSignificantDigitCount = 0

        if (str.length == 0)
            return "empty string"
        var ch = str[0]
        var ich = 1
        var chLast = '\u0000'

        val sign = ch == '-'
        if (ch == '-' || ch == '+') {
            if (ich == str.length)
                return "invalid"
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
                        // we will allow excess trailing zero digits ...
                        // ... as long as there has been a dot
                        // ... and we will ignore them
                        return "more than 34 significant digits"
                    }
                    if (hasDot)
                        ++fractionalDigitCount
                }
                ch == '.' -> {
                    if (hasDot)
                        return "double decimal point"
                    if (chLast == '_')
                        return "invalid _ placement"
                    hasDot = true
                }
                ch == '_' -> {
                    if (! hasCoefficientDigit || chLast == '.')
                        return "invalid _ placement"
                }
            }
            chLast = ch
            ch = if (ich < str.length) str[ich++] else '\u0000'
        }
        if (! hasCoefficientDigit)
            return null // try other options Infinity, NaN
        if (ch == 'E' || ch == 'e') {
            if (chLast == '_')
                return "invalid _ placement"
            hasE = true
            ch = if (ich < str.length) str[ich++] else '\u0000'
            if (ch == '_')
                return "invalid _ placement"
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
                        return "invalid _ placement"
                }
                chLast = ch
                ch = if (ich < str.length) str[ich++] else '\u0000'
            }
        }
        if (chLast == '_')
            return "invalid _ placement"
        if (ch != '\u0000' ||
            hasE && !hasExpDigit ||
            expSignificantDigitCount > 4)
            return "invalid"
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
        var qExp = signedExp - fractionalDigitCount
        var bitLen = calcBitLen128(dw1T, dw0T)
        var digitLen = calcDigitLen128(bitLen, dw1T, dw0T)
        if (digitLen == 0)
            qExp = min(max(qExp, -6176), 6111)
        if (qExp < -6176)
            return "out of decimal128 range"
        if (qExp > DECIMAL128_QMAX_6111) {
            val overage = qExp - DECIMAL128_QMAX_6111
            println("parseFiniteValueText => str:$str overage:$overage")
            if (overage > 34 - digitLen)
                return "out of decimal128 range"
            val (t1, t0) = DecPow10.umul128Pow10(dw1T, dw0T, overage)
            dw1T = t1
            dw0T = t0
            qExp = DECIMAL128_QMAX_6111
            bitLen = calcBitLen128(dw1T, dw0T)
            digitLen += overage
        }
        if ((qExp or bitLen) != 0)
            return Decimal(sign, qExp, digitLen, bitLen, dw1T, dw0T)
        return if (sign) Decimal.NEG_ZEROe0 else Decimal.POS_ZEROe0
    }

    fun toString(dec: Decimal): String {
        return when {
            dec.qExp == 0 -> toIntegerString(dec)
            dec.qExp >= MIN_SPECIAL_VALUE -> toSpecialValueString(dec)
            dec.qExp < 0 && dec.eExp >= -6 -> toDecimalPointString(dec)
            else -> toNormalizedScientificString(dec)
        }
    }

    private fun toSpecialValueString(dec: Decimal): String {
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

    private fun toIntegerString(dec: Decimal): String {
        if (dec.bitLen < 4) {
            val i = ((16 and dec.sign0Neg1) + dec.dw0.toInt()) and 0x1F // bounds-check-elimination
            return SMALL_INTEGER_STRINGS[i]
        }
        val utf8 = ByteArray(dec.sign01 + dec.digitLen)
        utf8[0] = '-'.code.toByte() // will be overwritten if positive
        IntegerParsePrint.u128ToUtf8(dec.digitLen, dec.dw1, dec.dw0, utf8, dec.sign01)
        return utf8.decodeToString()
    }

    private fun toDecimalPointString(dec: Decimal): String {
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

    private fun toNormalizedScientificString(dec: Decimal): String {
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
        utf8[0] = '-'.code.toByte() // overwritten if non-negative
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