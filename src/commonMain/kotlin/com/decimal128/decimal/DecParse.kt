// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.InvalidOperationReason.PARSE_COEFFICIENT_EXCEEDS_MAX_PRECISION
import com.decimal128.decimal.InvalidOperationReason.PARSE_INVALID_UNDERSCORE_LOCATION
import com.decimal128.decimal.InvalidOperationReason.PARSE_DOUBLE_DOT
import com.decimal128.decimal.InvalidOperationReason.PARSE_EMPTY_STRING
import com.decimal128.decimal.InvalidOperationReason.PARSE_MALFORMED
import com.decimal128.decimal.InvalidOperationReason.PARSE_NO_COEFFICIENT_DIGIT
import com.decimal128.decimal.InvalidOperationReason.PARSE_NO_EXPONENT_DIGIT
import com.decimal128.decimal.InvalidOperationReason.PARSE_UNEXPECTED_CHAR
import com.decimal128.decimal.InvalidOperationReason.PARSE_VALUE_OUT_OF_RANGE
import kotlin.math.max
import kotlin.math.min

internal fun parseToDecimal(str: String, ctx: DecContext = DecContext.current()): Decimal {
    val md = ctx.tmps.mdecArg1
    parseToMutDec(md, str, ctx)
    return Decimal.from(md)
}


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
fun parseToMutDec(md: MutDec, str: String, ctx: DecContext = DecContext.current()): MutDec {
    val strIterator = ctx.tmps.parseStringLatin1Iterator.reload(str)
    val mutDecOrReason = parseDecimalOrReason(md, strIterator, ctx)
    if (mutDecOrReason is MutDec)
        return mutDecOrReason
    val reason: InvalidOperationReason =
        if (mutDecOrReason is InvalidOperationReason) mutDecOrReason
        else throw IllegalStateException()
    if (ctx.decPrefs.parseMalformedThrowsNumberFormatException) {
        throw NumberFormatException("invalid decimal format:$reason:'$str'")
    }
    return ctx.setNanSignalInvalid(md, reason)
}

private fun parseDecimalOrReason(md: MutDec, txt: Latin1Iterator, ctx: DecContext): Any {
    var ch = txt.nextChar()
    if (ch == '+' || ch == '-')
        ch = txt.nextChar()
    txt.rewind()
    if (ch >= '0' && ch <= '9' || ch == '.')
        return parseFiniteValueText(md, txt, ctx)
    val chLower = (ch.code or 0x20).toChar()
    if (chLower == 'i')
        return parseInfinityText(md, txt)
    return parseNanText(md, txt)
}

