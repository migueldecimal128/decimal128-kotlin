@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal object C128ScalePow10 {

    fun c128ScaleUpPow10(x: Decimal, pow10: Int, signExp: Short): Decimal {
        // FIXME - UNTESTED!!
        verify { pow10 > 0 }
        val pow10BitLen = pow10BitLen(pow10)
        val (pow10dw1, pow10dw0) = pow10_128(pow10)
        val (p1, p0) = umul128x128to128(x.dw1, x.dw0, pow10dw1, pow10dw0)
        return Decimal.from(p1, p0, signExp)
    }

    fun c128ScaleUpPow10(sign: Boolean, dw1: Long, dw0: Long, qExp: Int, pow10: Int, ctx: DecContext): Decimal {
        verify { pow10 > 0 }
        val (pow10dw1, pow10dw0) = pow10_128(pow10)
        val (p1, p0) = umul128x128to128(dw1, dw0, pow10dw1, pow10dw0)
        return Decimal.from(sign, p1, p0, qExp - pow10)
    }

    fun c128ScaleUpPow10(dw1: Long, dw0: Long, pow10: Int): Pair<Long, Long> {
        verify { pow10 > 0 }
        val (pow10dw1, pow10dw0) = pow10_128(pow10)
        return umul128x128to128(dw1, dw0, pow10dw1, pow10dw0)
    }

    fun c128ScaleDownPow10(result: DwPair, dw1: Long, dw0: Long, pow10: Int): Residue {
        if (dw1 == 0L) {
            verify { pow10 < MIN_POW10_DIGIT_LEN_128}
            val denomPow10 = pow10_64(pow10)
            val q: Long
            val r: Long
            if (dw0 > 0) {
                q = dw0 / denomPow10
                r = dw0 % denomPow10
            } else {
                q = unsignedDiv(dw0, denomPow10)
                r = dw0 - (q * denomPow10)
            }
            val residue = Residue.fromRemainderDivisor(r, denomPow10)
            result.dw1 = 0L
            result.dw0 = q
            return residue
        }
        if (pow10 < BARRETT_POW10_MAXX)
            return barrettDivPow10(result, dw1, dw0, pow10)

        val t = C256()
        t.c256Set128(dw1, dw0)
        val s = C256()
        val residue = c256SetScaleDownPow10(s, t, pow10)
        result.dw1 = s.dw1
        result.dw0 = s.dw0
        return residue
    }

    fun barrettDivPow10(result: DwPair, dw1: Long, dw0: Long, pow10: Int): Residue {
        val denom = pow10_64(pow10)
        val mu = POW10[BARRETT_POW10_MU_OFFSET + pow10]

        val dwA = dw0 and 0xFFFF_FFFFL
        val dwB = dw0 ushr 32
        val dwG = dw1

        val qHatG = unsignedMulHi(dwG, mu)
        val rHatG = dwG - (qHatG * denom)
        val adjustG = ((rHatG - denom) shr 63).inv()
        val qG = qHatG - adjustG
        val rG = rHatG - (adjustG and denom)

        val ppB = (rG shl 32) or dwB
        val qHatB = unsignedMulHi(ppB, mu)
        val rHatB = ppB - (qHatB * denom)
        val adjustB = ((rHatB - denom) shr 63).inv()
        val qB = qHatB - adjustB
        val rB = rHatB - (adjustB and denom)

        val ppA = (rB shl 32) or dwA
        val qHatA = unsignedMulHi(ppA, mu)
        val rHatA = ppA - (qHatA * denom)
        val adjustA = ((rHatA - denom) shr 63).inv()
        val qA = qHatA - adjustA
        val rA = rHatA - (adjustA and denom)

        val remainder = rA

        result.dw1 = qG
        result.dw0 = (qB shl 32) or qA

        val residue = Residue.fromRemainderDivisor(remainder, denom)
        return residue
    }
}
