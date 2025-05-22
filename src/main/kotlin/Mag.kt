package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import java.math.BigDecimal
import java.math.BigInteger

const val SCIENTIFIC_EXP_MAX = 6144
const val Q_EXP_MAX = 6111 // 6144 - (PRECISION_34 - 1)
const val SCIENTIFIC_EXP_MIN = -6143
//const val TINY_EXPONENT = MIN_SCIENTIFIC_EXPONENT - (PRECISION_34 - 1) // -6176
const val Q_EXP_TINY = -6176 // EXP_SCIENTIFIC_MIN - (PRECISION_34 - 1)

const val NON_FINITE_MIN = 1000000
const val NON_FINITE_INF = 1000000
const val NON_FINITE_QNAN = 1000001
const val NON_FINITE_SNAN = 1000002

class Mag(/* exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long */) {
    val c:Coeff = Coeff()
    var qExp = 0
    constructor(exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long): this() {
        c.coeffSet256(dw3, dw2, dw1, dw0)
        this.qExp = exp
    }
    constructor(bd: BigDecimal): this() {
        magSet(bd)
    }
    constructor(exp: Int, bi: BigInteger): this() {
        magSet(exp, bi)
    }

    fun sciExp() = qExp + (c.digitLen - 1)

    fun coeffToBigInteger() = c.coeffToBigInteger()

    fun finalize(inboundResidue: Residue, sign: Boolean, ctx: Decimal128Context) {
        if (c.digitLen != 0) {
            var sciExp = qExp + (c.digitLen - 1)
            // IEEE754-2008 7.5: detect tininess on the unrounded result
            if (sciExp < SCIENTIFIC_EXP_MIN) {
                ctx.setUnderflow() // IEEE754-2008 7.5 Underflow page 38
            }

            val excess = Math.max(0, c.digitLen - PRECISION_34)
            val qTiny = Q_EXP_TINY - excess      // threshold for normalized

            // 2) Normalized result: round only if bd has >34 digits
            if (sciExp <= SCIENTIFIC_EXP_MAX && qExp >= qTiny) {
                val totalResidue =
                    if (excess == 0) {
                        inboundResidue
                    } else {
                        val roundingResidue = CoeffScalePow10.coeffScaleDownPow10(c, c, excess)
                        qExp += excess
                        assert(c.digitLen == 34)
                        assert(sciExp == qExp + (c.digitLen - 1))
                        roundingResidue.merge(inboundResidue)
                    }

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
                // then the result is EXACTly divisible by 10
                val residueExact = CoeffScalePow10.coeffScaleDownPow10(c, c, 1)
                assert(residueExact == Residue.EXACT)
                ++qExp
                assert(qExp + (c.digitLen - 1) == sciExp + 1)
                ++sciExp
                if (sciExp <= SCIENTIFIC_EXP_MAX)
                    return
                // rounding caused overflow
                // fall into next conditional and flow over
            }

            // 1) Overflow => +/- Infinity
            if (sciExp > SCIENTIFIC_EXP_MAX) {
                // overflow IEEE754-2008 7.4 Overflow page 37
                if (ctx.roundingDirection.overflowsToInfinity(sign)) {
                    c.coeffSetZero()
                    qExp = NON_FINITE_INF
                } else {
                    magSetMaxFinite()
                }
                ctx.setOverflow()
                ctx.setInexact()
                return
            }

            // 7.5.1: subnormal rounding (tiny result stays nonzero)
            val qMin = Q_EXP_TINY - c.digitLen           // threshold for subnormal cohort
            val overlap = qExp - qMin
            if (overlap >= 0) {
                val excess2 = c.digitLen - overlap
                val scaleResidue = CoeffScalePow10.coeffScaleDownPow10(c, c, excess2)
                qExp += excess2
                assert(c.digitLen <= 34)
                assert(sciExp == qExp + (c.digitLen - 1))

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
                ++qExp
                return
            }

            // underflow to zero
            c.coeffSetZero()
            qExp = Q_EXP_TINY
            ctx.setInexact()
            return
        }
        // zero case
        qExp = Math.max(Math.min(qExp, Q_EXP_MAX), Q_EXP_TINY)
    }


