package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

private val regexPlusUnderscore = Regex("[+_]")
private fun parseStringToBi(str: String): BigInteger {
    val s = str.replace(regexPlusUnderscore, "")
    val bi = when {
        s.startsWith("0x") -> BigInteger(s.substring(2), 16)
        s.startsWith("-0x") -> BigInteger(s.substring(3), 16).negate()
        else -> BigInteger(s)
    }
    return bi
}


class TestInt256Arithmetic {

    val verbose = true

    data class TC(val strA: String, val strB: String) {
        constructor(biA: BigInteger, biB: BigInteger) : this(biA.toString(), biB.toString())
        val biA = parseStringToBi(strA)
        val biB = parseStringToBi(strB)
        val sum = biA + biB
        val diff = biA - biB
        val prod = biA * biB
        val quot = if (biB.signum() == 0) BigInteger.ZERO else biA / biB
        val rem = if (biB.signum() == 0) BigInteger.ZERO else biA % biB
    }

    val tcs = arrayOf(
        TC("40000000000000000000", "20000000000000000001"),
        TC("400000000000000000000", "30000000000000000000"),
        TC("902475526234364831190", "359661921088628968292"),
        TC(BigInteger.ONE.shiftLeft(70), BigInteger.ONE.shiftLeft(69)),
        TC("2", "0x02"),
        TC("1111111111222222222233333333334444444444", "11111111112222222222"),
        TC("1234", "1"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    @Test
    fun testProblemChild() {
        val strA = "40000000000000000000"
        val strB = "20000000000000000001"
        val biA = BigInteger(strA)
        val biB = BigInteger(strB)
        val expected = biA % biB
        val a = Int256(strA)
        val b = Int256(strB)
        val rem = a % b

        assertEquals(expected.toString(), rem.toString())
    }

    val random = Random()

    fun randBi(): BigInteger {
        val bitLen = random.nextInt(0, 128)
        val bi = BigInteger(bitLen, random)
        return bi
    }

    @Test
    fun testRandom() {
        for (i in 0..<10000) {
            val biA = randBi()
            val biB = randBi()
            val tc = TC(biA, biB)
            test1(tc)
        }
    }

    fun test1(tc: TC) {
        val strA = tc.strA
        val strB = tc.strB
        if (verbose)
            println("$strA +-*/% $strB")
        val a = Int256(strA)
        assertEquals(tc.biA.bitLength(), a.bitLen)
        assertEquals(tc.biA.signum(), a.signum())
        val b = Int256(strB)
        assertEquals(tc.biB.bitLength(), b.bitLen)
        assertEquals(tc.biB.signum(), b.signum())

        if (tc.sum.bitLength() <= 256) {
            val sum = a + b
            assertEquals(tc.sum.toString(), "$sum")
        }
        if (tc.diff.bitLength() <= 256) {
            val diff = a - b
            assertEquals(tc.diff.toString(), "$diff")
        }
        if (tc.prod.bitLength() <= 256) {
            val prod = a * b
            assertEquals(tc.prod.toString(), "$prod")
        }
        if (tc.biB.signum() != 0) {
            val quot = a / b
            assertEquals(tc.quot.toString(), "$quot")
            val rem = a % b
            assertEquals(tc.rem.toString(), "$rem")
        }
    }
}
