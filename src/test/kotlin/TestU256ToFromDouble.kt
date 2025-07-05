package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger

class TestU256ToFromDouble {

    val verbose = true

    class TC(val str: String) {
        val d = str.toDouble()
        val coeff = run { val coeff = U256(); coeff.u256Set(str); coeff}
    }

    val tcs = arrayOf(
        TC("1"),
        TC("1000"),
        TC(BigInteger.ONE.shiftLeft(255).toString()),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        if (verbose)
            println("tc.d:${tc.d}, tc.coeff:${tc.coeff}")
        val coeff = U256()
        coeff.u256Set(tc.d)
        if (verbose)
            println(" ==> coeff:$coeff tc.coeff:${tc.coeff}")
        assertEquals(coeff, tc.coeff)
        assertEquals(tc.d, coeff.u256ToFloorDouble())
    }

}
