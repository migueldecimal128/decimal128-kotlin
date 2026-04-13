package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertTrue

class TestDecimalPow {

    val verbose = true

    data class TC(
        val baseStr: String,
        val powerStr: String,
        val expectedStr: String,
        val expectDivByZero: Boolean = false,
        val expectOverflow: Boolean = false,
        val expectUnderflow: Boolean = false,
        val expectInvalid: Boolean = false,
        val useEQ: Boolean = false
    )

    val tcs = arrayOf(
        // ---- power == 0 ------------------------------------------------------------
        TC("3.7", "0", "1E+0"),
        TC("-3.7", "0", "1E+0"),
        TC("0", "0", "1E+0"),
        TC("-0", "0", "1E+0"),
        TC("Inf", "0", "1E+0"),
        TC("-Inf", "0", "1E+0"),
        TC("NaN", "0", "1E+0"),

        // ---- power == 1 ------------------------------------------------------------
        TC("3.7", "1", "3.7"),
        TC("-3.7", "1", "-3.7"),

        // ---- integer powers (delegate to pown) -------------------------------------
        TC("2", "3", "8"),
        TC("2", "-1", "0.5"),
        TC("-3", "4", "81"),
        TC("-3", "3", "-27"),
        TC("10", "3", "1000"),
        TC("10", "-3", "0.001"),

        // ---- non-integer powers ----------------------------------------------------
        TC("100", "0.5", "10"),           // sqrt(100) = 10
        TC("1000", "0.333333333333333333333333333333333", "10", useEQ = true),  // cube root
        TC("2", "0.5", "1.4142135623730950488016887242096980", useEQ = true),   // sqrt(2)
        TC("10", "0.5", "3.1622776601683793319988935444327185", useEQ = true),  // sqrt(10)
        TC("2", "2.5", "5.6568542494923805819800645309937520", useEQ = true),

        // ---- negative base, non-integer power → invalid ----------------------------
        TC("-2", "0.5", "NaN", expectInvalid = true),
        TC("-3.7", "1.5", "NaN", expectInvalid = true),

        // ---- zero base -------------------------------------------------------------
        TC("0", "0.5", "0"),
        TC("0", "-1", "Inf", expectDivByZero = true),
        TC("0", "-0.5", "Inf", expectDivByZero = true),
        TC("-0", "0.5", "0"),
        TC("-0", "-1", "Inf", expectDivByZero = true),

        // ---- infinity base ---------------------------------------------------------
        TC("Inf", "0.5", "Inf"),
        TC("Inf", "-0.5", "0"),
        TC("Inf", "2", "Inf"),
        TC("Inf", "-2", "0"),
        TC("-Inf", "3", "-Inf"),
        TC("-Inf", "4", "Inf"),
        TC("-Inf", "-1", "-0"),
        TC("-Inf", "-2", "0"),

        // ---- special base values ---------------------------------------------------
        TC("1", "99999", "1"),
        TC("1", "0.12345", "1"),
        TC("-1", "Inf", "1"),
        TC("-1", "-Inf", "1"),

        // ---- power == infinity -----------------------------------------------------
        TC("0.5", "Inf", "0"),
        TC("0.5", "-Inf", "Inf"),
        TC("2", "Inf", "Inf"),
        TC("2", "-Inf", "0"),
        TC("1.5", "Inf", "Inf"),
        TC("0.9999", "Inf", "0"),
        TC("1.0001", "-Inf", "0"),

        // ---- overflow / underflow --------------------------------------------------
        TC("1E+100", "1000", "Inf", expectOverflow = true),
        TC("1E-100", "1000", "0E-6176", expectUnderflow = true),

        // ---- NaN -------------------------------------------------------------------
        TC("NaN", "2", "NaN"),
        TC("2", "NaN", "NaN"),
        TC("NaN", "NaN", "NaN"),
        TC("sNaN", "2", "NaN", expectInvalid = true),
        TC("2", "sNaN", "NaN", expectInvalid = true),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    private fun test1(tc: TC) {
        val ctx = DecContext.decimal128IEEE()
        ctx.eval {
            val base = tc.baseStr.toDecimal()
            val power = tc.powerStr.toDecimal()
            val result = base.pow(power)
            val expected = tc.expectedStr.toDecimal()
            if (tc.useEQ)
                assertTrue(result EQ expected, "pow(${tc.baseStr}, ${tc.powerStr}): expected $expected observed $result")
            else
                assertTrue(
                    result bitwiseEQ expected,
                    "pow(${tc.baseStr}, ${tc.powerStr}): expected $expected observed $result"
                )
            if (tc.expectDivByZero)
                assertTrue(
                    ctx.isSet(DecException.DIVIDE_BY_ZERO),
                    "Expected DIVIDE_BY_ZERO for pow(${tc.baseStr}, ${tc.powerStr})"
                )
            if (tc.expectOverflow)
                assertTrue(ctx.isSet(DecException.OVERFLOW), "Expected OVERFLOW for pow(${tc.baseStr}, ${tc.powerStr})")
            if (tc.expectUnderflow)
                assertTrue(ctx.isSet(DecException.UNDERFLOW), "Expected UNDERFLOW for pow(${tc.baseStr}, ${tc.powerStr})")
            if (tc.expectInvalid)
                assertTrue(
                    ctx.isSet(DecException.INVALID_OPERATION),
                    "Expected INVALID_OPERATION for pow(${tc.baseStr}, ${tc.powerStr})"
                )
        }

    }

}