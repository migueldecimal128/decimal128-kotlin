@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.bidcodec

import kotlin.math.max
import kotlin.math.min

/**
 * Thin codec converting between BID128 binary representations and decimal
 * string form, per IEEE 754-2019.
 *
 * BID128 values are represented as a pair of 64-bit longs: `dw1` (high 64 bits)
 * and `dw0` (low 64 bits). This object provides no arithmetic, no field
 * extraction, and no value-type wrapper — just string conversion in both
 * directions.
 *
 * For the full decimal128 implementation (arithmetic, comparison, rounding,
 * etc.), see the `decimal128-kotlin` artifact.
 */
public object Decimal128BidStringCodec {

    private const val VERIFY_ENABLED: Boolean = true

    private inline fun verify(block: () -> Boolean) {
        if (VERIFY_ENABLED)
            check(block())
    }

    /**
     * Convert a BID128 value to its decimal string representation.
     *
     * @param bid128Hi high 64 bits of the BID128 encoding
     * @param bid128Lo low 64 bits of the BID128 encoding
     * @return the decimal string form of the BID128 value
     */
    public fun toString(bid128Hi: Long, bid128Lo: Long): String {
        return decodeBid128toString(bid128Hi, bid128Lo)
    }

    /**
     * Parse a decimal string into a BID128 value, writing the result into [dest].
     *
     * On success: `dest[0]` is set to `dw1` (high 64 bits), `dest[1]` is set
     * to `dw0` (low 64 bits), and this method returns `null`.
     *
     * On failure: `dest[0]` and `dest[1]` are unchanged, and this method
     * returns a human-readable error message describing the failure. The
     * exact wording of error messages is not part of the API contract and
     * may change between versions.
     *
     * @param bid128Longs a `LongArray` of size at least 2, into which the parsed
     *             BID128 value is written on success
     * @param str  the decimal string to parse
     * @return `null` on success, or a human-readable error message on failure
     * @throws IllegalArgumentException if `dest.size < 2`
     */
    public fun parseReturnError(bid128Longs: LongArray, str: String): String? {
        require(bid128Longs.size >= 2) { "bid128Longs must have size >= 2, was ${bid128Longs.size}" }
        return encodeStringToBid128OrError(bid128Longs, str)
    }

    // -- decode to String --------------------------------------------------------

    // 34 nines
    private val coeffMaxHi = 0x0001ED09BEAD87C0L
    private val coeffMaxLo = 0x378D8E63FFFFFFFFL


    private fun decodeBid128toString(bid128Hi: Long, bid128Lo: Long): String {
        // IEEE754-2019 Table 3.6-Decimal2 Interchange format parameters -- p 23
        val k = 128 // storage width in bits
        val p = 34 // precision in digits
        val emax = 6144
        val bias = 6176
        val w5 = 17 // w+5, combination field width in bits
        val t = 110 // trailing significand field width in bits
        verify { 1 + w5 + t == k }

        // 1 + 17 + 110 == 128
        // 1 bit for the sign
        val sign = bid128Hi < 0L
        // w5 bit combination field ... for bid128 0x1FFFF
        val combination = (bid128Hi shr (t - 64)).toInt() and 0x1FFFF
        val coeffTHi = bid128Hi and ((1L shl t) - 1L)

        val coeffHi: Long
        val qExp: Int
        when {
            // if the top 2 bits are not 0b11
            combination shr (w5 - 2) != 0b11 -> {
                //  IEEE754-2019 3.5.2 c) 2) i) -- p 21
                //   If G0 and G1 together are one of 00, 01, or 10, then the biased
                //   exponent E is formed from G0 through Gw+1 and the significand
                //   is formed from bits Gw+2 through the end of the encoding (including T).
                //
                // w+5=13 w=8 Gw+1=G9 Gw+2=G10
                // the top 10 bits of the combination field represent the biased exponent ...
                val biasedExponentE = combination shr 3
                qExp = biasedExponentE - bias
                // ... and the bottom 3 bits are the most significant bits of the significand
                // coeff/significant is 3 + 50 == 53 ... but that's not all ... see below
                coeffHi = (combination and 0x07).toLong()
            }

            combination shr (w5 - 4) != 0b1111 -> {
                // IEEE754-2019 3.5.2 c) 2) ii) -- p 21
                //  If G0 and G1 together are 11 and G2 and G3 together are one
                //  of 00, 01, or 10, then the biased exponent E is formed from
                //  G2 through Gw+3 and the significand is formed by prefixing
                //  the 4 bits (8 + Gw+4) to T.
                //
                // w+5=13 w=8 Gw+3=G11 Gw+4=G12
                //
                val biasedExponentE = (combination ushr 1) and ((1 shl (w5 - 5 + 2)) - 1)
                qExp = biasedExponentE - bias
                coeffHi = 8L + (combination and 1).toLong()
            }
            // if the top 5 bits are 0b11110 then Infinity
            (combination shr (w5 - 5)) == 0b11110 -> {
                return infString(sign)
            }
            // if the top 5 bits are 0x11111 then NaN
            (combination shr (w5 - 5)) == 0b11111 -> {
                // with the next bit determining signaling NaN
                val payloadHi = coeffTHi
                val payloadLo = bid128Lo
                val isSignaling = ((combination shr (w5 - 6)) and 1) != 0
                return nanString(sign, isSignaling, payloadHi, payloadLo)
            }

            else -> {
                // all possible cases were covered above
                throw IllegalStateException()
            }
        }
        val dw1 = ((coeffHi shl (t - 64)) or coeffTHi)
        val dw0 = bid128Lo
        // IEEE754-2019 3.5.2 p21
        //  If the value exceeds the maximum, the significand c is
        //  non-canonical and the value used for c is zero.
        if ((dw1 or dw0) == 0L ||
            unsignedLT(coeffMaxHi, dw1) ||
            dw1 == coeffMaxHi && unsignedLT(coeffMaxLo, dw0)) {
                return zerString(sign, qExp)
            }
        return fnzString(sign, qExp, dw1, dw0)
    }

