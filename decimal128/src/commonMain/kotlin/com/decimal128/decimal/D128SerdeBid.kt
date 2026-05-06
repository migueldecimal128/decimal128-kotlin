// SPDX-License-Identifier: MIT

/**
 * Serialization helpers for IEEE-754 Decimal128 values using the
 * **Binary Integer Decimal2 (BID)** encoding format.
 *
 * This object provides low-level routines for converting a [`Decimal2`]
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

@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.decimalFNZ
import kotlin.math.min

/**
 * Encodes `d` into its 128-bit **BID** representation and writes the two
 * 64-bit words into `longs` starting at `offset`.
 *
 * @param d the decimal value to encode
 * @param longs destination array for the two 64-bit words
 * @param offset index of the first word to write
 * @param isLittleEndian if true, writes [lo, hi]; otherwise writes [hi, lo]
 */
fun bid128Encode(d: Decimal, out: Pentad) {
    out.dw1 = encodeBid128Hi(d)
    out.dw0 = d.dw0
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
private inline fun encodeSignAndGCombinationFieldBid128(d: Decimal): Long {
    // this is 4 digits for DPD format, but only the most significant 3-bits of BID
    val mostSigBits3 = (d.dw1 shr (110 - 64)).toInt() and 0x07
    val steal = d.steal
    val signBitLong = stealSignBit(steal).toLong() shl 63
    val gCombinationField = when {
        stealIsFIN(steal) -> {
            val qExp = stealQExp(steal)
            verify { qExp in QTINY_Neg6176..QMAX_6111 }
            val biasedQExp = qExp - QTINY_Neg6176 // remember qTiny is negative
            verify { (biasedQExp and 0x3000) != 0x3000 }
            (biasedQExp shl 3) or mostSigBits3
        }

        stealIsINF(steal) -> 0b11110 shl 12
        stealIsQNAN(steal) -> 0b111110 shl 11
        else -> {
            verify { stealIsSNAN(steal) }
            0b111111 shl 11
        }
    }
    val signCombo = signBitLong or (gCombinationField.toLong() shl 46)
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
private fun encodeBid128Hi(d: Decimal): Long {
    // Don't be confused by the fact that this is 3 bits
    // but below you will see 4 bits.
    // The format allows 4 bits, but only 3 are used.
    val signCombo = encodeSignAndGCombinationFieldBid128(d)
    val significand110Hi = d.dw1 and ((1L shl (110 - 64)) - 1L)
    val bidDecimal128Hi = signCombo or significand110Hi
    return bidDecimal128Hi
}

/**
 * Decodes a Decimal128 value in **BID** (Binary Integer Decimal) format
 * from its two 64-bit words, per IEEE 754-2019 section 3.5.2.
 *
 * The 128-bit encoding consists of:
 *  - 1 sign bit
 *  - 17-bit G-combination field (encodes exponent and high coefficient bits)
 *  - 110-bit trailing significand field T (46 high bits in `bid128Hi`, 64 bits in `bid128Lo`)
 *
 * The combination field is decoded as follows:
 *  - G[0:1] = `00`, `01`, or `10`: exponent from G[0:9], significand MSBs from G[10:12]
 *  - G[0:3] = `1100`–`1110`: exponent from G[2:11], significand MSBs = `1000` + G[12]
 *  - G[0:4] = `11110`: infinity
 *  - G[0:4] = `11111`: NaN (G[5] determines signaling vs quiet)
 *
 * Special value handling:
 *  - **Infinity**: all lower bits are ignored (or preserved if [allowNonCanonical])
 *  - **NaN**: lower 110 bits form the payload; G[1:4] are ignored
 *  - **Finite**: coefficient is validated against the 34-digit maximum (10^34 - 1);
 *    non-canonical coefficients (value > 10^34 - 1) are treated as zero
 *  - **NaN payload**: validated against the 33-digit maximum (10^33 - 1);
 *    non-canonical payloads are set to zero
 *
 * When [ctx] has [DecPrefs.decodeBidAllowNonCanonical] set to `true`, non-canonical
 * encodings are preserved rather than normalized to zero.
 *
 * @param bid128Hi the high 64 bits of the BID128 encoding
 * @param bid128Lo the low 64 bits of the BID128 encoding
 * @param ctx the decimal context; controls non-canonical encoding behavior via [DecPrefs]
 * @return the decoded [Decimal] value
 */
fun bid128Decode(bid128Hi: Long, bid128Lo: Long): Decimal {
    // IEEE754-2019 Table 3.6-Decimal2 Interchange format parameters -- p 23
    val k = 128 // storage width in bits
    val p = 34 // precision in digits
    val emax = 6144
    val bias = 6176
    val w5 = 17 // w+5, combination field width in bits
    val t = 110 // trailing significand field width in bits
    verify { 1 + w5 + t == k }

    // 34 nines
    val coeffMaxHi = 0x0001ED09BEAD87C0L
    val coeffMaxLo = 0x378D8E63FFFFFFFFL
    // 33 nines
    val payloadMaxHi = 0x0000314DC6448D93L
    val payloadMaxLo = 0x38C15B09FFFFFFFFL

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
            return Decimal.infinity(sign)
        }
        // if the top 5 bits are 0x11111 then NaN
        (combination shr (w5 - 5)) == 0b11111 -> {
            // with the next bit determining signaling NaN
            val payloadHi = coeffTHi
            val payloadLo = bid128Lo
            val isSignaling = ((combination shr (w5 - 6)) and 1) != 0
            return if (isSignaling) Decimal.sNaN(sign, payloadHi, payloadLo)
            else Decimal.qNaN(sign, payloadHi, payloadLo)
        }

        else -> {
            // all possible cases were covered above
            throw IllegalStateException()
        }
    }
    val dw1 = (coeffHi shl (t - 64)) or coeffTHi
    val dw0 = bid128Lo
    // IEEE754-2019 3.5.2 p21
    //  If the value exceeds the maximum, the significand c is
    //  non-canonical and the value used for c is zero.
    if ((dw1 or dw0) == 0L || dw1 > coeffMaxHi || dw1 == coeffMaxHi && dw0 > coeffMaxLo)
        return Decimal.zero(sign, qExp)
    return decimalFNZ(sign, qExp, dw1, dw0)
}

