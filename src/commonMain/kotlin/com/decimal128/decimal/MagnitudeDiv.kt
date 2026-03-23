package com.decimal128.decimal

import kotlin.math.min

object MagnitudeDiv {

    fun magDivFnzFnz_deprecated(z: MutDec, sign: Boolean, x: MutDec, y: MutDec, ctx: DecContext): Residue {
        val tmps = ctx.tmps
        val numeratorScale = ctx.precision + 1 - (x.digitLen - y.digitLen)
        val scaledNumerator = tmps.mdecArg1
        val pentad = tmps.pentad1
        c256SetScaleUpPow10(scaledNumerator, x, numeratorScale, pentad)
        val residue = when {
            (y.bitLen <= 64) -> c256SetDivX64(z, scaledNumerator, y.dw0, tmps.knuthD)
            else -> c256SetDiv(z, scaledNumerator, y, tmps)
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
                    val chunkRemainder = barrettDivModPow10(quot, z, chunk)
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
                            c256SetScaleDownPow10(z, z, pow10Count, pentad)
                            qZ += pow10Count
                        }
                        break
                    } else {
                        z.c256Set(quot)
                        ntz -= chunk
                        qZ += chunk
                    }
                } while (qZ < qPreferred && ntz > 0)
            } else if (c256IsMultipleOf10(z)) {
                c256SetScaleDownPow10(z, z, 1, pentad)
                ++qZ
            }
        }
        z.type = STEAL_TYP_FNZ
        z.qExp = qZ
        z.sign = sign
        return residue
    }

}

fun mutDecDivFnzFnz(z: MutDec, sign: Boolean, x: MutDec, y: MutDec, ctx: DecContext): MutDec {
    val tmps = ctx.tmps
    val numeratorScale = ctx.precision + 1 - (x.digitLen - y.digitLen)
    val scaledNumerator = tmps.mdecArg1
    val pentad = tmps.pentad1
    c256SetScaleUpPow10(scaledNumerator, x, numeratorScale, pentad)
    val residue = when {
        (y.bitLen <= 64) -> c256SetDivX64(z, scaledNumerator, y.dw0, tmps.knuthD)
        else -> c256SetDiv(z, scaledNumerator, y, tmps)
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
                val chunkRemainder = barrettDivModPow10(quot, z, chunk)
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
                        c256SetScaleDownPow10(z, z, pow10Count, pentad)
                        qZ += pow10Count
                    }
                    break
                } else {
                    z.c256Set(quot)
                    ntz -= chunk
                    qZ += chunk
                }
            } while (qZ < qPreferred && ntz > 0)
        } else if (c256IsMultipleOf10(z)) {
            c256SetScaleDownPow10(z, z, 1, pentad)
            ++qZ
        }
    }
    return z.roundAndFinalizeFnz(sign, qZ, residue, ctx)
}
