package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.math.MathContext
import java.util.*

class TestMagSet {

    val verbose = true

    companion object {
        /**
         * Convert a BigDecimal to the nearest IEEE-754r decimal128 value,
         * with a single correctly-rounded operation per category.
         *
         * Range tests use raw bd.precision() and exponent q = -scale:
         *  - Overflow: q > EMAX
         *  - Normalized: q >= qTiny (ETINY - excessDigits)
         *  - Subnormal: q >= qMin (ETINY - (d - 1))
         *  - Underflow to zero: q < qMin
         *
         * @param bd the input value
         * @param rm the rounding mode to use
         * @return the nearest decimal128-compliant BigDecimal
         * @throws ArithmeticException on overflow
         */
        fun toIeeeDecimal128(bd: BigDecimal, rm: RoundingMode): BigDecimal {
            // Decimal128 constants
            val precision = MathContext.DECIMAL128.precision  // 34
            val EMIN      = -6143
            val EMAX      =  6144
            val ETINY     = EMIN - (precision - 1)            // -6176

            // Raw exponent and digit count
            val q       = -bd.scale()
            val d       = bd.precision()
            val excess  = Math.max(0, d - precision)
            val qTiny   = ETINY - excess                      // threshold for normalized
            val qMin    = ETINY - d                           // threshold for subnormal cohort

            // 1) Overflow ⇒ ±Infinity
            if (q + d - 1 > EMAX) {
                return BigDecimal.ONE.scaleByPowerOfTen(9999)
            }

            // 2) Normalized result: round only if bd has >34 digits
            if (q >= qTiny) {
                return if (excess == 0) bd else bd.round(MathContext(precision, rm))
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


   }

    class TC(val bdA: BigDecimal, val ctx: Decimal128Context) {
        constructor(str: String, ctx: Decimal128Context) : this(BigDecimal(str), ctx)
        constructor(str: String) : this(str, Decimal128Context())
        constructor(bdA: BigDecimal) : this(bdA, Decimal128Context())
        val biA = bdA.unscaledValue()
        val expA = -bdA.scale()
        val bdRounded = toIeeeDecimal128(bdA, ctx.getMathContext().roundingMode)
        val biRounded = bdRounded.unscaledValue()
        val expRounded = -bdRounded.scale()
    }

    val cases = arrayOf(
        TC("1111111111222222222233333333334444e-6210"), // 34 digits =>
        TC("11111111112222222222333333333344444e-6211"), // 35 digits =>
        TC("9999999999888888888877777777776666e-6210"), // 34 digits =>
        TC("99999999998888888888777777777766666e-6211"), // 35 digits =>

        TC("1111111111222222222233333333334444e-6176"), // 34 digits
        TC("11111111112222222222333333333344444e-6177"), // 35 digits .. rounding but normal

        TC("11e-6177"),
        TC("11e-6178"),
        TC("99e-6178"),
        TC("1111111111222222222233333333334444e-6209"), // 34 digits => 1e-6176
        TC("1111111111222222222233333333334444e-6210"), // 34 digits =>
        TC("11111111112222222222333333333344444e-6210"), // 35 digits => 1e-6176


        TC("1111111111222222222233333333334444e-6176"), // 34 digits
        TC("11111111112222222222333333333344444e-6177"), // 35 digits .. rounding but normal
        TC("1111111111222222222233333333334444444444e-6182"), // 40 digits .. rounding but normal
        TC("1111111111222222222233333333334444444444e-6182"), // 40 digits .

        TC("1e-6175"),
        TC("15e-6175"),
        TC("1e-6177"),

        TC("1e-6176"),
        TC("19e-6177"),
        TC("1e-6177"),
        TC("15e-6175"),
        TC("15e-6176"),
        TC("15e-6177"),
        TC("15e-6178"),
        TC("15e-6179"),
        //TC("15e6145"),
        TC("1.234567890123456789012345678901234e6144"),
        TC("0.0000"),
        TC("0"),
        TC("1"),
        TC("1e6144"),
        //TC("1e6145"),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val case = TC(randBd())
            test1(case)
        }

    }

    val random = Random()

    fun randBd() : BigDecimal {
        val bitLength = random.nextInt(0, 256)
        val bi = BigInteger(bitLength, random)
        val exp = random.nextInt(12400) - 6200
        val bd = BigDecimal(bi).scaleByPowerOfTen(exp)
        return bd
    }

    fun test1(case: TC) {
        val bdA = case.bdA
        val bdRounded = case.bdRounded
        val biRounded = case.biRounded
        val expRounded = case.expRounded
        val mag = Mag()
        mag.magSet(bdA)
        if (verbose)
            println("bdA:$bdA => bdRounded:$bdRounded => biRounded:$biRounded + expRounded:$expRounded")
        val biCoeff = mag.coeffToBigInteger()
        if (verbose)
            println("coeff:$biCoeff + exp:${mag.exp}")
        if (expRounded == 9999) {
            assert(mag.exp == NON_FINITE_INF)
        } else {
            assertEquals(biRounded, biCoeff)
            assertEquals(expRounded, mag.exp)
        }
    }

}