    private fun infString(sign: Boolean) =
        if (sign) "-Infinity" else "Infinity"

    // 33 nines
    private val payloadMaxHi = 0x0000314DC6448D93L
    private val payloadMaxLo = 0x38C15B09FFFFFFFFL

    private fun nanString(sign: Boolean, isSignaling: Boolean, payloadHi: Long, payloadLo: Long): String {
        val base =
            if (!isSignaling)
                if (sign) "-NaN" else "NaN"
            else
                if (sign) "-sNaN" else "sNaN"
        if ((payloadHi or payloadLo) == 0L ||
            // non canonical IEEE 754-2019 3.5.2 c) 2)
            unsignedLT(payloadMaxHi, payloadHi) ||
            payloadHi == payloadMaxHi && unsignedLT(payloadMaxLo, payloadLo))
            return base
        val digitLen = calcDigitLen128(payloadHi, payloadLo)
        val utf8 = ByteArray(base.length + digitLen)
        for (i in 0..<base.length)
            utf8[i] = base[i].code.toByte()
        u128ToUtf8(digitLen, payloadHi, payloadLo, utf8, base.length)
        return utf8.decodeToString()
    }

    private val ZEROS = arrayOf(
        "0", "0.0", "0.00", "0.000", "0.0000", "0.00000", "0.000000", "",
        "-0", "-0.0", "-0.00", "-0.000", "-0.0000", "-0.00000", "-0.000000", "",
    )

    private fun zerString(sign: Boolean, qExp: Int): String {
        if (qExp <= 0 && qExp >= -6) {
            val signBit = if (sign) 1 else 0
            val index = -qExp + (signBit shl 3)
            return ZEROS[index and 0x0F]
        }
        val base = if (sign) "-0E" else "0E"
        return base + qExp.toString()
    }

    private fun fnzString(sign: Boolean, qExp: Int, dw1: Long, dw0: Long): String {
        val digitLen = calcDigitLen128(dw1, dw0)
        val eExp = qExp + (digitLen - 1)
        return when {
            qExp == 0 -> fnzToIntegerString(sign, dw1, dw0)
            qExp <= 0 && eExp >= -6 -> fnzToDecimalPointString(sign, qExp, digitLen, dw1, dw0)
            else -> fnzToNormalizedScientificString(sign, qExp, digitLen, dw1, dw0)
        }
    }

    private fun fnzToIntegerString(sign: Boolean, dw1: Long, dw0: Long): String {
        if (dw1 == 0L) {
            if (dw0 >= 0L)
                return (if (sign) -dw0 else dw0).toString()
            val base = dw0.toULong().toString()
            return if (sign) "-$base" else base
        }
        val digitLen = calcDigitLen128(dw1, dw0)
        val printDigitLen = digitLen + 1 - (-digitLen ushr 31)
        val signBit = if (sign) 1 else 0
        val utf8 = ByteArray(signBit + printDigitLen)
        utf8[0] = '-'.code.toByte()
        u128ToUtf8(printDigitLen, dw1, dw0, utf8, signBit)
        return utf8.decodeToString()
    }

    private fun fnzToDecimalPointString(sign: Boolean, qExp: Int, digitLen: Int, dw1: Long, dw0: Long): String {
        val digitsRightOfDecimal = -qExp
        val leadingZeroCount = max(1 + digitsRightOfDecimal - digitLen, 0)
        val signLen = if (sign) 1 else 0
        val decimalPointLen = 1
        val totalLen = signLen + leadingZeroCount + decimalPointLen + digitLen
        val utf8 = ByteArray(totalLen)
        utf8[0] = '-'.code.toByte()
        for (i in signLen..leadingZeroCount) // there is one extra here
            utf8[i] = '0'.code.toByte()
        u128ToUtf8(digitLen, dw1, dw0, utf8, signLen + leadingZeroCount)
        for (i in totalLen - 1 downTo totalLen - digitsRightOfDecimal)
            utf8[i] = utf8[i - 1]
        utf8[totalLen - digitsRightOfDecimal - 1] = '.'.code.toByte()
        return utf8.decodeToString()
    }

