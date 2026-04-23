package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestDecimalIntArithmetic {
    val verbose = false

    data class TC(val xStr: String, val n: Int, val expectedStr: String)

    // plus: x + n and n + x
    val plusTCs = arrayOf(
        TC("1", 1, "2"),
        TC("0.05", 1, "1.05"),      // interest rate use case: rate + 1
        TC("-1", 1, "0"),
        TC("1", -1, "0"),
        TC("0", Int.MIN_VALUE, "${Int.MIN_VALUE}"),
    )

    // minus: x - n and n - x
    val minusTCs = arrayOf(
        TC("1.05", 1, "0.05"),      // interest rate use case: grossRate - 1
        TC("1", 1, "0"),
        TC("0", 1, "-1"),
        TC("1", 0, "1"),
        TC("0", Int.MIN_VALUE, "2147483648"),   // 0 - Int.MIN_VALUE
    )

    // times: x * n and n * x
    val timesTCs = arrayOf(
        TC("1.05", 2, "2.10"),
        TC("0.05", 0, "0.00"),
        TC("-1.05", 2, "-2.10"),
    )

    // div: x / n
    val divTCs = arrayOf(
        TC("2", 1, "2"),
        TC("-9", -3, "3"),
        TC("1.05", 1, "1.05"),
        TC("-1", 2, "-0.5"),
    )

    val nDivTCs = arrayOf(
        TC("2", 1, "0.5"),       // 1 / 2
        TC("-4", 1, "-0.25"),      // 1 / 4
        TC("2", 3, "1.5"),       // 3 / 2

    )

    @Test
    fun testPlus() { for (tc in plusTCs) testPlus(tc) }
    @Test
    fun testMinus() { for (tc in minusTCs) testMinus(tc) }
    @Test
    fun testTimes() { for (tc in timesTCs) testTimes(tc) }
    @Test
    fun testDiv() { for (tc in divTCs) testDiv(tc) }
    @Test
    fun testNDiv() { for (tc in nDivTCs) testNDiv(tc) }

    fun testPlus(tc: TC) {
        if (verbose)
            println("plus: $tc")
        val x = tc.xStr.toDecimal()
        val n = tc.n
        val expected = tc.expectedStr.toDecimal()
        assertEquals(expected, x + n)
        assertEquals(expected, n + x)
    }

    fun testMinus(tc: TC) {
        if (verbose)
            println("minus: $tc")
        val x = tc.xStr.toDecimal()
        val n = tc.n
        val expected = tc.expectedStr.toDecimal()
        assertEquals(expected, x - n)
        val reversed = n - x
        assertEquals(-expected, reversed)
    }

    fun testTimes(tc: TC) {
        if (verbose)
            println("times: $tc")
        val x = tc.xStr.toDecimal()
        val n = tc.n
        val expected = tc.expectedStr.toDecimal()
        assertEquals(expected, x * n)
        assertEquals(expected, n * x)
    }

    fun testDiv(tc: TC) {
        if (verbose)
            println("div: $tc")
        val x = tc.xStr.toDecimal()
        val n = tc.n
        val expected = tc.expectedStr.toDecimal()
        assertEquals(expected, x / n)
    }

    fun testNDiv(tc: TC) {
        if (verbose)
            println("ndiv: $tc")
        val x = tc.xStr.toDecimal()
        val n = tc.n
        val expected = tc.expectedStr.toDecimal()
        assertEquals(expected, n / x)
    }
}