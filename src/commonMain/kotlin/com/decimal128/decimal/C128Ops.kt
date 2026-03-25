@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.decimal

internal fun c128UnscaledCompare(x: Decimal, y: Decimal): Int {
    val xBitLen = x.bitLen
    val yBitLen = y.bitLen
    if (xBitLen != yBitLen)
        return ((xBitLen - yBitLen) shr 30) or 1
    val cmp0 = unsignedCmp(x.dw0, y.dw0)
    val cmp1 = unsignedCmp(x.dw1, y.dw1)
    val cmp10 = if (cmp1 != 0) cmp1 else cmp0
    return cmp10
}


fun c128ScaleDownPow10(resultPentad: Pentad, dw1: Long, dw0: Long, pow10: Int): Residue {
    if (dw1 == 0L) {
        verify { pow10 < MIN_POW10_DIGIT_LEN_128 }
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
        resultPentad.dw1 = 0L
        resultPentad.dw0 = q
        return residue
    }
    if (pow10 < BARRETT_POW10_MAXX)
        return barrettDivPow10(resultPentad, dw1, dw0, pow10)

    val t = C256()
    t.c256Set128(dw1, dw0)
    val s = C256()
    val residue = c256SetScaleDownPow10(s, t, pow10, resultPentad)
    resultPentad.dw1 = s.dw1
    resultPentad.dw0 = s.dw0
    return residue
}

private fun barrettDivPow10(result: Pentad, dw1: Long, dw0: Long, pow10: Int): Residue {
    val lowBits = dw0 and ((1L shl pow10) - 1)
    val shifted1 = dw1 ushr pow10
    val shifted0 = (dw1 shl (64 - pow10)) or (dw0 ushr pow10)

    val denom5 = POW10[POW5_64_BASE + pow10]
    val mu = POW10[BARRETT_POW5_MU_BASE + pow10]

    val dwA = shifted0 and MASK32L
    val dwB = shifted0 ushr 32
    val dwG = shifted1

    val qHatG = unsignedMulHi(dwG, mu)
    val rHatG = dwG - (qHatG * denom5)
    val adjustG = ((rHatG - denom5) shr 63).inv()
    val qG = qHatG - adjustG
    val rG = rHatG - (adjustG and denom5)

    val ppB = (rG shl 32) or dwB
    val qHatB = unsignedMulHi(ppB, mu)
    val rHatB = ppB - (qHatB * denom5)
    val adjustB = ((rHatB - denom5) shr 63).inv()
    val qB = qHatB - adjustB
    val rB = rHatB - (adjustB and denom5)

    val ppA = (rB shl 32) or dwA
    val qHatA = unsignedMulHi(ppA, mu)
    val rHatA = ppA - (qHatA * denom5)
    val adjustA = ((rHatA - denom5) shr 63).inv()
    val qA = qHatA - adjustA
    val rA = rHatA - (adjustA and denom5)

    result.dw1 = qG
    result.dw0 = (qB shl 32) or qA

    val remainder10 = (rA shl pow10) + lowBits
    val denom10 = pow10_64(pow10)
    val residue = Residue.fromRemainderDivisor(remainder10, denom10)
    return residue
}
