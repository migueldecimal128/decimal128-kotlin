package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger

class TestC256ToFromDouble {

    val verbose = false

    class TC(val str: String) {
        val d = str.toDouble()
        val coeff = run { val coeff = C256(); coeff.c256Set(str); coeff}
    }

    val tcs = arrayOf(
        TC("1"),
        TC("1000"),
        TC(BigInteger.ONE.shiftLeft(252).toString()),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        if (verbose)
            println("tc.d:${tc.d}, tc.coeff:${tc.coeff}")
        val coeff = C256()
        coeff.c256Set(tc.d)
        if (verbose)
            println(" ==> coeff:$coeff tc.coeff:${tc.coeff}")
        assertEquals(coeff, tc.coeff)
        assertEquals(tc.d, coeff.c256ToFloorDouble())
    }

}
