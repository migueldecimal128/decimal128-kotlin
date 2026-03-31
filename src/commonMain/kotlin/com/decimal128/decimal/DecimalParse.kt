package com.decimal128.decimal

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val UPPER_CASE_MASK = ('a'.code - 'A'.code).inv()

private const val INFINITY_CHARS_BACKWARDS = (('y'.code.toLong() shl 56) or ('t'.code.toLong() shl 48) or
        ('i'.code.toLong() shl 40) or ('n'.code.toLong() shl 32) or
        ('i'.code.toLong() shl 24) or ('f'.code.toLong() shl 16) or
        ('n'.code.toLong() shl  8) or ('I'.code.toLong()))

private const val BYTE_ZERO = '0'.code.toByte()
private const val BYTE_DOT = '.'.code.toByte()
private const val BYTE_PLUS = '+'.code.toByte()
private const val BYTE_MINUS = '-'.code.toByte()

object DecimalParse {

    fun decFromString(x: MutDec, str: String, env: DecContext) =
        decFromText(x, StringLatin1Iterator(str), env)

    private fun decFromText(x: MutDec, src: Latin1Iterator, env: DecContext) {
        val maxPayloadDigitLen = env.precision - 1
        when {
            isFiniteValueText(x, src, env) -> return
            isInfinityText(x, src) -> return
            isNanText(x, src, maxPayloadDigitLen, env.parseDiscardNanPayload()) -> return
            isValidBidHexText(x, src) -> return
            isValidDpdHexText(x, src) -> return
            else -> x.setNaN()
        }
    }

    fun isFiniteValueText(x: MutDec, src: Latin1Iterator, ctx: DecContext): Boolean {
        var hasCoefficientDigit = false
        var significantDigitCount = 0 // does not count leading zeros
        var leadingFractionalZeroCount = 0  // NEW: track leading zeros after decimal
        var hasDot = false
        var hasExp = false
        var hasExpDigit = false
        var expSign = false
        var expSignificantDigitCount = 0

        var ch = src.nextChar()
        var chLast = '\u0000'

        val sign = ch == '-'
        if (ch == '-' || ch == '+')
            ch = src.nextChar()
        var fractionalDigitCount = 0  // NOW: only counts significant fractional digits
        var coeff19 = 0L
        var coeff34 = 0L
        var guardDigit = 0
        var stickyBits = 0
        var exp = 0

        while (ch in '0'..'9' || ch == '.' || ch == '_') {
            when {
                ch in '0'..'9' -> {
                    val n = ch - '0'

                    hasCoefficientDigit = true
                    // Track leading fractional zeros separately
                    if (hasDot && significantDigitCount == 0 && n == 0) {
                        leadingFractionalZeroCount++
                    } else {
                        significantDigitCount += (-(n or significantDigitCount)) ushr 31

                        if (significantDigitCount <= 19) {
                            coeff19 = coeff19 * 10L + n
                        } else if (significantDigitCount <= 34) {
                            coeff34 = coeff34 * 10L + n
                        } else if (significantDigitCount == 35) {
                            guardDigit = n
                        } else {
                            stickyBits = stickyBits or n
                        }

                        if (hasDot)
                            ++fractionalDigitCount
                    }
                }
                ch == '.' -> {
                    if (hasDot)
                        return false
                    if (chLast == '_')
                        return false
                    hasDot = true
                }
                ch == '_' -> {
                    if (!hasCoefficientDigit)
                        return false
                    if (hasDot && fractionalDigitCount == 0 && leadingFractionalZeroCount == 0)
                        return false
                }
            }
            chLast = ch
            ch = src.nextChar()
        }

        if (ch == 'E' || ch == 'e') {
            if (chLast == '_')
                return false
            hasExp = true
            ch = src.nextChar()
            if (ch == '_')
                return false
            if (ch == '+' || ch == '-') {
                expSign = ch == '-'
                ch = src.nextChar()
            }
            while (ch in '0'..'9' || ch == '_') {
                if (ch != '_') {
                    hasExpDigit = true
                    val eDigit = ch - '0'
                    expSignificantDigitCount +=
                        (-(eDigit or expSignificantDigitCount)) ushr 31
                    exp = exp * 10 + (ch - '0')
                } else {
                    if (!hasExpDigit)
                        return false
                }
                chLast = ch
                ch = src.nextChar()
            }
        }

        if (ch != '\u0000' ||
            chLast == '_' ||
            !hasCoefficientDigit ||
            hasExp && !hasExpDigit)
            return false

        // we have at least one digit ... but it might be all zeros
        val coeffDigitCount = min(34, significantDigitCount)
        x.c256Set64(coeff19)
        if (coeffDigitCount > 19) {
            val pow10 = coeffDigitCount - 19
            c256SetFmaPow10(x, x, pow10, coeff34)
        }

        if (expSignificantDigitCount > 4 || exp > 6200)
            exp = 6666

        val signedExp = if (expSign) -exp else exp
        val discardedDigitCount = significantDigitCount - coeffDigitCount
        val totalDigitsAfterDecimal = leadingFractionalZeroCount + fractionalDigitCount
        val qExp = signedExp - totalDigitsAfterDecimal + discardedDigitCount

        if (significantDigitCount == 0) {
            x.setZero(sign, qExp)
            return true
        }
        val hasDiscardedNonZero = guardDigit or stickyBits != 0

        if (!hasDiscardedNonZero && (qExp >= Q_TINY) && (qExp <= Q_MAX)) {
            x.steal = stealEncodeFNZ(sign, qExp, stealPackedLengths(x.steal))
            return true
        }
        val roundBit = if (guardDigit < 5) 0 else 1
        // if guardDigit > 5 then it is a sticky bit.
        val stickyBit = (-stickyBits or (5 - guardDigit)) ushr 31
        val residue = Residue.fromRoundBitStickBit(roundBit, stickyBit)
        x.roundAndFinalizeFnz(sign, qExp, residue, ctx)
        if (hasDiscardedNonZero)
            ctx.signalInexact(x)
        return true
    }

