package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

const val MAX_ADJUSTED_EXPONENT = 6144
const val MIN_ADJUSTED_EXPONENT = -6143
const val TINY_EXPONENT = MIN_ADJUSTED_EXPONENT - (PRECISION_34 - 1)

const val NON_FINITE_MIN = 1000000
const val NON_FINITE_INF = 1000000
const val NON_FINITE_QNAN = 1000001
const val NON_FINITE_SNAN = 1000002

class Mag(/* exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long */) {
    val c:Coeff = Coeff()
    var exp = 0
    constructor(exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long): this() {
        c.coeffSet256(dw3, dw2, dw1, dw0)
        this.exp = exp
    }
    constructor(bd: BigDecimal): this() {
        magSet(bd)
    }
    constructor(exp: Int, bi: BigInteger): this() {
        magSet(exp, bi)
    }

    fun finalize(inboundResidue: Residue, sign: Boolean, ctx: Decimal128Context) {
        // IEEE754-2008 7.5: detect tininess on the unrounded result
        val preRoundAdjustedExp = exp + (c.digitLen - 1)
        if (preRoundAdjustedExp < MIN_ADJUSTED_EXPONENT)
            ctx.setUnderflow() // IEEE754-2008 7.5 Underflow page 38

        var totalResidue = inboundResidue
        var excessDigits = c.digitLen - PRECISION_34
        if (excessDigits > 0) {
            val scaleResidue = CoeffScalePow10.coeffScaleDownPow10(c, c, excessDigits)
            totalResidue = scaleResidue.merge(inboundResidue)
        }
        if (totalResidue != EXACT) {
            val roundUp = totalResidue.ulpRoundUp(ctx.roundingDirection.negate(sign), c.dw0)
            if (roundUp) {
                c.coeffRoundUp()
                if (c.digitLen > PRECISION_34) {
                    // if we rolled into another digit because of roundup
                    // then the result is definitely divisible by 10
                    val residue2 = CoeffScalePow10.coeffScaleDownPow10(c, c, 1)
                    assert(residue2 == Residue.EXACT)
                    ++excessDigits
                }
            }
            ctx.setInexact()
        }
        if (excessDigits > 0) {
            exp += excessDigits
        }
        val postRoundAdjustedExp = exp + (c.digitLen - 1)
        if (postRoundAdjustedExp > MAX_ADJUSTED_EXPONENT) {
            // overflow IEEE754-2008 7.4 Overflow page 37
            if (ctx.roundingDirection.overflowsToInfinity(sign)) {
                c.coeffSetZero()
                exp = NON_FINITE_INF
            } else {
                magSetMaxFinite()
            }
            ctx.setOverflow()
            ctx.setInexact()
            return
        }
        if (exp >= TINY_EXPONENT)
            return
        // 7.5.1: subnormal rounding (tiny result stays nonzero)
        var tinyScaleDown = TINY_EXPONENT - exp
        if (c.digitLen >= tinyScaleDown) {
            val residue2 = CoeffScalePow10.coeffScaleDownPow10(c, c, tinyScaleDown)
            exp += tinyScaleDown
            if (residue2 != EXACT) {
                ctx.setInexact()
                val roundUp2 = residue2.ulpRoundUp(ctx.roundingDirection.negate(sign), c.dw0)
                if (roundUp2) {
                    val digitLenBeforeRoundUp = c.digitLen
                    c.coeffRoundUp()
                }
            }
            assert(exp == TINY_EXPONENT)
            return
        }
        // underflow to zero
        c.coeffSetZero()
        exp = TINY_EXPONENT
        ctx.setInexact()
    }

    fun magSetZero() {
        exp = 0
        c.coeffSetZero()
    }

    fun magSetMaxFinite() {
        exp = MAX_ADJUSTED_EXPONENT
        // 0x378D8E6400000000uL.toLong(), 0x0001ED09BEAD87C0uL.toLong(),
        // 10000000000000000000000000000000000 (10**34)
        val dw1Nines = 0x0001ED09BEAD87C0L
        val dw0Nines = 0x378D8E6400000000L
        c.coeffSet128(dw1Nines, dw0Nines)
    }

    fun magSet(dw0: Long) = magSet(0, dw0)

    fun magSet(exponent: Int, dw0: Long) {
        assert(exponent in TINY_EXPONENT..MAX_ADJUSTED_EXPONENT)
        exp = exponent
        c.coeffSet64(dw0)
    }

    fun magSet(exponent: Int, bi: BigInteger) =
        magSet(exponent, bi, Decimal128Context())

    fun magSet(exponent: Int, bi: BigInteger, ctx: Decimal128Context) {
        c.coeffSet(bi)
        exp = exponent
        finalize(EXACT, bi.signum() == -1, ctx)
    }

    fun magSet(bd: BigDecimal) {
        magSet(bd, Decimal128Context())
    }

    fun magSet(bd: BigDecimal, ctx: Decimal128Context) {
        val bdRounded = bd.round(MathContext.DECIMAL128)
        magSet(-bdRounded.scale(), bdRounded.unscaledValue(), ctx)
    }

    fun magSet(x:Mag) {
        exp = x.exp
        c.coeffSet(x.c)
    }

    fun magSet(str: String) = magSet(BigDecimal(str))

    fun magAdd(a: Mag, b: Mag, sign: Boolean, ctx: Decimal128Context) = MagAdd.magAdd(this, a, b, sign, ctx)

    fun magScaleB(a: Mag, e: Int, sign: Boolean, ctx: Decimal128Context) {
        c.coeffSet(a.c)
        exp = e
        finalize(Residue.EXACT, sign, ctx)
    }

    override fun toString(): String {
        return when {
            (exp < NON_FINITE_MIN) -> c.toString() + "E" + exp
            exp == NON_FINITE_INF -> "Inf"
            exp == NON_FINITE_QNAN -> "NaN" + c.toNaNString()
            exp == NON_FINITE_SNAN -> "sNaN" + c.toNaNString()
            else -> "?que? $exp"
        }
    }

}