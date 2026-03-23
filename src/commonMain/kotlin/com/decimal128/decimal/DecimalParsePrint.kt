package com.decimal128.decimal

import com.decimal128.decimal.IntegerParsePrint.int32ToUtf8
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val SPECIAL_NAME_INF =
    ('I'.code.toLong() shl 0) or ('n'.code.toLong() shl 8) or ('f'.code.toLong() shl 16)
private const val SPECIAL_NAME_NAN =
    ('N'.code.toLong() shl 0) or ('a'.code.toLong() shl 8) or ('N'.code.toLong() shl 16)

private const val UPPER_CASE_MASK = ('a'.code - 'A'.code).inv()

private const val INFINITY_CHARS_BACKWARDS = (('y'.code.toLong() shl 56) or ('t'.code.toLong() shl 48) or
        ('i'.code.toLong() shl 40) or ('n'.code.toLong() shl 32) or
        ('i'.code.toLong() shl 24) or ('f'.code.toLong() shl 16) or
        ('n'.code.toLong() shl  8) or ('I'.code.toLong()))

private fun u64ToUtf8(digitLen: Int, dw0: Long, bytes: ByteArray, off: Int, len: Int) : Int {
    val last = off + digitLen + (-digitLen shr 31)
    val count = last + 1 - off
    var d = dw0
    var i = count - 1
    do {
        val qA = unsignedMulHi(d, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
        val digitA = (( d - (qA * 10L)) + '0'.code).toByte()
        val qB = unsignedMulHi(qA, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
        val digitB = ((qA - (qB * 10L)) + '0'.code).toByte()
        val qC = unsignedMulHi(qB, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
        val digitC = ((qB - (qC * 10L)) + '0'.code).toByte()
        //val qD = umulHigh(qC, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
        //val digitD = qC - (qD * 10L)

        //val iD = i - 3; val maskD = -iD shr 31
        val tC = i - 2; val maskC = -tC shr 31; val iC = tC and maskC
        val tB = i - 1; val maskB = -tB shr 31; val iB = tB and maskB

        //bytes[off + (iD and maskD)] = ('0'.code + digitD).toByte()
        bytes[off + iC] = digitC
        bytes[off + iB] = digitB
        bytes[off + i] = digitA

        //d = qD
        //i -= 4
        d = qC
        i -= 3
    } while (i >= 0)
    return count
}

private const val BYTE_ZERO = '0'.code.toByte()
private const val BYTE_DOT = '.'.code.toByte()
private const val BYTE_PLUS = '+'.code.toByte()
private const val BYTE_MINUS = '-'.code.toByte()

object DecimalParsePrint {

    fun decToString(x: MutDec, ctx: DecContext? = null) : String {
        val prefs = ctx?.decPrefs ?: DecPrefs.KOTLIN_DEFAULT
        val printLen = calcPrintLen(x, prefs)
        val bytes = ctx?.tmps?.bytesPrint ?: ByteArray(MAX_DEC38_CHAR_LEN)
        val cb = decToUtf8_2(x, bytes, 0, prefs, ctx?.tmps?.c256Print)
        // FIXME ... ok ... so this fails when we pass in a
        //  value that is too long ... exceeds 34 digits
        //  At a minimum it should work for 38 digits DECIMAL_128_EXTENDED
        //  Separately, pull the ByteArray from env.decTemps
        if (printLen < cb)
            println("calculated printLen too low printLen:$printLen cb:$cb x:$x")
        return String(bytes, 0, cb)
    }

    private fun calcPrintLen(md: MutDec, prefs: DecPrefs): Int {
        return when {
            md.isFinite() -> calcPrintLenFinite(md, prefs)
            md.isInfinite() -> calcPrintLenInfinite(md, prefs)
            else -> calcPrintLenNaN(md, prefs)
        }
    }

    private fun calcPrintLenFinite(md: MutDec, prefs: DecPrefs): Int {
        val signLen = if (md.sign or prefs.printValuePlusSign) 1 else 0
        val expSignLen = if (md.qExp < 0 || md.qExp > 0 && prefs.printExponentPlusSign) 1 else 0
        val expLen = calcBitLen64(abs(md.qExp).toLong())
        val engineeringStringPadding = if (prefs.printEngineeringString) 3 else 0
        val dotLen = if (md.qExp == 0) 0 else 1
        val expELen = dotLen
        val coeffLen = when {
            md.qExp == 0 -> max(1, md.bitLen) // integer
            // FIXME ... zero might/will fail in this next case
            md.qExp < 0 && md.sciExp() >= -6 -> { // decimal point string
                val digitsRightOfDecimal = -md.qExp
                // FIXME ... why is there a max(foo, 0) here ... why can it go negative
                val leadingZeroCount = max(1 + digitsRightOfDecimal - md.digitLen, 0)
                val decimalPointLen = 1
                leadingZeroCount + decimalPointLen + md.digitLen
            }
            md.bitLen == 0 -> 1 + max(38, -min(0, md.qExp))
            else -> 38
        }
        return signLen + dotLen + coeffLen + expELen + expSignLen + expLen
    }

    private fun calcPrintLenInfinite(md: MutDec, prefs: DecPrefs): Int {
        val sign = if (md.sign or prefs.printValuePlusSign) 1 else 0
        val text = if (prefs.printInfinity8Chars) 8 else 3
        return sign + text
    }

    private fun calcPrintLenNaN(md: MutDec, prefs: DecPrefs): Int {
        val sign = if (prefs.printNaNSign and (md.sign or prefs.printNaNPlusSign)) 1 else 0
        val text = 3 + if (md.isSignaling() && !prefs.printCollapseSNaN) 1 else 0
        val payload = md.digitLen
        return sign + text + payload
    }

    private fun decToUtf8_2(x: MutDec, bytes: ByteArray, off: Int, prefs: DecPrefs, tmp: C256?): Int {
        val signByte = if (x.sign) BYTE_MINUS else BYTE_PLUS
        bytes[off] = signByte
        val signWidth = if (x.sign or prefs.printValuePlusSign) 1 else 0
        var ib = off + signWidth
        val qExp = x.qExp
        if (!x.isFinite()) {
            return if (x.isInfinite())
                infiniteToUtf8(x, bytes, ib, prefs) + signWidth
            else
                nanToUtf8(x, bytes, off, prefs, tmp)
        }
        val sciExp = x.sciExp()
        ib += when {
            qExp == 0 -> return finiteIntegerToUtf8(x, bytes, ib, tmp) + signWidth
            qExp < 0 && sciExp >= -6 -> return finiteNonScientificDecimalToUtf8(x, bytes, ib, tmp) + signWidth
            x.digitLen > 1 -> finiteScientificDecimalToUtf8(x, bytes, ib, tmp)
            else ->  finiteSingleDigitScientificToUtf8(x, bytes, ib)
        }
        bytes[ib++] = (if (prefs.printExponentUppercaseE) 'E' else 'e').code.toByte()
        bytes[ib] = (if (sciExp < 0) '-' else '+').code.toByte()
        ib += if ((sciExp < 0) or prefs.printExponentPlusSign) 1 else 0
        val sciExpAbs = abs(sciExp)
        ib += IntegerParsePrint.int32ToUtf8(sciExpAbs, bytes, ib)
        val cb = ib - off
        return cb
    }

    private fun finiteIntegerToUtf8(x: MutDec, bytes: ByteArray, off: Int, tmp: C256?): Int =
        IntegerParsePrint.c256ToUtf8(x, bytes, off, tmp)

    private fun finiteSingleDigitScientificToUtf8(x: MutDec, bytes: ByteArray, offWithSign: Int): Int {
        bytes[offWithSign] = (x.dw0.toInt() + '0'.code).toByte()
        return 1
    }

    private fun finiteNonScientificDecimalToUtf8(x: MutDec, bytes: ByteArray, off: Int, tmp: C256?): Int {
        val printDigitLen = x.digitLen + 1 - (-x.digitLen ushr 31)
        val digitsLeftOfDot = printDigitLen + x.qExp
        if (digitsLeftOfDot > 0) {
            val cb = IntegerParsePrint.c256ToUtf8(x, bytes, off + 1, tmp)
            for (i in 0..<digitsLeftOfDot)
                bytes[off + i] = bytes[off + i + 1]
            bytes[off + digitsLeftOfDot] = BYTE_DOT
            return cb + 1
        }
        val zerosRightOfDot = -digitsLeftOfDot
        val leadingZeroPlusDotCount = 1 + 1 + zerosRightOfDot
        for (i in 0..<leadingZeroPlusDotCount)
            bytes[off + i] = BYTE_ZERO
        bytes[off + 1] = BYTE_DOT
        val cb = IntegerParsePrint.c256ToUtf8(x, bytes, off + leadingZeroPlusDotCount, tmp)
        return leadingZeroPlusDotCount + cb
    }

    private fun finiteScientificDecimalToUtf8(x: MutDec, bytes: ByteArray, off: Int, tmp: C256?): Int {
        val cb = IntegerParsePrint.c256ToUtf8(x, bytes, off + 1, tmp)
        bytes[off] = bytes[off + 1]
        bytes[off + 1] = BYTE_DOT
        return cb + 1
    }

    private fun finiteToUtf8(x: MutDec, bytes: ByteArray, off: Int, prefs: DecPrefs, tmp: C256): Int {
        val qExp = x.qExp
        val signByte = if (x.sign) BYTE_MINUS else BYTE_PLUS
        bytes[off] = signByte
        var ib = off + if (x.sign or prefs.printCollapseSNaN) 1 else 0
        var exp = qExp

        val digitLen = x.digitLen
        val scale = -qExp
        val eSci = x.sciExp() // = qExp + digitLen  + (-digitLen shr 31)
        val isInteger = scale == 0
        // one more case here ... isSciInteger == is single digit significand with exponent
        val isSciDecimal = !isInteger && (scale < 0 || eSci < -6) && digitLen > 1
        val isNonSciDecimal = !isInteger && !isSciDecimal && digitLen > qExp && eSci >= -6
        val isNonSciDecimalLT1 = isNonSciDecimal && scale >= digitLen
        val isNonSciDecimalGE1 = isNonSciDecimal && scale < digitLen
        if (isNonSciDecimalLT1) {
            val zeroCount = 2 + -eSci - 1
            bytes.fill(BYTE_ZERO, ib, ib + zeroCount)
            bytes[ib + 1] = BYTE_DOT
            ib += zeroCount
        } else if (isSciDecimal) {
            ++ib // skip a byte for the decimal point ... move first digit into this slot shortly
        }
        // render integer coeff, including a single 0
        ib += IntegerParsePrint.c256ToUtf8(x, bytes, ib, tmp)
        if (isNonSciDecimal) {
            if (isNonSciDecimalGE1) {
                val decimalIndex = ib - scale
                System.arraycopy(bytes, decimalIndex, bytes, decimalIndex + 1, scale)
                bytes[decimalIndex] = BYTE_DOT
                ++ib
            }
            return ib - off
        }
        if (isSciDecimal) {
            val coeffStart = off + if (x.sign) 1 else 0
            bytes[coeffStart] = bytes[coeffStart + 1]
            bytes[coeffStart + 1] = BYTE_DOT
            exp = eSci
        }
        if (exp != 0) {
            bytes[ib++] = 'E'.code.toByte()
            val expSignChar = if (exp < 0) BYTE_MINUS else BYTE_PLUS
            // FIXME - figure out what to do about the sign char
            bytes[ib] = expSignChar
            ib += if (exp < 0) 0 else 1
            val wtf =  int32ToUtf8(exp, bytes, ib)
            ib += wtf
        }
        return ib - off
    }

    private fun infiniteToUtf8(z: MutDec, utf8: ByteArray, off: Int, prefs: DecPrefs): Int {
        var ib = off
        val charLen = if (prefs.printInfinity8Chars) 8 else 3
        var charsRemaining= charLen
        var shifter = INFINITY_CHARS_BACKWARDS
        val upperCaseMask = if (prefs.printInfinityAllCaps) UPPER_CASE_MASK else 0x7F
        do {
            utf8[ib++] = (shifter.toInt() and upperCaseMask).toByte()
            shifter = shifter ushr 8
            --charsRemaining
        } while (charsRemaining > 0)
        return ib - off
    }

    private fun nanToUtf8(z: MutDec, utf8: ByteArray, off: Int, prefs: DecPrefs, tmp: C256?): Int {
        var ib = off
        // write the sign ... but it might be overwritten
        utf8[off] = (if (z.sign) '-' else '+').code.toByte()
        ib += if (prefs.printNaNSign and (z.sign or prefs.printNaNPlusSign)) 1 else 0
        val upperCaseMask = if (prefs.printNaNAllCaps) UPPER_CASE_MASK else 0x7F
        utf8[ib  ] = ('s'.code or upperCaseMask).toByte()
        ib += if (z.isSignaling() && !prefs.printCollapseSNaN) 1 else 0
        utf8[ib    ] =  'N'.code.toByte()
        utf8[ib + 1] = ('a'.code or upperCaseMask).toByte()
        utf8[ib + 2] =  'N'.code.toByte()
        ib += 3
        if (z.bitLen > 0)
            ib += IntegerParsePrint.c256ToUtf8(z, utf8, ib, tmp)
        return ib - off
    }

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
        x.sign = sign
        x.type = STEAL_TYP_FNZ
        x.qExp = qExp

        val hasDiscardedNonZero = guardDigit or stickyBits != 0

        // FIXME ... make sure that this limiting range and properly scaling subnormal values
        if (!hasDiscardedNonZero && (qExp >= Q_TINY) && (qExp <= Q_MAX))
            return true
        val roundBit = if (guardDigit < 5) 0 else 1
        // if guardDigit > 5 then it is a sticky bit.
        val stickyBit = (-stickyBits or (5 - guardDigit)) ushr 31
        val residue = Residue.fromRoundBitStickBit(roundBit, stickyBit)
        x.roundAndFinalizeFnz(residue, ctx)
        if (hasDiscardedNonZero)
            ctx.signalInexact(x)
        return true
    }

    fun isNanText(x: MutDec, src: Latin1Iterator, maxPayloadDigits: Int, discardNanPayload: Boolean): Boolean {
        src.reset()
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
        x.sign = sign
        x.type = if (hasS) STEAL_NAN_SNAN else STEAL_NAN_QNAN
        x.qExp = if (hasS) NON_FINITE_SNAN else NON_FINITE_QNAN
        return true
    }

    fun isInfinityText(x: MutDec, src: Latin1Iterator): Boolean {
        src.reset()
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
        src.reset()
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
        src.reset()
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
