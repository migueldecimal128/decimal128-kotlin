package com.decimal128.decimal

import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_POSITIVE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class TestMutDecAddSub {

    val verbose = true

    class TC(val bdAraw: BigDecimal, val bdBraw: BigDecimal, val env: DecEnv) {
        constructor(strA: String, strB: String, rd: DecRounding) :
                this(BigDecimal(strA), BigDecimal(strB), DecEnv().with(rd))
        constructor(strA: String, strB: String) :
                this(BigDecimal(strA), BigDecimal(strB), DecEnv())
        constructor(bdA: BigDecimal, bdB: BigDecimal) : this(bdA, bdB, DecEnv())

        val rm = env.decRounding.mapToRoundingMode()
        val bdA = bdToIeeeDecimal128(bdAraw, rm)
        val bdAIsFinite = bdIsFinite(bdA)
        val bdB = bdToIeeeDecimal128(bdBraw, rm)
        val bdBIsFinite = bdIsFinite(bdB)
        val bdP = bdToIeeeDecimal128(bdA.add(bdB), rm)
    }

    val cases = arrayOf(
        TC("3.577396280843936609447212543753E-5366", "2.327539848910E-5939", ROUND_TOWARD_POSITIVE),
        TC("-4.949004E-4622", "2.06229042911321265462351537682015E-4592", ROUND_TIES_TO_AWAY),
        TC("-3.87184285392449585406072732173794E+5253", "3.4414437429652711952247662511911E+2910"),
        TC("1.3886853281837524782330363161313E-2355", "1.287963674772144018606726951628158E-2341"),
        TC("3.5564499921671956252714452E+621", "0E+5834", ROUND_TOWARD_POSITIVE),
        TC("3.577396280843936609447212543753E-5366", "2.327539848910E-5939", ROUND_TOWARD_POSITIVE),
        TC("2.14402028641E+4038", "9.0688499219445651743894779402E-76", ROUND_TOWARD_POSITIVE),
        TC("1.17100139250993218892100442826921E-2997", "1.03684390716810037961251682741E-3170"),
        TC("2", "3"),
        TC("0", "9e99"),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testBigDecimalAddZero() {
        val s0 = BigDecimal("0e-1").add(BigDecimal("0e-10"))
        println(s0)
    }

    @Test
    fun testRandom() {
        for (i in 0..<10000) {
            val tc = TC(randBd(), randBd(), randDecimal128Rounding())
            if (tc.bdAIsFinite && tc.bdBIsFinite)
                test1(tc)
        }

    }

    val random = Random()

    fun randBd() : BigDecimal {
        val bitLength = random.nextInt(0, 112)
        val bi = BigInteger(bitLength, random)
        val exp = random.nextInt(3*4096) - 6176
        val bd = BigDecimal(bi).scaleByPowerOfTen(exp)
        return if (random.nextBoolean()) bd.negate() else bd
    }

    fun randDecimal128Rounding(): DecEnv {
        val i = random.nextInt(4)
        val env = DecEnv().with(DecRounding.fromValue(i))
        return env
    }

    fun test1(tc: TC) {
        val bdA = tc.bdA
        val bdB = tc.bdB
        val expected = tc.bdP
        val env = tc.env
        val rm = env.decRounding.mapToRoundingMode()

        if (verbose)
            println("bdA:$bdA + bdB:$bdB (rm:$rm) => expected:$expected")
        env.context {

            val decimalA = newMutDec(bdA)
            val decimalB = newMutDec(bdB)
            val decimalS = newAdd(decimalA, decimalB)
            if (verbose)
                println("decimalS:$decimalS")
            assertEquals(expected.abs().unscaledValue(), decimalS.coeffToBigInteger())
            assertEquals(expected.signum() < 0, decimalS.sign)
            assertEquals(-expected.scale(), decimalS.qExp)
        }
    }

}
