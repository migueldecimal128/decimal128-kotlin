package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertTrue

class TestDecimalPown {

    val verbose = true

    data class TC(
        val baseStr: String,
        val n: Int,
        val expectedStr: String,
        val expectDivByZero: Boolean = false,
        val expectOverflow: Boolean = false,
        val expectUnderflow: Boolean = false,
        val expectInvalid: Boolean = false,
        val useEQ: Boolean = false  // true = numeric EQ, false = bitwiseEQ
    )

    val tcs = arrayOf(
        TC("1.0", 3, "1.000"),
        TC("10", 6144, "1E+6144"),


        // ---- pow == 0 ------------------------------------------------------------
        TC("3.7", 0, "1E+0"),
        TC("-3.7", 0, "1E+0"),
        TC("0", 0, "1E+0"),
        TC("-0", 0, "1E+0"),
        TC("Inf", 0, "1E+0"),
        TC("-Inf", 0, "1E+0"),

        // ---- pow == 1 ------------------------------------------------------------
        TC("3.7", 1, "3.7"),
        TC("-3.7", 1, "-3.7"),

        // ---- pow == 2 ------------------------------------------------------------
        TC("3", 2, "9"),
        TC("-3", 2, "9"),

        // ---- normal finite values ------------------------------------------------
        TC("2", 3, "8"),
        TC("-3", 4, "81"),
        TC("-3", 3, "-27"),
        TC("-3", 5, "-243"),

        // ---- powers of 10 -------------------------------------------------------
        TC("10", 10, "10_000_000_000"),
        TC("10", 0, "1"),
        TC("10", 1, "10"),
        TC("10", 2, "100"),
        TC("10", 3, "1000"),
        TC("10", 32, "1_00000000_00000000_00000000_00000000"),
        TC("10", 33, "1000000000000000000000000000000000"),
        TC("10", 34, "1E34"),
        TC("10", 6144, "1E+6144"),
        TC("10", 6145, "Inf", expectOverflow = true),
        TC("10", -1, "0.1"),
        TC("10", -6176, "1E-6176"),
        TC("10", -6177, "0E-6176", expectUnderflow = true),

        // cohort variants
        TC("10.0", 3, "1000.000"),
        TC("10.00", 3, "1000.000000"),
        TC("10.000", 3, "1000.000000000"),
        TC("10.0", 33, "1000000000000000000000000000000000"),
        TC("10", 10, "1e10", useEQ = true),

        TC("1E1", 3, "1E3"),
        TC(".1e2", 3, "1E3"),

        // negative base
        TC("-10", 3, "-1000"),
        TC("-10", 2, "100"),

        // 1E1 cohort variants
        TC("1E1", 3, "1E+3"),
        TC("1E1", 0, "1"),
        TC("1E1", 1, "1E+1"),
        TC("1E1", 2, "1E+2"),
        TC("1E1", -1, "1E-1"),
        TC("1E1", -3, "1E-3"),

        // 1E2 cohort
        TC("1E2", 3, "1E+6"),
        TC("1E2", -1, "1E-2"),

        // mixed cohorts
        TC("0.1E2", 3, "1E+3"),   // 0.1E2 = 10
        TC("10.0", 3, "1000.000"),
        TC("100E-1", 3, "1000.000"),  // 100E-1 = 10, Q = -1
        TC("1000E-2", 3, "1000.000000"),  // Q = -2

        // ---- negative pow --------------------------------------------------------
        TC("2", -1, "0.5"),
        TC("-2", -2, "0.25"),
        TC("-2", -1, "-0.5"),

        // ---- zero base -----------------------------------------------------------
        TC("0", 3, "0"),
        TC("-0", 3, "-0"),
        TC("-0", 4, "0"),
        TC("0", -1, "Inf", expectDivByZero = true),
        TC("-0", -1, "-Inf", expectDivByZero = true),
        TC("-0", -2, "Inf", expectDivByZero = true),

        // ---- zero base exponent cohort -------------------------------------------
        TC("0E+2", 3, "0E+6"),
        TC("-0E+1", 3, "-0E+3"),
        TC("-0E+1", 4, "0E+4"),

        // ---- infinity base -------------------------------------------------------
        TC("Inf", 3, "Inf"),
        TC("-Inf", 3, "-Inf"),
        TC("-Inf", 4, "Inf"),
        TC("Inf", -1, "0"),
        TC("-Inf", -1, "-0"),
        TC("-Inf", -2, "0"),

        // ---- NaN -----------------------------------------------------------------
        TC("NaN", 0, "1"),

        // ---- large pow / overflow / underflow ------------------------------------
        TC("1E+100", 1000, "Inf", expectOverflow = true),
        TC("-1E+100", 1001, "-Inf", expectOverflow = true),
        TC("-1E+100", 1000, "Inf", expectOverflow = true),
        TC("1E-100", 1000, "0E-6176", expectUnderflow = true),
        TC("-1E-100", 1001, "-0E-6176", expectUnderflow = true),

        // ---- magnitude 1 base -------------------------------------------------------
        TC("1", 0, "1"),
        TC("1", 1, "1"),
        TC("1", 1000, "1"),
        TC("-1", 1, "-1"),
        TC("-1", 2, "1"),
        TC("-1", 3, "-1"),
        TC("-1", 1000, "1"),
        TC("-1", 1001, "-1"),

        // magnitude 1 cohort variants
        TC("1.0", 3, "1.000"),
        TC("1.00", 3, "1.000000"),
        TC("1E1", 0, "1"),

        // ---- base between 0 and 1 ---------------------------------------------------
        TC("0.5", 2, "0.25"),
        TC("0.1", 3, "0.001"),
        TC("0.1", -1, "1E1"),

        // ---- sNaN -------------------------------------------------------------------
        TC("sNaN", 0, "NaN", expectInvalid = true),
        TC("sNaN", 1, "NaN", expectInvalid = true),
        TC("sNaN", 5, "NaN", expectInvalid = true),
        TC("sNaN", -1, "NaN", expectInvalid = true),

        TC("sNaN123", 5, "NaN123", expectInvalid = true),
        TC("sNaN456", 0, "NaN456", expectInvalid = true),

        // ---- qNaN -------------------------------------------------------------------
        TC("NaN", 5, "NaN"),
        TC("NaN", -1, "NaN"),
        TC("NaN123", 5, "NaN123"),
        TC("NaN", 0, "1"),  // NaN^0 = 1, not NaN
        )

