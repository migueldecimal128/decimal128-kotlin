package com.decimal128

import com.decimal128.CoeffSet.coeffSet
import com.decimal128.Residue.Companion.EXACT
import java.math.BigDecimal
import java.math.MathContext

const val MIN_EXPONENT = -6143
const val MAX_EXPONENT = 6144

const val NON_FINITE_MIN = 1000000


class Mag {
    val c:Coeff = Coeff()
    var exp = 0

    fun finalize(sign: Boolean, ctx: Decimal128Context) {
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

    fun magSetZero() {
        exp = 0
        c.coeffSetZero()
    }

    fun magSet(dw0: Long) = magSet(0, dw0)

    fun magSet(exponent: Int, dw0: Long) {
        assert(exponent in MIN_EXPONENT..MAX_EXPONENT)
        exp = exponent
        c.coeffSet64(dw0)
    }

    fun magSet(bd: BigDecimal) {
        val bdRounded = bd.round(MathContext.DECIMAL128)
        c.coeffSet(bdRounded.unscaledValue())
        exp = -bdRounded.scale()
    }

    fun magSet(x:Mag) {
        exp = x.exp
        c.coeffSet(x.c)
    }

    fun magSet(str: String) = magSet(BigDecimal(str))


}