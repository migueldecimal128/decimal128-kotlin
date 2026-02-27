package com.decimal128.decimal

import kotlin.math.min

object MagnitudeDiv {

    fun magDivFnzFnz(z: MutDec, sign: Boolean, x: MutDec, y: MutDec, ctx: DecContext): Residue {
        val numeratorScale = ctx.precision + 1 - (x.digitLen - y.digitLen)
        val scaledNumerator = ctx.decTmps.mdecArg1
        scaledNumerator.c256SetScaleUpPow10(x, numeratorScale)
        val residue = when {
            (y.bitLen <= 64) -> c256SetDivX64(z, scaledNumerator, y.dw0)
            else -> z.c256SetDiv(scaledNumerator, y)
        }
        val qPreferred = x.qExp - y.qExp
        var qZ = x.qExp - y.qExp - numeratorScale
        var ntz = z.dw0.countTrailingZeroBits()
        if (residue == Residue.EXACT && qZ < qPreferred && ntz > 0) {
            if (qZ + 1 < qPreferred) {
                val quot = C256()
                do {
                    val deltaQ = qPreferred - qZ
                    val chunk = min(min(9, deltaQ), ntz)
                    val chunkRemainder = DivBarrett.barrettDivModPow10(quot, z, chunk)
                    // FIXME -- the stripTrailingZeros code uses a faster way to
                    //  countTrailingZeroDigits
                    if (chunkRemainder > 0) {
                        var pow10Count = 0
                        var t = chunkRemainder
                        val M = 0xCCCCCCCCCCCCCCCDuL.toLong()
                        val S = 3
                        while (true) {
                            // val q = t / 10
                            // val r = t % 10
                            val q = unsignedMulHi(t, M) ushr S
                            val r = t - (q * 10)
                            if (r != 0L)
                                break
                            ++pow10Count
                            t = q
                        }
                        if (pow10Count > 0) {
                            z.c256SetScaleDownPow10(z, pow10Count)
                            qZ += pow10Count
                        }
                        break
                    } else {
                        z.c256Set(quot)
                        ntz -= chunk
                        qZ += chunk
                    }
                } while (qZ < qPreferred && ntz > 0)
            } else if (z.c256IsMultipleOf10()) {
                z.c256SetScaleDownPow10(z, 1)
                ++qZ
            }
        }
        z.qExp = qZ
        z.sign = sign
        return residue
    }

}