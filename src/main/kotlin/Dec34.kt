package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import java.math.BigDecimal

open class Dec34() : Mag() {
    var sign = 0

    constructor(sign: Boolean, qExp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) : this() {
        this.coeffSet256(dw3, dw2, dw1, dw0)
        this.qExp = qExp
        this.sign = if (sign) 1 else 0
    }

    constructor(bd: BigDecimal): this() {
        this.magSet(bd.abs())
        this.sign = bd.signum() ushr 31
    }

    constructor(str: String): this(BigDecimal(str))

    fun set(bd: BigDecimal) {
        magSet(bd.abs())
        this.sign = bd.signum() shr 31
    }

    fun set(x: Dec34) {
        magSet(x)
        this.sign = x.sign
    }

    fun setZero() {
        magSetZero()
        this.sign = 0
    }

    private fun setNaN(x: Dec34, y: Dec34, ctx: Decimal128Context) {
        val xQ = x.qExp
        val yQ = y.qExp
        val maxQ = Math.max(xQ, yQ)
        assert(maxQ >= NON_FINITE_QNAN)
        magSetZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    private fun sNaNOperand() {
        throw RuntimeException("sNaN operand")
    }

    private fun setNaN(x: Dec34, ctx: Decimal128Context) {
        val q = x.qExp
        assert(q >= NON_FINITE_QNAN)
        magSetZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    fun setNaN(ctx: Decimal128Context) {
        magSetZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    fun setNaN(payload: Int, ctx: Decimal128Context) {
        sign = 0
        coeffSet64(payload.toLong())
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    private fun setInfinite(sign: Int) {
        this.coeffSetOne()
        this.qExp = NON_FINITE_INF
        this.sign = sign
    }

    fun isInfinite() : Boolean {
        return qExp == NON_FINITE_INF
    }

    fun isNegative() : Boolean {
        return sign == 1
    }

    fun isFinite() : Boolean {
        return qExp < NON_FINITE_INF
    }

    fun isNumber() : Boolean {
        return qExp <= NON_FINITE_INF
    }

    // IEEE754-2008 5.4.1
    fun add(x: Dec34, y: Dec34, ctx: Decimal128Context) {
        val xQ = x.qExp
        val yQ = y.qExp
        val xSign = x.sign
        val ySign = y.sign
        val maxQ = Math.max(xQ, yQ)
        val minQ = Math.min(xQ, yQ)
        when {
            maxQ == NON_FINITE_INF -> when {
                (xSign == ySign) -> {
                    setInfinite(xSign)
                    ctx.setInexact(minQ != NON_FINITE_INF)
                }
                minQ == NON_FINITE_INF -> setNaN(ctx)
                xQ == NON_FINITE_INF -> {
                    setInfinite(xSign)
                    ctx.setInexact()
                }
                else -> {
                    setInfinite(ySign)
                    ctx.setInexact()
                }
            }
            maxQ >= NON_FINITE_QNAN -> setNaN(x, y, ctx)
            (xSign xor y.sign) == 0 -> {
                this.magAdd(x, y, xSign, ctx)
                this.sign = xSign
            }
            (x.magCompareTo(y) >= 0) -> {
                this.magSub(x, y, xSign, ctx)
                this.sign = xSign
            }
            else -> {
                this.magSub(y, x, y.sign, ctx)
                this.sign = y.sign
            }
        }
    }

    // IEEE754-2008 5.4.1
    fun sub(x: Dec34, y: Dec34, ctx: Decimal128Context) {
        val qX = x.qExp
        val qY = y.qExp
        val xSign = x.sign
        val ySign = y.sign
        val maxQ = Math.max(qX, qY)
        val minQ = Math.min(qX, qY)
        when {
            maxQ == NON_FINITE_INF -> {
                if (qX == NON_FINITE_INF) {
                    if (qY != NON_FINITE_INF || xSign != ySign) {
                        setInfinite(xSign)
                        ctx.setInexact(qY != NON_FINITE_INF)
                    } else {
                        setNaN(ctx)
                    }
                } else {
                    setInfinite(ySign xor 1)
                }
            }
            maxQ >= NON_FINITE_QNAN -> setNaN(x, y, ctx)
            (xSign xor y.sign) == 1 -> {
                this.magAdd(x, y, xSign, ctx)
                this.sign = xSign
            }
            (x.magCompareTo(y) >= 0) -> {
                this.magSub(x, y, xSign, ctx)
                this.sign = xSign
            }
            else -> {
                this.magSub(y, x, y.sign, ctx)
                this.sign = y.sign
            }
        }
    }

    // IEEE754-2008 5.4.1
    fun mul(x: Dec34, y: Dec34, ctx: Decimal128Context) {
        val qX = x.qExp
        val qY = y.qExp
        val productSign = x.sign xor y.sign
        val qMaxXY = Math.max(qX, qY)
        when {
            qMaxXY < NON_FINITE_INF -> {
                this.magMul(x, y, productSign, ctx)
                this.sign = productSign
            }
            qMaxXY == NON_FINITE_INF -> {
                if (x.coeffIsZero() || y.coeffIsZero()) {
                    setNaN(ctx)
                } else {
                    setInfinite(productSign)
                }
            }
            else -> setNaN(x, y, ctx)
        }
    }

    // IEEE754-2008 5.4.1
    fun fma(x: Dec34, y: Dec34, a: Dec34, ctx: Decimal128Context) {
        val qX = x.qExp
        val qY = y.qExp
        val qMaxXY = Math.max(qX, qY)
        val qA = a.qExp
        val productSign = x.sign xor y.sign
        if (a.sign == productSign) {
            val qMaxXYA = Math.max(qMaxXY, qA)
            when {
                qMaxXYA < NON_FINITE_INF -> {
                    this.magFma(x, y, a, productSign, ctx)
                    this.sign = productSign
                }
                qMaxXY == NON_FINITE_INF -> {
                    if (x.coeffIsZero() || y.coeffIsZero()) {
                        setNaN(ctx)
                    } else {
                        setInfinite(productSign)
                    }
                }
                else -> setNaN(x, y, ctx)
            }
        }
    }



    fun compareTo(x: Dec34, ctx: Decimal128Context) : Int {
        val qMax = Math.max(qExp, x.qExp)
        when {
            (qMax < NON_FINITE_INF) -> {
                if (coeffIsZero()) {
                    if (x.coeffIsZero())
                        return 0
                    else
                        return (x.sign shl 1) - 1
                }
                if (x.coeffIsZero()) {
                    return 1 - (sign shl 1)
                }
                if (sign != x.sign)
                    return 1 - (sign shl 1)
                val cmp = magCompareTo(x)
                val ret = (cmp xor -sign) + sign
                return ret
            }

            (qMax == NON_FINITE_INF) -> when {
                (sign != x.sign) -> {
                    return 1 - (sign shl 1)
                }

                (qExp == NON_FINITE_INF) -> {
                    if (x.qExp == NON_FINITE_INF)
                        return 0
                    return 1 - (sign shl 1)
                }

                else -> {
                    return (sign shl 1) - 1
                }
            }
            else -> throw RuntimeException("somebody is a NaN")
        }
    }

    fun roundToIntegral(x: Dec34, rd: RoundingDirection, ctx: Decimal128Context) {
        //FIXME - deal with special values
        sign = x.sign
        coeffRoundToIntegral(x, sign, rd, ctx)
    }

    fun roundToIntegralTiesToEven(x: Dec34, ctx: Decimal128Context) =
        roundToIntegral(x, ROUND_TIES_TO_EVEN, ctx)

    fun roundToIntegralTiesToAway(x: Dec34, ctx: Decimal128Context) =
        roundToIntegral(x, ROUND_TIES_TO_AWAY, ctx)

    fun roundToIntegralTowardZero(x: Dec34, ctx: Decimal128Context) =
        roundToIntegral(x, ROUND_TOWARD_ZERO, ctx)

    fun roundToIntegralTowardPositive(x: Dec34, ctx: Decimal128Context) =
        roundToIntegral(x, ROUND_TOWARD_POSITIVE, ctx)

    fun roundToIntegralTowardNegative(x: Dec34, ctx: Decimal128Context) =
        roundToIntegral(x, ROUND_TOWARD_NEGATIVE, ctx)

    fun setNextUp(x: Dec34, ctx: Decimal128Context) {
        set(x)
        when {
            (qExp < NON_FINITE_INF) -> {
                if (! coeffIsZero()) {
                    if (! coeffIs33Nines()) {
                        val headroom = PRECISION_34 - digitLen
                        this.coeffSetScaleUpPow10(this, headroom)
                        this.qExp -= headroom
                    }
                    if (sign == 0) {
                        coeffMutateIncrement()
                    } else {
                        coeffMutateDecrement()
                    }
                } else {
                    sign = 0
                    magSetMinFinite()
                }
            }
            (qExp == NON_FINITE_INF) -> {
                if (sign != 0)
                    magSetMaxFinite()
            }
            else -> {
                setNaN(ctx)
            }
        }
        roundAndFinalize(EXACT, sign, ROUND_TOWARD_POSITIVE, ctx)
    }

    fun setNextDown(x: Dec34, ctx: Decimal128Context) {
        set(x)
        when {
            (qExp < NON_FINITE_INF) -> {
                if (! coeffIsZero()) {
                    if (! coeffIs33Nines()) {
                        val headroom = PRECISION_34 - digitLen + 1
                        this.coeffSetScaleUpPow10(this, headroom)
                        this.qExp -= headroom
                    }
                    if ((sign xor 1) == 0) {
                        coeffMutateIncrement()
                    } else {
                        coeffMutateDecrement()
                    }
                } else {
                    sign = 1
                    magSetMinFinite()
                }
            }
            (qExp == NON_FINITE_INF) -> {
                if (sign != 1)
                    magSetMaxFinite()
            }
            else -> {
                setNaN(ctx)
            }
        }
        roundAndFinalize(EXACT, sign xor 1, ROUND_TOWARD_POSITIVE, ctx)
    }

    fun minNum(x: Dec34, y: Dec34, ctx: Decimal128Context) = minNum_helper(x, y, 0, ctx)
    fun maxNum(x: Dec34, y: Dec34, ctx: Decimal128Context) = minNum_helper(x, y, -1, ctx)

    private fun minNum_helper(x: Dec34, y: Dec34, invertCompareZeroOrNeg1: Int, ctx: Decimal128Context) {
        val qMax = Math.max(x.qExp, y.qExp)
        when {
            qMax <= NON_FINITE_INF -> {
                val cmp = (x.compareTo(y, ctx) xor invertCompareZeroOrNeg1) - invertCompareZeroOrNeg1
                set(if (cmp <= 0) x else y)
            }
            qMax == NON_FINITE_QNAN -> {
                set(if (x.qExp == NON_FINITE_QNAN) x else y)
            }
            else -> throw RuntimeException("somebody is a sNaN")
        }
    }

    fun minNumMag(x: Dec34, y: Dec34, ctx: Decimal128Context) = minNumMag_helper(x, y, 0, ctx)
    fun maxNumMag(x: Dec34, y: Dec34, ctx: Decimal128Context) = minNumMag_helper(x, y, -1, ctx)

    private fun minNumMag_helper(x: Dec34, y: Dec34, invertCompareZeroOrNeg1: Int, ctx: Decimal128Context) {
        val qMax = Math.max(x.qExp, y.qExp)
        when {
            qMax < NON_FINITE_INF -> {
                val cmp = (x.magCompareTo(y) xor invertCompareZeroOrNeg1) - invertCompareZeroOrNeg1
                set(if (cmp <= 0) x else y)
            }
            else -> minNum_helper(x, y, invertCompareZeroOrNeg1, ctx)
        }
    }

    fun setScale(x: Dec34, pow10: Int, ctx: Decimal128Context) {
        set(x)
        if (qExp < NON_FINITE_INF) {
            if (pow10 > 0)  //FIXME ... check range on pow10
                magMutateScaleUpPow10(pow10, sign, ctx)
            else if (pow10 < 0)
                magMutateScaleDownPow10(-pow10, sign, ctx)
        } else if (qExp == NON_FINITE_SNAN)
            sNaNOperand()
    }

    // IEEE754-2008 5.3.2
    fun quantize(x: Dec34, y: Dec34, ctx: Decimal128Context) {
        val targetQ = y.qExp
        setScale(x, -targetQ, ctx)
    }

    // IEEE754-2008 5.3.3
    fun scaleB(x: Dec34, pow10: Int, ctx: Decimal128Context) {
        set(x)
        when {
            qExp <= NON_FINITE_INF -> {
                qExp += pow10 //FIXME ... check range on pow10
                if (qExp > Q_EXP_MAX || qExp < Q_EXP_TINY)
                    roundAndFinalize(Residue.EXACT, sign, ctx)
            }
            x.qExp <= NON_FINITE_QNAN -> {}
            else -> sNaNOperand()
        }
    }

    // IEEE754-2008 5.3.3
    fun logB(): Int {
        return qExp
    }

    override fun equals(other: Any?) : Boolean {
        if (other is Dec34) {
            val qMax = Math.max(qExp, other.qExp)
            return when {
                qMax < NON_FINITE_INF -> when {
                    coeffIsZero() -> other.coeffIsZero()
                    other.coeffIsZero() -> false
                    else -> sign == other.sign && magEQ(other)
                }

                qMax == NON_FINITE_INF -> when {
                    qExp == NON_FINITE_INF && other.qExp == NON_FINITE_INF -> return sign == other.sign
                    else -> false
                }

                else -> throw RuntimeException("somebody is a NaN")
            }
        }
        return false
    }

    override fun toString() : String {
        return (if (sign == 0) '+' else '-') + super.toString()
    }

}
