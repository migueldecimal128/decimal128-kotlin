package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import java.math.BigDecimal
import java.math.BigInteger

//const val SCIENTIFIC_EXP_MAX = 6144
//const val Q_EXP_MAX = 6111 // 6144 - (PRECISION_34 - 1)
//const val SCIENTIFIC_EXP_MIN = -6143
//const val TINY_EXPONENT = MIN_SCIENTIFIC_EXPONENT - (PRECISION_34 - 1) // -6176
//const val Q_EXP_TINY = -6176 // EXP_SCIENTIFIC_MIN - (PRECISION_34 - 1)

const val MIN_SPECIAL_VALUE = 1000000000
const val NON_FINITE_INF = 1000000000
const val NON_FINITE_QNAN = 1000000001
const val NON_FINITE_SNAN = 1000000002

const val CAPPED_EXP_MIN = -25000
const val CAPPED_EXP_MAX = 25000

open class Mag(/* exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long */) : Coeff() {
    var qExp = 0
    constructor(exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long): this() {
        super.coeffSet256(dw3, dw2, dw1, dw0)
        this.qExp = exp
    }

    constructor(exponent: Int, bi: BigInteger, ctx: DecimalContext): this() {
        super.coeffSet(bi)
        qExp = exponent
        roundAndFinalize(EXACT, bi.signum() shr 31, ctx)
    }

    constructor(bd: BigDecimal, ctx: DecimalContext) : this(-bd.scale(), bd.unscaledValue().abs(), ctx)

    constructor(bd: BigDecimal) : this(bd, DecimalContext.newDecimal128Context())

    fun sciExp() = qExp + (digitLen - 1)

    fun roundAndFinalize(inboundResidue: Residue, sign: Int, ctx: DecimalContext) =
        roundAndFinalize(inboundResidue, sign, ctx.roundingDirection, ctx)

    fun roundAndFinalize(inboundResidue: Residue, sign: Int, roundingDirection: RoundingDirection, ctx: DecimalContext) {
        val eMax = ctx.eMax
        val eMin = ctx.eMin
        val precision = ctx.precision
        if (qExp < NON_FINITE_INF) {
            if (bitLen != 0) {
                var eExp = qExp + (digitLen - 1)
                // IEEE754-2008 7.5: detect tininess on the unrounded result
                if (eExp < eMin) {
                    ctx.setUnderflow() // IEEE754-2008 7.5 Underflow page 38
                }

                val excess = Math.max(0, digitLen - precision)
                val myQTiny = ctx.qTiny - excess      // threshold for normalized

                // 2) Normalized result: round only if bd has >34 digits
                if (eExp <= eMax && qExp >= myQTiny) {
                    val totalResidue =
                        if (excess == 0) {
                            inboundResidue
                        } else {
                            val roundingResidue = CoeffScalePow10.coeffScaleDownPow10(this, this, excess)
                            qExp += excess
                            assert(digitLen == precision)
                            assert(eExp == qExp + (digitLen - 1))
                            roundingResidue.merge(inboundResidue)
                        }

                    if (totalResidue == EXACT)
                        return

                    ctx.setInexact()
                    val roundUp = totalResidue.ulpRoundUp(roundingDirection.negate(sign), super.dw0)
                    if (!roundUp)
                        return
                    super.coeffMutateIncrement()
                    if (digitLen <= precision)
                        return
                    assert(digitLen == precision + 1)
                    // if we rolled into another digit because of roundup
                    // then the result is EXACTly divisible by 10
                    val residueExact = CoeffScalePow10.coeffScaleDownPow10(this, this, 1)
                    assert(residueExact == Residue.EXACT)
                    ++qExp
                    assert(qExp + (digitLen - 1) == eExp + 1)
                    ++eExp
                    if (eExp <= eMax)
                        return
                    // rounding caused overflow
                    // fall into next conditional and flow over
                }

                // 1) Overflow => +/- Infinity
                if (eExp > eMax) {
                    // overflow IEEE754-2008 7.4 Overflow page 37
                    if (roundingDirection.overflowsToInfinity(sign)) {
                        super.coeffSetOne()
                        qExp = NON_FINITE_INF
                    } else {
                        magSetMaxFinite(ctx)
                    }
                    ctx.setOverflow()
                    ctx.setInexact()
                    return
                }

                // 7.5.1: subnormal rounding (tiny result stays nonzero)
                val myQMin = ctx.qTiny - digitLen           // threshold for subnormal cohort
                val overlap = qExp - myQMin
                if (overlap >= 0) {
                    val excess2 = super.digitLen - overlap
                    val scaleResidue = CoeffScalePow10.coeffScaleDownPow10(this, this, excess2)
                    qExp += excess2
                    assert(digitLen <= precision)
                    assert(eExp == qExp + (digitLen - 1))

                    val totalResidue = scaleResidue.merge(inboundResidue)
                    if (totalResidue == EXACT)
                        return
                    ctx.setInexact()
                    val roundUp = totalResidue.ulpRoundUp(roundingDirection.negate(sign), super.dw0)
                    if (!roundUp)
                        return
                    super.coeffMutateIncrement()
                    if (digitLen <= precision)
                        return
                    assert(digitLen == precision + 1)
                    // if we rolled into another digit because of roundup
                    // then the result is definitely divisible by 10
                    val residueExact = CoeffScalePow10.coeffScaleDownPow10(this, this, 1)
                    assert(residueExact == Residue.EXACT)
                    ++qExp
                    return
                }

                // underflow ... swamped non-zero value
                if (roundingDirection.underflowsToZero(sign)) {
                    super.coeffSetZero()
                    qExp = NON_FINITE_INF
                } else {
                    magSetMinFinite(ctx)
                }
                qExp = ctx.qTiny
                ctx.setUnderflow()
                ctx.setInexact()
                return
            }
            // zero case
            qExp = Math.max(Math.min(qExp, ctx.qMax), ctx.qTiny)
        }
    }


