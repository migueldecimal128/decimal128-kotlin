@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.hugeint.Latin1Iterator
import com.decimal128.hugeint.StringLatin1Iterator

private const val DIVISOR_1E9 = 1_000_000_000L
private const val MU_1E9 = 0x44B82FA09

private const val M_U32_DIV_1E1 = 0xCCCCCCCDL
private const val S_U32_DIV_1E1 = 35

private const val M_U32_DIV_1E2 = 0x51EB851FL
private const val S_U32_DIV_1E2 = 37

private const val M_U32_DIV_1E4 = 0x346DC5D7
private const val S_U32_DIV_1E4 = 43

private const val M_U64_DIV_1E4 = 0x346DC5D63886594BL
private const val S_U64_DIV_1E4 = 11 // + 64 high

private const val M_U64_DIV_1E8 = -6067343680855748867 // (0xABCC77118461CEFD)
private const val S_U64_DIV_1E8 = 26 // + 64 high

private const val M_U64_DIV_1E10 = -2601111570856684097 // (0xDBE6FECEBDEDD5BF)
private const val S_U64_DIV_1E10 = 33

private const val M_U64_DIV_1E12 = 2535301200456458803L // (0x232F33025BD42233)
private const val S_U64_DIV_1E12 = 37

private const val M_U64_DIV_1E16 = 4153837486827862103L // (0x39A5652FB1137857)
private const val S_U64_DIV_1E16 = 51

internal object Int256ParsePrint {

    private inline fun U32(n: Int) = n.toLong() and 0xFFFF_FFFFL

    fun int256ToString(sign: Boolean, c: C256): String {
        val signMask = if (sign) -1L else 0L
        return if (c.bitLen <= 63) {
            ((c.dw0 xor signMask) - signMask).toString()
        } else {
            val sign01 = -signMask.toInt()
            int256ToStringImpl(sign01, c)
        }
    }

    private fun int256ToStringImpl(sign01: Int, c: C256): String {
        val utf8 = ByteArray(c.digitLen + sign01)
        utf8[0] = '-'.code.toByte() // if non-negative then this will be overwritten
        c256ToUtf8(c, utf8, sign01)
        return String(utf8)
    }

