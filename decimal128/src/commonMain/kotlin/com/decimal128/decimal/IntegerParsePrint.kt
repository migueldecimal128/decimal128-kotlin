@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

private const val DIVISOR_1E9 = 1_000_000_000L
private const val MU_1E9 = 0x44B82FA09L

private const val BARRETT_DIVISOR_1E8 = 1_0000_0000L
private const val BARRETT_MU_1E8 = 0x2AF31DC461

private const val M_U32_DIV_1E1 = 0xCCCCCCCDL
private const val S_U32_DIV_1E1 = 35

private const val M_U32_DIV_1E2 = 0x51EB851FL
private const val S_U32_DIV_1E2 = 37

private const val M_U32_DIV_1E4 = 0x346DC5D7uL
private const val S_U32_DIV_1E4 = 43

private const val M_U64_DIV_1E4 = 0x346DC5D63886594BL
private const val S_U64_DIV_1E4 = 11 // + 64 high

private const val M_U64_DIV_1E8 = Long.MIN_VALUE or 0x2BCC77118461CEFDL // -6067343680855748867
private const val S_U64_DIV_1E8 = 26 // + 64 high

// WARNING ... 1E9 has the add correction flag set because it overflows to 129 bits
private const val M_U64_DIV_1E9 = 0x12E0BE826D694B2FuL // 1360296554856532783uL
private const val S_U64_DIV_1E9 = 30

private const val M_U64_DIV_1E10 = 0xDBE6FECEBDEDD5BFuL // -2601111570856684097
private const val S_U64_DIV_1E10 = 33

private const val M_U64_DIV_1E12 = 2535301200456458803L // (0x232F33025BD42233)
private const val S_U64_DIV_1E12 = 37

private const val M_U64_DIV_1E16 = 4153837486827862103L // (0x39A5652FB1137857)
private const val S_U64_DIV_1E16 = 51

internal object IntegerParsePrint {

    internal fun c256ToString(sign: Boolean, c: C256): String {
        val bitLen = c.bitLen
        return when {
            bitLen == 0 -> if (sign) "-0" else "0"
            bitLen <= 63 ->
                (if (sign) -(c.dw0) else c.dw0).toString()
            else ->
                c256ToStringImpl(sign, c)
        }
    }

    private fun c256ToStringImpl(sign: Boolean, c: C256): String {
        val signBit = if (sign) 1 else 0
        val ascii = ByteArray(c.digitLen + signBit)
        ascii[0] = '-'.code.toByte() // if non-negative then this will be overwritten
        c256ToASCII(c, ascii, signBit)
        return ascii.decodeToString()
    }

    fun c256ToASCII(c: C256, ascii: ByteArray, off: Int, tmp: C256? = null): Int {
        // minimum printDigitLen is 1
        // add 1, then add -1 if the inbound digitLen was non-zero
        var remainingDigitCount = c.digitLen + 1 + (-c.digitLen shr 31)
        var t = c
        if (c.bitLen > 128) {
            t = tmp?.c256Set(c) ?: C256(c)
            do {
                val ibMaxx = off + t.digitLen
                val r = barrettDivMod_32_256(t, t, BARRETT_DIVISOR_1E8, BARRETT_MU_1E8)
                render8DigitsBeforeIndex(r, ascii, ibMaxx)
                remainingDigitCount -= 8
            } while (t.bitLen > 128)
        }
        return u128ToASCII(remainingDigitCount, t.dw1, t.dw0, ascii, off)
    }

    fun int32ToASCII(n: Int, ascii: ByteArray, off: Int): Int {
        val dwAbs = ((n xor (n shr 31)) - (n shr 31)).toUInt().toLong()
        val signBit = n ushr 31
        ascii[off] = '-'.code.toByte()
        val digitLen = calcDigitLen64(dwAbs)
        // digitPrintCount = max(digitLen, 1), branchless
        val digitPrintCount = digitLen + 1 + (-digitLen shr 31)
        u64ToASCII(digitPrintCount, dwAbs, ascii, off + signBit)
        return signBit + digitPrintCount
    }

