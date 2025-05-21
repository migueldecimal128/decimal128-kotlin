package com.decimal128

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

fun bdToIeeeDecimal128(bd: BigDecimal, rm: RoundingMode): BigDecimal {
    if (bd.signum() == 0) {
        val q = bd.scale()
        val boundedQ = Math.max(Math.min(q, 6176), -6111)
        val boundedZero = bd.setScale(boundedQ)
        return boundedZero
    }
    // Decimal128 constants
    val p34       = MathContext.DECIMAL128.precision  // 34
    val EMIN      = -6143
    val EMAX      =  6144
    val ETINY     = EMIN - (p34 - 1)            // -6176

    // Raw exponent and digit count
    val sign = bd.signum() < 0
    val q       = -bd.scale()
    val p       = bd.precision()
    var e       = q + p - 1
    val excess  = Math.max(0, p - p34)
    val qTiny   = ETINY - excess                      // threshold for normalized
    val qMin    = ETINY - p                           // threshold for subnormal cohort

    // 2) Normalized result: round only if bd has >34 digits
    if (e <= EMAX && q >= qTiny) {
        if (excess == 0)
            return bd
        val rounded = bd.round(MathContext(p34, rm))
        val qRounded = -rounded.scale()
        val pRounded = rounded.precision()
        assert(pRounded == 34)
        e = qRounded + 34 - 1
        if (e <= EMAX)
            return rounded
        // rounding caused overflow
        // fall into next conditional
    }
    // 1) Overflow ⇒ ±Infinity
    if (e > EMAX) {
        if (overflowsToInfinity(rm, sign))
            return BigDecimal.ZERO.scaleByPowerOfTen(1000000)
        else {
            val maxFinite = BigDecimal.ONE.
            scaleByPowerOfTen(34).subtract(BigDecimal.ONE).scaleByPowerOfTen(6144-33)
            return if (sign) maxFinite.negate() else maxFinite
        }
    }

    // 3) Subnormal cohort: one rounding to ULP_sub = 10^ETINY
    if (q >= qMin) {
        val ulpSub = BigDecimal.ONE.scaleByPowerOfTen(ETINY)
        val k      = bd.divide(ulpSub, 0, rm)         // single rounding here
        val result = k.scaleByPowerOfTen(ETINY)
        return if (bd.signum() < 0) result.negate() else result
    }

    // 4) Underflow to zero
    val zeroTiny = BigDecimal.ZERO.scaleByPowerOfTen(ETINY)
    return if (bd.signum() < 0) zeroTiny.negate() else zeroTiny
}

private fun overflowsToInfinity(rm: RoundingMode, sign: Boolean): Boolean {
    val toInfinity = when (rm) {
        RoundingMode.HALF_EVEN -> true
        RoundingMode.HALF_UP -> true
        RoundingMode.DOWN -> false
        RoundingMode.CEILING -> ! sign
        RoundingMode.FLOOR -> sign
        else -> throw RuntimeException("unrecognized RoundingMode:$rm")
    }
    return toInfinity
}

fun bdIsFinite(bd: BigDecimal) : Boolean {
    val eExp = -bd.scale()
    return eExp >= EXP_SCIENTIFIC_MIN && eExp <= EXP_SCIENTIFIC_MAX
}

