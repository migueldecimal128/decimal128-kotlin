// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.decimalFinite

private const val TEN_POW_15 = 1_000_000_000_000_000L
private const val TEN_POW_18 = 1_000_000_000_000_000_000L
private const val EIGHTEEN_NINES = TEN_POW_18 - 1L

/**
 * Decodes a 128-bit decimal value from DPD (Densely Packed Decimal) format.
 *
 * The 128-bit value is split into two 64-bit words, with [dpdHi] holding the
 * most significant bits (including the sign, combination field, and upper
 * significand) and [dpdLo] holding the lower 64 bits of the significand.
 *
 * The combination field is decoded as follows:
 * - G0-G1 != 11: Case A, leading significand digit 0-7
 * - G0-G3 != 1111: Case B, leading significand digit 8-9
 * - G0-G4 == 11110: Infinity
 * - G0-G4 == 11111: NaN (G5 indicates signaling)
 *
 * The trailing significand bits are decoded from 11 DPD declets into a
 * binary integer, which is then combined with the leading digit to form
 * the full coefficient.
 *
 * @param dpdHi The most significant 64 bits of the DPD-encoded value.
 * @param dpdLo The least significant 64 bits of the DPD-encoded value.
 * @return The decoded [Decimal] value.
 */
internal fun dpd128Decode(dpdHi: Long, dpdLo: Long): Decimal {
    val k = 128
    val bias = 6176
    val w = 12   // Exponent continuation width for decimal128
    val w5 = 17  // Total combination field width
    val t = 110  // Trailing field width

    val sign = dpdHi < 0L
    val combination = (dpdHi shr (t - 64)).toInt() and 0x1FFFF

    // In DPD, the lower w bits of the exponent are always at the bottom of G
    val expContinuation = combination and 0x0FFF // bits G5...G16

    val d0: Long
    val qExp: Int
    var isNaN = false

    when {
        // Case A: G0G1 is 00, 01, or 10 (Small leading digit 0-7)
        combination shr (w5 - 2) != 0b11 -> {
            // Exponent MSBs are G0, G1
            val expMSB = combination shr (w5 - 2)
            val biasedExponent = (expMSB shl w) or expContinuation
            qExp = biasedExponent - bias

            // Leading digit d0 is G2, G3, G4
            d0 = ((combination shr w) and 0b111).toLong()
        }

        // Case B: G0G1 is 11 and G2G3 is 00, 01, or 10 (Large leading digit 8-9)
        combination shr (w5 - 4) != 0b1111 -> {
            // Exponent MSBs are G2, G3
            val expMSB = (combination shr (w5 - 4)) and 0b11
            val biasedExponent = (expMSB shl w) or expContinuation
            qExp = biasedExponent - bias

            // Leading digit d0 is 8 + G4
            d0 = 8L + ((combination shr w) and 0b1).toLong()
        }

        // Infinity: G0...G4 is 11110
        (combination shr (w5 - 5)) == 0b11110 -> {
            return Decimal.infinity(sign)
        }

        // NaN: G0...G4 is 11111
        (combination shr (w5 - 5)) == 0b11111 -> {
            isNaN = true
            d0 = 0L
            qExp = 0
            /*
            val isSignaling = ((combination shr (w5 - 6)) and 1) != 0
            // DPD payloads are also encoded as declets in T
            val payloadHi = dpdHi and ((1L shl (t - 64)) - 1L)
            val payloadLo = dpdLo
            return if (isSignaling) Decimal.sNaN(sign, payloadHi, payloadLo)
            else Decimal.qNaN(sign, payloadHi, payloadLo)

             */
        }

        else -> throw IllegalStateException()
    }

    // Reconstruction phase:
    // DPD differs here because you must decode declets into a binary integer
    val decletsHi6 = ((d0 shl 50) or ((dpdHi and 0x3FFFFFFFFFFFL) shl 4) or (dpdLo ushr 60))
    val decletsLo6 = dpdLo and 0x0_FFFFF_FFFFF_FFFFFL // 60 bits
    val binHi = binFromDecletsUpTo7(decletsHi6)
    val binLo = binFromDecletsUpTo7(decletsLo6)
    val coeffHi: Long
    val coeffLo: Long
    if (binHi == 0L) {
        coeffLo = binLo
        coeffHi = 0L
    } else {
        coeffLo = (binHi * TEN_POW_18) + binLo
        val carry = if (unsignedLT(coeffLo, binLo)) 1 else 0
        coeffHi = unsignedMulHi(binHi, TEN_POW_18) + carry
    }
    if (isNaN) {
        val isSignaling = ((combination shr (w5 - 6)) and 1) != 0
        if (isSignaling)
            return Decimal.sNaN(sign, coeffHi, coeffLo)
        else
            return Decimal.qNaN(sign, coeffHi, coeffLo)
    }
    return decimalFinite(sign, qExp, coeffHi, coeffLo)
}

