@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.hugeint.Latin1Iterator
import com.decimal128.hugeint.StringLatin1Iterator
import kotlin.math.max

private const val DIVISOR_1E9 = 1_000_000_000L
private const val MU_1E9 = 0x44B82FA09

private const val M_U32_DIV_1E1 = 0xCCCCCCCDL
private const val S_U32_DIV_1E1 = 35

private const val M_U32_DIV_1E2 = 0x51EB851FL
private const val S_U32_DIV_1E2 = 37

private const val M_U64_DIV_1E4 = 0x346DC5D63886594BL
private const val S_U64_DIV_1E4 = 11 // + 64 high

private const val M_U64_DIV_1E8 = -6067343680855748867 // (0xABCC77118461CEFD)
private const val S_U64_DIV_1E8 = 26 // + 64 high

private val SINGLE_DIGIT_NUMBERS =
    arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "-0", "-1", "-2", "-3", "-4", "-5", "-6", "-7", "-8", "-9")

internal object Int256ParsePrint {

    private inline fun U32(n: Int) = n.toLong() and 0xFFFF_FFFFL

    fun int256ToString(sign: Boolean, u: C256): String {
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

    fun int256ToUtf8(sign: Boolean, u: C256, utf8: ByteArray, off: Int): Int {
        val s = if (sign) 1 else 0
        utf8[off] = '-'.code.toByte() // if positive then this will be overwritten
        u256ToUtf8(u, utf8, off + s)
        return off + s + Math.max(1, u.digitLen)
    }

    fun u256ToUtf8(u: C256, utf8: ByteArray, off: Int) {
        if (u.bitLen <= 64) {
            if (u.bitLen <= 32) {
                if (u.bitLen > 0) {
                    u32ToUtf8(u.digitLen, u.dw0.toInt(), utf8, off)
                    return
                }
                utf8[off] = '0'.code.toByte()
                return
            }
            u64ToUtf8(u.digitLen, u.dw0, utf8, off)
            return
        }
        // FIXME ... this should come from the current DecEnv
        val t = C256(u)
        while (t.bitLen > 192) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_256(t, t, DIVISOR_1E9, MU_1E9)
            nineDigitsToUtf8(r, utf8, off + ich)
        }
        while (t.bitLen > 128) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_192(t, t, DIVISOR_1E9, MU_1E9)
            nineDigitsToUtf8(r, utf8, off + ich)
        }
        while (t.bitLen > 64) {
            val ich = t.digitLen - 9
            val r = DivBarrett.barrettDivMod_32_128(t, t, DIVISOR_1E9, MU_1E9)
            nineDigitsToUtf8(r, utf8, off + ich)
        }
        check (t.bitLen > 0)
        u64ToUtf8(max(1, t.digitLen), t.dw0, utf8, off)
    }

    fun int32ToUtf8(n: Int, utf8: ByteArray, off: Int): Int {
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
        val digitLen = U256Pow10.calcDigitLen64(U32(nAbs))
        u32ToUtf8(digitLen, nAbs, utf8, offT)
        return offT + digitLen
    }

    private fun nineDigitsToUtf8(dw: Long, utf8: ByteArray, off: Int) =
        nineDigitsToUtf8_tree(dw, utf8, off)

    private fun nineDigitsToUtf8_tree(dw: Long, utf8: ByteArray, off: Int) {
        val abcde = unsignedMulHi(dw, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val fghi  = dw - (abcde * 10000L)

        val abc = (abcde * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val de = abcde - (abc * 100L)

        val fg = (fghi * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val hi = fghi - (fg * 100L)

        val a = (abc * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val bc = abc - (a * 100L)

        val b = (bc * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val c = bc - (b * 10L)

        val d = (de * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val e = de - (d * 10L)

        val f = (fg * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val g = fg - (f * 10L)

        val h = (hi * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val i = hi - (h * 10L)

        utf8[off + 0] = (a.toInt() + '0'.code).toByte()
        utf8[off + 1] = (b.toInt() + '0'.code).toByte()
        utf8[off + 2] = (c.toInt() + '0'.code).toByte()
        utf8[off + 3] = (d.toInt() + '0'.code).toByte()
        utf8[off + 4] = (e.toInt() + '0'.code).toByte()
        utf8[off + 5] = (f.toInt() + '0'.code).toByte()
        utf8[off + 6] = (g.toInt() + '0'.code).toByte()
        utf8[off + 7] = (h.toInt() + '0'.code).toByte()
        utf8[off + 8] = (i.toInt() + '0'.code).toByte()

    }

    private fun eightDigitsToUtf8_tree(dw: Long, utf8: ByteArray, off: Int) {
        val abcdefgh = dw

        val abcd = unsignedMulHi(abcdefgh, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val efgh  = abcdefgh - (abcd * 10000L)

        val ab = (abcd * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val cd = abcd - (ab * 100L)

        val ef = (efgh * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val gh = efgh - (ef * 100L)

        val a = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val b = ab - (a * 10L)

        val c = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val d = cd - (c * 10L)

        val e = (ef * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val f = ef - (e * 10L)

        val g = (gh * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val h = gh - (g * 10L)

        utf8[off + 0] = (a.toInt() + '0'.code).toByte()
        utf8[off + 1] = (b.toInt() + '0'.code).toByte()
        utf8[off + 2] = (c.toInt() + '0'.code).toByte()
        utf8[off + 3] = (d.toInt() + '0'.code).toByte()
        utf8[off + 4] = (e.toInt() + '0'.code).toByte()
        utf8[off + 5] = (f.toInt() + '0'.code).toByte()
        utf8[off + 6] = (g.toInt() + '0'.code).toByte()
        utf8[off + 7] = (h.toInt() + '0'.code).toByte()
    }

    private fun fourDigitsToUtf8_tree(dw: Long, utf8: ByteArray, off: Int) {
        val abcd = dw

        val ab = (abcd * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val cd = abcd - (ab * 100L)

        val a = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val b = ab - (a * 10L)

        val c = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val d = cd - (c * 10L)

        utf8[off + 0] = (a.toInt() + '0'.code).toByte()
        utf8[off + 1] = (b.toInt() + '0'.code).toByte()
        utf8[off + 2] = (c.toInt() + '0'.code).toByte()
        utf8[off + 3] = (d.toInt() + '0'.code).toByte()
    }

    private fun zeroTo4DigitsToUtf8(digitPrintCount: Int, dw: Long, utf8: ByteArray, off: Int) {
        val abcd = dw

        val ab = (abcd * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
        val cd = abcd - (ab * 100L)

        val a = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val b = ab - (a * 10L)

        val c = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val d = cd - (c * 10L)

        val firstByte = utf8[off]

        val tA = digitPrintCount - 4; val maskA = -tA shr 31; val iA = tA and maskA
        utf8[off + iA] = (a.toInt() + '0'.code).toByte()
        val tB = digitPrintCount - 3; val maskB = -tB shr 31; val iB = tB and maskB
        utf8[off + iB] = (b.toInt() + '0'.code).toByte()
        val tC = digitPrintCount - 2; val maskC = -tC shr 31; val iC = tC and maskC
        utf8[off + iC] = (c.toInt() + '0'.code).toByte()
        val tD = digitPrintCount - 1; val maskD = -tD shr 31; val iD = tD and maskD
        val bD = (d.toInt() + '0'.code).toByte()
        utf8[off + iD] = if (digitPrintCount == 0) firstByte else bD
    }

    private inline fun twoDigitsToUtf8_tree(dw: Long, utf8: ByteArray, off: Int) {
        val ab = dw

        val a = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
        val b = ab - (a * 10L)

        utf8[off + 0] = (a.toInt() + '0'.code).toByte()
        utf8[off + 1] = (b.toInt() + '0'.code).toByte()
    }


    internal fun u32ToUtf8(digitPrintCount: Int, w: Int, utf8: ByteArray, off: Int): Int {
        if (digitPrintCount in 1..10) {
            var d = U32(w)
            var i = digitPrintCount - 1
            do {
                val qA = (d * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
                val digitA = (( d - (qA * 10L)) + '0'.code).toByte()
                val qB = (qA * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
                val digitB = ((qA - (qB * 10L)) + '0'.code).toByte()
                val qC = (qB * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
                val digitC = ((qB - (qC * 10L)) + '0'.code).toByte()

                val tC = i - 2; val maskC = -tC shr 31; val iC = tC and maskC
                val tB = i - 1; val maskB = -tB shr 31; val iB = tB and maskB

                utf8[off + iC] = digitC
                utf8[off + iB] = digitB
                utf8[off + i] = digitA

                d = qC
                i -= 3
            } while (i >= 0)
            return off + digitPrintCount
        }
        throw IllegalArgumentException()
    }

    internal fun u64ToUtf8(digitPrintCount: Int, dw0: Long, utf8: ByteArray, off: Int): Int =
        u64ToUtf8_tree(digitPrintCount, dw0, utf8, off)

    internal fun u64ToUtf8_tree(digitPrintCount: Int, dw0: Long, utf8: ByteArray, off: Int): Int {
        if (digitPrintCount in 1..20) {
            var dw = dw0
            var remainingCount = digitPrintCount
            var offT = off + digitPrintCount
            while (remainingCount >= 8) {
                val dwT = unsignedMulHi(dw, M_U64_DIV_1E8) ushr S_U64_DIV_1E8
                val low8 = dw - (dwT * 100000000L)
                dw = dwT
                remainingCount -= 8
                offT -= 8
                eightDigitsToUtf8_tree(low8, utf8, offT)
            }
            if (remainingCount > 4) {
                val dwT = unsignedMulHi(dw, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
                val low4 = dw - (dwT * 10000L)
                dw = dwT
                remainingCount -= 4
                offT -= 4
                fourDigitsToUtf8_tree(low4, utf8, offT)
            }
            zeroTo4DigitsToUtf8(remainingCount, dw, utf8, off)
            return digitPrintCount
        }
        throw IllegalArgumentException()
    }

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
