package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.max
import kotlin.math.min

const val MIN_SPECIAL_VALUE = 1000000000
const val NON_FINITE_INF = 1000000000
const val NON_FINITE_QNAN = 1000000001
const val NON_FINITE_SNAN = 1000000002

const val CAPPED_EXP_MIN = -25000
const val CAPPED_EXP_MAX = 25000

@Suppress("NOTHING_TO_INLINE")
inline fun capExponentRange(e: Int): Int {
    return min(max(e, CAPPED_EXP_MIN), CAPPED_EXP_MAX)
}

open class Mag(/* exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long */) : Coeff() {
    var qExp = 0
    constructor(exp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long): this() {
        super.coeffSet256(dw3, dw2, dw1, dw0)
        this.qExp = exp
    }

    fun sciExp() = qExp + (digitLen - 1)

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

    fun magSetMinFinite(ctx: DecimalContext) {
        qExp = ctx.qTiny
        super.coeffSetOne()
    }

    fun magSet(exponent: Int, bi: BigInteger, ctx: DecimalContext) {
        coeffSet(bi)
        qExp = capExponentRange(exponent)
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

    fun magAdd(a: Mag, b: Mag, sign: Int, ctx: DecimalContext): Residue {
        val residue = MagAddSub.magAdd(this, a, b)
        return residue
    }

    fun magSub(a: Mag, b: Mag, sign: Int, ctx: DecimalContext): Residue {
        assert(a.magCompareTo(b) >= 0)
        val residue = MagAddSub.magSub(this, a, b)
        return residue
    }

    fun magMul(x: Mag, y: Mag, sign: Int, ctx: DecimalContext) {
        MagMul.magMul(this, x, y)
    }

    fun magSqr(x: Mag, ctx: DecimalContext) {
        MagMul.magSqr(this, x)
    }

    fun magDiv(x: Mag, y: Mag, sign: Int, ctx: DecimalContext): Residue {
        val residue = MagDiv.magDiv(this, x, y)
        return residue
    }

    fun magMutateScaleUpPow10(pow10: Int, sign: Int, ctx: DecimalContext): Residue {
        val headroom = PRECISION_34 - digitLen
        val scaleUp = min(headroom, pow10)
        this.coeffSetScaleUpPow10(this, scaleUp)
        qExp += pow10 - scaleUp
        return Residue.EXACT
    }

    fun magMutateScaleDownPow10(pow10: Int, sign: Int, ctx: DecimalContext): Residue {
        var residue = Residue.EXACT
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
        return residue
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