    private fun fnzToNormalizedScientificString(sign: Boolean, qExp: Int, digitLen: Int, dw1: Long, dw0: Long): String {
        verify { digitLen > 0 }
        val eExp = qExp + (digitLen - 1)
        val eExpAbs = (eExp xor (eExp shr 31)) - (eExp shr 31)
        val signLen = if (sign) 1 else 0
        val decimalPointLen = if (digitLen > 1) 1 else 0
        val printedDigitLen = digitLen + 1 - (-digitLen ushr 31)
        val expELen = 1
        val expSignLen = if (eExp < 0) 1 else 0
        val expDigitLen = max(calcDigitLen128(0L, eExpAbs.toLong()), 1)
        val totalLen = signLen + decimalPointLen + printedDigitLen + expELen + expSignLen + expDigitLen
        val utf8 = ByteArray(totalLen)
        utf8[0] = '-'.code.toByte()
        u128ToUtf8(printedDigitLen, dw1, dw0, utf8, signLen + decimalPointLen)
        if (decimalPointLen > 0) {
            utf8[signLen] = utf8[signLen + 1]
            utf8[signLen + 1] = '.'.code.toByte()
        }
        val iE = signLen + decimalPointLen + printedDigitLen
        utf8[iE] = 'E'.code.toByte()
        utf8[iE + 1] = '-'.code.toByte()
        renderTailDigitsBeforeIndex(expDigitLen, eExpAbs.toLong(), utf8, totalLen)
        return utf8.decodeToString()

    }

    private const val M_U64_DIV_1E8 = Long.MIN_VALUE or 0x2BCC77118461CEFDL // -6067343680855748867
    private const val S_U64_DIV_1E8 = 26 // + 64 high

    private const val MASK32L = 0x0000_0000_FFFF_FFFFL

