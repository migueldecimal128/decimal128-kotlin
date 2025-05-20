package com.decimal128

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.math.MathContext
import java.util.*

class TestMagSet {

    val verbose = false

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

        fun overflowsToInfinity(rm: RoundingMode, sign: Boolean): Boolean {
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


   }

    class TC(val bdA: BigDecimal, val ctx: Decimal128Context) {
        constructor(str: String, rd: RoundingDirection) : this(BigDecimal(str), Decimal128Context(rd))
        constructor(str: String) : this(BigDecimal(str), Decimal128Context())
        constructor(bdA: BigDecimal) : this(bdA, Decimal128Context())
        val biA = bdA.unscaledValue()
        val expA = -bdA.scale()
        val bdRounded = toIeeeDecimal128(bdA, ctx.getMathContext().roundingMode)
        val biRounded = bdRounded.unscaledValue()
        val expRounded = -bdRounded.scale()
    }

    val cases = arrayOf(
        TC("0E+6145", ROUND_TIES_TO_AWAY),
        TC("3.05079656515623149897192850E-6151", ROUND_TOWARD_ZERO),
        TC("2.2170825345518895501686941901839130E-5813"),
        TC("3.18227787914677743006698769411248000E+2275", ROUND_TIES_TO_AWAY),
        TC("11e-6177"),

        TC("9.9999999999999999999999999999999994E+6144"),
        TC("9.9999999999999999999999999999999995E+6144"),

        TC("7.36956901257177558648652733739540513555E-6144"), // double rounding!

        TC("6.57913228239533914943656987782647149234312929384644E+6233"),
        TC("6.57913228239533914943656987782647149234312929384644E+6233", ROUND_TOWARD_ZERO),

        TC("1.1111111112222222222333333333344446E+0", ROUND_TOWARD_ZERO),
        TC("1.362849775152463544468720357836334504420E+0", ROUND_TOWARD_ZERO),

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
            val case = TC(randBd(), randDecimal128Context())
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

    fun randDecimal128Context(): Decimal128Context {
        val i = random.nextInt(4)
        val ctx = Decimal128Context(RoundingDirection.fromValue(i))
        return ctx
    }

    fun test1(case: TC) {
        val bdA = case.bdA
        val bdRounded = case.bdRounded
        val biRounded = case.biRounded
        val expRounded = case.expRounded
        val ctx = case.ctx
        val roundingMode = ctx.getMathContext().roundingMode
        val mag = Mag()
        if (verbose)
            println("bdA:$bdA roundingMode:$roundingMode => bdRounded:$bdRounded => biRounded:$biRounded + expRounded:$expRounded")
        mag.magSet(bdA, ctx)
        val biCoeff = mag.coeffToBigInteger()
        if (verbose)
            println("coeff:$biCoeff + expQ:${mag.expQ}")
        if (biRounded != biCoeff || expRounded != mag.expQ) {
            println("bdA:$bdA roundingMode:$roundingMode => bdRounded:$bdRounded => biRounded:$biRounded + expRounded:$expRounded")
            println("coeff:$biCoeff + expQ:${mag.expQ}")
            assertEquals(biRounded, biCoeff)
            assertEquals(expRounded, mag.expQ)

        }
    }

}