    fun c256ToUtf8(c: C256, utf8: ByteArray, off: Int, tmp: C256? = null): Int {
        // minimum printDigitLen is 1
        // add 1, then add -1 if the inbound digitLen was non-zero
        val printDigitLen = c.digitLen + 1 + (-c.digitLen shr 31)
        if (c.bitLen <= 64) {
            render0To20Digits(printDigitLen, c.dw0, utf8, off)
            return printDigitLen
        }
        val t: C256 = tmp?.c256Set(c) ?: C256(c)
        while (t.bitLen > 128) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_256(t, t, DIVISOR_1E9, MU_1E9)
            render9Digits(r, utf8, off + ich)
        }
        while (t.bitLen > 64) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_128(t, t, DIVISOR_1E9, MU_1E9)
            render9Digits(r, utf8, off + ich)
        }
        render0To20Digits(t.digitLen, t.dw0, utf8, off)
        return printDigitLen
    }

    fun int32ToUtf8(n: Int, utf8: ByteArray, off: Int): Int {
        val dwAbs = U32((n xor (n shr 31)) - (n shr 31))
        val sign01 = n ushr 31
        utf8[off] = '-'.code.toByte()
        val digitLen = U256Pow10.calcDigitLen64(dwAbs)
        val digitPrintCount = digitLen + 1 + (-digitLen shr 31)
        render0To10Digits(digitPrintCount, dwAbs, utf8, off + sign01)
        return sign01 + digitPrintCount
    }

    fun int256ToUtf8(sign: Boolean, c: C256, utf8: ByteArray, off: Int): Int {
        val sign01 = if (sign) 1 else 0
        utf8[off] = '-'.code.toByte() // if non-negative then this will be overwritten
        c256ToUtf8(c, utf8, off + sign01)
        return sign01 + c256ToUtf8(c, utf8, off + sign01)
    }

    private fun render0To20Digits(digitPrintCount: Int, dw: Long, utf8: ByteArray, off: Int) {
        var remaining = digitPrintCount
        var t = dw
        if (digitPrintCount >= 10) {
            val q = unsignedMulHi(t, M_U64_DIV_1E10) ushr S_U64_DIV_1E10
            val abcdefghij = t - (q * 1_00000_00000L)
            t = q
            remaining -= 10
            render10Digits(abcdefghij, utf8, off + remaining)
        }
        render0To10Digits(remaining, t, utf8, off)
    }

    private fun render0To10Digits(digitPrintCount: Int, dw: Long, utf8: ByteArray, off: Int) {
        var remaining = digitPrintCount
        var t = dw
        if (digitPrintCount >= 8) {
            val q = unsignedMulHi(t, M_U64_DIV_1E8) ushr S_U64_DIV_1E8
            val abcdefgh = t - (q * 1_0000_0000L)
            t = q
            remaining -= 8
            render8Digits(abcdefgh, utf8, off + remaining)
        }
        if (remaining >= 4) {
            val q = (t * M_U32_DIV_1E4) ushr S_U32_DIV_1E4
            val abcd = t - (q * 10000L)
            t = q
            remaining -= 4
            render4Digits(abcd, utf8, off + remaining)
        }
        if (remaining > 0)
            render1To4Digits(remaining, t, utf8, off)
    }

    private fun render10Digits(abcdefghij: Long, utf8: ByteArray, off: Int) {
        val abcdef = unsignedMulHi(abcdefghij, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val ab = (abcdef * M_U32_DIV_1E4) ushr S_U32_DIV_1E4
        render2Digits(ab, utf8, off)
        val cdef = abcdef - (ab * 10000L)
        val ghij = abcdefghij - (abcdef * 10000L)
        render4Digits(cdef, utf8, off + 2)
        render4Digits(ghij, utf8, off + 6)
    }

    private fun render9Digits(abcdefghi: Long, utf8: ByteArray, off: Int) {
        val abcde = (abcdefghi * M_U32_DIV_1E4) ushr S_U32_DIV_1E4
        val a = (abcde * M_U32_DIV_1E4) ushr S_U32_DIV_1E4
        render1Digit(a, utf8, off)
        val bcde = abcde - (a * 10000L)
        val fghi = abcdefghi - (abcde * 10000L)
        render4Digits(bcde, utf8, off + 1)
        render4Digits(fghi, utf8, off + 5)
    }

    private fun render8Digits(abcdefgh: Long, utf8: ByteArray, off: Int) {
        val abcd = (abcdefgh * M_U32_DIV_1E4) ushr S_U32_DIV_1E4
        val efgh = abcdefgh - (abcd * 10000L)
        render4Digits(abcd, utf8, off)
        render4Digits(efgh, utf8, off + 4)
    }

    private inline fun render4Digits(abcd: Long, utf8: ByteArray, off: Int) {
        val ab = (abcd * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val cd = abcd - (ab * 100L)
        render2Digits(ab, utf8, off)
        render2Digits(cd, utf8, off + 2)
    }

    private inline fun render2Digits(ab: Long, utf8: ByteArray, off: Int) {
        val a = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val b = ab - (a * 10L)
        utf8[off + 0] = (a.toInt() + '0'.code).toByte()
        utf8[off + 1] = (b.toInt() + '0'.code).toByte()
    }

    private inline fun render1Digit(a: Long, utf8: ByteArray, off: Int) {
        utf8[off] = (a.toInt() + '0'.code).toByte()
    }

    private inline fun render1To4Digits(digitPrintCount: Int, dw: Long, utf8: ByteArray, off: Int) {
        val abcd = dw

        val ab = (abcd * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val aDw = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val a = aDw.toInt()
        val b = (ab - (aDw * 10L)).toInt()
        val tA = digitPrintCount - 4;
        val maskA = -tA shr 31;
        val iA = tA and maskA
        utf8[off + iA] = (a + '0'.code).toByte()
        val tB = digitPrintCount - 3;
        val maskB = -tB shr 31;
        val iB = tB and maskB
        utf8[off + iB] = (b + '0'.code).toByte()

        val cd = abcd - (ab * 100L)
        val cDw = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val c = cDw.toInt()
        val tC = digitPrintCount - 2;
        val maskC = -tC shr 31;
        val iC = tC and maskC
        utf8[off + iC] = (c + '0'.code).toByte()
        val d = (cd - (cDw * 10L)).toInt()
        val iD = digitPrintCount - 1
        utf8[off + iD] = (d + '0'.code).toByte()
    }

    internal fun u64ToUtf8(digitPrintCount: Int, dw0: Long, utf8: ByteArray, off: Int): Int =
        u64ToUtf8_sequential(digitPrintCount, dw0, utf8, off)

    internal fun u64ToUtf8_sequential(digitPrintCount: Int, dw0: Long, utf8: ByteArray, off: Int): Int {
        val z = C256()
        DivMagic.magicDivPow10_64(z, dw0, 8)
        if (digitPrintCount in 1..20) {
            var d = dw0
            var i = digitPrintCount - 1
            while (i >= 3) {
                val qA = unsignedMulHi(d, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitA = ((d - (qA * 10L)) + '0'.code).toByte()
                val qB = unsignedMulHi(qA, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitB = ((qA - (qB * 10L)) + '0'.code).toByte()
                val qC = unsignedMulHi(qB, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitC = ((qB - (qC * 10L)) + '0'.code).toByte()
                val qD = unsignedMulHi(qC, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitD = ((qC - (qD * 10L)) + '0'.code).toByte()

                utf8[off + i - 3] = digitD
                utf8[off + i - 2] = digitC
                utf8[off + i - 1] = digitB
                utf8[off + i    ] = digitA

                d = qD
                i -= 4
            }
            if (i >= 0) {
                val qA = unsignedMulHi(d, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitA = ((d - (qA * 10L)) + '0'.code).toByte()
                val qB = unsignedMulHi(qA, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitB = ((qA - (qB * 10L)) + '0'.code).toByte()
                val qC = unsignedMulHi(qB, 0xCCCCCCCCCCCCCCCDuL.toLong()) ushr 3
                val digitC = ((qB - (qC * 10L)) + '0'.code).toByte()

                val tB = i - 1;
                val maskB = -tB shr 31;
                val iB = tB and maskB

                utf8[off     ] = digitC
                utf8[off + iB] = digitB
                utf8[off + i ] = digitA

            }
            return off + digitPrintCount
        }
        throw IllegalArgumentException()
    }

    fun int256ToHexString(sign: Boolean, u: C256): String {
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

    fun u256ToHexUtf8(u: C256, hexitCount: Int, utf8: ByteArray, off: Int) {
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

    fun u256FromString(u: C256, allowSign: Boolean, str: String) =
        u256FromLatin1Iterator(u, allowSign, StringLatin1Iterator(str))

    fun u256FromLatin1Iterator(u: C256, allowSign: Boolean, src: Latin1Iterator): Boolean {
        u.c256SetZero()
        invalid_syntax@
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
                    break@invalid_syntax
                leadingZeroSeen = leadingZeroSeen or (ch == '0')
                ch = src.nextChar() // discard all leading zeros
            }
            if (ch == '\u0000') {
                if (leadingZeroSeen)
                    return sign
                break@invalid_syntax
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
                break@invalid_syntax
            if (accumulatorDigitCount > 0)
                u.u256MutateFmaPow10(accumulatorDigitCount, accumulator)
            return sign
        } while (false)
        throw IllegalArgumentException("invalid integer syntax:$src")
    }

    private fun u256FromHexLatin1Iterator(u: C256, allowSign: Boolean, src: Latin1Iterator): Boolean {
        u.c256SetZero()
        invalid_syntax@
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
                    break@invalid_syntax
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
                    else -> break@invalid_syntax
                }
                accumulator = (accumulator shl 4) or n
                ++accumulatorHexitCount
                if (accumulatorHexitCount < 16)
                    continue
                u.c256MutateShiftLeftOr(64, accumulator)
                accumulator = 0
                accumulatorHexitCount = 0
            }
            if (accumulatorHexitCount > 0)
                u.c256MutateShiftLeftOr(4 * accumulatorHexitCount, accumulator)
            return sign
        } while (false)
        throw NumberFormatException("invalid hex integer syntax:$src")
    }

}
