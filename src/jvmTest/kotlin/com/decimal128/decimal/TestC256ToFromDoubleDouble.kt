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
        // FIXME - The following case generates an unscaled addition where the
        //  addend has > 127 bits.
        //  This comes from a double-double where hi double has 76 digits
        //  and the lo double has 60 (?).
        //  Unclear to me whether this is a valid test or not.
        //  If I end up using DoubleDouble with > 38 digits then I should
        //  be able to extend the unscaledAdd code so that it allows for
        //  an added of > 38 digits.
        //  Currently it allows a 76 digit augend due to FMA, but I had
        //  completely forgotten about Double and DoubleDouble conversions.
        // TC(BigInteger.TEN.pow(76).subtract(BigInteger.ONE).toString()),

        TC("1"),
        TC("1000"),
        TC(BigInteger.ONE.shiftLeft(252).toString()),
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