    private fun u128ToUtf8(digitPrintCount: Int, dw1: Long, dw0: Long, utf8: ByteArray, off: Int): Int {
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
            render8DigitsBeforeIndex(r0, utf8, ich)
            ich -= 8
            remainingDigitCount -= 8
        }
        while (remainingDigitCount >= 8) {
            val t0 = unsignedMulHi(dw0T, M_U64_DIV_1E8) ushr S_U64_DIV_1E8
            val r0 = dw0T - (t0 * 1_0000_0000L)
            dw0T = t0
            render8DigitsBeforeIndex(r0, utf8, ich)
            ich -= 8
            remainingDigitCount -= 8
        }
        if (remainingDigitCount > 0)
            renderTailDigitsBeforeIndex(remainingDigitCount, dw0T, utf8, ich)
        return digitPrintCount
    }

    private const val M_U32_DIV_1E1 = 0xCCCCCCCDL
    private const val S_U32_DIV_1E1 = 35

    private const val M_U32_DIV_1E2 = 0x51EB851FL
    private const val S_U32_DIV_1E2 = 37

    private const val M_U64_DIV_1E4 = 0x346DC5D63886594BL
    private const val S_U64_DIV_1E4 = 11 // + 64 high

    private fun render8DigitsBeforeIndex(dw: Long, utf8: ByteArray, offMaxx: Int) {
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
        if (offMin >= 0 && offMaxx <= utf8.size) {
            utf8[offMaxx - 8] = (a.toInt() + '0'.code).toByte()
            utf8[offMaxx - 7] = (b.toInt() + '0'.code).toByte()
            utf8[offMaxx - 6] = (c.toInt() + '0'.code).toByte()
            utf8[offMaxx - 5] = (d.toInt() + '0'.code).toByte()
            utf8[offMaxx - 4] = (e.toInt() + '0'.code).toByte()
            utf8[offMaxx - 3] = (f.toInt() + '0'.code).toByte()
            utf8[offMaxx - 2] = (g.toInt() + '0'.code).toByte()
            utf8[offMaxx - 1] = (h.toInt() + '0'.code).toByte()
        } else {
            throw IndexOutOfBoundsException()
        }
    }

    fun renderTailDigitsBeforeIndex(renderDigitCount: Int, dw: Long, utf8: ByteArray, offMaxx: Int) {
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
            if (ib - 4 >= 0 && ib <= utf8.size) {
                utf8[ib - 4] = (a.toInt() + '0'.code).toByte()
                utf8[ib - 3] = (b.toInt() + '0'.code).toByte()
                utf8[ib - 2] = (c.toInt() + '0'.code).toByte()
                utf8[ib - 1] = (d.toInt() + '0'.code).toByte()
                ib -= 4
                remainingDigitCount -= 4
            } else {
                throw IllegalArgumentException()
            }
        }
        while (remainingDigitCount > 0) {
            val divTen = (t * 0xCCCCCCCDL) ushr 35
            val digit = (t - (divTen * 10L)).toInt()
            utf8[--ib] = ('0'.code + digit).toByte()
            t = divTen
            --remainingDigitCount
        }
        verify { offMaxx - ib == renderDigitCount }
    }

    private val POW10 = longArrayOf( // little-endian order
        1L, 0L,                            // 1 (0)
        0x000000000000000AuL.toLong(), 0L, // 10 (10**1)
        0x0000000000000064uL.toLong(), 0L, // 100 (10**2)
        0x00000000000003E8uL.toLong(), 0L, // 1000 (10**3)
        0x0000000000002710uL.toLong(), 0L, // 10000 (10**4)
        0x00000000000186A0uL.toLong(), 0L, // 100000 (10**5)
        0x00000000000F4240uL.toLong(), 0L, // 1000000 (10**6)
        0x0000000000989680uL.toLong(), 0L, // 10000000 (10**7)
        0x0000000005F5E100uL.toLong(), 0L, // 100000000 (10**8)
        0x000000003B9ACA00uL.toLong(), 0L, // 1000000000 (10**9)
        0x00000002540BE400uL.toLong(), 0L, // 10000000000 (10**10)
        0x000000174876E800uL.toLong(), 0L, // 100000000000 (10**11)
        0x000000E8D4A51000uL.toLong(), 0L, // 1000000000000 (10**12)
        0x000009184E72A000uL.toLong(), 0L, // 10000000000000 (10**13)
        0x00005AF3107A4000uL.toLong(), 0L, // 100000000000000 (10**14)
        0x00038D7EA4C68000uL.toLong(), 0L, // 1000000000000000 (10**15)
        0x002386F26FC10000uL.toLong(), 0L, // 10000000000000000 (10**16)
        0x016345785D8A0000uL.toLong(), 0L, // 100000000000000000 (10**17)
        0x0DE0B6B3A7640000uL.toLong(), 0L, // 1000000000000000000 (10**18)
        0x8AC7230489E80000uL.toLong(), 0L, // 10000000000000000000 (10**19)
        // minBitCount:64  maxBitCount:128
        0x6BC75E2D63100000uL.toLong(), 0x0000000000000005uL.toLong(), // 10**20
        0x35C9ADC5DEA00000uL.toLong(), 0x0000000000000036uL.toLong(), // 10**21
        0x19E0C9BAB2400000uL.toLong(), 0x000000000000021EuL.toLong(), // 10**22
        0x02C7E14AF6800000uL.toLong(), 0x000000000000152DuL.toLong(), // 10**23
        0x1BCECCEDA1000000uL.toLong(), 0x000000000000D3C2uL.toLong(), // 10**24
        0x161401484A000000uL.toLong(), 0x0000000000084595uL.toLong(), // 10**25
        0xDCC80CD2E4000000uL.toLong(), 0x000000000052B7D2uL.toLong(), // 10**26
        0x9FD0803CE8000000uL.toLong(), 0x00000000033B2E3CuL.toLong(), // 10**27
        0x3E25026110000000uL.toLong(), 0x00000000204FCE5EuL.toLong(), // 10**28
        0x6D7217CAA0000000uL.toLong(), 0x00000001431E0FAEuL.toLong(), // 10**29
        0x4674EDEA40000000uL.toLong(), 0x0000000C9F2C9CD0uL.toLong(), // 10**30
        0xC0914B2680000000uL.toLong(), 0x0000007E37BE2022uL.toLong(), // 10**31
        0x85ACEF8100000000uL.toLong(), 0x000004EE2D6D415BuL.toLong(), // 10**32
        0x38C15B0A00000000uL.toLong(), 0x0000314DC6448D93uL.toLong(), // 10**33
        0x378D8E6400000000uL.toLong(), 0x0001ED09BEAD87C0uL.toLong(), // 10**34
        0x2B878FE800000000uL.toLong(), 0x0013426172C74D82uL.toLong(), // 10**35
        0xB34B9F1000000000uL.toLong(), 0x00C097CE7BC90715uL.toLong(), // 10**36
        0x00F436A000000000uL.toLong(), 0x0785EE10D5DA46D9uL.toLong(), // 10**37
        0x098A224000000000uL.toLong(), 0x4B3B4CA85A86C47AuL.toLong(), // 10**38
    )

    private inline fun calcBitLen128(dw1: Long, dw0: Long): Int {
        val dw1IsZeroMask = ((dw1 or -dw1) shr 63).inv().toInt()
        val nlz1 = dw1.countLeadingZeroBits()
        val nlz0 = dw0.countLeadingZeroBits()
        val bitLen = 128 - nlz1 - (nlz0 and dw1IsZeroMask)
        return bitLen
    }

    private fun calcDigitLen128(dw1: Long, dw0: Long): Int {
        val bitLen = calcBitLen128(dw1, dw0)
        val loDigitCount = (bitLen * 1233) ushr 12
        val hiDigitCount = loDigitCount + 1
        val pow10Offset = (loDigitCount shl 1)
        val p1 = POW10[pow10Offset + 1]
        val p0 = POW10[pow10Offset    ]
        val cmp1 = unsignedCmp(dw1, p1)
        if (cmp1 != 0)
            return hiDigitCount - (cmp1 ushr 31)
        val cmp0 = unsignedCmp(dw0, p0)
        return hiDigitCount - (cmp0 ushr 31)
    }

    private inline fun umul128x64to128(x1: Long, x0: Long, y0: Long): Pair<Long, Long> {
        val pp00Hi = unsignedMulHi(x0, y0)
        val pp00Lo = x0 * y0
        val pp10Lo = x1 * y0

        val p0 = pp00Lo
        val p1 = pp00Hi + pp10Lo

        return p1 to p0
    }

    // -- encode bid128 from String --------------------------------------------------------

    private fun encodeStringToBid128OrError(bid128Longs: LongArray, str: String): String? {
        val txt = StringIterator(str)
        var ch = txt.nextChar()
        val sign = ch == '-'
        if (ch == '+' || ch == '-')
            ch = txt.nextChar()
        if (ch >= '0' && ch <= '9' || ch == '.')
            return parseFiniteValueText(bid128Longs, sign, ch, txt)
        val chLower = (ch.code or 0x20).toChar()
        if (chLower == 'i')
            return parseInfinityText(bid128Longs, sign, ch, txt)
        return parseNanText(bid128Longs, sign, ch, txt)
    }

    private class StringIterator(var str: String) {
        private var i = 0
        /** Returns the next character and advances the iterator, or '\u0000' if at end. */
        fun nextChar(): Char = if (i < str.length) str[i++] else '\u0000'

    }

    private fun parseInfinityText(bid128Longs: LongArray, sign: Boolean, chFirst: Char, txt: StringIterator): String? {
        var ch = chFirst
        var chPrevCode = 0
        for (target in "infinity") {
            if (ch.code or 0x20 != target.code) {
                if (ch.code == 0)
                    break
                else
                    return "failure parsing infinity"
            }
            chPrevCode = ch.code or 0x20
            ch = txt.nextChar()
        }
        if (ch.code != 0 || (chPrevCode != 'f'.code && chPrevCode != 'y'.code))
            return "failure parsing Infinity"
        bid128Longs[0] = 0x7800000000000000 or if (sign) Long.MIN_VALUE else 0
        bid128Longs[1] = 0L
        return null
    }

    private fun parseNanText(bid128Longs: LongArray, sign: Boolean, chFirst: Char, txt: StringIterator): String? {
        var ch = chFirst
        val hasS = (ch.code or 0x20) == 's'.code
        val hasQ = (ch.code or 0x20) == 'q'.code
        if (hasQ or hasS)
            ch = txt.nextChar()
        if (((ch.code or 0x20) != 'n'.code) ||
            ((txt.nextChar().code or 0x20) != 'a'.code) ||
            ((txt.nextChar().code or 0x20) != 'n'.code)
        )
            return "failure parsing NaN"
        var payloadDw0 = 0L
        var payloadDw1 = 0L
        ch = txt.nextChar()
        if (ch.code != 0) {
            var accumDigitCount = 0
            var accum19a = 0L
            var accum19b = 0L
            do {
                if (ch >= '0' && ch <= '9') {
                    val d = ch - '0'
                    // flush leading zeros from payload ... don't increment
                    accumDigitCount += (-(accumDigitCount or d)) ushr 31
                    if (accumDigitCount <= 19)
                        accum19a = (accum19a * 10L) + d.toLong()
                    else
                        accum19b = (accum19b * 10L) + d.toLong()
                } else {
                    if (ch != '(' && ch != ')' &&
                        ch != '[' && ch != ']' &&
                        ch != '{' && ch != '}'
                    )
                        return "non-digit NaN payload character"
                }
                ch = txt.nextChar()
            } while (ch.code != 0)
            if (accumDigitCount > 33) {
                // IEEE754-2019 3.5.2 Encodings
                //  ... as the payload of a NaN). If the value exceeds the maximum,
                //  the significand c is non-canonical and the value used for c is zero.
                payloadDw0 = 0
                payloadDw1 = 0
            } else {
                payloadDw0 = accum19a
                payloadDw1 = 0L
                if (accumDigitCount > 19) {
                    val pow10 = accumDigitCount - 19
                    val (hi, lo) = fmaPow10(accum19a, pow10, accum19b)
                    payloadDw1 = hi
                    payloadDw0 = lo
                }
            }
        }
        val withoutPayload = (if (hasS) 0x7E00000000000000 else 0x7C00000000000000) or (if (sign) Long.MIN_VALUE else 0L)
        bid128Longs[0] = withoutPayload or payloadDw1
        bid128Longs[1] = payloadDw0
        return null
    }

    private fun fmaPow10(x: Long, pow10: Int, a: Long): Pair<Long, Long> {
        val y = POW10[pow10 shl 1]
        val prodLo = x * y
        val prodHi = unsignedMulHi(x, y)
        val sumLo = prodLo + a
        val sumHi = prodHi + if (unsignedLT(sumLo, prodLo)) 1L else 0L
        return sumHi to sumLo
    }

    private const val EXACT   = 0
    private const val LT_HALF = 1
    private const val HALF    = 2
    private const val GT_HALF = 3

    private fun parseFiniteValueText(bid128Longs: LongArray, sign: Boolean, chFirst: Char, txt: StringIterator): String? {
        var residue = EXACT

        var hasCoefficientDigit = false // have we seen any digits at all, including zero
        var significantDigitCount = 0 // does not count leading zeros
        var hasDot = false
        var expSign = false

        var ch = chFirst
         if (ch.code == 0)
             return "empty string"

         var fractionalDigitCount = 0
         var accum19a = 0L
         var accum19b = 0L
         var exp = 0

         while (ch in '0'..'9' || ch == '.') {
             when (ch) {
                 in '0'..'9' -> {
                     val d = ch - '0'
                     hasCoefficientDigit = true
                     // count while flushing leading zeros
                     significantDigitCount += (-(significantDigitCount or d)) ushr 31
                     when {
                         significantDigitCount <= 19 -> accum19a = (accum19a * 10L) + d.toLong()
                         significantDigitCount <= 34 -> accum19b = (accum19b * 10L) + d.toLong()
                         significantDigitCount == 34 + 1 ->
                             residue = residueFromDecimalDigit(d)
                         else ->
                             residue = residueMerge(residue, residueFromDecimalDigit(d))
                     }
                     if (hasDot)
                         ++fractionalDigitCount
                 }

                 '.' -> when {
                     hasDot -> return "double radix dot"
                     else -> hasDot = true
                 }
             }
             ch = txt.nextChar()
         }
         if (!hasCoefficientDigit)
             return "no coefficient digit"
         // this path has at least one digit
         if (ch == 'E' || ch == 'e') {
             ch = txt.nextChar()
             if (ch == '+' || ch == '-') {
                 expSign = ch == '-'
                 ch = txt.nextChar()
             }
             var hasExpDigit = false
             var expSignificantDigitCount = 0
             while (ch in '0'..'9') {
                 hasExpDigit = true
                 val eDigit = ch - '0'
                 // count while flushing leading zeros
                 expSignificantDigitCount +=
                     (-(expSignificantDigitCount or eDigit)) ushr 31
                 exp = exp * 10 + eDigit
                 ch = txt.nextChar()
             }
             if (!hasExpDigit)
                 return "no exponent digit"
             // clamp exp to 9999 once after the loop
             if (expSignificantDigitCount > 4)
                 exp = 9999
         }
         if (ch.code != 0)
             return "unexpected char"
         // we have at least one digit
         var dw0 = accum19a
         var dw1 = 0L
         if (significantDigitCount > 19) {
             val pow10 = min(34, significantDigitCount) - 19
             val (hi, lo) = fmaPow10(accum19a, pow10, accum19b)
             dw1 = hi
             dw0 = lo
         }
         // at this point, our coeff <= precision digits
         // but we need to deal with residue and rounding
         // rounding rollover could affect the exponent
         //
         // when we accept oversize coefficients, then whether the excess
         // digits are to the right or left of the exponent will affect
         // the qExp ...
         val signedExp = if (expSign) -exp else exp
         val qExp = signedExp - fractionalDigitCount + max(0, significantDigitCount - 34)
         if ((dw1 or dw0) == 0L) {
             // allow any exponent with Zero
             bid128Zero(bid128Longs, sign, qExp)
         } else {
             bid128RoundAndFinalizeFnz(bid128Longs, sign, qExp, dw1, dw0, residue)
         }
         return null
    }

    private fun bid128Zero(bid128Longs: LongArray, sign: Boolean, qExp: Int) {
        val qClamped = max(min(qExp, 6111), -6176)
        bid128Set(bid128Longs, sign, qClamped, 0L, 0L)
    }

    private const val MAXX_COEFF_34_HI = 0x0001ED09BEAD87C0L // 10**34
    private const val MAXX_COEFF_34_LO = 0x378D8E6400000000L
    // 10**33 is the smallest 34-digit coefficient
    private const val MIN_PRECISION_34_COEFF_HI = 0x0000314DC6448D93L
    private const val MIN_PRECISION_34_COEFF_LO = 0x38C15B0A00000000L

    private fun bid128RoundAndFinalizeFnz(bid128Longs: LongArray, sign: Boolean, qExpIn: Int, dw1In: Long, dw0In: Long, residueIn: Int) {
        // Step 1: Fast path: already in valid decimal128 range ...
        //         ... and no roundTiesToEven rounding
        if (residueIn <= LT_HALF || residueIn == HALF && (dw0In and 1L) == 0L) {
            if (coeffQExpFit(dw1In, dw0In, qExpIn)) {
                bid128Set(bid128Longs, sign, qExpIn, dw1In, dw0In)
                return
            }
        }
        // Step 2: special values ... not applicable ... only FNZ

        // Step 3: zero coefficient ... not applicable ... only FNZ

        // Step 4: underflow
        // divert iff range truncation exceeds precision truncation
        val rangeTruncationNeeded = -6176 - qExpIn
        val digitLenIn = calcDigitLen128(dw1In, dw0In)
        val precisionTruncationNeeded = max(digitLenIn - 34, 0)
        if (rangeTruncationNeeded > precisionTruncationNeeded) {
            bid128FinalizeUnderflowRegion(bid128Longs, sign, qExpIn, dw1In, dw0In, residueIn)
            return
        }

        // Step 5: normalize to <= precision, accumulating residue
        var totalResidue = residueIn
        var dw1 = dw1In
        var dw0 = dw0In
        var qExp = qExpIn
        if (precisionTruncationNeeded > 0) {
            val truncationResidue: Int = u128ScaleDownPow10(bid128Longs, dw1, dw0, precisionTruncationNeeded)
            dw1 = bid128Longs[0]
            dw0 = bid128Longs[1]
            totalResidue = residueMerge(truncationResidue, totalResidue)
            qExp += precisionTruncationNeeded
            verify { calcDigitLen128(dw1, dw0) == 34 }
        }

        // step 6: rounding ... in this world only roundTiesToEven
        val applyRounding = totalResidue == GT_HALF || totalResidue == HALF && (dw0 and 1L) != 0L
        if (applyRounding) {
            // step 6.1: increment
            ++dw0
            dw1 += if (dw0 == 0L) 1L else 0L

            // step 6.2: rollover
            if (dw1 == MAXX_COEFF_34_HI && dw0 == MAXX_COEFF_34_LO) {
                dw1 = MIN_PRECISION_34_COEFF_HI
                dw0 = MIN_PRECISION_34_COEFF_LO
                ++qExp
            }
        }

        // step 7: check final bounds
        verify { qExp >= -6176 }
        verify { calcDigitLen128(dw1, dw0) <= 34 }
        if (qExp > 6111) {
            val qExcess = qExp - 6111
            val digitLen = calcDigitLen128(dw1, dw0)
            if (digitLen + qExcess <= 34)
                bid128FinalizeClamping(bid128Longs, sign, qExp, dw1, dw0)
            else
                bid128FinalizeOverflow(bid128Longs, sign)
            return
        }
        bid128Set(bid128Longs, sign, qExp, dw1, dw0)
    }

    private fun bid128Set(bid128Longs: LongArray, sign: Boolean, qExp: Int, dw1: Long, dw0: Long) {
        val qBiased = qExp + 6176
        val s = if (sign) Long.MIN_VALUE else 0
        val g = qBiased.toLong() shl (3 + 46)
        bid128Longs[0] = s or g or dw1
        bid128Longs[1] = dw0
    }

    private fun bid128FinalizeOverflow(bid128Longs: LongArray, sign: Boolean) {
        bid128Longs[0] = (if (sign) Long.MIN_VALUE else 0L) or 0x7800000000000000L
        bid128Longs[1] = 0L
    }

    private fun bid128FinalizeClamping(bid128Longs: LongArray, sign: Boolean, qExp: Int, dw1: Long, dw0: Long) {
        val qExcess = qExp - 6111
        verify { qExcess > 0 && qExcess <= 34 - calcDigitLen128(dw1, dw0) }
        val (dw1Scaled, dw0Scaled) = umul128xPow10to128(dw1, dw0, qExcess)
        verify { coeffQExpFit(dw1Scaled, dw0Scaled, 6111) }
        bid128Set(bid128Longs, sign, 6111, dw1Scaled, dw0Scaled)
    }

    private inline fun coeffQExpFit(dw1: Long, dw0: Long, qExp: Int): Boolean {
        if (qExp < -6176 || qExp > 6111)
            return false
        if (dw1.countLeadingZeroBits() > 128 - 113)
            return true
        val pow10Offset = 34 shl 1
        val maxxHi = POW10[pow10Offset + 1]
        return unsignedLT(dw1, maxxHi) || dw1 == maxxHi && unsignedLT(dw0, POW10[pow10Offset])
    }


    private fun bid128FinalizeUnderflowRegion(bid128Longs: LongArray, sign: Boolean, qExp: Int, dw1: Long, dw0: Long, residue: Int) {
        // IEEE 754 7.5 Underflow - handle subnormal region
        verify { qExp < -6176 }
        val truncationNeeded = -6176 - qExp
        val digitLen = calcDigitLen128(dw1, dw0)
        when {
            truncationNeeded > digitLen -> {
                // Result is swamped - becomes zero with roundTiesToEven
                // This is always inexact
                bid128Zero(bid128Longs, sign, -6176)
            }
            truncationNeeded == digitLen ->
                bid128FinalizeUnderflowBoundary(bid128Longs, sign, qExp, dw1, dw0, residue)
            else ->
                bid128FinalizeSubnormal(bid128Longs, sign, qExp, dw1, dw0, residue)
        }
    }

    /**
     * The most significant digit of our current coefficient is exactly
     * to the right of our range.
     * Therefore, the coefficient itself determines rounding.
     * Compare the coefficient with its own (10**digitLen) / 2
     */

    private fun bid128FinalizeUnderflowBoundary(bid128Longs: LongArray, sign: Boolean, qExp: Int, dw1: Long, dw0: Long, residueIn: Int) {
        val digitLen = calcDigitLen128(dw1, dw0)
        verify { digitLen != 0 }
        val scaleResidue = residueFromValuePow10(dw1, dw0, digitLen)
        val totalResidue = residueMerge(scaleResidue, residueIn)
        // roundTiesToEven ...
        // only if we are GT_HALF since otherwise the result is 0 ... which is even
        bid128Set(bid128Longs, sign, -6176, 0L, if (totalResidue == GT_HALF) 1L else 0L)
    }

    private fun residueFromValuePow10(dw1: Long, dw0: Long, pow10: Int): Int {
        if ((dw1 or dw0) == 0L)
            return EXACT
        val pow10Index = pow10 shl 1
        val pow10Hi = POW10[pow10Index + 1]
        val pow10Lo = POW10[pow10Index]

        val halfHi = pow10Hi ushr 1
        val halfLo = (pow10Hi shl 63) or (pow10Lo ushr 1)
        verify { dw1 >= 0 && halfHi >= 0 }
        if (dw1 != halfHi) {
            return if (dw1 < halfHi) LT_HALF else GT_HALF
        }
        if (dw0 != halfLo)
            return if (unsignedLT(dw0, halfLo)) LT_HALF else GT_HALF
        return HALF
    }

    private fun bid128FinalizeSubnormal(bid128Longs: LongArray, sign: Boolean, qExpIn: Int,
                                        dw1In: Long, dw0In: Long, residueIn: Int) {
        val truncationNeeded = -6176 - qExpIn
        verify { truncationNeeded > 0 && truncationNeeded < calcDigitLen128(dw1In, dw0In) }

        val scaleResidue = u128ScaleDownPow10(bid128Longs, dw1In, dw0In, truncationNeeded)
        val totalResidue = residueMerge(scaleResidue, residueIn)
        var dw1T = bid128Longs[0]
        var dw0T = bid128Longs[1]
        var qExpT = -6176

        val roundUp =
            totalResidue == GT_HALF || totalResidue == HALF && (dw0T and 1L) != 0L
        if (roundUp) {
            // apply rounding
            ++dw0T
            dw1T += if (dw0T == 0L) 1L else 0L
            if (dw1T == MAXX_COEFF_34_HI && dw0T == MAXX_COEFF_34_LO) {
                dw1T = MIN_PRECISION_34_COEFF_HI
                dw0T = MIN_PRECISION_34_COEFF_LO
                ++qExpT
            }
        }
        bid128Set(bid128Longs, sign, qExpT, dw1T, dw0T)
    }

    private fun u128ScaleDownPow10(bid128Longs: LongArray, dw1: Long, dw0: Long, precisionTruncationNeeded: Int): Int {
        verify { (dw1 or dw0) != 0L }
        verify { precisionTruncationNeeded > 0 && precisionTruncationNeeded < calcDigitLen128(dw1, dw0) }
        var scaleResidue = EXACT
        var dw1T = dw1
        var dw0T = dw0
        var digitsRemaining = precisionTruncationNeeded
        do {
            val stepPow10 = min(digitsRemaining, 9)
            val rem = u128DivModPow10Max9(bid128Longs, dw1T, dw0T, stepPow10)
            dw1T = bid128Longs[0]
            dw0T = bid128Longs[1]
            val stepResidue = residueFromValuePow10(0L, rem, stepPow10)
            scaleResidue = residueMerge(stepResidue, scaleResidue)
            digitsRemaining -= stepPow10
        } while (digitsRemaining > 0)
        return scaleResidue
    }

    private fun u128DivModPow10Max9(quotLongs: LongArray, dwHi: Long, dwLo: Long, pow10: Int): Long {
        require(pow10 in 1..9) { "n must be in 1..9: pow10=$pow10" }
        require(dwHi in 0 until (1L shl 49)) { "dHi exceeds 49 bits: dHi=$dwHi" }

        val mask = (1L shl pow10) - 1
        val b = dwLo and mask
        val sHi = dwHi ushr pow10
        val sLo = (dwLo ushr pow10) or (dwHi shl (64 - pow10))

        val k = 50 - pow10
        val maskK = (1L shl k) - 1
        val hi63 = (sHi shl (14 + pow10)) or (sLo ushr k)
        val loK = sLo and maskK
        // remember ... this POW10 table has 2 slots per power, so double the index
        val d = POW10[pow10 shl 1] ushr pow10     // = 5^n
        val q1 = hi63 / d
        val r1 = hi63 % d
        val mid = (r1 shl k) or loK
        val q0 = mid / d
        val r0 = mid % d

        val qLo = (q1 shl k) + q0
        val qHi = q1 ushr (14 + pow10)
        val remainder = (r0 shl pow10) or b

        quotLongs[0] = qHi
        quotLongs[1] = qLo
        return remainder
    }


    private const val DIGIT_MAP = 0b11_11_11_11_10_01_01_01_01_00
    private fun residueFromDecimalDigit(digit: Int): Int = (DIGIT_MAP shr (digit shl 1)) and 0x03

    private fun residueMerge(oldResidue: Int, newStickyResidue: Int): Int {
        val s = (newStickyResidue and 1) or (newStickyResidue ushr 1)
        val r = (oldResidue or s) and 0x03
        return r
    }

    private inline fun umul128xPow10to128(dw1: Long, dw0: Long, pow10: Int): Pair<Long, Long> {
        val pow10Offset = pow10 shl 1
        val pow10Dw1 = POW10[pow10Offset + 1]
        val pow10Dw0 = POW10[pow10Offset    ]
        return umul128x128to128(dw1, dw0, pow10Dw1, pow10Dw0)
    }

    private inline fun umul128x128to128(x1: Long, x0: Long, y1: Long, y0: Long): Pair<Long, Long> {
        val pp00Hi = unsignedMulHi(x0, y0)
        val pp00Lo = x0 * y0
        val pp10Lo = x1 * y0
        val pp01Lo = x0 * y1

        val p0 = pp00Lo
        val p1 = pp00Hi + pp10Lo + pp01Lo

        return p1 to p0
    }

    private fun unsignedCmp(x: Long, y: Long): Int =
        (x xor Long.MIN_VALUE).compareTo(y xor Long.MIN_VALUE)

    private fun unsignedLT(x: Long, y: Long): Boolean =
        (x xor Long.MIN_VALUE) < (y xor Long.MIN_VALUE)

    private fun unsignedMulHi(x: Long, y: Long): Long {
        val xULong = x.toULong()
        val yULong = y.toULong()
        val xLo = xULong and 0xFFFFFFFFUL
        val xHi = xULong shr 32
        val yLo = yULong and 0xFFFFFFFFUL
        val yHi = yULong shr 32

        val pp00 = xLo * yLo
        val pp01 = xHi * yLo
        val pp10 = xLo * yHi
        val pp11 = xHi * yHi

        val mid = pp01 + pp10  // may overflow
        val midCarry = if (mid < pp01) 1UL else 0UL  // carry from mid overflow

        val midWithLo = (pp00 shr 32) + (mid and 0xFFFFFFFFUL)
        // midWithLo cannot overflow: max is (2^32-1) + (2^32-1) < 2^33

        return (pp11 + (mid shr 32) + (midCarry shl 32) + (midWithLo shr 32)).toLong()
    }
}
