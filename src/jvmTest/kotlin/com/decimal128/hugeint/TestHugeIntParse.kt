package com.decimal128.hugeint

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigInteger

class TestHugeIntParse {
    val verbose = true

    class TC(val str: String, val isValid: Boolean = true) {
    }

    val tcs = arrayOf(
        TC("0xCAFE_BABE_DEAD_BEEF"),
        TC("1_", false),
        TC("0x0"),
        TC("0x_0", false),
        TC("0"),
        TC("1"),
        TC("-1"),
        TC("0x", false),
        TC("-00000000000000000099"),
        TC("-0_000_00000000000000_9_9"),
        TC("-0_000_00000000000000_9_9_", false),
        TC("-_00000000000000000099", false),
        TC("0xCAFE_BABE_DEAD_BEEF"),
        TC("1111111111_2222222222_3333333333_4444444444_5555555555" +
                "_6666666666_7777777777_8888888888_9999999999_0000000000"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc.str, tc.isValid)
    }

    fun test1(str: String, isValid: Boolean) {
        if (verbose)
            println("str:$str isValid:$isValid")
        try {
            val hi = HugeInt.fromString(str)
            assertTrue(isValid)
            val bi = getBigInteger(str)

            val hiStr = hi.toString()
            val biStr = bi.toString()
            assertEquals(biStr, hiStr)

            if (str.contains("0x") || str.contains("0X"))
                testHex(str)
        } catch (e: IllegalArgumentException) {
            assertFalse(isValid)
        }

    }

    fun testHex(hexStr: String) {
        val hi0 = HugeInt.fromHexString(hexStr)
        val without0x = hexStr.replace("0x", "").replace("0X", "")
        val hi1 = HugeInt.fromHexString(without0x)
        assertEquals(hi0, hi1)
        val withoutUnderscores = without0x.replace("_", "")
        val hi2 = HugeInt.fromHexString(withoutUnderscores)

        assertEquals(hi0, hi1)
        assertEquals(hi0, hi2)

        val bi = BigInteger(withoutUnderscores, 16)
        val hiStr = hi0.toString()
        val biStr = bi.toString()
        assertEquals(biStr, hiStr)
    }

    fun getBigInteger(str: String): BigInteger {
        val strNoUnderscores = str.replace("_", "")
        if (strNoUnderscores.startsWith("0x"))
            return BigInteger(strNoUnderscores.substring(2), 16)
        if (strNoUnderscores.startsWith("+0x") || strNoUnderscores.startsWith("-0x"))
            return BigInteger(strNoUnderscores.substring(3), 16)
        return BigInteger(strNoUnderscores)
    }
}
