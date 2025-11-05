package com.decimal128.decimal

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.lang.Math.max
import java.lang.Math.min

private const val P34   = 34
private const val EMIN  = -6143
private const val EMAX  =  6144
private const val QTINY = EMIN - (P34 - 1) // -6176
private const val QMAX  = EMAX - (P34 - 1) // 6111

const val INFINITY_SCALE = 16381
// Note: Cannot make the coefficient of INFINITY == ZERO because
// we need to represent NEG_INFINITY and BigDecimal does not
// support negative zero -0
private val POS_INFINITY_SURROGATE = BigDecimal.ONE.scaleByPowerOfTen(INFINITY_SCALE)
private val NEG_INFINITY_SURROGATE = POS_INFINITY_SURROGATE.negate()
private const val QNAN_SCALE = 16382
private const val SNAN_SCALE = 16383
private val MAX_FINITE =
    BigDecimal.ONE.scaleByPowerOfTen(34).subtract(BigDecimal.ONE).scaleByPowerOfTen(6144-33)
private val NEG_MAX_FINITE = MAX_FINITE.negate()
private val MIN_FINITE = BigDecimal.ONE.scaleByPowerOfTen(QTINY)
private val NEG_MIN_FINITE = MIN_FINITE.negate()
private val ZERO_TINY = BigDecimal.ZERO.scaleByPowerOfTen(QTINY)
private val NEG_ZERO_TINY = ZERO_TINY.negate()

fun strToBdIeeeDecimal128(str: String, rm: RoundingMode): BigDecimal {
    val noUnderscores = str.replace("_", "")
    return bdToIeeeDecimal128(BigDecimal(noUnderscores), rm)
}

fun bdToIeeeDecimal128(bd: BigDecimal, rm: RoundingMode): BigDecimal {
    val q = -bd.scale()
    when {
        q == INFINITY_SCALE -> return if (bd.signum() < 0) NEG_INFINITY_SURROGATE else POS_INFINITY_SURROGATE
        q in QNAN_SCALE..SNAN_SCALE -> return bd
    }
    if (bd.signum() == 0) {
        val boundedQ = max(min(q, 6111), -6176)
        val boundedZero = bd.setScale(-boundedQ)
        return boundedZero
    }
    // Decimal128 constants
    val p34       = MathContext.DECIMAL128.precision  // 34

    // Raw exponent and digit count
    val sign = bd.signum() < 0
    val p       = bd.precision()
    var e       = q + p - 1
    val excess  = max(0, p - p34)
    val qTiny   = QTINY - excess                      // threshold for normalized
    val qMin    = QTINY - p                           // threshold for subnormal cohort

    // 2) Normalized result: round only if bd has >34 digits
    if (q <= QMAX && q >= qTiny) {
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
    if (e <= EMAX && q > QMAX) {
        // clamp/fold-down case
        val qExcess = q - QMAX
        val headroom = P34 - p
        val oldScale = bd.scale()
        if (headroom >= qExcess) {
            val bdClamp = bd.setScale(-QMAX)
            return bdClamp
        }
    }
    // 1) Overflow ⇒ ±Infinity
    if (e > EMAX) {
        if (overflowsToInfinity(rm, sign))
            return if (sign) NEG_INFINITY_SURROGATE else POS_INFINITY_SURROGATE
        else {
            return if (sign) NEG_MAX_FINITE else MAX_FINITE
        }
    }

    // 3) Subnormal cohort: one rounding to ULP_sub = 10^ETINY
    if (q >= qMin) {
        val ulpSub = BigDecimal.ONE.scaleByPowerOfTen(QTINY)
        val k      = bd.divide(ulpSub, 0, rm)         // single rounding here
        val result = k.scaleByPowerOfTen(QTINY)
        return if (bd.signum() < 0) result.negate() else result
    }

    // 4) Underflow to zero
    if (underflowsToZero(rm, sign)) {
        return if (bd.signum() < 0) NEG_ZERO_TINY else ZERO_TINY
    } else {
        return if (bd.signum() < 0) NEG_MIN_FINITE else MIN_FINITE
    }
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

private fun underflowsToZero(rm: RoundingMode, sign: Boolean): Boolean {
    val toZero = when (rm) {
        RoundingMode.HALF_EVEN -> true
        RoundingMode.HALF_UP -> true
        RoundingMode.DOWN -> true
        RoundingMode.CEILING -> sign
        RoundingMode.FLOOR -> ! sign
        else -> throw RuntimeException("unrecognized RoundingMode:$rm")
    }
    return toZero
}

fun bdIsFinite(bd: BigDecimal) : Boolean {
    val eExp = -bd.scale()
    return eExp < MIN_SPECIAL_VALUE
}

fun bdToDecimal128String(bd: BigDecimal, toEngineeringExp: Boolean = false): String {
    val decimal128 = bdToIeeeDecimal128(bd, RoundingMode.HALF_EVEN)
    val q = -decimal128.scale()
    val isNeg = bd.signum() < 0
    val magnitude = decimal128.unscaledValue().abs()
    return when {
        q < NON_FINITE_INF && !toEngineeringExp -> decimal128.toString()
        q < NON_FINITE_INF -> decimal128.toEngineeringString()
        q == NON_FINITE_INF -> if (isNeg) "-Inf" else "Inf"
        q == NON_FINITE_QNAN -> (if (isNeg) "-NaN" else "NaN") + if (magnitude.bitLength() == 0) "" else magnitude
        q == NON_FINITE_SNAN -> (if (isNeg) "-sNaN" else "sNaN") + if (magnitude.bitLength() == 0) "" else magnitude
        else -> throw RuntimeException("invalid exponent for ieee754r decimal128")
    }
}

