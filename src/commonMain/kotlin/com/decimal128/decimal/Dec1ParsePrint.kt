@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.bigint.Magia
import kotlin.math.max
import kotlin.math.min

object Dec1ParsePrint {
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
    fun parseDecimal(str: String, allowOversizeCoefficient: Boolean = false): Decimal {
        val decOrErrStr = parseDecimalOrErrorString(str, allowOversizeCoefficient)
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
    fun parseDecimalOrErrorString(str: String, allowOversizeCoefficient: Boolean): Any? {
        var d: Any? = parseFiniteValueText(str, allowOversizeCoefficient)
        if (d is Decimal || d is String)
            return d
        d = parseInfinityText(str)
        if (d is Decimal)
            return d
        d = parseNanText(str, allowOversizeCoefficient)
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

    private const val MAX128_HI19 = 3402823669209384634L // hi 19 digits of 2**128-1
    private const val MAX128_LO20 = 72997609508796735L   // lo 20 digits of 2**128-1

    private const val NINES_33_HI = 0x0000314DC6448D93L
    private const val NINES_33_LO = 0x38C15B09FFFFFFFFL

    private const val NINES_38_HI = 0x04B3B4CA85A86C47L
    private const val NINES_38_LO = Long.MIN_VALUE or 0x2098A223FFFFFFFFL

    private const val NINES_19 = (999_999_999_999_999_999L * 10L) + 9L
    private const val NINES_14 = 99_999_999_999_999L

    /**
     * Parses a textual NaN representation into a [`Decimal`] value.
     *
     * Accepted forms (case-insensitive):
     *
     *   - `nan`,`qnan`, `snan`
     *   - Optional leading sign: `+nan`, `-snan`
     *   - Optional `q` prefix, though `qnan` and `nan` are treated identically
     *   - Any characters may follow the `nan` / `snan` prefix
     *   - Digits after the `nan` are parsed for the payload
     *
     * After the prefix, the parser scans the remainder of the string and
     * collects **decimal digits only** (in order). These digits form the NaN
     * payload.
     *
     * Canonical DPD and BID Decimal128 NaN payloads allow at most
     * **33 decimal digits**. If `allowOversizeCoefficient` is `false`,
     * payloads longer than 33 digits are clamped to the largest canonical
     * 33-digit payload ... 33 nines.
     *
     * If `allowOversizeCoefficient` is `true`, the parser accepts any number of
     * digits, although the value is clamped to 38 digits ... 38 nines.
     *
     * Leading zeros are ignored when computing the payload length; if no digits
     * appear at all, the payload is zero.
     *
     * The function returns either a quiet NaN (`qNaN`) or a signaling NaN
     * (`sNaN`) depending on whether the prefix began with `s`. If the input
     * does not begin with a valid NaN text prefix, this returns `null`.
     *
     * @param allowOversizePayload
     *        Whether payloads longer than the canonical 33-digit limit should be
     *        accepted (up to the 128-bit numerical maximum), rather than being
     *        clamped to the canonical range.
     *
     * @return the parsed NaN value, or `null` if the string is not a NaN form.
     */
    fun parseNanText(str: String, allowOversizePayload: Boolean = false): Decimal? {
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
        var accumDigitCount = 0
        var accum19a = 0L
        var accum19b = 0L
        while (ich < str.length) {
            val chDigit = str[ich++]
            // be very lenient when parsing NaN payload
            // IEEE754 says nothing about external text representation of NaN payload
            // could have parens, brackets, etc.
            if (chDigit !in '0'..'9')
                continue
            val d = chDigit - '0'
            // flush leading zeros from payload ... don't increment
            accumDigitCount += (-(accumDigitCount or d)) ushr 31
            when {
                accumDigitCount <= 19 -> accum19a = (accum19a * 10L) + d.toLong()
                accumDigitCount > 33 && !allowOversizePayload -> {
                    accum19a = NINES_19
                    accum19b = NINES_14
                    accumDigitCount = 33
                    break;
                }
                accumDigitCount > 38 -> {
                    accum19a = NINES_19
                    accum19b = NINES_19
                    accumDigitCount = 38
                    break;
                }
                else -> {
                    accum19b = (accum19b * 10L) + d.toLong()
                }
            }
        }
        var payloadDw0 = accum19a
        var payloadDw1 = 0L
        if (accumDigitCount > 19) {
            val p10 = pow10_64(accumDigitCount - 19)
            payloadDw0 = accum19a * p10
            payloadDw1 = unsignedMulHi(accum19a, p10)
            payloadDw0 += accum19b
            payloadDw1 += if (unsignedLT(payloadDw0, accum19b)) 1L else 0L
        }
        return Decimal.NaN(sign, hasS, payloadDw1, payloadDw0, allowOversizePayload)
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
    fun parseFiniteValueText(str: String, allowOversizeCoefficient: Boolean = false): Any? {
        var hasCoefficientDigit = false
        var significantDigitCount = 0 // does not count leading zeros
        var hasDot = false
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
        var accum19a = 0L
        var accum19b = 0L
        var exp = 0

        while (ch in '0'..'9' || ch == '.' || ch == '_') {
            when (ch) {
                in '0'..'9' -> {
                    val d = ch - '0'
                    hasCoefficientDigit = true
                    significantDigitCount += (-(significantDigitCount or d)) ushr 31
                    when {
                        significantDigitCount <= 19 -> accum19a = (accum19a * 10L) + d.toLong()
                        significantDigitCount > 34 && !allowOversizeCoefficient ->
                            return "more than decimal128 34 significant digits"

                        significantDigitCount > 38 ->
                            return "oversize coefficient exceeds decimal128_extended 38 digits"

                        else -> accum19b = (accum19b * 10L) + d.toLong()
                    }
                    if (hasDot)
                        ++fractionalDigitCount
                }

                '.' -> when {
                    hasDot -> return "double decimal point"
                    chLast == '_' -> return "invalid _ placement"
                    else -> hasDot = true
                }

                '_' ->
                    if (! hasCoefficientDigit || chLast == '.')
                        return "invalid _ placement"
            }
            chLast = ch
            ch = if (ich < str.length) str[ich++] else '\u0000'
        }
        if (! hasCoefficientDigit)
            return null // try other options Infinity, NaN
        // this path has at least one digit
        if (ch == 'E' || ch == 'e') {
            var hasExpDigit = false
            if (chLast == '_')
                return "invalid _ placement"
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
                        (-(expSignificantDigitCount or eDigit)) ushr 31
                    if (expSignificantDigitCount > 4)
                        return "exponent out of decimal128 range"
                    exp = exp * 10 + (ch - '0')
                } else {
                    if (! hasExpDigit)
                        return "invalid _ placement"
                }
                chLast = ch
                ch = if (ich < str.length) str[ich++] else '\u0000'
            }
            if (! hasExpDigit)
                return "E with no exponent digits"
        }
        if (chLast == '_')
            return "invalid _ placement"
        if (ch != '\u0000')
            return "invalid"
        // we have at least one digit
        var dw0T = accum19a
        var dw1T = 0L
        if (significantDigitCount > 19) {
            val p10 = pow10_64(significantDigitCount - 19)
            dw0T = accum19a * p10
            dw1T = unsignedMulHi(accum19a, p10)
            dw0T += accum19b
            dw1T += if (unsignedLT(dw0T,accum19b)) 1L else 0L
        }
        val signedExp = if (expSign) -exp else exp
        var qExp = signedExp - fractionalDigitCount
        var bitLen = calcBitLen128(dw1T, dw0T)
        var digitLen = calcDigitLen128(bitLen, dw1T, dw0T)
        if (digitLen == 0)
            qExp = min(max(qExp, -6176), 6111)
        if (qExp < -6176)
            return "exponent out of decimal128 range"
        if (qExp > DECIMAL128_QMAX_6111) {
            val overage = qExp - DECIMAL128_QMAX_6111
            //println("parseFiniteValueText => str:$str overage:$overage")
            if (!allowOversizeCoefficient && overage > 34 - digitLen)
                return "out of decimal128 range"
            else if (allowOversizeCoefficient && overage > 38 - digitLen)
                return "out of decimal128_extended range"
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
        if ((dec.dw1 or dec.dw0) == 0L)
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
        val expDigitLen = max(calcDigitLen64(eExpAbs.toLong()), 1)
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
        val j = Magia.renderTailDigitsBeforeIndex(eExpAbs.toLong(), utf8, utf8.size)
        verify { j == expDigitLen }
        return utf8.decodeToString()
    }



}