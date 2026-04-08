package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestMutDecToLongOrMinValue {

    val verbose = true

    data class TC(val strDecimal: String, val expected: Long)

    val tcs = arrayOf(
        TC("10000000000000000000.0", Long.MIN_VALUE),  // 20 digit integer, too big
        TC("0", 0L),
        TC("1.0", 1L),
        TC(".1E1", 1L),
        TC("1.000000000000000000", 1L),
        TC("1.0000000000000000000", 1L),
        TC("1.00000000000000000000", 1L),
        TC("1.00000000000000000000000000000000000000000000000000000000000000000000", 1L),
        TC("1e18", 1000000000000000000L),
        TC("1e19", Long.MIN_VALUE),
        TC("9.223372036854775807E18", Long.MAX_VALUE),
        TC("9.223372036854775808E18", Long.MIN_VALUE),
        TC("6.022E23", Long.MIN_VALUE),

        TC(Long.MAX_VALUE.toString(), Long.MAX_VALUE),
        TC(Long.MIN_VALUE.toString(), Long.MIN_VALUE),

        TC("-1", -1L),
        TC("-1.0", -1L),
        TC("-1e18", -1000000000000000000L),
        TC("-9.223372036854775807E18", -Long.MAX_VALUE),  // i.e. Long.MIN_VALUE + 1
        TC("-9.223372036854775808E18", Long.MIN_VALUE),   // actual Long.MIN_VALUE, sentinel a

        TC("1.5", Long.MIN_VALUE),
        TC("0.1", Long.MIN_VALUE),
        TC("1.0000000000000000001", Long.MIN_VALUE),  // just over 1 by a tiny fraction

        TC("0.01", Long.MIN_VALUE),   // qNeg > digitLen
        TC("0.10", Long.MIN_VALUE),   // qNeg == digitLen
        TC("1.0", 1L),                // qNeg < digitLen, exact

        TC("1000000000000000000.0", 1000000000000000000L),  // 19 digits, exact
        TC("10000000000000000000.0", Long.MIN_VALUE),  // 20 digit integer, too big

        TC("Infinity", Long.MIN_VALUE),
        TC("-Infinity", Long.MIN_VALUE),
        TC("NaN", Long.MIN_VALUE),
        TC("-NaN999", Long.MIN_VALUE),
        )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        if (verbose)
            println(tc)
        val md = MutDec().set(tc.strDecimal)
        val observed = md.toLongOrMinValue()
        assertEquals(tc.expected, observed)
    }

}