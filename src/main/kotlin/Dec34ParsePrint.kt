package com.decimal128

import java.lang.Math.unsignedMultiplyHigh
import java.nio.charset.StandardCharsets

private const val SPECIAL_NAMES =
    ('I'.code.toLong() shl 0) or ('n'.code.toLong() shl 8) or ('f'.code.toLong() shl 16) or
            ('s'.code.toLong() shl 24) or ('N'.code.toLong() shl 32) or ('a'.code.toLong() shl 40) or ('N'.code.toLong() shl 48)

private fun copyBytes(str: String, bytes: ByteArray, off: Int, len: Int) : Int {
    val strLen = str.length
    if (strLen > len)
        throw RuntimeException("insufficient buffer space")
    for (i in 0..<strLen) {
        bytes[off + i] = str[i].code.toByte()
    }
    return strLen
}

private fun u64ToChars_x(digitLen: Int, dw0: Long, bytes: ByteArray, off: Int, len: Int) : Int {
    val last = off + digitLen + (-digitLen shr 31)
    val count = last + 1 - off
    var d = dw0
    var i = count - 1
    do {
        val qA = unsignedMultiplyHigh(d, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
        val digitA = d - (qA * 10L)
        val qB = unsignedMultiplyHigh(qA, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
        val digitB = qA - (qB * 10L)
        val qC = unsignedMultiplyHigh(qB, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
        val digitC = qB - (qC * 10L)
        val qD = unsignedMultiplyHigh(qC, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
        val digitD = qC - (qD * 10L)

        bytes[off + i - 0] = ('0'.code + digitA).toByte()
        if (i == 0)
            break

        bytes[off + i - 1] = ('0'.code + digitB).toByte()
        if (i == 1)
            break

        bytes[off + i - 2] = ('0'.code + digitC).toByte()
        if (i == 2)
            break

        bytes[off + i - 3] = ('0'.code + digitD).toByte()
        if (i == 3)
            break

        d = qD
        i -= 4
    } while (i >= 0)
    return count
}

private fun u64ToChars(digitLen: Int, dw0: Long, bytes: ByteArray, off: Int, len: Int) : Int {
    val last = off + digitLen + (-digitLen shr 31)
    val count = last + 1 - off
    var d = dw0
    var i = count - 1
    do {
        val qA = unsignedMultiplyHigh(d, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
        val digitA = (( d - (qA * 10L)) + '0'.code).toByte()
        val qB = unsignedMultiplyHigh(qA, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
        val digitB = ((qA - (qB * 10L)) + '0'.code).toByte()
        val qC = unsignedMultiplyHigh(qB, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
        val digitC = ((qB - (qC * 10L)) + '0'.code).toByte()
        //val qD = unsignedMultiplyHigh(qC, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
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

private fun u128ToChars(digitLen: Int, dw1: Long, dw0: Long, bytes: ByteArray, off: Int, len: Int) : Int {
    var digitLenT = digitLen
    assert(dw1 != 0L)
    val divisor = 1_000_000_000L
    val mu = 0x44B82FA09
    val (q1, q0, r0) = barrettDivMod_32_128(dw1, dw0, divisor, mu)
    val cbHi = (
            if (q1 != 0L)
                u128ToChars(digitLen - 9, q1, q0, bytes, off, len)
            else
                u64ToChars(digitLen - 9, q0, bytes, off, len)
            )
    val cbLo = u64ToChars(9, r0, bytes, off + cbHi, len - cbHi)
    return cbHi + cbLo
}

@Suppress("NOTHING_TO_INLINE")
private inline fun barrettDivMod_32_128(dw1: Long, dw0: Long, denom: Long, mu: Long): Triple<Long, Long, Long> {

    val dwA = dw0 and 0xFFFF_FFFFL
    val dwB = dw0 ushr 32
    val dwG = dw1

    val qGhat = unsignedMultiplyHigh(dwG, mu)
    val rGhat = dwG - (qGhat * denom)
    val adjustG = rGhat >= denom
    val qG = qGhat + if (adjustG) 1L else 0L
    val rG = rGhat - if (adjustG) denom else 0L

    val ppB = (rG shl 32) or dwB
    val qBhat = unsignedMultiplyHigh(ppB, mu)
    val rBhat = ppB - (qBhat * denom)
    val adjustB = rBhat >= denom
    val qB = qBhat + if (adjustB) 1L else 0L
    val rB = rBhat - if (adjustB) denom else 0L

    val ppA = (rB shl 32) or dwA
    val qAhat = unsignedMultiplyHigh(ppA, mu)
    val rAhat = ppA - (qAhat * denom)
    val adjustA = rAhat >= denom
    val qA = qAhat + if (adjustA) 1L else 0L
    val rA = rAhat - if (adjustA) denom else 0L

    val remainder = rA

    val q1 = qG
    val q0 = (qB shl 32) or qA

    return Triple(q1, q0, remainder)
}

private const val BYTE_ZERO = '0'.code.toByte()
private const val BYTE_DOT = '.'.code.toByte()
private const val BYTE_PLUS = '+'.code.toByte()
private const val BYTE_MINUS = '-'.code.toByte()

object Dec34ParsePrint {

    fun decToString(x: Dec34) : String {
        val bytes = ByteArray(MAX_DEC34_CHAR_LEN)
        val cb = decToChars(x, bytes, 0, MAX_DEC34_CHAR_LEN)
        return String(bytes, 0, cb, StandardCharsets.UTF_8)
    }

    fun decToChars(x: Dec34, bytes: ByteArray, off: Int, len: Int) : Int {
        if (off < 0 || len < 0)
            throw IllegalArgumentException()
        val q = x.qExp
        insufficient_buffer@
        do {
            val limit = off + len
            val dw0 = x.dw0
            val signByte = if (x.sign == 0) BYTE_PLUS else BYTE_MINUS
            bytes[off] = signByte
            var exp = q
            var ib = off + x.sign
            if (q >= NON_FINITE_INF) {
                val isSNaN = if (q == NON_FINITE_SNAN) 1 else 0
                val shift = ((NON_FINITE_INF - q) shr 31) and (32 - (isSNaN shl 3))
                val chars = SPECIAL_NAMES ushr shift
                bytes[off + 1] = chars.toByte()
                bytes[off + 2] = (chars ushr  8).toByte()
                bytes[off + 3 + isSNaN] = (chars ushr 24).toByte()
                bytes[off + 3] = (chars ushr 16).toByte()
                ib = 4 + isSNaN
                if (q == NON_FINITE_INF || x.coeffIsZero())
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
            } else if (isSciDecimal)
                ++ib
            // render integer coeff, including a single 0
            when {
                (len - ib < x.digitLen) -> break@insufficient_buffer
                (x.bitLen <= 64) -> ib += u64ToChars(x.digitLen, dw0, bytes, ib, limit - ib)
                (x.bitLen <= 128) -> ib += u128ToChars(x.digitLen, x.dw1, dw0, bytes, ib, limit - ib)
                else -> throw RuntimeException("coeff.bitLen > 128 not impl")
            }
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
                val coeffStart = off + x.sign
                bytes[coeffStart] = bytes[coeffStart + 1]
                bytes[coeffStart + 1] = BYTE_DOT
                exp = e
            }
            if (exp != 0) {
                if (limit - ib < 3)
                    break@insufficient_buffer
                bytes[ib++] = 'E'.code.toByte()
                val expAbs = Math.abs(exp.toLong())
                val expByte = if (exp < 0) BYTE_MINUS else BYTE_PLUS
                bytes[ib++] = expByte
                val expDigitLen = CoeffPow10.calcDigitLen64(expAbs)
                if (limit - ib < expDigitLen)
                    break@insufficient_buffer
                ib += u64ToChars(expDigitLen, expAbs, bytes, ib, limit - ib)
            }
            return ib - off
        } while (false)
        throw RuntimeException("insufficient buffer space")
    }

    fun decFromString(x: Dec34, str: String, ctx: Decimal128Context) {
        var ichFirstSignificantDigit = -1 // strips leading zeros, but not the last one
        var significantDigitCount = 0 // does not count leading zeros
        var ichDot = -1
        var ichExp = -1
        var ichExpFirstSignificantDigit = -1 // strips leading zeros, but not the last one
        var expSignificantDigitCount = 0

        val strLen = str.length
        var ich = 0
        var ch: Char

        var sign = 0
        var fractionalDigitCount = 0
        var coeff19 = 0L
        var coeff34 = 0L
        var guardDigit = 0
        var stickyBits = 0
        var expSign = 0
        var exp = 0

        invalid_syntax@
        do {
            no_more_chars@
            do {
                if (ich == strLen)
                    break@no_more_chars
                ch = str[ich++]
                if (ch == '+' || ch == '-') {
                    sign = if (ch == '-') 1 else 0
                    if (ich == strLen)
                        break@no_more_chars
                    ch = str[ich++]
                }
                while (ch in '0'..'9' || ch == '.' || ch == '_') {
                    when {
                        ch in '0'..'9' -> {
                            val n = ch - '0'
                            significantDigitCount +=
                                (-(n or significantDigitCount)) ushr 31
                            ichFirstSignificantDigit = (
                                    if ((ichFirstSignificantDigit or -significantDigitCount) < 0)
                                        ich
                                    else
                                        ichFirstSignificantDigit
                                    )
                            if (significantDigitCount <= 19) {
                                coeff19 = coeff19 * 10L + n
                            } else if (significantDigitCount <= 34) {
                                coeff34 = coeff34 * 10 + n
                            } else if (significantDigitCount == 35) {
                                guardDigit = n
                            } else {
                                stickyBits = stickyBits or n
                            }
                            if (ichDot >= 0)
                                ++fractionalDigitCount
                        }
                        ch == '.' -> {
                            if (ichDot >= 0)
                                break@invalid_syntax
                            ichDot = ich
                        }
                    }
                    if (ich == strLen)
                        break@no_more_chars
                    ch = str[ich++]
                }
                if (ch == 'E' || ch == 'e') {
                    ichExp = ich
                    if (ich == strLen)
                        break@no_more_chars
                    ch = str[ich++]
                    if (ch == '+' || ch == '-') {
                        expSign = if (ch == '-') 1 else 0
                        if (ich == strLen)
                            break@no_more_chars
                        ch = str[ich++]
                    }
                    while (ch in '0'..'9' || ch == '_') {
                        if (ch in '0'..'9') {
                            expSignificantDigitCount +=
                                (('0' - ch) or -expSignificantDigitCount) ushr 31
                            ichExpFirstSignificantDigit = (
                                    if ((ichExpFirstSignificantDigit or -expSignificantDigitCount) < 0)
                                        ich
                                    else
                                        ichExpFirstSignificantDigit
                                    )
                            exp = exp * 10 + (ch - '0')
                        }
                        if (ich == strLen)
                            break@no_more_chars
                        ch = str[ich++]
                    }
                }
                // extraneous chars ... invalid syntax
                break@invalid_syntax
            } while (false)
            // no more chars
            if ((ichFirstSignificantDigit < 0) ||
                (ichExp > 0 && ichExpFirstSignificantDigit < 0) ||
                (expSignificantDigitCount > 9)
                )
                break@invalid_syntax
            // we have at least one digit
            val coeffDigitCount = Math.min(34, significantDigitCount)
            x.coeffSet64(coeff19)
            if (coeffDigitCount > 19) {
                val pow10 = coeffDigitCount - 19
                x.coeffMutateFmaPow10(pow10, coeff34)
                }
            x.sign = sign
            val signedExp = (exp xor -expSign) + expSign
            val integerDigitCount = significantDigitCount - fractionalDigitCount
            val discardedIntegerDigitCount = Math.max(0, integerDigitCount - 34)
            val qExp = signedExp + discardedIntegerDigitCount - fractionalDigitCount
            x.qExp = qExp
            if (((guardDigit or stickyBits) == 0) && (qExp >= Q_EXP_TINY) && (qExp <= Q_EXP_MAX))
                return
            val roundBit = if (guardDigit < 5) 0 else 1
            val stickyBit = (-stickyBits) ushr 31
            val residue = Residue.residueFrom(roundBit, stickyBit)
            x.roundAndFinalize(residue, sign, ctx)
            return
        } while (false)
        // invalid syntax
        x.setNaN(NAN_INVALID_SYNTAX, ctx)
        return
    }
}