    @Test
    fun testDecimalCases() {
        for (tc in tcs)
            testDecimal(tc)
    }

    @Test
    fun testMutDecCases() {
        for (tc in tcs)
            testMutDec(tc)
    }

    private fun testDecimal(tc: TC) {
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val base = tc.baseStr.toDecimal()
            val result = base.pow(tc.n)
            val expected = tc.expectedStr.toDecimal()
            if (tc.useEQ)
                assertTrue(result EQ expected, "pow(${tc.baseStr}, ${tc.n}): expected $expected observed $result")
            else
                assertTrue(
                    result bitwiseEQ expected,
                    "pow(${tc.baseStr}, ${tc.n}): expected $expected observed $result"
                )
            if (tc.expectDivByZero)
                assertTrue(
                    ctx.isSet(DecException.DIVIDE_BY_ZERO),
                    "Expected DIVIDE_BY_ZERO for pow(${tc.baseStr}, ${tc.n})"
                )
            if (tc.expectOverflow)
                assertTrue(ctx.isSet(DecException.OVERFLOW), "Expected OVERFLOW for pow(${tc.baseStr}, ${tc.n})")
            if (tc.expectUnderflow)
                assertTrue(ctx.isSet(DecException.UNDERFLOW), "Expected UNDERFLOW for pow(${tc.baseStr}, ${tc.n})")
            if (tc.expectInvalid)
                assertTrue(
                    ctx.isSet(DecException.INVALID_OPERATION),
                    "Expected INVALID_OPERATION for pow(${tc.baseStr}, ${tc.n})"
                )
        }

    }

    private fun testMutDec(tc: TC) {
        if (verbose)
            println("$tc")
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val base = MutDec().set(tc.baseStr)
            val result = MutDec().setPown(base, tc.n)
            val expected = MutDec().set(tc.expectedStr)
            if (tc.useEQ)
                assertTrue(result EQ expected, "pow(${tc.baseStr}, ${tc.n}): expected $expected observed $result")
            else
                assertTrue(
                    result bitwiseEQ expected,
                    "pow(${tc.baseStr}, ${tc.n}): expected $expected observed $result"
                )
            if (tc.expectDivByZero)
                assertTrue(
                    ctx.isSet(DecException.DIVIDE_BY_ZERO),
                    "Expected DIVIDE_BY_ZERO for pow(${tc.baseStr}, ${tc.n})"
                )
            if (tc.expectOverflow)
                assertTrue(ctx.isSet(DecException.OVERFLOW), "Expected OVERFLOW for pow(${tc.baseStr}, ${tc.n})")
            if (tc.expectUnderflow)
                assertTrue(ctx.isSet(DecException.UNDERFLOW), "Expected UNDERFLOW for pow(${tc.baseStr}, ${tc.n})")
            if (tc.expectInvalid)
                assertTrue(
                    ctx.isSet(DecException.INVALID_OPERATION),
                    "Expected INVALID_OPERATION for pow(${tc.baseStr}, ${tc.n})"
                )
        }

    }

}

