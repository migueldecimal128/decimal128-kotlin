package com.decimal128

import com.decimal128.Residue.Companion.EXACT

const val MIN_EXPONENT = -6143
const val MAX_EXPONENT = 6144

const val NON_FINITE_MIN = 1000000


class Finite {
    val c:Coeff = Coeff()
    var exp = 0
    var sign = false



    fun finalize(ctx: Decimal128Context) {
        var scaleDelta = c.digitLen - PRECISION_34
        if (scaleDelta > 0) {
            val residue = CoeffScalePow10.coeffScaleDownPow10(c, c, scaleDelta)
            if (residue != EXACT) {
                val roundUp = residue.ulpRoundUp(ctx.roundingDirection.negate(sign), c.dw0)
                if (roundUp) {
                    c.roundUp()
                    if (c.digitLen > PRECISION_34) {
                        // if we rolled into another digit because of roundup
                        // then the result is definitely divisible by 10
                        val residue2 = CoeffScalePow10.coeffScaleDownPow10(c, c, 1)
                        assert(residue2 == Residue.EXACT)
                        ++scaleDelta
                    }
                }
                ctx.setInexact()
            }
            exp += scaleDelta
        }
        if (exp in MIN_EXPONENT..MAX_EXPONENT)
            return
        throw RuntimeException("not impl")
    }
}