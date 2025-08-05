package com.decimal128

import kotlin.math.min

object MagnitudeDiv {

    fun magDiv(z: Decimal, x: Decimal, y: Decimal, ctx: DecimalContext): Residue {
        if (!x.u256IsZero()) {
            val numeratorScale = ctx.precision + 1 - (x.digitLen - y.digitLen)
            val yBitLen = y.bitLen
            val y0 = y.dw0
            val scaledNumerator = if (z === y && yBitLen > 64) U256() else z
            scaledNumerator.u256SetScaleUpPow10(x, numeratorScale)
            val residue = when {
                (y.bitLen <= 64) -> z.u256SetDivX64(scaledNumerator, y.dw0)
                else -> z.u256SetDiv(scaledNumerator, y)
            }
            val qPreferred = x.qExp - y.qExp
            var qZ = x.qExp - y.qExp - numeratorScale
            var ntz = z.dw0.countTrailingZeroBits()
            if (residue == Residue.EXACT && qZ < qPreferred && ntz > 0) {
                if (qZ + 1 < qPreferred) {
                    val quot = U256()
                    do {
                        val deltaQ = qPreferred - qZ
                        val chunk = min(min(9, deltaQ), ntz)
                        val chunkRemainder = DivBarrett.barrettDivModPow10(quot, z, chunk)
                        if (chunkRemainder > 0) {
                            var pow10Count = 0
                            var t = chunkRemainder
                            val M = 0xCCCCCCCCCCCCCCCDuL.toLong()
                            while (true) {
                                // val q = t / 10
                                // val r = t % 10
                                val q = umulHigh(t, M) ushr 3
                                val r = t - (q * 10)
                                if (r != 0L)
                                    break
                                ++pow10Count
                                t = q
                            }
                            if (pow10Count > 0) {
                                z.u256SetScaleDownPow10(z, pow10Count)
                                qZ += pow10Count
                            }
                            break
                        } else {
                            z.u256Set(quot)
                            ntz -= chunk
                            qZ += chunk
                        }
                    } while (qZ < qPreferred && ntz > 0)
                } else if (z.u256IsMultipleOf10()) {
                    z.u256SetScaleDownPow10(z, 1)
                    ++qZ
                }
            }
            z.qExp = qZ
            return residue
        }
        // x is zero
        val qPreferred = x.qExp - y.qExp
        z.u256SetZero()
        z.qExp = qPreferred
        return Residue.EXACT
    }

}