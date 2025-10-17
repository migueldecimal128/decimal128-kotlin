package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TestMutDecQuantize {

    val verbose = false

    class TC(val bdX: BigDecimal, val bdY: BigDecimal, val env: env) {
        constructor(strX: String, strY: String) :
                this(BigDecimal(strX), BigDecimal(strY), env())
        val targetScale = bdY.scale()
        val targetQ = -targetScale
        val expected =
            bdToIeeeDecimal128(bdX.setScale(targetScale,
                env.decRounding.mapToRoundingMode()),
                env.decRounding.mapToRoundingMode())
    }

    val tcs = arrayOf(
        TC("2e0", "1.00"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        val bdX = tc.bdX
        val bdY = tc.bdY
        val targetQ = tc.targetQ
        val expected = tc.expected
        if (verbose)
            println("bdX:$bdX bdY:$bdY targetQ:$targetQ => expected:$expected")

        val d = MutDec()
        val e = MutDec()
        val f = MutDec()
        d.set(bdX)
        e.set(bdY)
        f.quantize(d,e, tc.env)
        if (verbose)
            println("f:$f")
    }
}