/**
 * Encodes a [Decimal] value into DPD (Densely Packed Decimal) 128-bit format.
 *
 * The coefficient is split into three parts:
 * - The most significant digit (0-9), encoded directly into the combination field
 * - The next 15 digits, encoded as 5 DPD declets into the high significand bits
 * - The least significant 18 digits, encoded as 6 DPD declets into the low significand bits
 *
 * The result is written to [out] as two 64-bit words:
 * - [Pentad.dw1]: sign bit, 17-bit combination field (G), and upper 46 bits of T
 * - [Pentad.dw0]: lower 64 bits of T
 *
 * @param d The decimal value to encode. Must have at most 34 significant digits.
 * @param out The destination for the encoded 128-bit value.
 */
internal fun dpd128Encode(d: Decimal, out: Pentad) {
    val steal = d.steal
    verify { stealDigitLen(steal) <= 34 }
    var mostSigBcd4 = 0
    var declets5Hi = 0L
    var binLo = d.dw0
    if (stealDigitLen(steal) > 18) {
        val tmps = DecContext.current().tmps
        val q = tmps.c256.c256Set128(d.dw1, d.dw0)
        val r = c256SetDivRemX64(q, q, TEN_POW_18, tmps.knuthD)
        binLo = r
        var binHi = q.dw0
        if (q.digitLen > 15) {
            val t = q.dw0 / TEN_POW_15
            binHi = q.dw0 - (t * TEN_POW_15)
            mostSigBcd4 = t.toInt()
            verify { mostSigBcd4 in 0..9 }
        }
        verify { binHi in 0L..<TEN_POW_15 }
        declets5Hi = declets6FromBin(binHi)
    }
    val declets6Lo = declets6FromBin(binLo)
    val signCombo = encodeSignAndGCombinationFieldDpd128(steal, mostSigBcd4)
    val T_hi_bits = 46 // bits of T residing in the Hi Long
    val dpdLo = (declets5Hi shl 60) or declets6Lo
    val dpdHi = (signCombo.toLong() shl T_hi_bits) or (declets5Hi ushr 4)
    out.dw1 = dpdHi
    out.dw0 = dpdLo
}

/**
 * Encodes the sign bit and 17-bit G (combination) field for a DPD-128 value.
 *
 * The combination field encodes the sign, exponent MSBs, and most significant
 * digit according to the IEEE 754-2008 DPD rules:
 * - Case A (digit 0-7): `0ee ddd` or `10e eee` — exponent MSBs in G0-G1, digit in G2-G4
 * - Case B (digit 8-9): `110e ed` or `111e ed` — exponent MSBs in G2-G3, digit low bit in G4
 * - Infinity: G0-G4 = `11110`
 * - NaN: G0-G4 = `11111`, G5 = signaling flag
 *
 * @param steal The packed metadata word of the decimal value.
 * @param mostSigBcd4 The most significant decimal digit (0-9). Ignored for Infinity and NaN.
 * @return An 18-bit value: bit 17 is the sign, bits 16-0 are the G field.
 */
