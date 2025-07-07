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

    fun int256ToHexString(sign: Boolean, u: U256): String {
        val s = if (sign) 1 else 0
        if (u.bitLen == 0)
            return "0x0"
        val hexitCount = (u.bitLen + 3) ushr 2
        val bytes = ByteArray(s + 2 + hexitCount)
        bytes[0    ] = '-'.code.toByte()
        bytes[s    ] = '0'.code.toByte()
        bytes[s + 1] = 'x'.code.toByte()
        u256ToHexChars(u, hexitCount, bytes, s + 2)
        return String(bytes, StandardCharsets.UTF_8)
    }

    fun u256ToHexChars(u: U256, hexitCount: Int, bytes: ByteArray, off: Int) {
        var hexitsRemaining = hexitCount
        var ich = off
        for (i in u.bitLen ushr 6 downTo 0) {
            val dw = u[i]
            val thisHexitCount = if ((hexitsRemaining and 0x0F) != 0) (hexitsRemaining and 0x0F) else 16
            u64ToHexChars(dw, thisHexitCount, bytes, ich)
            hexitsRemaining -= thisHexitCount
            ich += thisHexitCount
        }
        assert(hexitsRemaining == 0)
        assert(ich == bytes.size)
    }

    fun u64ToHexChars(dw: Long, hexitCount: Int, bytes: ByteArray, off: Int) {
        var t = dw
        assert(hexitCount in 1..16)
        for (i in hexitCount - 1 downTo 0) {
            val h = (t and 0x0FL).toInt()
            val ch = if (h < 10) '0' + h else 'A' - 10 + h
            bytes[off + i] = ch.code.toByte()
            t = t ushr 4
        }
    }

    fun u256FromString(u: U256, allowSign: Boolean, str: String): Boolean {
        u.u256SetZero()
        val strLen = str.length
        if (strLen == 0)
            throw IllegalArgumentException("cannot parse empty string")
        var totalDigitCount = 0
        var accumulator = 0L
        var accumulatorDigitCount = 0
        var nonZeroSeen = false
        var i = 0
        val sign = (
                if (allowSign && (str[0] == '-' || str[0] == '+')) {
                    ++i
                    str[0] == '-'
                } else {
                    false
                })
        if (i + 1 < strLen && str[i] == '0' && (str[i + 1].code or 0x20) == 'x'.code) {
            u256FromHexString(u, str, i)
            return sign
        }
        do {
            if (str[i] == '_')
                break
            var ch = '0'
            while (i < strLen) {
                ch = str[i++]
                if (ch !in '0'..'9') {
                    if (ch == '_')
                        continue
                    break
                }
                val n = ch - '0'
                ++totalDigitCount
                nonZeroSeen = nonZeroSeen or (n > 0)
                if (! nonZeroSeen)
                    continue
                accumulator = accumulator * 10 + n
                ++accumulatorDigitCount
                if (accumulatorDigitCount < 19)
                    continue
                u.u256MutateFmaPow10(19, accumulator)
                accumulator = 0L
                accumulatorDigitCount = 0
            }
            if (ch == '_')
                break
            if (accumulatorDigitCount > 0)
                u.u256MutateFmaPow10(accumulatorDigitCount, accumulator)
            if (totalDigitCount > 0)
                return sign
        } while (false)
        throw IllegalArgumentException("invalid syntax:$str")
    }

    private fun u256FromHexString(u: U256, str: String, ichStart: Int) {
        val strLen = str.length
        if (ichStart + 2 >= strLen)
            throw IllegalArgumentException("hex string too short")
        var accumulator = 0L
        var accumulatorHexitCount = 0
        var ich = ichStart + 2
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
            u.u256MutateShiftLeftOr(64, accumulator)
            accumulator = 0
            accumulatorHexitCount = 0
        }
        if (accumulatorHexitCount > 0)
            u.u256MutateShiftLeftOr(4 * accumulatorHexitCount, accumulator)
    }

}
