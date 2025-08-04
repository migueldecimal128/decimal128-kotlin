package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestInt256ParsePrint {

    val regexPlusUnderscore = Regex("[+_]")
    fun parseStringToBi(str: String): BigInteger {
        val s = str.replace(regexPlusUnderscore, "")
        val bi = when {
            s.startsWith("0x") -> BigInteger(s.substring(2), 16)
            s.startsWith("-0x") -> BigInteger(s.substring(3), 16).negate()
            else -> BigInteger(s)
        }
        return bi
    }

    val verbose = false

    val tcs = arrayOf(
        "5368225503980853049254831206148184042523111474414498591855",
        "0",
        "0_0",
        "1_0_0",
        "-0x0000_0001",
        "0xCafe_Babe",
        "-0xDeadBeef",
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    val random = Random()
    @Test
    fun testRandom() {
        for (i in 0..<10000) {
            val bitLen = random.nextInt(0, 256)
            val bi = BigInteger(bitLen, random)
            val str = bi.toString()
            test1(str)
            test1("-" + str)
            if (str.length >= 2) {
                val underscoreCount = random.nextInt(str.length)
                var strUnderscore = str
                for (j in 0..<underscoreCount) {
                    val index = random.nextInt(1, str.length)
                    strUnderscore = strUnderscore.substring(0, index) + '_' + strUnderscore.substring(index)
                }
                test1(strUnderscore)
                test1("-" + strUnderscore)
            }
        }
    }


    fun test1(tc: String) {
        if (verbose)
            println("$tc")
        val n = Int256(tc)
        val observed = n.toString();
        val tcStripped = tc.replace(regexPlusUnderscore, "")
        val bi = when {
            tcStripped.startsWith("0x") -> BigInteger(tcStripped.substring(2), 16)
            tcStripped.startsWith("-0x") -> BigInteger(tcStripped.substring(3), 16).negate()
            else -> BigInteger(tcStripped)
        }
        val expected = bi.toString()
        assertEquals(expected, observed)

        val hexStr = n.toHexString()
        val biHexStr = (if (bi.signum() < 0) "-" else "") + ("0x" + bi.abs().toString(16).uppercase())

        assertEquals(biHexStr, hexStr)
    }

}