private fun encodeSignAndGCombinationFieldDpd128(steal: Int, mostSigBcd4: Int): Int {
    val sBit = stealSignBit(steal)
    val gField: Int

    // 1. Convert to Biased Exponent
    val biasedQExp = stealQExp(steal) + 6176

    when (stealTyp(steal)) {
        STEAL_TYP_ZER,
        STEAL_TYP_FNZ -> {
            verify { mostSigBcd4 in 0..9 }
            // Split 14-bit biasedExp into 2 MSB and 12 LSB
            val expMSB = (biasedQExp shr 12) and 0x3
            val expLSB = biasedQExp and 0x0FFF

            val gPrefix = if (mostSigBcd4 <= 7) {
                // Case A: 0xxxx or 10xxx
                (expMSB shl 3) or (mostSigBcd4 and 0x7)
            } else {
                // Case B: 110xx or 1110x
                0b11000 or (expMSB shl 1) or (mostSigBcd4 and 0x1)
            }
            // Result is G prefix (5 bits) + G LSBs (12 bits)
            gField = (gPrefix shl 12) or expLSB
        }

        STEAL_TYP_INF -> {
            // Infinity: G0-G4 = 11110, remaining G bits are 0
            gField = 0b11110 shl 12
        }

        STEAL_TYP_NAN -> {
            // NaN: G0-G4 = 11111, G5 = isSignaling
            val isSignaling = if (stealIsSNAN(steal)) 1 else 0
            gField = (0b11111 shl 12) or (isSignaling shl 11)
        }

        else -> throw IllegalStateException()
    }
    return (sBit shl 17) or gField
}

/**
 * Decodes up to 7 DPD declets packed into a [Long] into a binary integer.
 *
 * Declets are packed 10 bits each from least significant to most significant.
 * The topmost declet may use fewer than 10 bits (at most 4), corresponding to
 * the leading digit of a 34-digit decimal128 coefficient (0-9).
 *
 * @param declets7 Up to 7 packed DPD declets, 10 bits each, least significant first.
 * @return The decoded binary value.
 */
private fun binFromDecletsUpTo7(declets7: Long): Long {
    var bin = 0L
    var decletsT = declets7
    var multiplier = 1L
    while (decletsT != 0L) {
        val declet = decletsT and 0x3FFL
        val bin1 = dpdDecodeDeclet(declet)
        bin += bin1 * multiplier
        multiplier *= 1000L
        decletsT = decletsT ushr 10
    }
    return bin
}

/**
 * Encodes a binary integer into 6 DPD (Densely Packed Decimal) declets.
 *
 * Repeatedly extracts the least significant 3 decimal digits via multiplication
 * by the magic constant reciprocal of 1000, encodes them as a 10-bit DPD declet,
 * and packs them from least significant to most significant, 10 bits each.
 *
 * @param bin The binary value to encode. Must be in the range `0..999_999_999_999_999_999`
 * (18 nines, i.e. at most 18 decimal digits).
 * @return The 6 packed DPD declets, 10 bits each, least significant first, in a 60-bit value.
 */
private fun declets6FromBin(bin: Long): Long {
    require(bin in 0L..EIGHTEEN_NINES)
    var t = bin
    var declets6 = 0L
    var shift = 0
    while (t != 0L) {
        val q = unsignedMulHi(t, 0x020C49BA5E353F7DL) ushr 3
        val r = t - (q * 1000L)
        val declet = dpdEncodeDeclet(r)
        declets6 = declets6 or (declet shl shift)
        shift += 10
        t = q
    }
    return declets6
}

/**
 * Decode a 10-bit IEEE-754 DPD declet (bits 9..0) into [0,999], using pure booleans.
 * Mapping: bit9..bit0 → p q r s t u v w x y
 * Equations: Cowlishaw's dpd2bcd (handles canonical + non-canonical).
 */
