// SPDX-License-Identifier: MIT
@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

import com.decimal128.decimal.Decimal.Companion.decimalFinite

private const val TEN_POW_15 = 1_000_000_000_000_000L
private const val TEN_POW_18 = 1_000_000_000_000_000_000L
private const val EIGHTEEN_NINES = TEN_POW_18 - 1L

internal object D128SerdeDpd {

    fun decodeDpd128(dpdHi: Long, dpdLo: Long): Decimal {
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
        val binHi = binFromDeclets7(decletsHi6)
        val binLo = binFromDeclets7(decletsLo6)
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

    fun encodeDpd128(d: MutDec, out: Pentad) {
        verify { d.digitLen <= 34 }
        var mostSigBcd4 = 0
        var declets5Hi = 0L
        var binLo = d.dw0
        if (d.digitLen > 18) {
            val tmps = DecContext.current().tmps
            val q = tmps.c256
            val r = c256SetDivRemX64(q, d, TEN_POW_18, tmps.knuthD)
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
        val steal = d.steal
        val signCombo = encodeSignAndGCombinationFieldDpd128(steal, mostSigBcd4)
        val T_hi_bits = 46 // bits of T residing in the Hi Long
        val dpdLo = (declets5Hi shl 60) or declets6Lo
        val dpdHi = (signCombo.toLong() shl T_hi_bits) or (declets5Hi ushr 4)
        out.dw1 = dpdHi
        out.dw0 = dpdLo
    }

}

private fun encodeSignAndGCombinationFieldDpd128(
    steal: Int,
    mostSigBcd4: Int
): Int {
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

// there could be up to 7 declets with the top declet having
// only 4 bits instead of 10
private fun binFromDeclets7(declets7: Long): Long {
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
