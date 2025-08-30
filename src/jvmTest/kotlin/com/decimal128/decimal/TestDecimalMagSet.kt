package com.decimal128.decimal

import com.decimal128.decimal.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.decimal.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class TestDecimalMagSet {

    val verbose = false

    class TC(val bdA: BigDecimal, val ctx: DecimalContext) {
        constructor(str: String, rd: RoundingDirection) : this(BigDecimal(str), DecimalContext(rd))
        constructor(str: String) : this(BigDecimal(str), DecimalContext())
        constructor(bdA: BigDecimal) : this(bdA, DecimalContext())
        val biA = bdA.unscaledValue()
        val expA = -bdA.scale()
        val bdRounded = bdToIeeeDecimal128(bdA, ctx.getMathContext().roundingMode)
        val biRounded = bdRounded.unscaledValue()
        val expRounded = -bdRounded.scale()
    }

    val cases = arrayOf(
        TC("9.9999999999999999999999999999999995E+6144"), // overflow to Infinity

        TC("1e-6175"),
        TC("0E+6145", ROUND_TIES_TO_AWAY),
        TC("3.05079656515623149897192850E-6151", ROUND_TOWARD_ZERO),
        TC("2.2170825345518895501686941901839130E-5813"),
        TC("3.18227787914677743006698769411248000E+2275", ROUND_TIES_TO_AWAY),
        TC("11e-6177"),

        TC("9.9999999999999999999999999999999994E+6144"),
        TC("9.9999999999999999999999999999999995E+6144"), // overflow to Infinity

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

    fun randDecimal128Context(): DecimalContext {
        val i = random.nextInt(4)
        val ctx = DecimalContext(RoundingDirection.fromValue(i))
        return ctx
    }

    fun test1(case: TC) {
        val bdA = case.bdA
        val bdRounded = case.bdRounded
        val biRounded = case.biRounded
        val expRounded = case.expRounded
        val ctx = case.ctx
        val roundingMode = ctx.getMathContext().roundingMode
        val dec = Decimal()
        if (verbose)
            println("bdA:$bdA roundingMode:$roundingMode => bdRounded:$bdRounded => biRounded:$biRounded + expRounded:$expRounded")
        dec.set(bdA, ctx)
        val biCoeff = dec.coeffToBigInteger()
        if (verbose)
            println("coeff:$biCoeff + expQ:${dec.qExp}")
        if ((biRounded != biCoeff && expRounded != NON_FINITE_INF) || expRounded != dec.qExp) {
            println("bdA:$bdA roundingMode:$roundingMode => bdRounded:$bdRounded => biRounded:$biRounded + expRounded:$expRounded")
            println("coeff:$biCoeff + expQ:${dec.qExp}")
            assertEquals(biRounded, biCoeff)
            assertEquals(expRounded, dec.qExp)

        }
    }

}
