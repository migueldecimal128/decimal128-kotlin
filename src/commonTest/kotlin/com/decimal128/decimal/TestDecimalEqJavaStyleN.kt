package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestDecimalEqJavaStyleN {

    val verbose = true

    data class TC(val xStr: String, val n: Int, val expected: Boolean)

    val tcs = arrayOf(
        // zero
        TC("+0", 0, true),
        TC("-0", 0, true),          // -0 == 0 for equality
        TC("+0.0", 0, true),
        TC("-0.0", 0, true),
        TC("+0e6111", 0, true),
        TC("-0e6111", 0, true),
        TC("+0e-6176", 0, true),
        TC("-0e-6176", 0, true),
        TC("0", 1, false),
        TC("0", -1, false),
        TC("0", Int.MAX_VALUE, false),
        TC("0", Int.MIN_VALUE, false),

        // cohort members — numerically equal, different encoding
        TC("1.0", 1, true),
        TC("1.00", 1, true),
        TC("-1.0", -1, true),
        TC("-1.00", -1, true),
        TC("100.00", 100, true),

        // exact integers, equal
        TC("1", 1, true),
        TC("-1", -1, true),
        TC("100", 100, true),
        TC("-100", -100, true),
        TC("2147483647", Int.MAX_VALUE, true),
        TC("-2147483648", Int.MIN_VALUE, true),

        // exact integers, not equal
        TC("1", 2, false),
        TC("-1", -2, false),
        TC("2147483647", Int.MIN_VALUE, false),
        TC("-2147483648", Int.MAX_VALUE, false),

        // fractional — never equal to Int
        TC("1.5", 1, false),
        TC("-1.5", -1, false),
        TC("0.5", 0, false),
        TC("1.9999999", 1, false),
        TC("-1.9999999", -1, false),

        // overflow — beyond Int range
        TC("2147483648", Int.MAX_VALUE, false),     // Int.MAX_VALUE + 1
        TC("-2147483649", Int.MIN_VALUE, false),    // Int.MIN_VALUE - 1
        TC("1E+20", 0, false),
        TC("-1E+20", 0, false),

        // infinity
        TC("Infinity", 0, false),
        TC("Infinity", Int.MAX_VALUE, false),
        TC("-Infinity", 0, false),
        TC("-Infinity", Int.MIN_VALUE, false),

        // NaN
        TC("NaN", 0, false),
        TC("NaN", Int.MAX_VALUE, false),
        TC("sNaN", 0, false),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test(tc)
    }

    fun test(tc: TC) {
        if (verbose)
            println(tc)
        val x = tc.xStr.toDecimal()
        val n = tc.n
        val expected = tc.expected

        val observed = x EQ n
        assertEquals(expected, observed)

        val observedN = n EQ x
        assertEquals(expected, observedN)

        val observedNot = x NE n
        assertEquals(!expected, observedNot)

        val observedNotN = n NE x
        assertEquals(!expected, observedNotN)
    }
}