    fun finalize_incorrect(inboundResidue: Residue, sign: Boolean, ctx: Decimal128Context) {
        // IEEE754-2008 7.5: detect tininess on the unrounded result
        val preRoundAdjustedExp = qExp + (c.digitLen - 1)
        if (preRoundAdjustedExp < SCIENTIFIC_EXP_MIN)
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
            qExp += excessDigits
        }
        val postRoundAdjustedExp = qExp + (c.digitLen - 1)
        if (postRoundAdjustedExp > SCIENTIFIC_EXP_MAX) {
            // overflow IEEE754-2008 7.4 Overflow page 37
            if (ctx.roundingDirection.overflowsToInfinity(sign)) {
                c.coeffSetZero()
                qExp = NON_FINITE_INF
            } else {
                magSetMaxFinite()
            }
            ctx.setOverflow()
            ctx.setInexact()
            return
        }
        if (qExp >= Q_EXP_TINY)
            return
        // 7.5.1: subnormal rounding (tiny result stays nonzero)
        var tinyScaleDown = Q_EXP_TINY - qExp
        if (c.digitLen >= tinyScaleDown) {
            val residue2 = CoeffScalePow10.coeffScaleDownPow10(c, c, tinyScaleDown)
            qExp += tinyScaleDown
            if (residue2 != EXACT) {
                ctx.setInexact()
                val roundUp2 = residue2.ulpRoundUp(ctx.roundingDirection.negate(sign), c.dw0)
                if (roundUp2) {
                    val digitLenBeforeRoundUp = c.digitLen
                    c.coeffIncrement()
                }
            }
            assert(qExp == Q_EXP_TINY)
            return
        }
        // underflow to zero
        c.coeffSetZero()
        qExp = Q_EXP_TINY
        ctx.setInexact()
    }

    fun magSetZero() {
        qExp = 0
        c.coeffSetZero()
    }

    fun magSetMaxFinite() {
        qExp = Q_EXP_MAX
        // 0x378D8E6400000000uL.toLong(), 0x0001ED09BEAD87C0uL.toLong(),
        // 10000000000000000000000000000000000 (10**34)
        val dw1Nines = 0x0001ED09BEAD87C0L
        val dw0Nines = 0x378D8E6400000000L - 1L
        c.coeffSet128(dw1Nines, dw0Nines)
    }

    fun magSet(dw0: Long) = magSet(0, dw0)

    fun magSet(exponent: Int, dw0: Long) {
        assert(exponent in Q_EXP_TINY..SCIENTIFIC_EXP_MAX)
        qExp = exponent
        c.coeffSet64(dw0)
    }

    fun magSet(exponent: Int, bi: BigInteger) =
        magSet(exponent, bi, Decimal128Context())

    fun magSet(exponent: Int, bi: BigInteger, ctx: Decimal128Context) {
        c.coeffSet(bi)
        qExp = exponent
        finalize(EXACT, bi.signum() == -1, ctx)
    }

    fun magSet(bd: BigDecimal) {
        magSet(bd, Decimal128Context())
    }

    fun magSet(bd: BigDecimal, ctx: Decimal128Context) {
        magSet(-bd.scale(), bd.unscaledValue(), ctx)
    }

    fun magSet(x:Mag) {
        qExp = x.qExp
        c.coeffSet(x.c)
    }

    fun magSet(str: String) = magSet(BigDecimal(str))

    fun magAdd(a: Mag, b: Mag, sign: Boolean, ctx: Decimal128Context) {
        val residue = MagAdd.magAdd(this, a, b)
        finalize(residue, sign, ctx)
    }

    fun magSub(a: Mag, b: Mag, sign: Boolean, ctx: Decimal128Context) {
        assert(a.magCompareTo(b) >= 0)
        val residue = MagAdd.magSub(this, a, b)
        finalize(residue, sign, ctx)
    }

    fun magMul(x: Mag, y: Mag, sign: Boolean, ctx: Decimal128Context) {
        MagMul.magMul(this, x, y)
        finalize(EXACT, sign, ctx)
    }

    fun magScaleB(a: Mag, e: Int, sign: Boolean, ctx: Decimal128Context) {
        c.coeffSet(a.c)
        qExp = e
        finalize(Residue.EXACT, sign, ctx)
    }

    fun magCompareTo(other: Mag) : Int {
        val thisIsZero = this.c.isZero()
        val otherIsZero = other.c.isZero()
        val eitherIsZero = thisIsZero or otherIsZero
        when {
            !eitherIsZero -> {
                val cmpExpSci = this.sciExp().compareTo(other.sciExp())
                if (cmpExpSci != 0)
                    return cmpExpSci
                val expDelta = this.qExp - other.qExp
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
        if (this.sciExp() != other.sciExp())
            return bothAreZero
        if (! eitherIsZero) {
            val expDelta = this.qExp - other.qExp
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
            (qExp < NON_FINITE_MIN) -> c.toString() + "E" + qExp
            qExp == NON_FINITE_INF -> "Inf"
            qExp == NON_FINITE_QNAN -> "NaN" + c.toNaNDiagnosticString()
            qExp == NON_FINITE_SNAN -> "sNaN" + c.toNaNDiagnosticString()
            else -> "?que? $qExp"
        }
    }

}