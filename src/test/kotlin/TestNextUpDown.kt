package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigDecimal

class TestNextUpDown {

    val tcs = arrayOf(
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
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(bd: BigDecimal) {
        val n = Dec34(bd)
        val up = Dec34()
        val down = Dec34()
        val ctx = DecimalContext()
        up.setNextUp(n, ctx)
        down.setNextDown(n, ctx)

        println("n:$n up:$up down:$down")

        val t = Dec34()

        t.setNextUp(down, ctx)
        assertEquals(n, t)

        t.setNextDown(up, ctx)
        assertEquals(n, t)

        assert(n.compareTo(up, ctx) < 0)
        assert(n.compareTo(down, ctx) > 0)
    }
}