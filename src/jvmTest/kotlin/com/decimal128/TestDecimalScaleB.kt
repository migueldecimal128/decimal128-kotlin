package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigDecimal.ONE

class TestDecimalScaleB {

    class TC(val bd: BigDecimal, val pow10: Int, val ctx: DecimalContext) {
        constructor(bd: BigDecimal, pow10: Int) : this(bd, pow10, DecimalContext())
        val expected =
            bdToIeeeDecimal128(bd.scaleByPowerOfTen(pow10),
                        ctx.roundingDirection.mapToRoundingMode())
    }

    val tcs = arrayOf(
        TC(ONE.scaleByPowerOfTen(34).subtract(ONE), 6112),
        TC(ONE, 0),
        TC(ONE, 1),
        TC(ONE, -1),
        TC(ONE.scaleByPowerOfTen(34).subtract(ONE), 6111),
        TC(ONE.scaleByPowerOfTen(34).subtract(ONE), 6112),
        TC(ONE, 6143),
        TC(ONE, 6144),
        TC(ONE, 6145),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        val bd = tc.bd
        val pow10 = tc.pow10
        val expected = tc.expected
        println("$bd pow10:$pow10 => $expected")

        val d = Decimal()
        d.set(bd)
        val s = Decimal()
        s.scaleB(d, pow10, tc.ctx)
        val observed = (
                if (s.isFinite())
                    BigDecimal(s.toString())
                else
                    BigDecimal("1e1000000000")
                )
        assertEquals(expected, observed)
    }
}