    fun magSetZero() {
        qExp = 0
        super.coeffSetZero()
    }

    fun magSetMaxFinite(ctx: DecimalContext) {
        qExp = ctx.qMax
        // 0x378D8E6400000000uL.toLong(), 0x0001ED09BEAD87C0uL.toLong(),
        // 10000000000000000000000000000000000 (10**34)
        val offset = CoeffPow10.pow10Offset(ctx.precision)
        if (ctx.precision < MIN_POW10_DIGIT_LEN_128) {
            super.coeffSet64(POW10[offset] - 1)
        } else if (ctx.precision < MIN_POW10_DIGIT_LEN_192) {
            super.coeffSet128(POW10[offset + 1], POW10[offset] - 1)
        } else
            throw IllegalArgumentException()
    }

    fun magIsMaxFinite(ctx: DecimalContext): Boolean {
        if (qExp < ctx.qMax)
            return false
        return coeffIsAllNines(ctx.precision)
    }

    fun magSetMinFinite(ctx: DecimalContext) {
        qExp = ctx.qTiny
        super.coeffSetOne()
    }

    fun magIsMinFinite(ctx: DecimalContext): Boolean {
        return qExp == ctx.qTiny && bitLen == 1
    }


    fun magSet(dw0: Long) = magSet(0, dw0)

    fun magSet(exponent: Int, dw0: Long) {
        qExp = exponent
        super.coeffSet64(dw0)
    }

    fun magSet(exponent: Int, bi: BigInteger, ctx: DecimalContext) {
        super.coeffSet(bi)
        qExp = exponent
        roundAndFinalize(EXACT, bi.signum() shr 31, ctx)
    }

    fun magSet(bd: BigDecimal) = magSet(bd, DecimalContext.newDecimal128Context())

    fun magSet(bd: BigDecimal, ctx: DecimalContext) {
        magSet(-bd.scale(), bd.unscaledValue().abs(), ctx)
    }

    fun magSet(x:Mag) {
        qExp = x.qExp
        super.coeffSet(x)
    }

    fun magSet(str: String) = magSet(BigDecimal(str), DecimalContext.newDecimal128Context())

    fun magAdd(a: Mag, b: Mag, sign: Int, ctx: DecimalContext) {
        val residue = MagAddSub.magAdd(this, a, b)
        roundAndFinalize(residue, sign, ctx)
    }

    fun magSub(a: Mag, b: Mag, sign: Int, ctx: DecimalContext) {
        assert(a.magCompareTo(b) >= 0)
        val residue = MagAddSub.magSub(this, a, b)
        roundAndFinalize(residue, sign, ctx)
    }

    fun magMul(x: Mag, y: Mag, sign: Int, ctx: DecimalContext) {
        MagMul.magMul(this, x, y)
        roundAndFinalize(EXACT, sign, ctx)
    }

    fun magSqr(x: Mag, ctx: DecimalContext) {
        MagMul.magSqr(this, x)
        roundAndFinalize(EXACT, 0, ctx)
    }

    fun magDiv(x: Mag, y: Mag, sign: Int, ctx: DecimalContext) {
        val residue = MagDiv.magDiv(this, x, y)
        roundAndFinalize(residue, sign, ctx)
    }

    fun magMutateScalePow10(pow10: Int, sign: Int, ctx: DecimalContext) {
        if (pow10 > 0)
            magMutateScaleUpPow10(pow10, sign, ctx)
        else if (pow10 < 0)
            magMutateScaleDownPow10(-pow10, sign, ctx)
    }

    fun magMutateScaleUpPow10(pow10: Int, sign: Int, ctx: DecimalContext) {
        val headroom = PRECISION_34 - digitLen
        val scaleUp = Math.min(headroom, pow10)
        val residue = this.coeffSetScaleUpPow10(this, scaleUp)
        qExp += pow10 - scaleUp
        if (qExp > ctx.qMax)
            roundAndFinalize(Residue.EXACT, sign, ctx)
    }

    fun magMutateScaleDownPow10(pow10: Int, sign: Int, ctx: DecimalContext) {
        val residue: Residue
        if (! coeffIsZero()) {
            if (pow10 >= digitLen) {
                residue = (
                        if (pow10 == digitLen)
                            Residue.residueFrom(this)
                        else
                            Residue.LT_HALF
                        )
                coeffSetZero()
            }
        } else {
            residue = this.coeffSetScaleDownPow10(this, pow10)
        }
        qExp -= pow10
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

    internal fun coeffRoundToIntegral(x: Mag, sign: Int, rd: RoundingDirection, ctx: DecimalContext) {
        if (qExp < 0) {
            val residue = this.coeffSetScaleDownPow10(x, -qExp)
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
            qExp == NON_FINITE_QNAN -> "NaN" + super.coeffToNaNDiagnosticString()
            qExp == NON_FINITE_SNAN -> "sNaN" + super.coeffToNaNDiagnosticString()
            else -> "?que? $qExp"
        }
    }

}