/**
 * Parses a textual infinity representation into a [Decimal] value.
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
private fun parseInfinityText(md: MutDec, txt: Latin1Iterator): Any {
    var ch = txt.nextChar()
    val sign = ch == '-'
    if (ch == '-' || ch == '+')
        ch = txt.nextChar()
    var chPrevCode = 0
    for (target in "infinity") {
        if (ch.code or 0x20 != target.code) {
            if (ch.code == 0)
                break
            else
                return PARSE_MALFORMED
        }
        chPrevCode = ch.code or 0x20
        ch = txt.nextChar()
    }
    if (ch.code != 0 || (chPrevCode != 'f'.code && chPrevCode != 'y'.code))
        return PARSE_MALFORMED
    return md.setInfinite(sign)
}

/**
 * Parses a textual NaN representation into a [Decimal] value.
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
private fun parseNanText(md: MutDec, txt: Latin1Iterator): Any {
    var ch = txt.nextChar()
    val sign = ch == '-'
    if (ch == '-' || ch == '+')
        ch = txt.nextChar()
    val hasS = (ch.code or 0x20) == 's'.code
    val hasQ = (ch.code or 0x20) == 'q'.code
    if (hasQ or hasS)
        ch = txt.nextChar()
    if (((ch.code or 0x20) != 'n'.code) ||
        ((txt.nextCharCode() or 0x20) != 'a'.code) ||
        ((txt.nextCharCode() or 0x20) != 'n'.code)
    )
        return PARSE_MALFORMED
    var payloadDw0: Long = 0L
    var payloadDw1: Long = 0L
    ch = txt.nextChar()
    if (ch.code != 0) {
        var accumDigitCount = 0
        var accum19a = 0L
        var accum19b = 0L
        do {
            if (ch >= '0' && ch <= '9') {
                val d = ch - '0'
                // flush leading zeros from payload ... don't increment
                accumDigitCount += (-(accumDigitCount or d)) ushr 31
                if (accumDigitCount <= 19)
                    accum19a = (accum19a * 10L) + d.toLong()
                else
                    accum19b = (accum19b * 10L) + d.toLong()
            } else {
                if (ch != '(' && ch != ')' &&
                    ch != '[' && ch != ']' &&
                    ch != '{' && ch != '}'
                )
                    return InvalidOperationReason.PARSE_NON_DIGIT_AFTER_NAN
            }
            ch = txt.nextChar()
        } while (ch.code != 0)
        if (accumDigitCount > NAN_PAYLOAD_PRECISION) {
            // Colishaw decTest says that an oversized payload throws Conversion_syntax
            // IEEE754-2019 3.5.2 Encodings says:
            //  The maximum value of the binary-encoded significand is the same as that
            //  of the corresponding decimal-encoded significand; that is,
            //  10 (3 × J + 1) −1 (or 10 (3 × J ) −1 when T is used
            //  as the payload of a NaN). If the value exceeds the maximum,
            //  the significand c is non-canonical and the value used for c is zero.
            //
            // This is parsing, so I suppose there is some room for interpretation.
            // I choose to interpret this case as coeff == 0
            payloadDw0 = 0
            payloadDw1 = 0
        } else {
            payloadDw0 = accum19a
            payloadDw1 = 0L
            if (accumDigitCount > 19) {
                val p10 = pow10_64(accumDigitCount - 19)
                payloadDw0 = accum19a * p10
                payloadDw1 = unsignedMulHi(accum19a, p10)
                payloadDw0 += accum19b
                payloadDw1 += if (unsignedLT(payloadDw0, accum19b)) 1L else 0L
            }
        }
    }
    return md.setNaN(sign, isSignaling = hasS, payloadDw1, payloadDw0)
}

private fun parseFiniteValueText(md: MutDec, txt: Latin1Iterator, ctx: DecContext): Any {
    val precision = ctx.precision
    var residue: Residue = Residue.EXACT

    var hasCoefficientDigit = false
    var significantDigitCount = 0 // does not count leading zeros
    var hasDot = false
    var expSign = false

    var ch = txt.nextChar()
    if (ch.code == 0)
        return PARSE_EMPTY_STRING
    var chLast = '\u0000'

    val sign = ch == '-'
    if (ch == '-' || ch == '+')
        ch = txt.nextChar()
    var fractionalDigitCount = 0
    var accum19a = 0L
    var accum19b = 0L
    var exp = 0

    while (ch in '0'..'9' || ch == '.' || ch == '_') {
        when (ch) {
            in '0'..'9' -> {
                val d = ch - '0'
                hasCoefficientDigit = true
                // count while flushing leading zeros
                significantDigitCount += (-(significantDigitCount or d)) ushr 31
                when {
                    significantDigitCount <= 19 -> accum19a = (accum19a * 10L) + d.toLong()
                    significantDigitCount <= precision -> accum19b = (accum19b * 10L) + d.toLong()
                    significantDigitCount == precision + 1 ->
                        residue = Residue.fromDecimalDigit(d)

                    else ->
                        residue = residue.merge(Residue.fromDecimalDigit(d))
                }
                if (hasDot)
                    ++fractionalDigitCount
            }

            '.' -> when {
                hasDot -> return PARSE_DOUBLE_DOT
                chLast == '_' -> return PARSE_INVALID_UNDERSCORE_LOCATION
                else -> hasDot = true
            }

            '_' ->
                if (!hasCoefficientDigit || chLast == '.')
                    return PARSE_INVALID_UNDERSCORE_LOCATION
        }
        chLast = ch
        ch = txt.nextChar()
    }
    if (!hasCoefficientDigit)
        return PARSE_NO_COEFFICIENT_DIGIT
    // this path has at least one digit
    if (ch == 'E' || ch == 'e') {
        if (chLast == '_')
            return PARSE_INVALID_UNDERSCORE_LOCATION
        ch = txt.nextChar()
        if (ch == '_')
            return PARSE_INVALID_UNDERSCORE_LOCATION
        if (ch == '+' || ch == '-') {
            expSign = ch == '-'
            ch = txt.nextChar()
        }
        var hasExpDigit = false
        var expSignificantDigitCount = 0
        while (ch in '0'..'9' || ch == '_') {
            if (ch != '_') {
                hasExpDigit = true
                val eDigit = ch - '0'
                // count while flushing leading zeros
                expSignificantDigitCount +=
                    (-(expSignificantDigitCount or eDigit)) ushr 31
                exp = exp * 10 + eDigit
            } else {
                if (!hasExpDigit)
                    return PARSE_INVALID_UNDERSCORE_LOCATION
            }
            chLast = ch
            ch = txt.nextChar()
        }
        if (!hasExpDigit)
            return PARSE_NO_EXPONENT_DIGIT
        // clamp exp to 9999 once after the loop
        if (expSignificantDigitCount > 4)
            exp = 9999
    }
    if (chLast == '_')
        return PARSE_INVALID_UNDERSCORE_LOCATION
    if (ch.code != 0)
        return PARSE_UNEXPECTED_CHAR
    // we have at least one digit
    var dw0T = accum19a
    var dw1T = 0L
    if (significantDigitCount > 19) {
        val pow10b = min(precision, significantDigitCount) - 19
        val p10 = pow10_64(pow10b)
        dw0T = accum19a * p10
        dw1T = unsignedMulHi(accum19a, p10)
        dw0T += accum19b
        dw1T += if (unsignedLT(dw0T, accum19b)) 1L else 0L
    }
    if (significantDigitCount > precision && ctx.decPrefs.parseThrowOnDigitOverflow)
        return PARSE_COEFFICIENT_EXCEEDS_MAX_PRECISION
    // at this point, our coeff <= precision digits
    // but we need to deal with residue and rounding
    // rounding rollover could affect the exponent
    //
    // when we accept oversize coefficients, then whether the excess
    // digits are to the right or left of the exponent will affect
    // the qExp ...
    val signedExp = if (expSign) -exp else exp
    val qExp = signedExp - fractionalDigitCount + max(0, significantDigitCount - precision)
    if ((dw0T or dw1T) == 0L) // allow any exponent with Zero
        return md.setZero(sign, qExp)
    md.c256Set128(dw1T, dw0T)
    md.roundAndFinalizeFnz(sign, qExp, residue, ctx)
    if (!md.isFiniteNonZero() && ctx.decPrefs.parseThrowOnOutOfRange) {
        return PARSE_VALUE_OUT_OF_RANGE
    }
    return md
}


