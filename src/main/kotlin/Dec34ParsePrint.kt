package com.decimal128

import java.lang.Math.unsignedMultiplyHigh
import java.nio.charset.StandardCharsets

private val SPECIAL_NAMES = arrayOf("Inf", "NaN", "sNaN")

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


object Dec34ParsePrint {

    fun toString(x: Dec34) : String {
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
            val signChar = if (x.sign == 0) '+' else '-'
            bytes[off] = signChar.code.toByte()
            var ib = off + x.sign
            if (q >= NON_FINITE_INF) {
                val str = SPECIAL_NAMES[q - NON_FINITE_INF]
                ib = off + 1 + copyBytes(str, bytes, off + 1, limit - ib)
                if (x.qExp == NON_FINITE_INF || x.coeffIsZero())
                    return ib - off
                // drop thru to add NaN payload
            }
            val xDigitLen = x.digitLen
            var exp = q
            val scale = -q
            val e = q + xDigitLen  + (-xDigitLen shr 31)
            val isInteger = scale == 0
            // one more case here ... isSciInteger == is single digit significand with exponent
            val isSciDecimal = !isInteger && (scale < 0 || e < -6) && xDigitLen > 1
            val hasNonSciDecimal = !isInteger && !isSciDecimal && xDigitLen > q && e >= -6
            val isNonSciDecimalLT1 = hasNonSciDecimal && scale >= xDigitLen
            val isNonSciDecimalGE1 = hasNonSciDecimal && scale < xDigitLen
            if (isNonSciDecimalLT1) {
                val zeroCount = 2 + -e - 1
                bytes.fill('0'.code.toByte(), ib, ib + zeroCount)
                bytes[ib + 1] = '.'.code.toByte()
                ib += zeroCount
                exp = 0
            } else if (isSciDecimal)
                ++ib
            // render integer coeff, including a single 0
            when {
                (len - ib < x.digitLen) -> break@insufficient_buffer
                (x.bitLen <= 64) -> ib += u64ToChars(x.digitLen, dw0, bytes, ib, limit - ib)
                (x.bitLen <= 128) -> ib += u128ToChars(x.digitLen, x.dw1, dw0, bytes, ib, limit - ib)
                else -> throw RuntimeException("coeff.bitLen > 128 not impl")
            }
            if (isSciDecimal) {
                val coeffStart = off + x.sign
                bytes[coeffStart] = bytes[coeffStart + 1]
                bytes[coeffStart + 1] = '.'.code.toByte()
                exp = e
            }
            if (isNonSciDecimalGE1) {
                val decimalIndex = ib - scale
                System.arraycopy(bytes, decimalIndex, bytes, decimalIndex + 1, scale)
                bytes[decimalIndex] = '.'.code.toByte()
                return ib + 1 - off
            }
            if (exp != 0) {
                if (limit - ib < 3)
                    break@insufficient_buffer
                bytes[ib++] = 'E'.code.toByte()
                val expAbs = Math.abs(exp.toLong())
                val expSign = (if (exp < 0) '-' else '+').code.toByte()
                bytes[ib++] = expSign
                val expDigitLen = CoeffPow10.calcDigitLen64(expAbs)
                if (limit - ib < expDigitLen)
                    break@insufficient_buffer
                ib += u64ToChars(expDigitLen, expAbs, bytes, ib, limit - ib)
            }
            return ib - off
        } while (false)
        throw RuntimeException("insufficient buffer space")
    }
}