    internal fun u64ToASCII(digitPrintCount: Int, dw0: Long, ascii: ByteArray, off: Int): Int {
        var digitsRemaining = digitPrintCount
        var ich = off + digitsRemaining
        var dwT = dw0
        while (digitsRemaining >= 8) {
            val q = unsignedMulHi(dwT, M_U64_DIV_1E8) ushr S_U64_DIV_1E8
            val r = dwT - (q * 1_0000_0000L)
            dwT = q
            render8DigitsBeforeIndex(r, ascii, ich)
            ich -= 8
            digitsRemaining -= 8
        }
        if (digitsRemaining > 0)
            renderTailDigitsBeforeIndex(digitsRemaining, dwT, ascii, ich)
        return digitPrintCount
    }

    internal fun u128ToASCII(digitPrintCount: Int, dw1: Long, dw0: Long, ascii: ByteArray, off: Int): Int {
        var dw1T = dw1
        var dw0T = dw0
        var ich = off + digitPrintCount
        var remainingDigitCount = digitPrintCount
        while (dw1T != 0L) {
            val q2 = unsignedMulHi(dw1T, M_U64_DIV_1E8) ushr S_U64_DIV_1E8
            val r2 = dw1T - (q2 * 1_0000_0000L)
            val s2 = (r2 shl 32) or (dw0T ushr 32)
            val q1 = unsignedMulHi(s2, M_U64_DIV_1E8) ushr S_U64_DIV_1E8
            val r1 = s2 - (q1 * 1_0000_0000L)
            val s1 = (r1 shl 32) or (dw0T and MASK32L)
            val q0 = unsignedMulHi(s1, M_U64_DIV_1E8) ushr S_U64_DIV_1E8
            val r0 = s1 - (q0 * 1_0000_0000L)
            dw0T = (q1 shl 32) + q0
            dw1T = q2
            render8DigitsBeforeIndex(r0, ascii, ich)
            ich -= 8
            remainingDigitCount -= 8
        }
        while (remainingDigitCount >= 8) {
            val t0 = unsignedMulHi(dw0T, M_U64_DIV_1E8) ushr S_U64_DIV_1E8
            val r0 = dw0T - (t0 * 1_0000_0000L)
            dw0T = t0
            render8DigitsBeforeIndex(r0, ascii, ich)
            ich -= 8
            remainingDigitCount -= 8
        }
        if (remainingDigitCount > 0)
            renderTailDigitsBeforeIndex(remainingDigitCount, dw0T, ascii, ich)
        return digitPrintCount
    }

    fun int256ToHexString(sign: Boolean, u: C256): String {
        val s = if (sign) 1 else 0
        if (u.digitLen == 0)
            return "0x0"
        val hexitCount = (u.bitLen + 3) ushr 2
        val bytes = ByteArray(s + 2 + hexitCount)
        bytes[0    ] = '-'.code.toByte()
        bytes[s    ] = '0'.code.toByte()
        bytes[s + 1] = 'x'.code.toByte()
        u256ToHexASCII(u, hexitCount, bytes, s + 2)
        return bytes.decodeToString()
    }

    fun u256ToHexASCII(u: C256, hexitCount: Int, ascii: ByteArray, off: Int) {
        var hexitsRemaining = hexitCount
        var ich = off
        for (i in (u.bitLen - 1) ushr 6 downTo 0) {
            val dw = u[i]
            val thisHexitCount = if ((hexitsRemaining and 0x0F) != 0) (hexitsRemaining and 0x0F) else 16
            u64ToHexASCII(dw, thisHexitCount, ascii, ich)
            hexitsRemaining -= thisHexitCount
            ich += thisHexitCount
        }
        verify { hexitsRemaining == 0 }
        verify { ich == ascii.size }
    }

