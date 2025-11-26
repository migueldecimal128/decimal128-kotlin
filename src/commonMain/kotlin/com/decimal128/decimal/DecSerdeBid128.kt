@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.U256Bits.calcBitLen128

/**
 * Serialization helpers for IEEE-754 Decimal128 values using the
 * **Binary Integer Decimal (BID)** encoding format.
 *
 * This object provides low-level routines for converting a [`Decimal`]
 * instance to and from its canonical **128-bit BID representation**.
 * Two output forms are supported:
 *
 *  • a pair of 64-bit words written into a `LongArray`
 *  • a 16-byte sequence written into a `ByteArray`
 *
 * Endianness is controlled explicitly via the `isLittleEndian` parameter.
 *   - When `isLittleEndian == false`, the output is **big-endian**, with the
 *     most-significant 64-bit word (containing the sign, exponent, and
 *     combination field) stored first.
 *   - When `isLittleEndian == true`, the two words (or 16 bytes) are written
 *     in **little-endian** order (least-significant word/byte first).
 *
 * These routines perform only *bit-pattern serialization* of the already
 * encoded BID128 form. No rounding, normalization, or interpretation is
 * performed. The caller is responsible for providing arrays large enough to
 * hold the output:
 *
 *  • `LongArray`: requires two contiguous entries starting at `offset`
 *  • `ByteArray`: requires sixteen contiguous bytes starting at `offset`
 *
 * These methods are intended for advanced users who need exact control over
 * the external binary representation of Decimal128 values—for example:
 *
 *  • interop with BSON/MongoDB (big-endian BID128)
 *  • storage in binary file formats
 *  • low-level transport protocols
 *
 * They are **not** required or used for normal arithmetic or text formatting.
 */
object DecSerdeBid128 {

    /**
     * Encodes `d` into its 128-bit **BID** representation and writes the two
     * 64-bit words into `longs` starting at `offset`.
     *
     * @param d the decimal value to encode
     * @param longs destination array for the two 64-bit words
     * @param offset index of the first word to write
     * @param isLittleEndian if true, writes [lo, hi]; otherwise writes [hi, lo]
     */
    fun encodeBid128(d: Decimal, longs: LongArray, offset: Int = 0, isLittleEndian: Boolean = false) {
        require (offset >= 0 && offset + 1 < longs.size)

        val bid128Hi = encodeBid128Hi(d)
        val bid128Lo = d.dw0

        val iLS = offset + if (isLittleEndian) 0 else 1
        val iMS = offset + if (isLittleEndian) 1 else 0
        longs[iLS] = bid128Lo.toLong()
        longs[iMS] = bid128Hi.toLong()
    }

    /**
     * Encodes `d` into its 128-bit **BID** representation and writes the
     * 16-byte result into `bytes` starting at `offset`.
     *
     * @param d the decimal value to encode
     * @param bytes destination byte array (must have 16 bytes from `offset`)
     * @param offset starting index for the encoded bytes
     * @param isLittleEndian if true, writes least-significant byte first; otherwise big-endian
     */
    fun encodeBid128(d: Decimal, bytes: ByteArray, offset: Int = 0, isLittleEndian: Boolean = false) {
        require (offset >= 0 && offset + 15 < bytes.size)

        val bid128Hi = encodeBid128Hi(d)
        val bid128Lo = d.dw0
        val lo = if (isLittleEndian) bid128Lo else bid128Hi
        val hi = if (isLittleEndian) bid128Hi else bid128Lo
        val shiftStep = if (isLittleEndian) 8 else -8
        var shift = if (isLittleEndian) 0 else 56
        for (i in offset..offset + 7) {
            bytes[i    ] = (lo shr shift).toByte()
            bytes[i + 8] = (hi shr shift).toByte()
            shift += shiftStep
        }
    }

