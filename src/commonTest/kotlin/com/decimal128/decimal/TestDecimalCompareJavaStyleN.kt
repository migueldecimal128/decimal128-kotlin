package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestDecimalCompareJavaStyleN {
    val verbose = false

    data class TC(val xStr: String, val n: Int, val expected: Int)

    val tcs = arrayOf(
        // zero vs zero
        TC("0", 0, 0),
        TC("-0", 0, -1),        // Java-style: -0 < +0
        TC("0", 0, 0),

        // zero vs non-zero
        TC("0", 1, -1),
        TC("0", -1, 1),
        TC("0", Int.MAX_VALUE, -1),
        TC("0", Int.MIN_VALUE, 1),

        // positive finite vs zero
        TC("1", 0, 1),
        TC("0.5", 0, 1),

        // negative finite vs zero
        TC("-1", 0, -1),
        TC("-0.5", 0, -1),

        // opposite signs
        TC("1", -1, 1),
        TC("-1", 1, -1),

        // equal values
        TC("1", 1, 0),
        TC("-1", -1, 0),
        TC("100", 100, 0),
        TC("-100", -100, 0),

        // cohort members — exact integral, numerically equal
        TC("1.0", 1, 0),
        TC("1.00", 1, 0),
        TC("-1.0", -1, 0),

        // fractional — same truncation, x wins
        TC("1.5", 1, 1),
        TC("-1.5", -1, -1),
        TC("1.9999999", 1, 1),
        TC("-1.9999999", -1, -1),

        // fractional — truncation differs
        TC("2.5", 1, 1),
        TC("-2.5", -1, -1),

        // x less than n, same sign
        TC("1", 2, -1),
        TC("-1", -2, 1),

        // boundary values
        TC("2147483647", Int.MAX_VALUE, 0),         // Int.MAX_VALUE
        TC("-2147483648", Int.MIN_VALUE, 0),         // Int.MIN_VALUE
        TC("2147483648", Int.MAX_VALUE, 1),          // Int.MAX_VALUE + 1
        TC("-2147483649", Int.MIN_VALUE, -1),        // Int.MIN_VALUE - 1

        // overflow — x magnitude beyond Long range
        TC("1E+20", Int.MAX_VALUE, 1),
        TC("-1E+20", Int.MIN_VALUE, -1),
        TC("9.999999999999999999999999999999999E+6144", Int.MAX_VALUE, 1),  // max finite

        // infinity
        TC("Infinity", 0, 1),
        TC("Infinity", Int.MAX_VALUE, 1),
        TC("-Infinity", 0, -1),
        TC("-Infinity", Int.MIN_VALUE, -1),

        // NaN
        TC("NaN", 0, 1),
        TC("NaN", Int.MAX_VALUE, 1),
        TC("NaN", Int.MIN_VALUE, 1),
        TC("-NaN", 0, 1),
        TC("sNaN", 0, 1),
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

        val observed = x.compareTo(n)
        assertEquals(expected, observed)

        val observedN = n.compareTo(x)
        assertEquals(-expected, observedN)
    }
}