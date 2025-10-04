package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*

class TestMutDecMagMul {

    val verbose = false

    class TC(val bdAraw: BigDecimal, val bdBraw: BigDecimal, val decEnv: DecEnv) {
        constructor(strA: String, strB: String, decRounding: DecRounding) :
                this(BigDecimal(strA), BigDecimal(strB), DecEnv().with(decRounding))
        constructor(strA: String, strB: String) :
                this(BigDecimal(strA), BigDecimal(strB), DecEnv())
        constructor(bdA: BigDecimal, bdB: BigDecimal) : this(bdA, bdB, DecEnv())

        val rm = decEnv.decRounding.mapToRoundingMode()
        val bdA = bdToIeeeDecimal128(bdAraw, rm)
        val bdAIsFinite = bdIsFinite(bdA)
        val bdB = bdToIeeeDecimal128(bdBraw, rm)
        val bdBIsFinite = bdIsFinite(bdB)
        val bdP = bdToIeeeDecimal128(bdA.multiply(bdB), rm)
    }

    val cases = arrayOf(
        TC("2.132605340208488890479E+1158", "4.6211615602131956617E+5175"),
        TC("1.2967505698781432914870320E-3651", "5.56450878649625E-2965", DecRounding.ROUND_TOWARD_POSITIVE),
        TC("1", "0"),
        TC("1", "0e-6176"),
        TC("1.17100139250993218892100442826921E-2997", "1.03684390716810037961251682741E-3170"),
        TC("2", "3"),
        TC("0", "9e99"),
        )

    @Test
    fun testBrokenIeee() {
        val bd = bdToIeeeDecimal128(BigDecimal.ZERO, RoundingMode.HALF_EVEN)
        println("bd:$bd")
    }

    @Test
    fun testProblemChild() {
        val tc = TC("1.2967505698781432914870320E-3651", "5.56450878649625E-2965", DecRounding.ROUND_TOWARD_POSITIVE)
        test1(tc)
    }

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
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

    fun randDecimal128Context(): DecEnv {
        val i = random.nextInt(5)
        val decEnv = DecEnv().with(DecRounding.fromValue(i))
        return decEnv
    }

    fun test1(tc: TC) {
        val bdA = tc.bdA
        val bdB = tc.bdB
        val expected = tc.bdP
        val decEnv = tc.decEnv
        val rm = decEnv.decRounding.mapToRoundingMode()

        if (verbose)
            println("bdA:$bdA * bdB:$bdB (rm:$rm) => expected:$expected")

        val decA = newMutDec(bdA)
        val decB = newMutDec(bdB)
        val decP = MutDec()
        decP.setMul(decA, decB, decEnv)
        if (decP.qExp != NON_FINITE_INF)
            assertEquals(expected.unscaledValue(), decP.coeffToBigInteger())
        assertEquals(-expected.scale(), decP.qExp)
    }

}