    /**
     * Decodes a 128-bit **BID** value from two 64-bit words in `longs`
     * starting at `offset`.
     *
     * @param longs source array containing the encoded words
     * @param offset index of the first word to read
     * @param isLittleEndian if true, reads [lo, hi]; otherwise reads [hi, lo]
     * @return the decoded `Decimal` value
     */
    fun decodeBid128(longs: LongArray, offset: Int = 0, isLittleEndian: Boolean = false): Decimal {
        require (offset >= 0 && offset + 1 < longs.size)

        val iLS = offset + if (isLittleEndian) 0 else 1
        val iMS = offset + if (isLittleEndian) 1 else 0
        val bid128Hi = longs[iMS].toULong()
        val bid128Lo = longs[iLS].toULong()

        return decodeBid128ULongs(bid128Hi, bid128Lo)
    }

    /**
     * Decodes a 128-bit **BID** value from 16 bytes in `bytes`
     * starting at `offset`.
     *
     * @param bytes source byte array containing the encoded value
     * @param offset starting index of the 16-byte BID128 sequence
     * @param isLittleEndian if true, reads least-significant byte first; otherwise big-endian
     * @return the decoded `Decimal` value
     */
    fun decodeBid128(bytes: ByteArray, offset: Int = 0, isLittleEndian: Boolean = false): Decimal {
        require (offset >= 0 && offset + 15 < bytes.size)

        var lo = 0uL
        var hi = 0uL
        val shiftStep = if (isLittleEndian) 8 else -8
        var shift = if (isLittleEndian) 0 else 56
        for (i in offset..offset + 7) {
            lo = lo or ((bytes[i    ].toULong() and 0xFFuL) shl shift)
            hi = hi or ((bytes[i + 8].toULong() and 0xFFuL) shl shift)
            shift += shiftStep
        }
        val bid128Hi = if (isLittleEndian) hi else lo
        val bid128Lo = if (isLittleEndian) lo else hi
        return decodeBid128ULongs(bid128Hi, bid128Lo)
    }

    private const val QTINY_Neg6176 = -6176
    private const val QMAX_6111 = 6111

    /**
     * Encodes the Decimal128 BID **sign bit** and **17-bit G-combination field**.
     *
     * This produces the upper 18 bits of the BID128 high word:
     *  - 1 sign bit (bit 63)
     *  - 17-bit combination field (bits 62–46), derived from `qExp` and
     *    the leading coefficient bits (`mostSigBits4`, of which BID uses only 3).
     *
     * Finite values encode a biased exponent and leading coefficient bits.
     * Non-finite values (∞, qNaN, sNaN) map to their reserved combination patterns.
     *
     * @param sign whether the value is negative
     * @param qExp the quantized exponent or a non-finite tag
     * @param mostSigBits4 the most significant 4 coefficient digits (top 3 bits used by BID)
     * @return the sign and 17-bit combination field positioned in bits 63–46
     */
    private inline fun encodeSignAndGCombinationFieldBid128(sign: Boolean, qExp: Int, mostSigBits4: Int) : ULong {
        // this is 4 digits for DPD format, but only the most significant 3-bits of BID
        require (mostSigBits4 in 0..9)
        val signBit = if (sign) 1uL shl 63 else 0uL
        val gCombinationField = when {
            qExp < MIN_SPECIAL_VALUE -> {
                require(qExp in QTINY_Neg6176..QMAX_6111)
                val biasedQExp = qExp - QTINY_Neg6176 // remember qTiny is negative
                check((biasedQExp and 0x3000) != 0x3000)
                if ((mostSigBits4 and 0x08) == 0)
                    (biasedQExp shl 3) or mostSigBits4
                else
                    0x18000 or (biasedQExp shl 1) or (mostSigBits4 and 1)
            }
            qExp == NON_FINITE_INF  -> 0b11110 shl 12
            qExp == NON_FINITE_QNAN -> 0b111110 shl 11
            qExp == NON_FINITE_SNAN -> 0b111111 shl 11
            else -> throw RuntimeException("unrecognized")
        }
        val signCombo = signBit or (gCombinationField.toULong() shl 46)
        return signCombo
    }

