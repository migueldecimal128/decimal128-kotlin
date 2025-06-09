package com.decimal128

import java.lang.Long.numberOfTrailingZeros
import java.lang.Math.unsignedMultiplyHigh

object MagDiv {

    fun magDiv(z: Mag, x: Mag, y: Mag): Residue {
        if (!x.coeffIsZero()) {
            val numeratorScale = 34 + 1 - (x.digitLen - y.digitLen)
            val yBitLen = y.bitLen
            val y0 = y.dw0
            val scaledNumerator = if (z === y && yBitLen > 64) Coeff() else z
            scaledNumerator.coeffSetScaleUpPow10(x, numeratorScale)
            val residue = when {
                (y.bitLen <= 64) -> z.coeffSetDivx64(scaledNumerator, y.dw0)
                else -> z.coeffSetDiv(scaledNumerator, y)
            }
            val qPreferred = x.qExp - y.qExp
            var qZ = x.qExp - y.qExp - numeratorScale
            var ntz = numberOfTrailingZeros(z.dw0)
            if (residue == Residue.EXACT && qZ < qPreferred && ntz > 0) {
                if (qZ + 1 < qPreferred) {
                    val quot = Coeff()
                    do {
                        val deltaQ = qPreferred - qZ
                        val chunk = Math.min(Math.min(9, deltaQ), ntz)
                        val chunkRemainder = DivBarrett.barrettDivModPow10(quot, z, chunk)
                        if (chunkRemainder > 0) {
                            var pow10Count = 0
                            var t = chunkRemainder
                            val M = 0xCCCCCCCCCCCCCCCDuL.toLong()
                            while (true) {
                                // val q = t / 10
                                // val r = t % 10
                                val q = unsignedMultiplyHigh(t, M) ushr 3
                                val r = t - (q * 10)
                                if (r != 0L)
                                    break
                                ++pow10Count
                                t = q
                            }
                            if (pow10Count > 0) {
                                z.coeffSetScaleDownPow10(z, pow10Count)
                                qZ += pow10Count
                            }
                            break
                        } else {
                            z.coeffSet(quot)
                            ntz -= chunk
                            qZ += chunk
                        }
                    } while (qZ < qPreferred && ntz > 0)
                } else if (z.coeffIsMultipleOf10()) {
                    z.coeffSetScaleDownPow10(z, 1)
                    ++qZ
                }
            }
            z.qExp = qZ
            return residue
        }
        // x is zero
        val qPreferred = x.qExp - y.qExp
        z.coeffSetZero()
        z.qExp = qPreferred
        return Residue.EXACT
    }

    fun magDivx64(z: Mag, x: Mag, yDigitLen: Int, qY: Int, y0: Long): Residue {
        val numeratorScale = 34 - (x.digitLen - yDigitLen)
        z.coeffSetScaleUpPow10(x, numeratorScale)
        val residue = z.coeffSetDivx64(z, y0)
        val qPreferred = x.qExp - qY
        var qZ = x.qExp - qY - numeratorScale
        if (residue == Residue.EXACT) {
            while (qZ < qPreferred && z.coeffIsMultipleOf10()) {
                z.coeffSetScaleDownPow10(z, 1)
                ++qZ
            }
        }
        z.qExp = qZ
        return residue
    }
}