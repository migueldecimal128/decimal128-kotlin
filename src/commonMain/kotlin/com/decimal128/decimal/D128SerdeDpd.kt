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
