package com.decimal128

import java.lang.Math.unsignedMultiplyHigh
import java.nio.charset.StandardCharsets

private const val DIVISOR_1E9 = 1_000_000_000L
private const val MU_1E9 = 0x44B82FA09

private val SINGLE_DIGIT_NUMBERS =
    arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")

object CoeffPrintParse {

    fun coeffToString(c: Coeff): String {
        if (c.digitLen > 1) {
            val bytes = ByteArray(c.digitLen)
            coeffToChars(c, bytes)
            return String(bytes, StandardCharsets.UTF_8)
        } else {
            return SINGLE_DIGIT_NUMBERS[c.dw0.toInt()]
        }
    }

    private fun coeffToChars(c: Coeff, bytes: ByteArray) {
        if (c.bitLen <= 64) {
            u64ToChars(c.digitLen, c.dw0, bytes, 0)
            return
        }
        val t = Coeff(c)
        while (t.bitLen > 192) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_256(t, t, DIVISOR_1E9, MU_1E9)
            u64ToChars(9, r, bytes, ich)
        }
        while (t.bitLen > 128) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_192(t, t, DIVISOR_1E9, MU_1E9)
            u64ToChars(9, r, bytes, ich)
        }
        while (t.bitLen > 64) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_128(t, t, DIVISOR_1E9, MU_1E9)
            u64ToChars(9, r, bytes, ich)
        }
        u64ToChars(Math.max(1, t.digitLen), t.dw0, bytes, 0)
    }

    private fun u64ToChars(digitLen: Int, dw0: Long, bytes: ByteArray, off: Int) {
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

            val tC = i - 2; val maskC = -tC shr 31; val iC = tC and maskC
            val tB = i - 1; val maskB = -tB shr 31; val iB = tB and maskB

            bytes[off + iC] = digitC
            bytes[off + iB] = digitB
            bytes[off + i] = digitA

            d = qC
            i -= 3
        } while (i >= 0)
    }

    fun coeffFromString(c: Coeff, str: String) {
        c.coeffSetZero()
        val strLen = str.length
        when {
            (strLen == 0) ->
                throw IllegalArgumentException("cannot parse empty string")
            str.startsWith("0x") -> {
                coeffFromHexString(c, str)
                return
            }
        }

        var totalDigitCount = 0
        var accumulator = 0L
        var accumulatorDigitCount = 0
        var i = 0
        while (i < strLen && str[i] == '0')
            ++i
        while (i < strLen) {
            val ch = str[i++]
            if (ch !in '0'..'9') {
                if (ch == '_' && i > 0)
                    continue
                throw RuntimeException("unsigned integer parse error")
            }
            val n = ch - '0'
            ++totalDigitCount
            accumulator = accumulator * 10 + n
            ++accumulatorDigitCount
            if (accumulatorDigitCount < 19)
                continue
            c.coeffMutateFmaPow10(19, accumulator)
            accumulator = 0L
            accumulatorDigitCount = 0
        }
        if (accumulatorDigitCount > 0)
            c.coeffMutateFmaPow10(accumulatorDigitCount, accumulator)
    }

    private fun coeffFromHexString(c:Coeff, str: String) {
        val strLen = str.length
        if (strLen < 3)
            throw IllegalArgumentException("hex string too short")
        var accumulator = 0L
        var accumulatorHexitCount = 0
        var ich = 3
        while (ich < strLen) {
            val ch = str[ich++]
            if (ch == '_')
                continue
            val n = (
                    if (ch in '0'..'9')
                        ch - '0'
                    else if ((ch.code or 0x20) in 'a'.code .. 'f'.code)
                        (ch.code or 0x20) - 'a'.code + 10
                    else
                        throw IllegalArgumentException("invalid hex:" + str)
                    )
            accumulator = (accumulator shl 4) or n.toLong()
            ++accumulatorHexitCount
            if (accumulatorHexitCount < 16)
                continue
            c.coeffMutateShiftLeftOr(64, accumulator)
            accumulator = 0
            accumulatorHexitCount = 0
        }
        if (accumulatorHexitCount > 0)
            c.coeffMutateShiftLeftOr(4 * accumulatorHexitCount, accumulator)
    }

}
