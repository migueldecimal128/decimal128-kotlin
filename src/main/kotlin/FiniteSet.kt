package com.decimal128

import com.decimal128.CoeffSet.coeffSet
import com.decimal128.CoeffSet.coeffSetZero
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

object FiniteSet {

    fun finiteSetZero(f: Finite) {
        f.sign = false
        f.exp = 0
        coeffSetZero(f.c)
    }

    fun finiteSet(f: Finite, l: Long) = finiteSet(f, l < 0, 0, abs(l))

    fun finiteSet(f: Finite, exponent: Int, l: Long) = finiteSet(f, l < 0, exponent, abs(l))

    fun finiteSet(f: Finite, sign: Boolean, exponent: Int, dw0: Long) {
        assert(exponent in MIN_EXPONENT..MAX_EXPONENT)
        f.sign = sign
        f.exp = exponent
        f.c.setCoeff64(dw0)
    }

    fun finiteSet(f: Finite, bd: BigDecimal) {
        val bdRounded = bd.round(MathContext.DECIMAL128)
        coeffSet(f.c, bdRounded.unscaledValue())
        f.exp = -bdRounded.scale()
        f.sign = bd.signum() < 0
    }

    fun finiteSet(f: Finite, x:Finite) {
        if (f != x) {
            f.sign = x.sign
            f.exp = x.exp
            coeffSet(f.c, x.c)
        }
    }

    fun finiteSet(f: Finite, str: String) = finiteSet(f, BigDecimal(str))

}
