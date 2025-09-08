package com.decimal128.decimal

import com.decimal128.hugeint.Latin1Iterator
import com.decimal128.hugeint.StringLatin1Iterator
import kotlin.math.max

private const val DIVISOR_1E9 = 1_000_000_000L
private const val MU_1E9 = 0x44B82FA09

private val SINGLE_DIGIT_NUMBERS =
    arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "-0", "-1", "-2", "-3", "-4", "-5", "-6", "-7", "-8", "-9")

internal object Int256ParsePrint {

    fun int256ToString(sign: Boolean, u: U256): String {
        val s = if (sign) 1 else 0
        if (u.digitLen > 1) {
            val utf8 = ByteArray(u.digitLen + s)
            utf8[0] = '-'.code.toByte() // if positive then this will be overwritten
            u256ToUtf8(u, utf8, s)
            return String(utf8)
        } else {
            return SINGLE_DIGIT_NUMBERS[(10 and -s) + u.dw0.toInt()]
        }
    }

    fun int256ToUtf8(sign: Boolean, u: U256, utf8: ByteArray, off: Int): Int {
        val s = if (sign) 1 else 0
        utf8[off] = '-'.code.toByte() // if positive then this will be overwritten
        u256ToUtf8(u, utf8, off + s)
        return off + s + Math.max(1, u.digitLen)
    }

