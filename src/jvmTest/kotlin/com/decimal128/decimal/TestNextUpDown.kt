package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigDecimal

class TestNextUpDown {

    val verbose = false

    val tcs = arrayOf(
        BigDecimal("-1e-6176"),
        BigDecimal("1e-6176"),
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
        val n = newMutDec(bd)
        val up = MutDec()
        val down = MutDec()
        val env = DecContext.decimal128Kotlin()
        up.setNextUp(n, env)
        down.setNextDown(n, env)

        if (verbose)
            println("n:$n up:$up down:$down")

        val t = MutDec()

        t.setNextUp(down, env)
        assertEquals(n, t)

        t.setNextDown(up, env)
        assertEquals(n, t)

        assert(n.compareTo(up) < 0)
        assert(n.compareTo(down) > 0)
    }
}