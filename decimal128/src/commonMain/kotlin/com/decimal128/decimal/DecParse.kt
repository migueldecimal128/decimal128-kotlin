// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.InvalidCause.PARSE_INVALID_UNDERSCORE_LOCATION
import com.decimal128.decimal.InvalidCause.PARSE_DOUBLE_DOT
import com.decimal128.decimal.InvalidCause.PARSE_EMPTY_STRING
import com.decimal128.decimal.InvalidCause.PARSE_MALFORMED
import com.decimal128.decimal.InvalidCause.PARSE_NO_COEFFICIENT_DIGIT
import com.decimal128.decimal.InvalidCause.PARSE_NO_EXPONENT_DIGIT
import com.decimal128.decimal.InvalidCause.PARSE_UNEXPECTED_CHAR
import kotlin.math.max
import kotlin.math.min

/**
 * Parses [str] into a [Decimal], using using the current [DecContext] for
 * precision, rounding, and parse preferences.
 *
 * Behaviour on failure mirrors [parseToMutDec].
 *
 * @param str the string to parse
 * @return the parsed [Decimal]
 * @throws NumberFormatException if [str] is malformed and
 * [DecPrefs.parseMalformedThrowsNumberFormatException] is set
 */
internal fun parseToDecimal(str: String): Decimal {
    val ctx = DecContext.current()
    val md = ctx.tmps.mdecFmaParseConvert
    parseToMutDec(md, str, ctx)
    return Decimal.from(md)
}

/**
 * Parses [str] into [md], using [ctx] for precision, rounding, and parse preferences.
 *
 * On success, returns [md]. On failure, behaviour depends on [DecPrefs]:
 * - If [DecPrefs.parseMalformedThrowsNumberFormatException] is set, throws
 *   [NumberFormatException].
 * - Otherwise, signals an invalid-operation condition and returns [md] set to NaN.
 *
 * @param md the destination to write the parsed value into
 * @param str the string to parse
 * @param ctx precision, rounding mode, and parse preferences; defaults to the current context
 * @return [md]
 * @throws NumberFormatException if [str] is malformed and
 * [DecPrefs.parseMalformedThrowsNumberFormatException] is set
 */
internal fun parseToMutDec(md: MutDec, str: String, ctx: DecContext = DecContext.current()): MutDec {
    val strIterator = ctx.tmps.parseStringLatin1Iterator.reload(str)
    val mutDecOrReason = parseMutDecOrReason(md, strIterator, ctx)
    if (mutDecOrReason is MutDec)
        return mutDecOrReason
    val reason: InvalidCause =
        if (mutDecOrReason is InvalidCause) mutDecOrReason
        else throw IllegalStateException()
    if (ctx.parsePrefs.throwOnMalformedText) {
        throw NumberFormatException("invalid decimal format:$reason:'$str'")
    }
    return ctx.setNanSignalInvalidOperation(md, reason)
}

private fun parseMutDecOrReason(md: MutDec, txt: Latin1Iterator, ctx: DecContext): Any {
    var ch = txt.nextChar()
    if (ch >= '0' && ch <= '9')
        return parseFiniteValueText(md, sign = false, ch, txt, ctx)
    val sign = ch == '-'
    if (ch == '+' || ch == '-')
        ch = txt.nextChar()
    if (ch >= '0' && ch <= '9' || ch == '.')
        return parseFiniteValueText(md, sign, ch, txt, ctx)
    val chLower = (ch.code or 0x20).toChar()
    if (chLower == 'i')
        return parseInfinityText(md, sign, ch, txt)
    return parseNanText(md, sign, ch, txt, ctx.parsePrefs.collapseSNAN)
}

/**
 * Parses a positive or negative infinity from [txt] into [md].
 *
 * Accepted forms (case-insensitive): `inf`, `infinity`, with an optional
 * leading `+` or `-` sign. The iterator must be exhausted after the match;
 * trailing characters are rejected.
 *
 * @param md the destination to write the infinity value into
 * @param txt iterator over the full input, positioned before any sign character
 * @return [md] on success, or [InvalidCause.PARSE_MALFORMED] if the
 * input is not a valid infinity form
 */
