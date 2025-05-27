package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import java.math.BigDecimal
import java.math.BigInteger

const val SCIENTIFIC_EXP_MAX = 6144
const val Q_EXP_MAX = 6111 // 6144 - (PRECISION_34 - 1)
const val SCIENTIFIC_EXP_MIN = -6143
//const val TINY_EXPONENT = MIN_SCIENTIFIC_EXPONENT - (PRECISION_34 - 1) // -6176
const val Q_EXP_TINY = -6176 // EXP_SCIENTIFIC_MIN - (PRECISION_34 - 1)

const val MIN_SPECIAL_VALUE = 1000000
const val NON_FINITE_INF = 1000000
const val NON_FINITE_QNAN = 1000001
const val NON_FINITE_SNAN = 1000002

open class Mag(/* exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long */) : Coeff() {
    var qExp = 0
    constructor(exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long): this() {
        super.coeffSet256(dw3, dw2, dw1, dw0)
        this.qExp = exp
    }
    constructor(bd: BigDecimal): this() {
        magSet(bd)
    }
    constructor(exp: Int, bi: BigInteger): this() {
        magSet(exp, bi)
    }

    fun sciExp() = qExp + (digitLen - 1)

    fun roundAndFinalize(inboundResidue: Residue, sign: Int, ctx: Decimal128Context) =
        roundAndFinalize(inboundResidue, sign, ctx.roundingDirection, ctx)

    fun roundAndFinalize(inboundResidue: Residue, sign: Int, roundingDirection: RoundingDirection, ctx: Decimal128Context) {
        if (qExp < NON_FINITE_INF) {
            if (super.digitLen != 0) {
                var sciExp = qExp + (super.digitLen - 1)
                // IEEE754-2008 7.5: detect tininess on the unrounded result
                if (sciExp < SCIENTIFIC_EXP_MIN) {
                    ctx.setUnderflow() // IEEE754-2008 7.5 Underflow page 38
                }

                val excess = Math.max(0, super.digitLen - PRECISION_34)
                val qTiny = Q_EXP_TINY - excess      // threshold for normalized

                // 2) Normalized result: round only if bd has >34 digits
                if (sciExp <= SCIENTIFIC_EXP_MAX && qExp >= qTiny) {
                    val totalResidue =
                        if (excess == 0) {
                            inboundResidue
                        } else {
                            val roundingResidue = CoeffScalePow10.coeffScaleDownPow10(this, this, excess)
                            qExp += excess
                            assert(super.digitLen == 34)
                            assert(sciExp == qExp + (super.digitLen - 1))
                            roundingResidue.merge(inboundResidue)
                        }

                    if (totalResidue == EXACT)
                        return

                    ctx.setInexact()
                    val roundUp = totalResidue.ulpRoundUp(roundingDirection.negate(sign), super.dw0)
                    if (!roundUp)
                        return
                    super.coeffMutateIncrement()
                    if (super.digitLen <= PRECISION_34)
                        return
                    assert(super.digitLen == 35)
                    // if we rolled into another digit because of roundup
                    // then the result is EXACTly divisible by 10
                    val residueExact = CoeffScalePow10.coeffScaleDownPow10(this, this, 1)
                    assert(residueExact == Residue.EXACT)
                    ++qExp
                    assert(qExp + (super.digitLen - 1) == sciExp + 1)
                    ++sciExp
                    if (sciExp <= SCIENTIFIC_EXP_MAX)
                        return
                    // rounding caused overflow
                    // fall into next conditional and flow over
                }

                // 1) Overflow => +/- Infinity
                if (sciExp > SCIENTIFIC_EXP_MAX) {
                    // overflow IEEE754-2008 7.4 Overflow page 37
                    if (roundingDirection.overflowsToInfinity(sign)) {
                        super.coeffSetZero()
                        qExp = NON_FINITE_INF
                    } else {
                        magSetMaxFinite()
                    }
                    ctx.setOverflow()
                    ctx.setInexact()
                    return
                }

                // 7.5.1: subnormal rounding (tiny result stays nonzero)
                val qMin = Q_EXP_TINY - super.digitLen           // threshold for subnormal cohort
                val overlap = qExp - qMin
                if (overlap >= 0) {
                    val excess2 = super.digitLen - overlap
                    val scaleResidue = CoeffScalePow10.coeffScaleDownPow10(this, this, excess2)
                    qExp += excess2
                    assert(super.digitLen <= 34)
                    assert(sciExp == qExp + (super.digitLen - 1))

                    val totalResidue = scaleResidue.merge(inboundResidue)
                    if (totalResidue == EXACT)
                        return
                    ctx.setInexact()
                    val roundUp = totalResidue.ulpRoundUp(roundingDirection.negate(sign), super.dw0)
                    if (!roundUp)
                        return
                    super.coeffMutateIncrement()
                    if (super.digitLen <= PRECISION_34)
                        return
                    assert(super.digitLen == 35)
                    // if we rolled into another digit because of roundup
                    // then the result is definitely divisible by 10
                    val residueExact = CoeffScalePow10.coeffScaleDownPow10(this, this, 1)
                    assert(residueExact == Residue.EXACT)
                    ++qExp
                    return
                }

                // underflow to zero
                super.coeffSetZero()
                qExp = Q_EXP_TINY
                ctx.setInexact()
                return
            }
            // zero case
            qExp = Math.max(Math.min(qExp, Q_EXP_MAX), Q_EXP_TINY)
        }
    }


