package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import java.math.BigDecimal
import java.math.BigInteger

const val EXP_SCIENTIFIC_MAX = 6144
const val EXP_Q_MAX = 6111 // 6144 - (PRECISION_34 - 1)
const val EXP_SCIENTIFIC_MIN = -6143
//const val TINY_EXPONENT = MIN_SCIENTIFIC_EXPONENT - (PRECISION_34 - 1) // -6176
const val EXP_Q_TINY = -6176 // EXP_SCIENTIFIC_MIN - (PRECISION_34 - 1)

const val NON_FINITE_MIN = 1000000
const val NON_FINITE_INF = 1000000
const val NON_FINITE_QNAN = 1000001
const val NON_FINITE_SNAN = 1000002

class Mag(/* exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long */) {
    val c:Coeff = Coeff()
    var expQ = 0
    constructor(exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long): this() {
        c.coeffSet256(dw3, dw2, dw1, dw0)
        this.expQ = exp
    }
    constructor(bd: BigDecimal): this() {
        magSet(bd)
    }
    constructor(exp: Int, bi: BigInteger): this() {
        magSet(exp, bi)
    }

    fun expSci() = expQ + (c.digitLen - 1)

    fun coeffToBigInteger() = c.coeffToBigInteger()

    fun finalize(inboundResidue: Residue, sign: Boolean, ctx: Decimal128Context) {
        if (c.digitLen != 0) {
            var expSci = expQ + (c.digitLen - 1)
            // IEEE754-2008 7.5: detect tininess on the unrounded result
            if (expSci < EXP_SCIENTIFIC_MIN) {
                ctx.setUnderflow() // IEEE754-2008 7.5 Underflow page 38
            }

            val excess = Math.max(0, c.digitLen - PRECISION_34)
            val qTiny = EXP_Q_TINY - excess      // threshold for normalized

            // 2) Normalized result: round only if bd has >34 digits
            if (expSci <= EXP_SCIENTIFIC_MAX && expQ >= qTiny) {
                if (excess == 0)
                    return
                val scaleResidue = CoeffScalePow10.coeffScaleDownPow10(c, c, excess)
                expQ += excess
                assert(c.digitLen == 34)
                assert(expSci == expQ + (c.digitLen - 1))

                val totalResidue = scaleResidue.merge(inboundResidue)
                if (totalResidue == EXACT)
                    return
                ctx.setInexact()
                val roundUp = totalResidue.ulpRoundUp(ctx.roundingDirection.negate(sign), c.dw0)
                if (!roundUp)
                    return
                c.coeffIncrement()
                if (c.digitLen <= PRECISION_34)
                    return
                assert(c.digitLen == 35)
                // if we rolled into another digit because of roundup
                // then the result is definitely divisible by 10
                val residueExact = CoeffScalePow10.coeffScaleDownPow10(c, c, 1)
                assert(residueExact == Residue.EXACT)
                ++expQ
                assert(expQ + (c.digitLen - 1) == expSci + 1)
                ++expSci
                if (expSci <= EXP_SCIENTIFIC_MAX)
                    return
                // rounding caused overflow
                // fall into next conditional and flow over
            }

            // 1) Overflow => +/- Infinity
            if (expSci > EXP_SCIENTIFIC_MAX) {
                // overflow IEEE754-2008 7.4 Overflow page 37
                if (ctx.roundingDirection.overflowsToInfinity(sign)) {
                    c.coeffSetZero()
                    expQ = NON_FINITE_INF
                } else {
                    magSetMaxFinite()
                }
                ctx.setOverflow()
                ctx.setInexact()
                return
            }

            // 7.5.1: subnormal rounding (tiny result stays nonzero)
            val qMin = EXP_Q_TINY - c.digitLen           // threshold for subnormal cohort
            val overlap = expQ - qMin
            if (overlap >= 0) {
                val excess2 = c.digitLen - overlap
                val scaleResidue = CoeffScalePow10.coeffScaleDownPow10(c, c, excess2)
                expQ += excess2
                assert(c.digitLen <= 34)
                assert(expSci == expQ + (c.digitLen - 1))

                val totalResidue = scaleResidue.merge(inboundResidue)
                if (totalResidue == EXACT)
                    return
                ctx.setInexact()
                val roundUp = totalResidue.ulpRoundUp(ctx.roundingDirection.negate(sign), c.dw0)
                if (!roundUp)
                    return
                c.coeffIncrement()
                if (c.digitLen <= PRECISION_34)
                    return
                assert(c.digitLen == 35)
                // if we rolled into another digit because of roundup
                // then the result is definitely divisible by 10
                val residueExact = CoeffScalePow10.coeffScaleDownPow10(c, c, 1)
                assert(residueExact == Residue.EXACT)
                ++expQ
                return
            }

            // underflow to zero
            c.coeffSetZero()
            expQ = EXP_Q_TINY
            ctx.setInexact()
            return
        }
        // zero case
        expQ = Math.max(Math.min(expQ, EXP_Q_MAX), EXP_Q_TINY)
    }


