package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigDecimal

class TestNextUpDown {

    val tcs = arrayOf(
        BigDecimal("1e-6176"),
        BigDecimal("-1e-6176"),
        BigDecimal("-9999999999999999999999999999999999e6111"),
        BigDecimal("9999999999999999999999999999999999e6111"),
        BigDecimal("1"),
        BigDecimal("8"),
        BigDecimal("-8"),
        BigDecimal("0.1"),
        BigDecimal("-0.1"),
        BigDecimal.TEN.negate(),
        BigDecimal.TEN,
        BigDecimal.ONE,
        BigDecimal("9999999999999999999999999999999999e6111"),
        BigDecimal("-9999999999999999999999999999999999e6111"),
        BigDecimal("1e-6176"),
        BigDecimal("-1e-6176"),
        BigDecimal.ZERO,
        BigDecimal("10"),
        BigDecimal("100"),
        BigDecimal("1000"),
        BigDecimal("10000"),
        BigDecimal("100000"),
        BigDecimal("100000000000000000000000000000"),
        BigDecimal("1000000000000000000000000000000"),
        BigDecimal("10000000000000000000000000000000"),
        BigDecimal("100000000000000000000000000000000"),
        BigDecimal("1000000000000000000000000000000000"),
        BigDecimal("1E0"),
        BigDecimal("1E1"),
        BigDecimal("1E2"),
        BigDecimal("1E3"),
        BigDecimal("1E4"),
        BigDecimal("1E5"),
        BigDecimal("1E-1"),
        BigDecimal("1E-2"),
        BigDecimal("1E-3"),
        BigDecimal("1E-4"),
        BigDecimal("1E-5"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(bd: BigDecimal) {
        val n = Decimal(bd)
        val up = Decimal()
        val down = Decimal()
        val ctx = DecimalContext()
        up.setNextUp(n, ctx)
        down.setNextDown(n, ctx)

        println("n:$n up:$up down:$down")

        val t = Decimal()

        t.setNextUp(down, ctx)
        assertEquals(n, t)

        t.setNextDown(up, ctx)
        assertEquals(n, t)

        assert(n.compareTo(up, ctx) < 0)
        assert(n.compareTo(down, ctx) > 0)
    }
}