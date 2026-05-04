package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals


class TestMutDecToString2 {

    val verbose = false

    val tcs = arrayOf(
        "-9",
        "NaN",
        "-sNaN123456789012345678901234567890",
        "6.02E+23",
        "0", "1", "-9",
        "0.0", "-0.0",
        "12.34", "-567.89",
        "0.000001",
        "-1E+2",
        "3.14159",
        "6.02E+23",
        "-9.999999999999999999999999999999999E-999",
        "Infinity", "-Infinity",
        "NaN", "-NaN", "NaN123", "-NaN456789",
        "sNaN", "-sNaN", "sNaN987654321",
        "-sNaN123456789012345678901234567890",
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: String) {
        if (verbose)
            println("tc:$tc")
        val md = MutDec().set(tc)
        val str = md.toString()
        if (verbose)
            println(" -> dec:$str")
        assertEquals(tc, str)
    }
}