    /**
     * Assembles the high 64-bit word of a Decimal128 value in **BID** format.
     *
     * This extracts the top 3 coefficient bits from `d.dw1`, builds the
     * 1-bit sign and 17-bit G-combination field, and combines them with
     * the upper 46 bits of the 113-bit significand stored in `d.dw1`.
     *
     * @param d the decimal value to encode
     * @return the fully assembled high 64-bit BID128 word
     */
    private fun encodeBid128Hi(d: Decimal): ULong {
        // Don't be confused by the fact that this is 3 bits
        // but below you will see 4 bits.
        // The format allows 4 bits, but only 3 are used.
        val mostSignificant3 = (d.dw1 shr (110 - 64)).toInt() and 0x07
        val signCombo = encodeSignAndGCombinationFieldBid128(d.sign, d.qExp, mostSignificant3)
        val significand110Hi = d.dw1 and ((1uL shl (110 - 64)) - 1uL)
        val bidDecimal128Hi = signCombo or significand110Hi
        return bidDecimal128Hi
    }

    /**
     * Decodes a Decimal128 value in **BID** format from its two 64-bit words.
     *
     * This extracts the sign, the 17-bit G-combination field, and the upper
     * 46 coefficient bits from `bid128Hi`, combines them with the remaining
     * 64 coefficient bits in `bid128Lo`, and reconstructs the corresponding
     * `Decimal` value. Finite numbers, infinities, quiet NaNs, and signaling
     * NaNs are handled per IEEE 754-2019 rules.
     *
     *  - For **finite values**, coefficients with more than 34 decimal digits
     *    are non-canonical; such encodings are treated as having coefficient 0.
     *  - For **infinity**, all lower bits are ignored.
     *  - For **NaNs**, lower bits of the combination field are ignored,
     *    the lower 110 (46 + 64) bits form the payload; non-canonical
     *    payloads (more than 33 digits) have the payload set to 0.
     *
     * @param bid128Hi the high 64 bits of the BID128 encoding
     * @param bid128Lo the low 64 bits of the BID128 encoding
     * @return the decoded `Decimal` value
     */
    private fun decodeBid128ULongs(bid128Hi: ULong, bid128Lo: ULong): Decimal {
        // 1 + 17 + 46 == 64
        // 1 bit for the sign
        val sign = bid128Hi.toLong() < 0L
        // 17 bit combination field ... 0x1FFFF
        val combination = (bid128Hi shr 46).toInt() and 0x1FFFF
        // 46 bits for the middle-chunk of the coefficient 46 + 64 == 110
        val coefficientMid46 = bid128Hi and ((1uL shl (110 - 64)) - 1uL)
        // To identify bits, IEEE754-2019 uses the terminology G0-G16 to identify
        // the bits of the 17-bit combination field, with G0 as most significant.
        when {
            // if the top 2 bits are not 0b11
            (combination and 0x18000) != 0x18000 -> {
                //  IEEE754-2019 3.5.2 c) 2) i) -- p 21
                //   If G0 and G1 together are one of 00, 01, or 10, then the biased
                //   exponent E is formed from G0 through G16 and the significand
                //   is formed from bits G17 through the end of the encoding (including T).
                //
                // the top 14 bits of the combination field represent the biased exponent ...
                val biasedExponent = combination shr 3
                val qExp = biasedExponent + QTINY_Neg6176 // this is effectively a subtraction
                // ... and the bottom 3 bits are the most significant bits of the significand
                // coeff/significant is 3 + 46 + 64 == 113 ... but that's not all ... see below
                val mostSignificant3 = (combination and 0x07).toULong() shl (110 - 64)
                val dw1 = mostSignificant3 or coefficientMid46
                val dw0 = bid128Lo
                val bitLen = calcBitLen128(dw1, dw0)
                val digitLen = U256Pow10.calcDigitLen128(bitLen, dw1, dw0)
                // IEEE754-2019 3.5.2 p21
                //  If the value exceeds the maximum, the significand c is
                //  non-canonical and the value used for c is zero.
                if (digitLen > 34)
                    return Decimal.zero(sign, qExp)
                return Decimal(sign, qExp, bitLen, digitLen, dw1, dw0)
            }
            // otherwise, the top two bits are 0b11
            // if the top 5 bits are 0x11110 then Infinity
            (combination and 0x1F000) == 0x1E000 -> return Decimal.infinity(sign)
            // if the top 5 bits are 0x11111 then NaN
            (combination and 0x1F000) == 0x1F000 -> {
                // with the next bit determining signaling NaN
                val isSignaling = (combination and 0x1F800) == 0x1F800
                return Decimal.NaN(sign, isSignaling, coefficientMid46, bid128Lo)
            }
            // otherwise, top 4 bits were 0x1100 0x1101 0x1110
            else -> {
                // IEEE754-2019 3.5.2 c) 2) ii) -- p 21
                //  If G0 and G1 together are 11 and G2 and G3 together are one
                //  of 00, 01, or 10, then the biased exponent E is formed from
                //  G2 through G18 and the significand is formed by prefixing
                //  the 4 bits (8 + G19) to T.
                //
                // This pattern is non-canonical for decimal128 because all
                // all 114-bit coefficients starting with 0b100x exceed 34
                // decimal digits.
                // This encoding exists because for bid64 and bid32 having
                // the top bit set is needed, so they simply replicated the
                // encoding technique for bid128.
                // this bit combination *is* used by DPD format.
                //
                // E = bits [15:2] (G2..Gw+3), C = 0, keep sign S.
                val E = (combination ushr 1) and 0x3FFF   // 14 bits
                val qExp = E + QTINY_Neg6176              // preserve exponent
                return Decimal.zero(sign, qExp)
            }
        }
    }

