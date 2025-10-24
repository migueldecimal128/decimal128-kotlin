package com.decimal128.decimal

import com.decimal128.decimal.Int256ParsePrint.int32ToUtf8
import com.decimal128.hugeint.Latin1Iterator
import com.decimal128.hugeint.StringLatin1Iterator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val SPECIAL_NAMES =
    ('I'.code.toLong() shl 0) or ('n'.code.toLong() shl 8) or ('f'.code.toLong() shl 16) or
            ('s'.code.toLong() shl 24) or ('N'.code.toLong() shl 32) or ('a'.code.toLong() shl 40) or ('N'.code.toLong() shl 48)

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

    fun decToString(x: MutDec) : String {
        val bytes = ByteArray(MAX_DEC34_CHAR_LEN)
        val cb = decToUtf8(x, bytes, 0, MAX_DEC34_CHAR_LEN)
        return String(bytes, 0, cb)
    }

    fun decToUtf8(x: MutDec, bytes: ByteArray, off: Int, len: Int) : Int {
        if (off < 0 || len < 0)
            throw IllegalArgumentException()
        val q = x.qExp
        insufficient_buffer@
        do {
            val limit = off + len
            val dw0 = x.dw0
            val signByte = if (x.sign) BYTE_MINUS else BYTE_PLUS
            bytes[off] = signByte
            var exp = q
            var ib = off + if (x.sign) 1 else 0
            if (q >= NON_FINITE_INF) {
                val isSNaN = if (q == NON_FINITE_SNAN) 1 else 0
                val shift = ((NON_FINITE_INF - q) shr 31) and (32 - (isSNaN shl 3))
                val chars = SPECIAL_NAMES ushr shift
                bytes[off + 1] = chars.toByte()
                bytes[off + 2] = (chars ushr  8).toByte()
                bytes[off + 3 + isSNaN] = (chars ushr 24).toByte()
                bytes[off + 3] = (chars ushr 16).toByte()
                ib = 4 + isSNaN
                if (q == NON_FINITE_INF || x.c256IsZero())
                    return ib - off
                // drop thru to add NaN payload
                exp = 0
            }
            val xDigitLen = x.digitLen
            val scale = -q
            val e = q + xDigitLen  + (-xDigitLen shr 31)
            val isInteger = scale == 0
            // one more case here ... isSciInteger == is single digit significand with exponent
            val isSciDecimal = !isInteger && (scale < 0 || e < -6) && xDigitLen > 1
            val isNonSciDecimal = !isInteger && !isSciDecimal && xDigitLen > q && e >= -6
            val isNonSciDecimalLT1 = isNonSciDecimal && scale >= xDigitLen
            val isNonSciDecimalGE1 = isNonSciDecimal && scale < xDigitLen
            if (isNonSciDecimalLT1) {
                val zeroCount = 2 + -e - 1
                bytes.fill(BYTE_ZERO, ib, ib + zeroCount)
                bytes[ib + 1] = BYTE_DOT
                ib += zeroCount
            } else if (isSciDecimal) {
                ++ib // skip a byte for the decimal point ... move first digit into this slot shortly
            }
            // render integer coeff, including a single 0
            if (len - ib < x.digitLen)
                break@insufficient_buffer
            Int256ParsePrint.u256ToUtf8(x, bytes, ib)
            ib += max(x.digitLen, 1) // if x.digitLen == 0 then 1
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
                exp = e
            }
            if (exp != 0) {
                if (limit - ib < 3)
                    break@insufficient_buffer
                bytes[ib++] = 'E'.code.toByte()
                val expSignChar = if (exp < 0) BYTE_MINUS else BYTE_PLUS
                // FIXME - figure out what to do about the sign char
                bytes[ib] = expSignChar
                ib += if (exp < 0) 0 else 1
                val wtf =  int32ToUtf8(exp, bytes, ib)
                ib += wtf
            }
            return ib - off
        } while (false)
        throw RuntimeException("insufficient buffer space")
    }

    fun decFromString(x: MutDec, str: String, env: DecEnv) =
        decFromText(x, StringLatin1Iterator(str), env)

    private fun decFromText(x: MutDec, src: Latin1Iterator, env: DecEnv) {
        val maxPayloadDigitLen = env.precision - 1
        when {
            isFiniteValueText(x, src, env) -> return
            isInfinityText(x, src) -> return
            isNanText(x, src, maxPayloadDigitLen, env.parseDiscardNanPayload()) -> return
            isValidBidHexText(x, src) -> return
            isValidDpdHexText(x, src) -> return
            else -> x.setNaN(if (env.parseDiscardNanPayload()) 0 else NAN_INVALID_SYNTAX, env)
        }
    }

    fun isFiniteValueText(x: MutDec, src: Latin1Iterator, env: DecEnv): Boolean {
        var hasCoefficientDigit = false
        var significantDigitCount = 0 // does not count leading zeros
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
        var fractionalDigitCount = 0
        var coeff19 = 0L
        // FIXME ... looks like I wired this to 34 digits and it will fail for
        //  38 digit DECIMAL128_EXTENDED
        var coeff34 = 0L
        var guardDigit = 0
        var stickyBits = 0
        var exp = 0

        while (ch in '0'..'9' || ch == '.' || ch == '_') {
            when {
                ch in '0'..'9' -> {
                    val n = ch - '0'
                    hasCoefficientDigit = true
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
                ch == '.' -> {
                    if (hasDot)
                        return false
                    if (chLast == '_')
                        return false
                    hasDot = true
                }
                ch == '_' -> {
                    if (! hasCoefficientDigit)
                        return false
                    if (hasDot && fractionalDigitCount == 0)
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
                        (eDigit or -expSignificantDigitCount) ushr 31
                    exp = exp * 10 + (ch - '0')
                } else {
                    if (! hasExpDigit)
                        return false
                }
                chLast = ch
                ch = src.nextChar()
            }
        }
        if (ch != '\u0000' ||
            chLast == '_' ||
            ! hasCoefficientDigit ||
            hasExp && !hasExpDigit ||
            expSignificantDigitCount > 9)
            return false
        // we have at least one digit
        val coeffDigitCount = min(34, significantDigitCount)
        x.c256Set64(coeff19)
        if (coeffDigitCount > 19) {
            val pow10 = coeffDigitCount - 19
            x.u256MutateFmaPow10(pow10, coeff34)
        }
        x.sign = sign
        val signedExp = if (expSign) -exp else exp
        val integerDigitCount = significantDigitCount - fractionalDigitCount
        val discardedIntegerDigitCount = max(0, integerDigitCount - 34)
        val qExp = signedExp + discardedIntegerDigitCount - fractionalDigitCount
        x.qExp = qExp
        // FIXME ... make sure that this limiting range and properly scaling subnormal values
        if (((guardDigit or stickyBits) == 0) && (qExp >= env.qTiny) && (qExp <= env.qMax))
            return true
        val roundBit = if (guardDigit < 5) 0 else 1
        val stickyBit = (-stickyBits) ushr 31
        val residue = Residue.residueFrom(roundBit, stickyBit)
        x.roundAndFinalize(residue, env)
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
                x.u256MutateFmaPow10(payloadDigitCount - 19, accumulator38)
        }
        x.sign = sign
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
