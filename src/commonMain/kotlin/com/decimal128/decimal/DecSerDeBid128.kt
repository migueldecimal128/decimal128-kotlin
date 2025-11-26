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
object DecSerDeBid128 {

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
     * 46 bits of the significand from `bid128Hi`, combines them with the
     * remaining 64 significand bits in `bid128Lo`, and reconstructs the
     * corresponding `Decimal` value. Finite, infinite, quiet NaN, signaling NaN,
     * and non-canonical encodings are all handled per IEEE 754-2019 rules.
     *
     * @param bid128Hi the high 64 bits of the BID128 encoding
     * @param bid128Lo the low 64 bits of the BID128 encoding
     * @return the decoded `Decimal` value
     */
    private fun decodeBid128ULongs(bid128Hi: ULong, bid128Lo: ULong): Decimal {
        val sign = bid128Hi.toLong() < 0L
        val combination = (bid128Hi shr 46).toInt() and 0x1FFFF
        val significand110Hi = bid128Hi and ((1uL shl (110 - 64)) - 1uL)
        val d = when {
            (combination and 0x18000) != 0x18000 -> {
                val biasedExponent = combination shr 3
                val qExp = biasedExponent + QTINY_Neg6176 // this is effectively a subtraction
                val mostSignificant3 = (combination and 0x07).toULong() shl (110 - 64)
                var dw1 = mostSignificant3 or significand110Hi
                var dw0 = bid128Lo
                var bitLen = calcBitLen128(dw1, dw0)
                var digitLen = U256Pow10.calcDigitLen128(bitLen, dw1, dw0)
                if (digitLen > 34) {
                    // IEEE754-2019 3.5.2 p21
                    //  If the value exceeds the maximum, the significand c is
                    //  non-canonical and the value used for c is zero.
                    dw1 = 0uL; dw0 = 0uL; bitLen = 0; digitLen = 0
                }
                Decimal(sign, qExp, bitLen, digitLen, dw1, dw0)
            }
            (combination and 0x1F000) == 0x1E000 -> Decimal.infinity(sign)
            (combination and 0x1F000) == 0x1F000 -> {
                val isSignaling = (combination and 0x800) == 0x800
                Decimal.NaN(sign, isSignaling, significand110Hi, bid128Lo)
            }
            else -> {
                // large-form finite pattern => non-canonical for decimal128:
                // E = bits [15:2] (G2..Gw+3), C = 0, keep sign S.
                val E = (combination ushr 1) and 0x3FFF   // 14 bits
                val qExp = E + QTINY_Neg6176                // preserve exponent
                Decimal.zero(sign, qExp)
            }
        }
        return d
    }

}