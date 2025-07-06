package com.decimal128

import java.lang.Math.unsignedMultiplyHigh
import java.nio.charset.StandardCharsets
import java.lang.Math.max

private const val DIVISOR_1E9 = 1_000_000_000L
private const val MU_1E9 = 0x44B82FA09

private val SINGLE_DIGIT_NUMBERS =
    arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "-0", "-1", "-2", "-3", "-4", "-5", "-6", "-7", "-8", "-9")

internal object Int256ParsePrint {

    fun int256ToString(sign: Boolean, u: U256): String {
        val s = if (sign) 1 else 0
        if (u.digitLen > 1) {
            val bytes = ByteArray(u.digitLen + s)
            bytes[0] = '-'.code.toByte() // if positive then this will be overwritten
            u256ToChars(u, bytes, s)
            return String(bytes, StandardCharsets.UTF_8)
        } else {
            return SINGLE_DIGIT_NUMBERS[(10 and -s) + u.dw0.toInt()]
        }
    }

    fun u256ToChars(u: U256, bytes: ByteArray, off: Int) {
        if (u.bitLen <= 64) {
            u64ToChars(u.digitLen, u.dw0, bytes, off)
            return
        }
        val t = U256(u)
        while (t.bitLen > 192) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_256(t, t, DIVISOR_1E9, MU_1E9)
            u64ToChars(9, r, bytes, off + ich)
        }
        while (t.bitLen > 128) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_192(t, t, DIVISOR_1E9, MU_1E9)
            u64ToChars(9, r, bytes, off + ich)
        }
        while (t.bitLen > 64) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_128(t, t, DIVISOR_1E9, MU_1E9)
            u64ToChars(9, r, bytes, off + ich)
        }
        u64ToChars(max(1, t.digitLen), t.dw0, bytes, off)
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

    fun u256FromString(c: U256, str: String) {
        c.u256SetZero()
        val strLen = str.length
        when {
            (strLen == 0) ->
                throw IllegalArgumentException("cannot parse empty string")
            str.startsWith("0x") -> {
                u256FromHexString(c, str)
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
            c.u256MutateFmaPow10(19, accumulator)
            accumulator = 0L
            accumulatorDigitCount = 0
        }
        if (accumulatorDigitCount > 0)
            c.u256MutateFmaPow10(accumulatorDigitCount, accumulator)
    }

    private fun u256FromHexString(c:U256, str: String) {
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
            c.u256MutateShiftLeftOr(64, accumulator)
            accumulator = 0
            accumulatorHexitCount = 0
        }
        if (accumulatorHexitCount > 0)
            c.u256MutateShiftLeftOr(4 * accumulatorHexitCount, accumulator)
    }

}