private inline fun parseInfinityText(md: MutDec, sign: Boolean, chFirst: Char, txt: Latin1Iterator): Any {
    var ch = chFirst
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
 * Parses a NaN from [txt] into [md].
 *
 * Accepted prefixes (case-insensitive), with an optional leading `+` or `-`:
 * `nan`, `qnan`, `snan`. After the prefix, any mix of decimal digits and
 * bracket characters (`()`, `[]`, `{}`) may follow; digits are collected as
 * the NaN payload. Any other character returns
 * [InvalidCause.PARSE_NON_DIGIT_AFTER_NAN].
 *
 * The payload is capped at 33 significant digits.
 * If more digits are present the payload is set to zero, per IEEE 754-2019
 * §3.5.2. Leading zeros do not count toward the limit.
 *
 * A signaling NaN is produced when the prefix is `snan` and
 * [collapseSNaN] is `false`; otherwise a quiet NaN is produced.
 *
 * @param md the destination to write the NaN value into
 * @param txt iterator over the full input, positioned before any sign character
 * @param collapseSNaN if `true`, an `snan` prefix produces a quiet NaN
 * @return [md] on success, or [InvalidCause.PARSE_MALFORMED] if the
 * input does not begin with a valid NaN prefix, or
 * [InvalidCause.PARSE_NON_DIGIT_AFTER_NAN] if an unexpected
 * character follows the prefix
 */
private inline fun parseNanText(md: MutDec, sign: Boolean, chFirst: Char, txt: Latin1Iterator, collapseSNaN: Boolean): Any {
    var ch = chFirst
    val hasS = (ch.code or 0x20) == 's'.code
    val hasQ = (ch.code or 0x20) == 'q'.code
    if (hasQ or hasS)
        ch = txt.nextChar()
    if (((ch.code or 0x20) != 'n'.code) ||
        ((txt.nextCharCode() or 0x20) != 'a'.code) ||
        ((txt.nextCharCode() or 0x20) != 'n'.code)
    )
        return PARSE_MALFORMED
    var payloadDw0 = 0L
    var payloadDw1 = 0L
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
                    return InvalidCause.PARSE_NON_DIGIT_AFTER_NAN
            }
            ch = txt.nextChar()
        } while (ch.code != 0)
        if (accumDigitCount > 33) {
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
    return md.setNaN(sign, isSignaling = hasS and !collapseSNaN, payloadDw1, payloadDw0)
}

/**
 * Parses a finite decimal value from [txt] into [md].
 *
 * Accepts an optional leading `+` or `-`, followed by a coefficient of
 * decimal digits, and an optional exponent (`e` or `E`). Underscores are
 * permitted as digit separators in both the coefficient and exponent, subject
 * to the restrictions below. The iterator must be exhausted after the match.
 *
 * **Coefficient rules**
 * - At least one digit is required.
 * - A single `.` may appear anywhere among the digits.
 * - Underscores may separate digits but not appear at the start, immediately
 *   before or after `.`, or at the end of the coefficient.
 *
 * **Exponent rules**
 * - Introduced by `e` or `E`, with an optional `+` or `-`.
 * - At least one digit is required.
 * - Underscores may separate exponent digits but not appear before the first
 *   digit or at the end.
 * - Values with more than 4 significant exponent digits are clamped to 9999.
 *
 * **Rounding**
 * - Coefficients exceeding [DecContext.precision] significant digits are
 *   rounded according to [ctx]. If
 *   [DecPrefs.parseThrowOnDigitOverflow][ctx] is set, excess digits return
 *   [PARSE_COEFFICIENT_EXCEEDS_MAX_PRECISION] instead.
 * - After rounding, if the result is out of range and
 *   [DecPrefs.parseThrowOnOutOfRange][ctx] is set, returns
 *   [PARSE_VALUE_OUT_OF_RANGE].
 *
 * @param md the destination to write the parsed value into
 * @param txt iterator over the full input, positioned before any sign character
 * @param ctx precision, rounding mode, and parse preferences
 * @return [md] on success, or one of:
 * - [PARSE_EMPTY_STRING] — input was empty
 * - [PARSE_NO_COEFFICIENT_DIGIT] — no digit before the exponent
 * - [PARSE_DOUBLE_DOT] — more than one `.` in the coefficient
 * - [PARSE_INVALID_UNDERSCORE_LOCATION] — underscore in a disallowed position
 * - [PARSE_NO_EXPONENT_DIGIT] — `e`/`E` not followed by a digit
 * - [PARSE_UNEXPECTED_CHAR] — unrecognised character in the input
 * - [PARSE_COEFFICIENT_EXCEEDS_MAX_PRECISION] — see above
 * - [PARSE_VALUE_OUT_OF_RANGE] — see above
 */
private inline fun parseFiniteValueText(md: MutDec, sign: Boolean, chFirst: Char, txt: Latin1Iterator, ctx: DecContext): Any {
    val precision = ctx.precision
    var residue: Residue = Residue.EXACT

    var hasCoefficientDigit = false // have we seen any digits at all, including zero
    var significantDigitCount = 0 // does not count leading zeros
    var hasInexactDigit = false
    var hasDot = false
    var expSign = false

    var ch = chFirst
    if (ch.code == 0)
        return PARSE_EMPTY_STRING

    var fractionalDigitCount = 0
    var accum19a = 0L
    var accum19b = 0L
    var exp = 0

    var chLast = '\u0000'
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
                    significantDigitCount == precision + 1 -> {
                        residue = Residue.fromDecimalDigit(d)
                        hasInexactDigit = hasInexactDigit or (ch != '0')
                    }

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
    if (hasInexactDigit && ctx.parsePrefs.throwOnInexact)
        throw NumberFormatException("Coefficient exceeds 34 digits of precision:$txt")
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
    if (!md.isFiniteNonZero()) {
        if (md.isInfinite() && ctx.parsePrefs.throwOnOverflow) {
            throw NumberFormatException("parsed text value overflows to Infinity:$txt")
        } else if (md.isZero() && ctx.parsePrefs.throwOnUnderflow) {
            throw NumberFormatException("parsed text value underflows to 0:$txt")
        }
    }
    return md
}