internal fun dpdDecodeDeclet(declet: Long): Long { // tagged internal only for testing
    require(declet in 0..1023) { "declet must be 10 bits" }

    if (declet == 0L)
        return 0L

    // Unpack to bits p..y
    val p = (declet ushr 9) and 1
    val q = (declet ushr 8) and 1
    val r = (declet ushr 7) and 1
    val s = (declet ushr 6) and 1
    val t = (declet ushr 5) and 1
    val u = (declet ushr 4) and 1
    val v = (declet ushr 3) and 1
    val w = (declet ushr 2) and 1
    val x = (declet ushr 1) and 1
    val y = declet and 1

    val ns = s xor 1
    val nx = x xor 1
    val nv = v xor 1
    val nw = w xor 1
    val nt = t xor 1

    val a = (v and w) and (ns or t or nx)
    val b = p and (nv or nw or (s and t.inv() and x))
    val c = q and (nv or nw or (s and t.inv() and x))
    val d = r

    val e = v and ((nw and x) or (t.inv() and x) or (s and x))
    val f = (s and (nv or nx)) or (p and ns and t and v and w and x)
    val g = (t and (nv or nx)) or (q and ns and t and w)
    val h = u

    val i = v and ((nw and nx) or (w and x and (s or t)))
    val j = ((nv and w)
            or (s and v and nw and x)
            or (p and w and (nx or (ns and t.inv()))))
    val k = (nv and x) or (t and nw and x) or (q and v and w and (nx or (ns and nt)))
    val m = y

    // Pack BCD digit bits → three digits
    val d2 = (a shl 3) or (b shl 2) or (c shl 1) or d
    val d1 = (e shl 3) or (f shl 2) or (g shl 1) or h
    val d0 = (i shl 3) or (j shl 2) or (k shl 1) or m

    require(d2 in 0..9) { "d2 out of range: $d2" }
    require(d1 in 0..9) { "d1 out of range: $d1" }
    require(d0 in 0..9) { "d0 out of range: $d0" }

    return d2 * 100 + d1 * 10 + d0
}

internal fun dpdEncodeDeclet(n: Long): Long {  // tagged internal only for testing
    require(n in 0..999) { "n must be in 0..999" }

    if (n == 0L)
        return 0L

    // reciprocal-multiply digit extraction (optimized for n <= 999)
    val q10 = (n * 205) ushr 11      // n / 10
    val q100 = (n * 41) ushr 12      // n / 100

    val d2 = q100
    val d1 = q10 - (q100 * 10)
    val d0 = n - (q10 * 10)

    // BCD bits (MSB→LSB: 8,4,2,1)
    val a = (d2 shr 3) and 1
    val b = (d2 shr 2) and 1
    val c = (d2 shr 1) and 1
    val d = d2 and 1

    val e = (d1 shr 3) and 1
    val f = (d1 shr 2) and 1
    val g = (d1 shr 1) and 1
    val h = d1 and 1

    val i = (d0 shr 3) and 1
    val j = (d0 shr 2) and 1
    val k = (d0 shr 1) and 1
    val m = d0 and 1

    val na = a xor 1
    val ne = e xor 1
    val ni = i xor 1

    val p = b or ((a and j) or ((a and f) and i))
    val q = c or ((a and k) or ((a and g) and i))
    val r = d
    val s = (f and (na or ni)) or ((na and (e and j)) or (e and i))
    val t = g or ((na and (e and k)) or (a and i))
    val u = h
    val v = a or (e or i)
    val w = a or ((e and i) or (ne and j))
    val x = e or ((a and i) or (na and k))
    val y = m

    return (p shl 9) or (q shl 8) or (r shl 7) or (s shl 6) or
            (t shl 5) or (u shl 4) or (v shl 3) or (w shl 2) or
            (x shl 1) or y
}

