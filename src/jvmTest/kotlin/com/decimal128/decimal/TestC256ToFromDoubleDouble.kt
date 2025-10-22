package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger

class TestC256ToFromDoubleDouble {

    val verbose = false

    class TC(val str: String) {
        val coeff = run { val coeff = C256(); coeff.c256Set(str); coeff}
    }

    val tcs = arrayOf(
        TC("1"),
        TC("1000"),
        TC(BigInteger.ONE.shiftLeft(255).toString()),
        TC("123456789012345678901234567890"),
        TC("12345678901234567890123456789012"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        if (verbose)
            println("tc.str:${tc.str} tc.coeff:${tc.coeff}")
        val dd = tc.coeff.c256ToNewDoubleDouble()
        if (verbose)
            println(" ==> dd:$dd tc.coeff:${tc.coeff}")

        val coeff2 = C256()
        coeff2.c256Set(dd)

        assertEquals(tc.coeff, coeff2)
    }

}
