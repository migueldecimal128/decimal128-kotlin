package com.decimal128

object MagMul {

    fun magMul(z: Mag, x: Mag, y: Mag) {
        z.coeffSetMul(x, y)
        z.qExp = x.qExp + y.qExp
    }

    fun magFma(z: Mag, x: Mag, y: Mag, a: Mag): Residue {
        val qProd = x.qExp + y.qExp
        if (x.coeffIsNotZero() && y.coeffIsNotZero()) {
            if (a.coeffIsZero()) {
                z.coeffSetMul(x, y)
                z.qExp = qProd
                return Residue.EXACT
            }
            val qDelta = qProd - a.qExp
            if (qDelta == 0) {
                z.qExp = qProd
                z.coeffSetFma(x, y, a)
                return Residue.EXACT
            }
            require(a.bitLen <= 128)
            val a1 = a.dw1
            val a0 = a.dw0

            val eX = x.sciExp()
            val eY = y.sciExp()
            val eA = a.sciExp()

            val eProdMin = eX + eY
            val eProdMax = eProdMin + 1
            val eMinDelta = eProdMin - eA
            val eMaxDelta = eProdMax - eA

            val maxProdBitLen = x.bitLen + y.bitLen
            val minProdBitLen = maxProdBitLen - 1
            if (qDelta > 0) {
                if (qDelta >= PRECISION_34) {
                    // x*y swamps a
                    val residue = if (qDelta == PRECISION_34) Residue.residueFrom(a) else Residue.LT_HALF
                    z.coeffSetMul(x, y)
                    z.qExp = qProd
                    return residue
                }
                // scale the product then add a
                // it could be the case that z === a ... so let's copy the coeff of a
                z.coeffSetMul(x, y)
                z.coeffSetFmaPow10(z, qDelta, a1, a0)
                z.qExp = a.qExp
                return Residue.EXACT
            }
            // qDelta < 0
            val qDeltaAbs = -qDelta
            if (qDeltaAbs >= PRECISION_34) {
                // a swamps x*y
                val residue = (
                        if (qDeltaAbs == PRECISION_34) {
                            z.coeffSetMul(x, y)
                            Residue.residueFrom(z)
                        } else {
                            Residue.LT_HALF
                        })
                z.magSet(a)
                return residue
            }
            // overlap ... scale a up
            val aScaled = Dec34()
            aScaled.coeffSetScaleUpPow10(a, qDeltaAbs)
            z.coeffSetMul(x, y)
            z.coeffSetAdd(z, aScaled)
            z.qExp = a.qExp - qDeltaAbs
            return Residue.EXACT
        } else {
            z.magSet(a)
            return Residue.EXACT
        }
    }

}