    /**
     * Parses a 128-bit BID hex value in the IntelRDFPMathLib20U3 test format.
     *
     * Found in intel decimal floating point library:
     * https://www.intel.com/content/www/us/en/developer/articles/tool/intel-decimal-floating-point-math-library.html
     * https://www.netlib.org/misc/intel/
     *
     * The test suite is the file: IntelRDFPMathLib20U3/TESTS/readtest.in
     *
     * It contains text representations of decimal32, decimal64, and decimal128 values,
     * some of which are in hexadecimal.
     *
     * The 128-bit hex values consist of 32 hexadecimal digits enclosed in square brackets,
     * with an optional comma separating the upper and lower 64-bit words:
     *
     *  • `[bc92000000000000,0000000000000000]`
     *  • `[291a7e63609fe32501e21199f946252b]`
     *
     * @param str the input string in Intel BID128 hex format
     * @return a triple of (isValid, hiWord, loWord)
     */
    fun parseIntelBidHex(str: String): Triple<Boolean, ULong, ULong> {
        if (str.length !in 34..35 ||
            str[0] != '[' || str[str.lastIndex] != ']' ||
            str.length == 35 && str[17] != ',')
            return Triple(false, 0uL, 0uL)
        val (isValidHi, bid128Hi) = parseHexDword(str, 1)
        val (isValidLo, bid128Lo) = parseHexDword(str, (str.length + 1) shr 1)
        return Triple(isValidHi && isValidLo, bid128Hi, bid128Lo)
    }

    private fun parseHexDword(str: String, off: Int): Pair<Boolean, ULong> {
        var dw = 0uL
        for (i in 0..15) {
            val ch = str[off + i]
            dw = when (ch) {
                in '0'..'9' -> (dw shl 4) or (ch - '0').toULong()
                in 'A'..'F' -> (dw shl 4) or (ch - 'A' + 10).toULong()
                in 'a'..'f' -> (dw shl 4) or (ch - 'a' + 10).toULong()
                else -> return false to 0uL
            }
        }
        return true to dw
    }



}