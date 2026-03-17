package com.decimal128.decimal

import com.decimal128.decimal.DecRounding.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_POSITIVE
import com.decimal128.decimal.DecRounding.Companion.ROUND_TOWARD_ZERO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class TestMutDecMagSub {

    val verbose = false

    class TC(val bdXraw: BigDecimal, val bdYraw: BigDecimal, val ctx: DecContext) {
        constructor(strA: String, strB: String, rd: DecRounding) :
                this(BigDecimal(strA), BigDecimal(strB), DecContext.decimal128Kotlin().with(rd))
        constructor(strA: String, strB: String) :
                this(BigDecimal(strA), BigDecimal(strB), DecContext.decimal128Kotlin())
        constructor(bdA: BigDecimal, bdB: BigDecimal) : this(bdA, bdB, DecContext.decimal128Kotlin())

        val flipFlop = bdXraw.compareTo(bdYraw) >= 0
        val rm = ctx.decRounding.mapToRoundingMode()
        val bdA = bdToIeeeDecimal128(if (flipFlop) bdXraw else bdYraw, rm)
        val bdAIsFinite = bdIsFinite(bdA)
        val bdB = bdToIeeeDecimal128(if (flipFlop) bdYraw else bdXraw, rm)
        val bdBIsFinite = bdIsFinite(bdB)
        val bdP = bdToIeeeDecimal128(bdA.subtract(bdB), rm)
    }

    val cases = arrayOf(
        TC("2E36", "1E0", ROUND_TOWARD_ZERO),

        TC("1.000000000000000000000000000E26", "2.0000000009999999999E0", ROUND_TOWARD_POSITIVE),
        TC("1.000000000000000000000000000E-2973", "2.0000000009999999999E-2999", ROUND_TOWARD_POSITIVE),
        TC("1.125118998110872714580497160E-2973", "3.0306332009363000323E-2999", ROUND_TOWARD_POSITIVE),
        TC("735087187510156E+2145", "539161499716564458254E+2105"),
        TC("7.35087187510156E+2159", "5.39161499716564458254E+2125"),
        TC("22E0", "1E1"),
        TC("4.574611751494966291851610463E-6043", "2.987013640E-6048", ROUND_TOWARD_ZERO),
        TC("1.579E-3843", "2.12384601155035325007772872958E-3874"),

        TC("1E35", "10E0"),
        TC("3.4396855678324845813315E-5448", "3.0264730275769748987327314530281E-5479", ROUND_TOWARD_ZERO),
        TC("1E35", "10E0"),
        TC("1E35", "100E0"),
        TC("1E35", "1000E0"),

        TC("3.4396855678324845813315E-5448", "3.0264730275769748987327314530281E-5479", ROUND_TOWARD_ZERO),

        TC("10E0", "1E1"),
        TC("1111111111222222222233333333334444E0", "1E1"),
        TC("1111111111222222222233333333334444E0", "1E2"),
        TC("1111111111222222222233333333334444E0", "1E32"),
        TC("1111111111222222222233333333334444E0", "1E33"),

        TC("2E33", "1E0"),
        TC("2E34", "1E0"),
        TC("2E35", "1E0"),
        TC("2E36", "1E0"),
        TC("1E33", "1E0"),
        TC("1E34", "1E0"),
        TC("1E35", "1E0"),
        TC("1E36", "1E0"),
        TC("2E33", "1E0", ROUND_TIES_TO_AWAY),
        TC("2E34", "1E0", ROUND_TIES_TO_AWAY),
        TC("2E35", "1E0", ROUND_TIES_TO_AWAY),
        TC("2E36", "1E0", ROUND_TIES_TO_AWAY),
        TC("1E33", "1E0", ROUND_TIES_TO_AWAY),
        TC("1E34", "1E0", ROUND_TIES_TO_AWAY),
        TC("1E35", "1E0", ROUND_TIES_TO_AWAY),
        TC("1E36", "1E0", ROUND_TIES_TO_AWAY),
        TC("2E33", "1E0", ROUND_TOWARD_ZERO),
        TC("2E34", "1E0", ROUND_TOWARD_ZERO),
        TC("2E35", "1E0", ROUND_TOWARD_ZERO),
        TC("2E36", "1E0", ROUND_TOWARD_ZERO),
        TC("1E33", "1E0", ROUND_TOWARD_ZERO),
        TC("1E34", "1E0", ROUND_TOWARD_ZERO),
        TC("1E35", "1E0", ROUND_TOWARD_ZERO),
        TC("1E36", "1E0", ROUND_TOWARD_ZERO),
        TC("2E33", "1E0", ROUND_TOWARD_POSITIVE),
        TC("2E34", "1E0", ROUND_TOWARD_POSITIVE),
        TC("2E35", "1E0", ROUND_TOWARD_POSITIVE),
        TC("2E36", "1E0", ROUND_TOWARD_POSITIVE),
        TC("1E33", "1E0", ROUND_TOWARD_POSITIVE),
        TC("1E34", "1E0", ROUND_TOWARD_POSITIVE),
        TC("1E35", "1E0", ROUND_TOWARD_POSITIVE),
        TC("1E36", "1E0", ROUND_TOWARD_POSITIVE),
        TC("3.4396855678324845813315E-5448", "3.0264730275769748987327314530281E-5479", ROUND_TOWARD_ZERO),
        TC("1.0E100", "1E0", ROUND_TOWARD_ZERO),
        TC("0E0", "0E1"),
        TC("0E+2565", "0E-2319"),
        TC("1E34", "1E0"),
        TC("1E35", "10E0"),
        TC("1E35", "100E0"),
        TC("1E35", "1000E0"),
        TC("3E-5477", "1.146E-5509"),
        TC("1E35", "1E0"),
        TC("3.5564499921671956252714452E+621", "0E+5834", ROUND_TOWARD_POSITIVE),
        TC("9e99", "0"),
        TC("1E-2353", "1E-2373"),
        TC("1E-2353", "1000000000000000000E-2373"),
        TC("1E-2353", "2222222222222222222E-2373"),
        TC("1111111111111E-2353", "222222222222222222E-2372"),
        TC("1111111111111E-2353", "2222222222222222222E-2373"),
        TC("1.111111111111E-2341", "2.22222222222222222E-2355"),
        TC("1.111111111111E-2341", "2.222222222222222222E-2355"),
        TC("1.111111111111111111111111111111111E-2341", "2.2222222222222222222222222222222E-2355"),
        TC("1.287963674772144018606726951628158E-2341", "1.3886853281837524782330363161313E-2355"),
        TC("1.3886853281837524782330363161313E-2355", "1.287963674772144018606726951628158E-2341"),
        TC("3.577396280843936609447212543753E4", "1e0", ROUND_TOWARD_POSITIVE),
        TC("1", "1e-50", ROUND_TOWARD_POSITIVE),
        TC("3.577396280843936609447212543753E-5366", "2.327539848910E-5939", ROUND_TOWARD_POSITIVE),
        TC("2.14402028641E+4038", "9.0688499219445651743894779402E-76", ROUND_TOWARD_POSITIVE),
        TC("1.17100139250993218892100442826921E-2997", "1.03684390716810037961251682741E-3170"),
        TC("100", "1"),
        TC("2", "3"),
        TC("9e99", "0"),
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
            val tc = TC(randBd(), randBd(), randDecimal128Context())
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
        return bd
    }

    fun randDecimal128Context(): DecContext {
        val i = random.nextInt(4)
        val env = DecContext.decimal128Kotlin().with(DecRounding.fromValue(i))
        return env
    }

    fun test1(tc: TC) {
        val bdA = tc.bdA
        val bdB = tc.bdB
        val expected = tc.bdP
        val env = tc.ctx
        val rm = env.decRounding.mapToRoundingMode()

        if (verbose)
            println("bdA:$bdA - bdB:$bdB (rm:$rm) => expected:$expected")

        val decA = newMutDec(bdA)
        val decB = newMutDec(bdB)
        val decD = MutDec()
        decD.setSub(decA, decB, env)
        val expectedCoeff = expected.unscaledValue()
        val expectedQExp = -expected.scale()
        val observedCoeff = decD.coeffToBigInteger()
        val observedQExp = decD.qExp
        if (expectedCoeff != observedCoeff || expectedQExp != observedQExp)
            println("expected:$expectedCoeff e $expectedQExp observed:$observedCoeff e $observedQExp")
        assertEquals(expected.unscaledValue(), decD.coeffToBigInteger())
        assertEquals(-expected.scale(), decD.qExp)
    }

}
