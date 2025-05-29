package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*

class TestDec34ToString {

    val verbose = false

    class TC(val strVal: String) {
        constructor(bd: BigDecimal) : this(bdToDecimal128String(bd))
        val bd = bdToIeeeDecimal128(BigDecimal(strVal), RoundingMode.HALF_EVEN)
        val expected = bdToDecimal128String(bd)
    }

    val tcs = arrayOf(
        TC("1631964395413992086E-18"),
        TC("4.9355924106324532159349E-2908"),
        TC("3.38E-5899"),
        TC("0"),
        TC(".9"),
        TC("9e-1"),
        TC("9e-6"),
        TC("99e2"),
        TC("9e-7"),
        TC("9.e1"),
        TC("9e1"),
        TC("99e2"),
        TC("9"),
        TC("99"),
        TC("999"),
        TC("9999"),
        TC("99999"),
        TC("1111111111222222222233333333334444"),
        TC("0"),
        TC("1e9999"),
        TC("1234567890"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        val strVal = tc.strVal
        val bd = tc.bd
        val expected = tc.expected
        if (verbose)
            println("$strVal bd:$bd => expected:$expected")

        val d = Dec34(bd)
        val observed = Dec34ParsePrint.toString(d)

        if (verbose)
            println(" => observed:$observed")
        assertEquals(expected, observed)
    }

    @Test
    fun testRandom() {
        for (i in 0..<10000) {
            val bd = randBd()
            val tc = TC(bd)
            test1(tc)
        }
    }

    val random = Random()

    fun randBd() : BigDecimal {
        val bitLength = random.nextInt(0, 112)
        val bi = BigInteger(bitLength, random)
        val sign = random.nextBoolean()
        val exp = random.nextInt(3*4096) - 6176
        val bd = BigDecimal(bi).scaleByPowerOfTen(exp)
        return if (sign) bd.negate() else bd
    }


}