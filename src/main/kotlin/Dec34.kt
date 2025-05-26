package com.decimal128

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_EVEN
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_NEGATIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import java.math.BigDecimal

class Dec34() {
    val mag = Mag()
    var sign = 0

    constructor(sign: Boolean, qExp: Int, dw3: Long, dw2: Long, dw1: Long, dw0: Long) : this() {
        mag.c.coeffSet256(dw3, dw2, dw1, dw0)
        mag.qExp = qExp
        this.sign = if (sign) 1 else 0
    }

    constructor(bd: BigDecimal): this() {
        mag.magSet(bd)
        this.sign = bd.signum() shr 31
    }

    constructor(str: String): this(BigDecimal(str))

    fun set(bd: BigDecimal) {
        mag.magSet(bd)
        this.sign = bd.signum() shr 31
    }

    fun set(x: Dec34) {
        mag.magSet(x.mag)
        this.sign = x.sign
    }

    private fun setNaN(x: Dec34, y: Dec34, ctx: Decimal128Context) {
        val xQ = x.mag.qExp
        val yQ = y.mag.qExp
        val maxQ = Math.max(xQ, yQ)
        assert(maxQ >= NON_FINITE_QNAN)
        mag.magSetZero()
        mag.qExp = NON_FINITE_QNAN
        //FIXME - see IEEE754r 6.2
    }

    private fun setInfinite(x: Dec34, y: Dec34, ctx: Decimal128Context) {
        val xQ = x.mag.qExp
        val yQ = y.mag.qExp
        val xSign = x.sign
        val maxQ = Math.max(xQ, yQ)
        set(if (xQ == maxQ) x else y)
        mag.qExp = NON_FINITE_INF
    }

    fun add(x: Dec34, y: Dec34, ctx: Decimal128Context) {
        val xQ = x.mag.qExp
        val yQ = y.mag.qExp
        val xSign = x.sign
        val maxQ = Math.max(xQ, yQ)
        when {
            maxQ == NON_FINITE_INF -> setInfinite(x, y, ctx)
            maxQ >= NON_FINITE_QNAN -> setNaN(x, y, ctx)
            (xSign xor y.sign) == 0 -> {
                this.mag.magAdd(x.mag, y.mag, xSign, ctx)
                this.sign = xSign
            }
            (x.mag.magCompareTo(y.mag) >= 0) -> {
                this.mag.magSub(x.mag, y.mag, xSign, ctx)
                this.sign = xSign
            }
            else -> {
                this.mag.magSub(y.mag, x.mag, y.sign, ctx)
                this.sign = y.sign
            }
        }
    }

    fun sub(x: Dec34, y: Dec34, ctx: Decimal128Context) {
        val xQ = x.mag.qExp
        val yQ = y.mag.qExp
        val xSign = x.sign
        val maxQ = Math.max(xQ, yQ)
        when {
            maxQ == NON_FINITE_INF -> setInfinite(x, y, ctx)
            maxQ >= NON_FINITE_QNAN -> setNaN(x, y, ctx)
            (xSign xor y.sign) == 1 -> {
                this.mag.magAdd(x.mag, y.mag, xSign, ctx)
                this.sign = xSign
            }
            (x.mag.magCompareTo(y.mag) >= 0) -> {
                this.mag.magSub(x.mag, y.mag, xSign, ctx)
                this.sign = xSign
            }
            else -> {
                this.mag.magSub(y.mag, x.mag, y.sign, ctx)
                this.sign = y.sign
            }
        }
    }

    fun mul(x: Dec34, y: Dec34, ctx: Decimal128Context) {
        val xQ = x.mag.qExp
        val yQ = y.mag.qExp
        val resultSign = x.sign xor y.sign
        val maxQ = Math.max(xQ, yQ)
        when {
            maxQ == NON_FINITE_INF -> setInfinite(x, y, ctx)
            maxQ >= NON_FINITE_QNAN -> setNaN(x, y, ctx)
            else -> {
                this.mag.magMul(x.mag, y.mag, resultSign, ctx)
                this.sign = resultSign
            }
        }
    }

    fun compareTo(x: Dec34, ctx: Decimal128Context) : Int {
        if (sign != x.sign)
            return 1 - (sign shl 1)
        val cmp = mag.magCompareTo(x.mag)
        val ret = (cmp xor -sign) + sign
        return ret
    }

    fun roundToIntegral(x: Dec34, rd: RoundingDirection, ctx: Decimal128Context) {
        //FIXME - deal with special values
        sign = x.sign
        mag.coeffRoundToIntegral(x.mag, sign, rd, ctx)
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

}
