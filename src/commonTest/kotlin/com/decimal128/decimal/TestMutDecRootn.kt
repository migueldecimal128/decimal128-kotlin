package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestMutDecRootn {
    val verbose = false

    data class TC(val xStr: String, val n: Int, val expectedStr: String)

    val tcs = arrayOf(

        TC("2", Int.MIN_VALUE, "0.9999999996772281916588167078079415"),     // n=0 after negation? or invalid
        TC("2", Int.MIN_VALUE, "0.9999999996772281916588167078079415"),     // n=0 after negation? or invalid

        // basic sanity
        TC("1", 1, "1"),
        TC("1", 2, "1"),
        TC("1", -1, "1"),
        TC("8", 3, "2"),
        TC("16", 4, "2"),

        // n == 0: invalid operation
        TC("1", 0, "NaN"),
        TC("0", 0, "NaN"),
        TC("-1", 0, "NaN"),

        // x < 0 and n even: invalid operation
        TC("-1", 2, "NaN"),
        TC("-4", 2, "NaN"),
        TC("-1", 4, "NaN"),
        TC("-1", -2, "NaN"),

        // x < 0 and n odd: valid
        TC("-1", 1, "-1"),
        TC("-1", 3, "-1"),
        TC("-27", 3, "-3"),
        TC("-1", -1, "-1"),

        // perfect roots
        TC("4", 2, "2"),
        TC("9", 2, "3"),
        TC("8", 3, "2"),
        TC("27", 3, "3"),
        TC("16", 4, "2"),
        TC("100", 2, "10"),

        // n = -1: reciprocal
        TC("2", -1, "0.5"),
        TC("4", -1, "0.25"),
        TC("0.5", -1, "2"),

        // negative odd n
        TC("8", -3, "0.5"),
        // FIXME - this should end in 3, not 4
        TC("27", -3, "0.3333333333333333333333333333333334"),

        // ±0, n > 0
        TC("0", 1, "0"),
        TC("0", 2, "0"),
        TC("0", 3, "0"),
        TC("-0", 1, "-0"),
        TC("-0", 3, "-0"),
        TC("-0", 2, "0"),   // even n, sign dropped

        // ±0, n < 0: divideByZero -> ±Inf
        TC("0", -1, "Infinity"),
        TC("0", -2, "Infinity"),
        TC("-0", -1, "-Infinity"),  // odd n, sign preserved
        TC("-0", -3, "-Infinity"),  // odd n, sign preserved
        TC("-0", -2, "Infinity"),  // even n, sign dropped

        // +∞
        TC("+Inf", 1, "Infinity"),
        TC("+Inf", 2, "Infinity"),
        TC("+Inf", -1, "0"),
        TC("+Inf", -2, "0"),

        // -∞, odd n
        TC("-Inf", 1, "-Infinity"),
        TC("-Inf", 3, "-Infinity"),
        TC("-Inf", -1, "-0"),
        TC("-Inf", -3, "-0"),

        // -∞, even n: invalid operation
        TC("-Inf", 2, "NaN"),
        TC("-Inf", 4, "NaN"),
        TC("-Inf", -2, "NaN"),
        TC("-Inf", -4, "NaN"),

        // qNaN propagation
        TC("NaN", 1, "NaN"),
        TC("NaN", -1, "NaN"),
        TC("NaN", 2, "NaN"),

        // extreme n
        TC("2", Int.MAX_VALUE, "1.000000000322771808595667268407085"),       // 2^(1/huge) -> 1
        TC("2", Int.MIN_VALUE, "0.9999999996772281916588167078079415"),     // n=0 after negation? or invalid
        TC("1E+6144", 2, "1.00000000000000000E3072"),
        TC("1E-6176", 2, "1E-3088"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test(tc)
    }

    fun test(tc: TC) {
        if (verbose)
            println(tc)
        val x = MutDec().set(tc.xStr)
        val n = tc.n

        val observed = MutDec().setRootn(x, n)

        assertEquals(tc.expectedStr, observed.toString())

    }
}