    fun u256ToUtf8(u: U256, utf8: ByteArray, off: Int) {
        if (u.bitLen <= 64) {
            u64ToUtf8(u.digitLen, u.dw0, utf8, off)
            return
        }
        val t = U256(u)
        while (t.bitLen > 192) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_256(t, t, DIVISOR_1E9, MU_1E9)
            u64ToUtf8(9, r, utf8, off + ich)
        }
        while (t.bitLen > 128) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_192(t, t, DIVISOR_1E9, MU_1E9)
            u64ToUtf8(9, r, utf8, off + ich)
        }
        while (t.bitLen > 64) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_128(t, t, DIVISOR_1E9, MU_1E9)
            u64ToUtf8(9, r, utf8, off + ich)
        }
        u64ToUtf8(max(1, t.digitLen), t.dw0, utf8, off)
    }

    fun intToUtf8(n: Int, utf8: ByteArray, off: Int): Int {
        var nAbs = n
        var offT = off
        if (n < 0) {
            nAbs = -n
            offT = off + 1
            utf8[off] = '-'.code.toByte()
        }
        if (nAbs < 10) {
            utf8[offT] = ('0' + nAbs).code.toByte()
            return offT + 1
        }

        val digitLen = U256Pow10.calcDigitLen64(nAbs.toLong())
        u64ToUtf8(digitLen, nAbs.toLong(), utf8, offT)
        return offT + digitLen
    }

    private fun u64ToUtf8(digitLen: Int, dw0: Long, utf8: ByteArray, off: Int) {
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

            val tC = i - 2; val maskC = -tC shr 31; val iC = tC and maskC
            val tB = i - 1; val maskB = -tB shr 31; val iB = tB and maskB

            utf8[off + iC] = digitC
            utf8[off + iB] = digitB
            utf8[off + i] = digitA

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
        u256ToHexUtf8(u, hexitCount, bytes, s + 2)
        return String(bytes)
    }

    fun u256ToHexUtf8(u: U256, hexitCount: Int, utf8: ByteArray, off: Int) {
        var hexitsRemaining = hexitCount
        var ich = off
        for (i in (u.bitLen - 1) ushr 6 downTo 0) {
            val dw = u[i]
            val thisHexitCount = if ((hexitsRemaining and 0x0F) != 0) (hexitsRemaining and 0x0F) else 16
            u64ToHexUtf8(dw, thisHexitCount, utf8, ich)
            hexitsRemaining -= thisHexitCount
            ich += thisHexitCount
        }
        check(hexitsRemaining == 0)
        check(ich == utf8.size)
    }

    fun u64ToHexUtf8(dw: Long, hexitCount: Int, utf8: ByteArray, off: Int) {
        var t = dw
        check(hexitCount in 1..16)
        for (i in hexitCount - 1 downTo 0) {
            val h = (t and 0x0FL).toInt()
            val ch = if (h < 10) '0' + h else 'A' - 10 + h
            utf8[off + i] = ch.code.toByte()
            t = t ushr 4
        }
    }

    fun u256FromString(u: U256, allowSign: Boolean, str: String) =
        u256FromLatin1Iterator(u, allowSign, StringLatin1Iterator(str, 0, str.length))

    fun u256FromString_old(u: U256, allowSign: Boolean, str: String): Boolean {
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

    fun u256FromLatin1Iterator(u: U256, allowSign: Boolean, src: Latin1Iterator): Boolean {
        u.u256SetZero()
        parseError@
        do {
            var leadingZeroSeen = false
            var ch = src.nextChar()
            val sign = allowSign && ch == '-'
            if (allowSign && (ch == '-' || ch == '+')) // discard leading sign
                ch = src.nextChar()
            if (ch == '0') { // discard leading zero
                ch = src.nextChar()
                if (ch == 'x' || ch == 'X')
                    return u256FromHexLatin1Iterator(u, allowSign, src.reset())
                leadingZeroSeen = true
            }
            while (ch == '0' || ch == '_') {
                if (ch == '_' && !leadingZeroSeen)
                    break@parseError
                leadingZeroSeen = leadingZeroSeen or (ch == '0')
                ch = src.nextChar() // discard all leading zeros
            }
            if (ch == '\u0000') {
                if (leadingZeroSeen)
                    return sign
                break@parseError
            }
            var accumulator = 0L
            var accumulatorDigitCount = 0
            var chLast = '\u0000'
            src.prevChar() // back up one char
            while (true) {
                chLast = ch
                ch = src.nextChar()
                if (ch !in '0'..'9') {
                    if (ch == '_')
                        continue
                    break
                }
                val n = (ch - '0').toLong()
                accumulator = accumulator * 10L + n
                ++accumulatorDigitCount
                if (accumulatorDigitCount < 19)
                    continue
                u.u256MutateFmaPow10(19, accumulator)
                accumulator = 0L
                accumulatorDigitCount = 0
            }
            if (ch != '\u0000' || chLast == '_')
                break@parseError
            if (accumulatorDigitCount > 0)
                u.u256MutateFmaPow10(accumulatorDigitCount, accumulator)
            return sign
        } while (false)
        throw IllegalArgumentException("invalid integer syntax:$src")
    }

    private fun u256FromHexLatin1Iterator(u: U256, allowSign: Boolean, src: Latin1Iterator): Boolean {
        u.u256SetZero()
        parseError@
        do {
            var leadingZeroSeen = false
            var ch = src.nextChar()
            val sign = allowSign && ch == '-'
            if (allowSign && (ch == '-' || ch == '+'))
                ch = src.nextChar()
            if (ch == '0') {
                ch = src.nextChar()
                if (ch == 'x' || ch == 'X')
                    ch = src.nextChar()
                else
                    leadingZeroSeen = true
            }
            while (ch == '0' || ch == '_') {
                if (ch == '_' && !leadingZeroSeen)
                    break@parseError
                leadingZeroSeen = leadingZeroSeen or (ch == '0')
                ch = src.nextChar()
            }
            if (ch != '\u0000')
                src.prevChar() // back up 1 char
            var accumulator = 0L
            var accumulatorHexitCount = 0
            while (true) {
                ch = src.nextChar()
                val n = when {
                    ch in '0'..'9' -> (ch - '0').toLong()
                    (ch.code or 0x20) in 'a'.code..'f'.code ->
                        (ch.code or 0x20) - 'a'.code + 10L

                    ch == '_' -> continue
                    ch == '\u0000' -> break
                    else -> break@parseError
                }
                accumulator = (accumulator shl 4) or n
                ++accumulatorHexitCount
                if (accumulatorHexitCount < 16)
                    continue
                u.u256MutateShiftLeftOr(64, accumulator)
                accumulator = 0
                accumulatorHexitCount = 0
            }
            if (accumulatorHexitCount > 0)
                u.u256MutateShiftLeftOr(4 * accumulatorHexitCount, accumulator)
            return sign
        } while (false)
        throw NumberFormatException("invalid hex integer syntax:$src")
    }

}