    fun magSetZero() {
        qExp = 0
        super.coeffSetZero()
    }

    fun magSetMaxFinite() {
        qExp = Q_EXP_MAX
        // 0x378D8E6400000000uL.toLong(), 0x0001ED09BEAD87C0uL.toLong(),
        // 10000000000000000000000000000000000 (10**34)
        super.coeffSet128(DW1_34_NINES, DW0_34_NINES)
    }

    fun magIsMaxFinite(): Boolean {
        return qExp == Q_EXP_MAX &&
                bitLen == BITLEN_34_NINES &&
                dw1 == DW1_34_NINES &&
                dw0 == DW0_34_NINES
    }

    fun magSetMinFinite() {
        qExp = Q_EXP_TINY
        super.coeffSetOne()
    }

    fun magIsMinFinite(): Boolean {
        return qExp == Q_EXP_TINY && bitLen == 1
    }


    fun magSet(dw0: Long) = magSet(0, dw0)

    fun magSet(exponent: Int, dw0: Long) {
        assert(exponent in Q_EXP_TINY..SCIENTIFIC_EXP_MAX)
        qExp = exponent
        super.coeffSet64(dw0)
    }

    fun magSet(exponent: Int, bi: BigInteger) =
        magSet(exponent, bi, Decimal128Context())

    fun magSet(exponent: Int, bi: BigInteger, ctx: Decimal128Context) {
        super.coeffSet(bi)
        qExp = exponent
        roundAndFinalize(EXACT, bi.signum() shr 31, ctx)
    }

    fun magSet(bd: BigDecimal) {
        magSet(bd, Decimal128Context())
    }

    fun magSet(bd: BigDecimal, ctx: Decimal128Context) {
        magSet(-bd.scale(), bd.unscaledValue(), ctx)
    }

    fun magSet(x:Mag) {
        qExp = x.qExp
        super.coeffSet(x)
    }

    fun magSet(str: String) = magSet(BigDecimal(str))

    fun magAdd(a: Mag, b: Mag, sign: Int, ctx: Decimal128Context) {
        val residue = MagAddSub.magAdd(this, a, b)
        roundAndFinalize(residue, sign, ctx)
    }

    fun magSub(a: Mag, b: Mag, sign: Int, ctx: Decimal128Context) {
        assert(a.magCompareTo(b) >= 0)
        val residue = MagAddSub.magSub(this, a, b)
        roundAndFinalize(residue, sign, ctx)
        val z = this
        val zDigitLen = z.digitLen
        val zExp = z.qExp
        val final = this
    }

    fun magMul(x: Mag, y: Mag, sign: Int, ctx: Decimal128Context) {
        MagMul.magMul(this, x, y)
        roundAndFinalize(EXACT, sign, ctx)
    }

    fun magScaleB(x: Mag, e: Int, sign: Int, ctx: Decimal128Context) {
        super.coeffSet(x)
        qExp = e
        roundAndFinalize(Residue.EXACT, sign, ctx)
    }

    fun magCompareTo(other: Mag) : Int {
        val thisIsZero = coeffIsZero()
        val otherIsZero = other.coeffIsZero()
        val eitherIsZero = thisIsZero or otherIsZero
        when {
            !eitherIsZero -> {
                val cmpExpSci = this.sciExp().compareTo(other.sciExp())
                if (cmpExpSci != 0)
                    return cmpExpSci
                val expDelta = this.qExp - other.qExp
                val ret = when {
                    expDelta == 0 -> coeffUnscaledCompareTo(other)
                    expDelta > 0 -> -other.coeffScaledCompareTo(this, expDelta)
                    else -> coeffScaledCompareTo(other, -expDelta)
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
        val thisIsZero = this.coeffIsZero()
        val otherIsZero = other.coeffIsZero()
        val bothAreZero = thisIsZero and otherIsZero
        val eitherIsZero = thisIsZero or otherIsZero
        if (this.sciExp() != other.sciExp())
            return bothAreZero
        if (! eitherIsZero) {
            val expDelta = this.qExp - other.qExp
            return when {
                expDelta == 0 -> this.coeffUnscaledEQ(other)
                expDelta > 0 -> other.coeffScaledEQ(this, expDelta)
                else -> this.coeffScaledEQ(other, -expDelta)
            }
        }
        return bothAreZero
    }

    internal fun coeffRoundToIntegral(x: Mag, sign: Int, rd: RoundingDirection, ctx: Decimal128Context) {
        if (qExp < 0) {
            val residue = this.coeffScaleDownPow10(x, -qExp)
            qExp = 0
            roundAndFinalize(residue, sign, rd, ctx)
        } else {
            magSet(x)
        }
    }

    fun magMutateNextDown() {
        coeffMutateDecrement()
        if (coeffIsZero()) {
            coeffSet64(9L)
            --qExp
        }
    }

    override fun toString(): String {
        return when {
            (qExp < MIN_SPECIAL_VALUE) -> super.toString() + "E" + qExp
            qExp == NON_FINITE_INF -> "Inf"
            qExp == NON_FINITE_QNAN -> "NaN" + super.toNaNDiagnosticString()
            qExp == NON_FINITE_SNAN -> "sNaN" + super.toNaNDiagnosticString()
            else -> "?que? $qExp"
        }
    }

}