    fun finalize_incorrect(inboundResidue: Residue, sign: Boolean, ctx: Decimal128Context) {
        // IEEE754-2008 7.5: detect tininess on the unrounded result
        val preRoundAdjustedExp = expQ + (c.digitLen - 1)
        if (preRoundAdjustedExp < EXP_SCIENTIFIC_MIN)
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
                c.coeffIncrement()
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
            expQ += excessDigits
        }
        val postRoundAdjustedExp = expQ + (c.digitLen - 1)
        if (postRoundAdjustedExp > EXP_SCIENTIFIC_MAX) {
            // overflow IEEE754-2008 7.4 Overflow page 37
            if (ctx.roundingDirection.overflowsToInfinity(sign)) {
                c.coeffSetZero()
                expQ = NON_FINITE_INF
            } else {
                magSetMaxFinite()
            }
            ctx.setOverflow()
            ctx.setInexact()
            return
        }
        if (expQ >= EXP_Q_TINY)
            return
        // 7.5.1: subnormal rounding (tiny result stays nonzero)
        var tinyScaleDown = EXP_Q_TINY - expQ
        if (c.digitLen >= tinyScaleDown) {
            val residue2 = CoeffScalePow10.coeffScaleDownPow10(c, c, tinyScaleDown)
            expQ += tinyScaleDown
            if (residue2 != EXACT) {
                ctx.setInexact()
                val roundUp2 = residue2.ulpRoundUp(ctx.roundingDirection.negate(sign), c.dw0)
                if (roundUp2) {
                    val digitLenBeforeRoundUp = c.digitLen
                    c.coeffIncrement()
                }
            }
            assert(expQ == EXP_Q_TINY)
            return
        }
        // underflow to zero
        c.coeffSetZero()
        expQ = EXP_Q_TINY
        ctx.setInexact()
    }

    fun magSetZero() {
        expQ = 0
        c.coeffSetZero()
    }

    fun magSetMaxFinite() {
        expQ = EXP_Q_MAX
        // 0x378D8E6400000000uL.toLong(), 0x0001ED09BEAD87C0uL.toLong(),
        // 10000000000000000000000000000000000 (10**34)
        val dw1Nines = 0x0001ED09BEAD87C0L
        val dw0Nines = 0x378D8E6400000000L - 1L
        c.coeffSet128(dw1Nines, dw0Nines)
    }

    fun magSet(dw0: Long) = magSet(0, dw0)

    fun magSet(exponent: Int, dw0: Long) {
        assert(exponent in EXP_Q_TINY..EXP_SCIENTIFIC_MAX)
        expQ = exponent
        c.coeffSet64(dw0)
    }

    fun magSet(exponent: Int, bi: BigInteger) =
        magSet(exponent, bi, Decimal128Context())

    fun magSet(exponent: Int, bi: BigInteger, ctx: Decimal128Context) {
        c.coeffSet(bi)
        expQ = exponent
        finalize(EXACT, bi.signum() == -1, ctx)
    }

    fun magSet(bd: BigDecimal) {
        magSet(bd, Decimal128Context())
    }

    fun magSet(bd: BigDecimal, ctx: Decimal128Context) {
        magSet(-bd.scale(), bd.unscaledValue(), ctx)
    }

    fun magSet(x:Mag) {
        expQ = x.expQ
        c.coeffSet(x.c)
    }

    fun magSet(str: String) = magSet(BigDecimal(str))

    fun magAdd(a: Mag, b: Mag, sign: Boolean, ctx: Decimal128Context) {
        val residue = MagAdd.magAdd(this, a, b)
        finalize(residue, sign, ctx)
    }

    fun magScaleB(a: Mag, e: Int, sign: Boolean, ctx: Decimal128Context) {
        c.coeffSet(a.c)
        expQ = e
        finalize(Residue.EXACT, sign, ctx)
    }

    fun magCompareTo(other: Mag) : Int {
        val thisIsZero = this.c.isZero()
        val otherIsZero = other.c.isZero()
        val eitherIsZero = thisIsZero or otherIsZero
        when {
            !eitherIsZero -> {
                val cmpExpSci = this.expSci().compareTo(other.expSci())
                if (cmpExpSci != 0)
                    return cmpExpSci
                val expDelta = this.expQ - other.expQ
                val ret = when {
                    expDelta == 0 -> this.c.unscaledCompareTo(other.c)
                    expDelta > 0 -> -other.c.scaledCompareTo(this.c, expDelta)
                    else -> this.c.scaledCompareTo(other.c, -expDelta)
                }
                return ret
            }
            thisIsZero -> {
                return if (otherIsZero) 0 else -1
            }
            else -> {
                return 1
            }
        }
    }

    fun magEQ(other: Mag) : Boolean {
        val thisIsZero = this.c.isZero()
        val otherIsZero = other.c.isZero()
        val bothAreZero = thisIsZero and otherIsZero
        val eitherIsZero = thisIsZero or otherIsZero
        if (this.expSci() != other.expSci())
            return bothAreZero
        if (! eitherIsZero) {
            val expDelta = this.expQ - other.expQ
            return when {
                expDelta == 0 -> this.c.unscaledEQ(other.c)
                expDelta > 0 -> this.c.scaledEQ(other.c, expDelta)
                else -> other.c.scaledEQ(this.c, -expDelta)
            }
        }
        return bothAreZero
    }

    override fun toString(): String {
        return when {
            (expQ < NON_FINITE_MIN) -> c.toString() + "E" + expQ
            expQ == NON_FINITE_INF -> "Inf"
            expQ == NON_FINITE_QNAN -> "NaN" + c.toNaNDiagnosticString()
            expQ == NON_FINITE_SNAN -> "sNaN" + c.toNaNDiagnosticString()
            else -> "?que? $expQ"
        }
    }

}