package com.decimal128
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
            if (residue == Residue.EXACT) {
                while (qZ < qPreferred && z.coeffIsMultipleOf10()) {
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