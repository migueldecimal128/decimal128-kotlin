package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestMutDecCompound {

    val verbose = false

    data class TC(val xStr: String, val n: Int, val expectedStr: String)

    val tcs = arrayOf(
        TC("0.05", Int.MIN_VALUE, "0E-6176"),        // discounts to zero
        TC("1", Int.MIN_VALUE, "0"),

        // compound(x, 0) is 1 for x ≥ -1
        TC("0", 0, "1"),
        TC("0.5", 0, "1"),
        TC("-1", 0, "1"),
        TC("1E+10", 0, "1"),

        // compound(±0, n) is 1
        TC("0", 1, "1"),
        TC("0", -1, "1"),
        TC("0", 100, "1"),

        // compound(-1, n) is +∞ and signals divideByZero for n < 0
        TC("-1", -1, "Infinity"),   // signals divByZero
        TC("-1", -5, "Infinity"),   // signals divByZero

        // compound(-1, n) is +0 for n > 0
        TC("-1", 1, "0"),
        TC("-1", 5, "0"),

        // compound(+∞, n) is +∞ for n > 0
        TC("Infinity", 1, "Infinity"),
        TC("Infinity", 10, "Infinity"),

        // compound(+∞, n) is +0 for n < 0
        TC("Infinity", -1, "0"),
        TC("Infinity", -10, "0"),

        // compound(+∞, 0) is 1 (x ≥ -1 rule takes precedence)
        TC("Infinity", 0, "1"),

        // compound(x, n) is qNaN and signals invalidOperation for x < -1
        TC("-2", 1, "NaN"),     // signals invalidOperation
        TC("-1.0001", 1, "NaN"), // signals invalidOperation
        TC("-Inf", 1, "NaN"),   // signals invalidOperation
        TC("-Inf", 0, "NaN"),   // x < -1 takes precedence over n=0

        // compound(qNaN, n) is qNaN for n ≠ 0
        TC("NaN", 1, "NaN"),
        TC("NaN", -1, "NaN"),

        // compound(qNaN, 0) is 1 per spec
        TC("NaN", 0, "1"),

        // Normal compounding - spot checks
        TC("0.05", 1, "1.05"),
        TC("0.05", 2, "1.1025"),
        TC("1", 1, "2"),
        TC("1", 2, "4"),

        // Discounting (negative n)
        TC("0.05", -1, "0.9523809523809523809523809523809524"),
        TC("1", -1, "0.5"),
        TC("1", -2, "0.25"),

        // These should actually overflow to +Inf
        TC("1", Int.MAX_VALUE, "Infinity"),           // base 2, grows fast
        TC("9.999999999E+6144", 2, "Infinity"),       // huge base squared
        TC("9.999999999E+6144", Int.MAX_VALUE, "Infinity"),

        // Very large positive n
        TC("0.05", Int.MAX_VALUE, "Infinity"),      // grows without bound
        TC("1", Int.MAX_VALUE, "Infinity"),         // 2^MAX_INT

        // Very large negative n (discounting)
        TC("0.05", Int.MIN_VALUE, "0E-6176"),        // discounts to zero
        TC("0.01", -1_000_000, "4.228802060756949496317010804840068E-4322"),
        TC("1", Int.MIN_VALUE, "0"),

        // x very close to -1 from above (near zero base)
        TC("-0.9999999999", 1, "1E-10"),        // 1+x is tiny positive
        TC("-0.9999999999", -1, "1E10"),       // discounting with tiny base
        TC("-0.9999999999", Int.MAX_VALUE, "0E-6176"), // tiny base ^ huge n -> 0
        TC("-0.9999999999", Int.MIN_VALUE, "Infinity"), // tiny base ^ huge neg n -> Inf

        // x very close to -1 from below (should signal invalidOperation)
        TC("-1.0000000001", 1, "NaN"),

        // x extremely large
        TC("9.999999999E+6144", Int.MAX_VALUE, "Infinity"),  // max decimal ^ max int
        TC("9.999999999E+6144", -1, "1.0000000001000000000100000000010E-6145"),               // 1/huge

        // x very small positive (near zero but not zero)
        TC("1E-6144", Int.MAX_VALUE, "1.000000000000000000000000000000000"),      // (1 + tiny)^huge - depends on precision
        TC("1E-6144", 1, "1.000000000000000000000000000000000"),     // rounds to 1 at most precisions

        // n = 1 (identity-ish)
        TC("0", 1, "1"),                        // (1+0)^1 = 1
        TC("-1", 1, "0"),                       // (1+-1)^1 = 0
        TC("1E+6144", 1, "1.000000000000000000000000000000000E6144"),        // may overflow to Infinity depending on precision

        // Quantum edge cases for zero result
        TC("-1", Int.MAX_VALUE, "0"),          // check qExp = floor(MAX_INT * min(0, Q(-1)))
        TC("-1.000", Int.MAX_VALUE, "0E-6176"),      // Q(x) = -3, qExp = floor(MAX_INT * -3) -> clamped
        TC("-1.000", 2, "0.000000"),                  // qExp = floor(2 * -3) = -6
        TC("-1.0", 2, "0.00"),                    // qExp = floor(2 * -1) = -2
    )

    @Test
    fun testCasesMutDec() {
        for (tc in tcs)
            testMutDec(tc)
    }

    fun testMutDec(tc: TC) {
        if (verbose)
            println(tc)
        val x = MutDec().set(tc.xStr)
        val n = tc.n

        val observed = MutDec().setCompound(x, n)

        assertEquals(tc.expectedStr, observed.toString())

    }

    @Test
    fun testCasesDecimal() {
        for (tc in tcs)
            testDecimal(tc)
    }

    fun testDecimal(tc: TC) {
        if (verbose)
            println(tc)
        val x = tc.xStr.toDecimal()
        val n = tc.n

        val observed = x.compound(n)

        assertEquals(tc.expectedStr, observed.toString())

    }

}