    fun u64ToHexASCII(dw: Long, hexitCount: Int, ascii: ByteArray, off: Int) {
        var t = dw
        verify { hexitCount in 1..16 }
        for (i in hexitCount - 1 downTo 0) {
            val h = (t and 0x0FL).toInt()
            val ch = if (h < 10) '0' + h else 'A' - 10 + h
            ascii[off + i] = ch.code.toByte()
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
                    return u256FromHexLatin1Iterator(u, allowSign, src.rewind())
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
                c256SetFmaPow10(u, u, 19, accumulator)
                accumulator = 0L
                accumulatorDigitCount = 0
            }
            if (ch != '\u0000' || chLast == '_')
                break@invalid_syntax
            if (accumulatorDigitCount > 0)
                c256SetFmaPow10(u, u, accumulatorDigitCount, accumulator)
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

    private const val M_U32_DIV_1E1 = 0xCCCCCCCDL
    private const val S_U32_DIV_1E1 = 35

    private const val M_U32_DIV_1E2 = 0x51EB851FL
    private const val S_U32_DIV_1E2 = 37

    private const val M_U64_DIV_1E4 = 0x346DC5D63886594BL
    private const val S_U64_DIV_1E4 = 11 // + 64 high

    // these magic reciprocal constants only work for values up to 10**9 / 10**4
    private const val M_1E9_DIV_1E4 = 879_609_303L
    private const val S_1E9_DIV_1E4 = 43

    fun renderTailDigitsBeforeIndex(renderDigitCount: Int, dw: Long, ascii: ByteArray, offMaxx: Int) {
        var t = dw
        var remainingDigitCount = renderDigitCount
        var ib = offMaxx
        while (remainingDigitCount >= 4) {
            val t0 = unsignedMulHi(t, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
            val abcd = t - (t0 * 10000L)
            t = t0
            val ab = (abcd * M_U32_DIV_1E2) ushr S_U32_DIV_1E2
            val cd = abcd - (ab * 100L)
            val a = (ab * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
            val b = ab - (a * 10L)
            val c = (cd * M_U32_DIV_1E1) ushr S_U32_DIV_1E1
            val d = cd - (c * 10L)
            if (ib - 4 >= 0 && ib <= ascii.size) {
                ascii[ib - 4] = (a.toInt() + '0'.code).toByte()
                ascii[ib - 3] = (b.toInt() + '0'.code).toByte()
                ascii[ib - 2] = (c.toInt() + '0'.code).toByte()
                ascii[ib - 1] = (d.toInt() + '0'.code).toByte()
                ib -= 4
                remainingDigitCount -= 4
            } else {
                throw IllegalArgumentException()
            }
        }
        while (remainingDigitCount > 0) {
            val divTen = (t * 0xCCCCCCCDL) ushr 35
            val digit = (t - (divTen * 10L)).toInt()
            ascii[--ib] = ('0'.code + digit).toByte()
            t = divTen
            --remainingDigitCount
        }
        verify { offMaxx - ib == renderDigitCount }
    }

    private fun render8DigitsBeforeIndex(dw: Long, ascii: ByteArray, offMaxx: Int) {
        val abcd = unsignedMulHi(dw, M_U64_DIV_1E4) ushr S_U64_DIV_1E4
        val efgh  = dw - (abcd * 10000L)

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

        // Explicit bounds check to enable elimination of individual checks
        val offMin = offMaxx - 8
        if (offMin >= 0 && offMaxx <= ascii.size) {
            ascii[offMaxx - 8] = (a.toInt() + '0'.code).toByte()
            ascii[offMaxx - 7] = (b.toInt() + '0'.code).toByte()
            ascii[offMaxx - 6] = (c.toInt() + '0'.code).toByte()
            ascii[offMaxx - 5] = (d.toInt() + '0'.code).toByte()
            ascii[offMaxx - 4] = (e.toInt() + '0'.code).toByte()
            ascii[offMaxx - 3] = (f.toInt() + '0'.code).toByte()
            ascii[offMaxx - 2] = (g.toInt() + '0'.code).toByte()
            ascii[offMaxx - 1] = (h.toInt() + '0'.code).toByte()
        } else {
            throw IndexOutOfBoundsException()
        }
    }

}