    fun isNanText(x: MutDec, src: Latin1Iterator, maxPayloadDigits: Int, discardNanPayload: Boolean): Boolean {
        src.rewind()
        x.setZero()
        var ch = src.nextChar()
        val sign = ch == '-'
        if (ch == '-' || ch == '+')
            ch = src.nextChar()
        val hasS = (ch.code or 0x20) == 's'.code
        if (hasS)
            ch = src.nextChar()
        for (target in "nan") {
            if ((ch.code or 0x20) != target.code)
                return false
            ch = src.nextChar()
        }
        var payloadDigitCount = 0
        var accumulator19 = 0L
        var accumulator38 = 0L
        while (ch in '0'..'9') {
            val d = (ch - '0').toLong()
            when {
                payloadDigitCount < 19 -> accumulator19 = (accumulator19 * 10L) + d
                payloadDigitCount < 38 -> accumulator38 = (accumulator38 * 10L) + d
                else -> return false
            }
            ++payloadDigitCount
            ch = src.nextChar()
        }
        if (ch != '\u0000' || payloadDigitCount > maxPayloadDigits)
            return false
        if (!discardNanPayload && payloadDigitCount > 0) {
            x.c256Set64(accumulator19)
            if (payloadDigitCount > 19)
                c256SetFmaPow10(x, x, payloadDigitCount - 19, accumulator38)
        }
        x.setNaN(sign, hasS, x.dw1, x.dw0)
        return true
    }

    fun isInfinityText(x: MutDec, src: Latin1Iterator): Boolean {
        src.rewind()
        var ch = src.nextChar()
        val sign = ch == '-'
        if (ch == '-' || ch == '+')
            ch = src.nextChar()
        var chLast = 0
        for (target in "infinity") {
            chLast = ch.code or 0x20
            if (chLast != target.code)
                return false
            ch = src.nextChar()
            if (ch == '\u0000')
                break
        }
        if (ch != '\u0000' || (chLast != 'f'.code && chLast != 'y'.code))
            return false
        x.setInfinite(sign)
        return true
    }

    fun isValidBidHexText(x: MutDec, src: Latin1Iterator): Boolean {
        src.rewind()
        var ch = src.nextChar()
        if (ch != '[')
            return false
        if (! parseHexDword(src, x))
            return false
        val bidHi = x.dw0
        if (src.peek() == ',')
            src.nextChar()
        if (! parseHexDword(src, x))
            return false
        val bidLo = x.dw0
        ch = src.nextChar()
        if (ch != ']' || src.hasNext())
            return false
        x.setBid128(bidHi, bidLo)
        return true
    }

    private fun parseHexDword(src: Latin1Iterator, x: MutDec): Boolean {
        var dw = 0L
        for (i in 0..15) {
            val ch = src.nextChar()
            when {
                ch in '0'..'9' -> dw = (dw shl 4) or (ch - '0').toLong()
                ch in 'A'..'F' -> dw = (dw shl 4) or (ch - 'A' + 10).toLong()
                ch in 'a'..'f' -> dw = (dw shl 4) or (ch - 'a' + 10).toLong()
                else -> return false
            }
        }
        x.c256Set64(dw)
        return true
    }

    fun isValidDpdHexText(x: MutDec, src: Latin1Iterator): Boolean {
        src.rewind()
        if (src.nextChar() != '#')
            return false
        if (! parseHexDword(src, x))
            return false
        val dpdHi = x.dw0
        if (! parseHexDword(src, x))
            return false
        val dpdLo = x.dw0
        x.setDpd128(dpdHi, dpdLo)
        return true
    }

}
