package com.decimal128

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import java.math.BigDecimal

open class Finite() : Mag() {
    var sign = 0

    constructor(sign: Boolean, qExp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) : this() {
        this.coeffSet256(dw3, dw2, dw1, dw0)
        this.qExp = qExp
        this.sign = if (sign) 1 else 0
    }

    constructor(bd: BigDecimal): this() {
        this.magSet(bd)
        this.sign = bd.signum() shr 31
    }

    constructor(str: String): this(BigDecimal(str))

    fun set(bd: BigDecimal) {
        magSet(bd)
        this.sign = bd.signum() shr 31
    }

    fun set(x: Finite) {
        magSet(x)
        this.sign = x.sign
    }

    private fun setNaN(x: Finite, y: Finite, ctx: Decimal128Context) {
        val xQ = x.qExp
        val yQ = y.qExp
        val maxQ = Math.max(xQ, yQ)
        assert(maxQ >= NON_FINITE_QNAN)
        magSetZero()
        qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    private fun setInfinite(x: Finite, y: Finite, ctx: Decimal128Context) {
        val xQ = x.qExp
        val yQ = y.qExp
        val xSign = x.sign
        val maxQ = Math.max(xQ, yQ)
        set(if (xQ == maxQ) x else y)
        qExp = NON_FINITE_INF
    }

    fun add(x: Finite, y: Finite, ctx: Decimal128Context) {
        val xQ = x.qExp
        val yQ = y.qExp
        val xSign = x.sign
        val maxQ = Math.max(xQ, yQ)
        when {
            maxQ == NON_FINITE_INF -> setInfinite(x, y, ctx)
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

    fun sub(x: Finite, y: Finite, ctx: Decimal128Context) {
        val xQ = x.qExp
        val yQ = y.qExp
        val xSign = x.sign
        val maxQ = Math.max(xQ, yQ)
        when {
            maxQ == NON_FINITE_INF -> setInfinite(x, y, ctx)
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

    fun mul(x: Finite, y: Finite, ctx: Decimal128Context) {
        val xQ = x.qExp
        val yQ = y.qExp
        val resultSign = x.sign xor y.sign
        val maxQ = Math.max(xQ, yQ)
        when {
            maxQ == NON_FINITE_INF -> setInfinite(x, y, ctx)
            maxQ >= NON_FINITE_QNAN -> setNaN(x, y, ctx)
            else -> {
                this.magMul(x, y, resultSign, ctx)
                this.sign = resultSign
            }
        }
    }

    fun compareTo(x: Finite, ctx: Decimal128Context) : Int {
        if (sign != x.sign)
            return 1 - (sign shl 1)
        val cmp = magCompareTo(x)
        val ret = (cmp xor -sign) + sign
        return ret
    }

    fun roundToIntegral(x: Finite, rd: RoundingDirection, ctx: Decimal128Context) {
        //FIXME - deal with special values
        sign = x.sign
        coeffRoundToIntegral(x, sign, rd, ctx)
    }

    fun roundToIntegralTiesToEven(x: Finite, ctx: Decimal128Context) =
        roundToIntegral(x, ROUND_TIES_TO_EVEN, ctx)

    fun roundToIntegralTiesToAway(x: Finite, ctx: Decimal128Context) =
        roundToIntegral(x, ROUND_TIES_TO_AWAY, ctx)

    fun roundToIntegralTowardZero(x: Finite, ctx: Decimal128Context) =
        roundToIntegral(x, ROUND_TOWARD_ZERO, ctx)

    fun roundToIntegralTowardPositive(x: Finite, ctx: Decimal128Context) =
        roundToIntegral(x, ROUND_TOWARD_POSITIVE, ctx)

    fun roundToIntegralTowardNegative(x: Finite, ctx: Decimal128Context) =
        roundToIntegral(x, ROUND_TOWARD_NEGATIVE, ctx)

}