/*
// deferring support for smaller formats until someone asks for it.
// This code for decoding bid64 and bid32 has *not* been tested.
// there is at least one problem with payload.digitLen
//
fun bid64Decode(bid64: Long, allowNonCanonical: Boolean = false): Decimal {
    // IEEE754-2019 Table 3.6-Decimal2 Interchange format parameters -- p 23
    val k = 64 // storage width in bits
    val p = 16 // precision in digits
    val emax = 384
    val bias = 398
    val w5 = 13 // w+5, combination field width in bits
    val t = 50 // trailing significand field width in bits
    verify { 1 + w5 + t == k }
    val coeffMax: Long
    val payloadMax: Long
    val infPayload: Long
    if (allowNonCanonical) {
        coeffMax = (0b1001L shl t) + ((1L shl t) - 1L)
        payloadMax = (0b1001L shl t) + ((1L shl t) - 1L)
        infPayload = (bid64 shl 6) shr 6
    } else {
        coeffMax = 9999_9999_9999_9999L
        payloadMax = 999_999_999_999_999L
        infPayload = 0L
    }
    // 1 + 13 + 50 == 64
    // 1 bit for the sign
    val sign = bid64 < 0L
    // w5 bit combination field ... for bid64 0x1FFF
    val combination = ((bid64 and 0x7FFF_FFFF_FFFF_FFFFL) shr t).toInt()
    // t bits for the coefficient
    val coeffT = bid64 and ((1L shl t) - 1L)

    return decodeBidHelper(w5, bias, t, sign, combination, coeffT, coeffMax, payloadMax, infPayload)
}

fun bid32Decode(bid32: Int, allowNonCanonical: Boolean = false): Decimal {
    // IEEE754-2019 Table 3.6-Decimal2 Interchange format parameters -- p 23
    val k = 32 // storage width in bits
    val p = 7 // precision in digits
    val emax = 96
    val bias = 101
    val w5 = 11 // w+5, combination field width in bits
    val t = 20 // trailing significand field width in bits
    verify { 1 + w5 + t == k }
    val coeffMax: Long
    val payloadMax: Long
    val infPayload: Long
    if (allowNonCanonical) {
        coeffMax = (0b1001L shl t) + ((1L shl t) - 1L)
        payloadMax = (0b1001L shl t) + ((1L shl t) - 1L)
        infPayload = ((bid32 shl 6) shr 6).toLong()
    } else {
        coeffMax = 9_999_999L
        payloadMax = 999_999L
        infPayload = 0L
    }
    // 1 + 11 + 20 == 32
    // 1 bit for the sign
    val sign = bid32 < 0
    // w5 bit combination field ... for bid64 0x1FFF
    val combination = (bid32 and 0x7FFF_FFFF) shr t
    // t bits for the coefficient
    val coeffT = (bid32 and ((1 shl t) - 1)).toLong()

    return decodeBidHelper(w5, bias, t, sign, combination, coeffT, coeffMax, payloadMax, infPayload)
}

private fun decodeBidHelper(
    w5: Int, bias: Int, t: Int,
    sign: Boolean, combination: Int, coeffT: Long,
    coeffMax: Long, payloadMax: Long, infPayload: Long
): Decimal {
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
        (combination shr (w5 - 5)) == 0b11110 ->
            return Decimal.infinityNonCanonical(sign, 0L, infPayload)
        // if the top 5 bits are 0x11111 then NaN
        (combination shr (w5 - 5)) == 0b11111 -> {
            // with the next bit determining signaling NaN
            val payloadDw0 = min(coeffT, payloadMax)
            val isSignaling = ((combination shr (w5 - 6)) and 1) != 0
            return if (isSignaling) Decimal.sNaN(sign, 0L, payloadDw0) else Decimal.qNaN(sign, 0L, payloadDw0)
        }

        else -> {
            // all possible cases were covered above
            throw IllegalStateException()
        }
    }
    val dw0 = (coeffHi shl t) or coeffT
    // IEEE754-2019 3.5.2 p21
    //  If the value exceeds the maximum, the significand c is
    //  non-canonical and the value used for c is zero.
    if (dw0 > coeffMax)
        return Decimal.zero(sign, qExp)
    return decimalFNZ(sign, qExp, 0L, dw0)
}

 */

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
internal fun bid128ParseIntelHex(pentad: Pentad, str: String) {
    if (str.length !in 34..35 ||
        str[0] != '[' || str[str.lastIndex] != ']' ||
        str.length == 35 && str[17] != ','
    ) {
        pentad.w = 0
        return
    }
    parseHexDword(pentad, str, 1)
    val isValidHiBit = pentad.w
    val bid128Hi = pentad.dw0
    parseHexDword(pentad, str, (str.length + 1) shr 1)
    pentad.w = pentad.w and isValidHiBit
    pentad.dw1 = bid128Hi
}

private fun parseHexDword(pentad: Pentad, str: String, off: Int) {
    var dw = 0L
    for (i in 0..15) {
        val ch = str[off + i]
        dw = when (ch) {
            in '0'..'9' -> (dw shl 4) or (ch - '0').toLong()
            in 'A'..'F' -> (dw shl 4) or (ch - 'A' + 10).toLong()
            in 'a'..'f' -> (dw shl 4) or (ch - 'a' + 10).toLong()
            else -> {
                pentad.w = 0
                return
            }
        }
    }
    pentad.w = 1
    pentad.dw0 = dw
}
