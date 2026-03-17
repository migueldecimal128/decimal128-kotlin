package com.decimal128.decimal

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestDecWithScale {

    val verbose = false

    data class TC(val str: String, val scale: Int, val expected: String)

    val tcs = arrayOf(
        TC("0.10", 1, "0.1"),
        TC("1", 1, "1.0"),
        TC("1", 2, "1.00"),
        TC("1", 33, "1.000000000000000000000000000000000"),
        TC("0.10", 1, "0.1"),
        TC("0", 2, "0.00"),
        TC("0.10", 1, "0.1"),
        TC("0.12", 1, "NaN"),
        TC("1.20000", 1, "1.2"),
        TC("1e-2", 4, "0.0100"),

        TC("1.000000000", 0, "1"),
        TC("100", -2, "1E+2"),
        TC("1.23", 0, "NaN"),
    )

    @Test
    fun testCases() {
        for (tc in tcs) {
            if (verbose)
                println(tc)
            test1(tc)
        }
    }

    fun test1(tc: TC) {
        val ctx = DecContext.decimal128Kotlin()
        val x = Decimal.from(tc.str)
        val scaled = x.withScale(tc.scale, ctx)
        assertEquals(tc.expected, scaled.toString())
    }

}
