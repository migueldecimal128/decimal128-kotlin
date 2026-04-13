package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDecimalPow {

    val verbose = true

    data class TC(
        val baseStr: String,
        val powerStr: String,
        val expectedStr: String,
        val divByZero: Boolean = false,
        val overflow: Boolean = false,
        val underflow: Boolean = false,
        val invalidOperation: Boolean = false,
        val inexact: Boolean = false,
        val useEQ: Boolean = false
    )

    val tcs = arrayOf(
        TC("1", "9999999999999999999999999999999999", "1"),
        TC("1.00", "2", "1.0000"),
        TC("-1", "Inf", "1"),


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
        //TC("1000", "0.333333333333333333333333333333333", "10", useEQ = true),  // cube root
        TC("2", "0.5", "1.4142135623730950488016887242096980", inexact = true, useEQ = true),   // sqrt(2)
        TC("10", "0.5", "3.162277660168379331998893544432719", inexact = true, useEQ = true),  // sqrt(10)
        TC("2", "2.5", "5.656854249492380195206754896838792", inexact = true, useEQ = true),

        // ---- negative base, non-integer power → invalid ----------------------------
        TC("-2", "0.5", "NaN", invalidOperation = true),
        TC("-3.7", "1.5", "NaN", invalidOperation = true),

        // ---- zero base -------------------------------------------------------------
        TC("0", "0.5", "0"),
        TC("0", "-1", "Inf", divByZero = true),
        TC("0", "-0.5", "Inf", divByZero = true),
        TC("-0", "0.5", "0"),
        TC("-0", "-1", "-Inf", divByZero = true),

        // pow(±0, y) for various y
        TC("0", "2", "0"),           // even integer → +0
        TC("-0", "2", "0"),          // even integer → +0 (not -0)
        TC("-0", "3", "-0"),         // odd integer → -0
        TC("0", "-2", "Inf", divByZero = true),   // even negative integer → +∞
        TC("-0", "-2", "Inf", divByZero = true),  // even negative integer → +∞
        TC("-0", "-3", "-Inf", divByZero = true), // odd negative integer → -∞
        TC("0", "Inf", "0"),         // +∞ exponent → +0
        TC("-0", "Inf", "0"),        // +∞ exponent → +0
        TC("0", "-Inf", "Inf"),      // -∞ exponent → +∞
        TC("-0", "-Inf", "Inf"),     // -∞ exponent → +∞
        TC("0", "0.5", "0"),         // non-integer > 0 → +0
        TC("-0", "0.5", "0"),        // non-integer > 0 → +0 (not -0)
        TC("0", "-0.5", "Inf", divByZero = true),  // non-integer < 0 → +∞
        TC("-0", "-0.5", "Inf", divByZero = true), // non-integer < 0 → +∞

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
        TC("1E+100", "1000", "Inf", inexact = true, overflow = true),
        TC("1E-100", "1000", "0E-6176", inexact = true, underflow = true),

        // ---- NaN -------------------------------------------------------------------
        TC("NaN", "2", "NaN"),
        TC("2", "NaN", "NaN"),
        TC("NaN", "NaN", "NaN"),
        TC("sNaN", "2", "NaN", invalidOperation = true),
        TC("2", "sNaN", "NaN", invalidOperation = true),

        // pow(+1, y) = 1 for any y
        TC("1", "0", "1"),
        TC("1", "1", "1"),
        TC("1", "2", "1"),
        TC("1", "-1", "1"),
        TC("1", "0.5", "1"),
        TC("1", "-0.5", "1"),
        TC("1", "Inf", "1"),
        TC("1", "-Inf", "1"),
        TC("1", "NaN", "1"),
        TC("1", "9999999999999999999999999999999999", "1"),
        TC("1", "-9999999999999999999999999999999999", "1"),

        // cohort variants of 1
        TC("1.0", "0.5", "1.0"),
        TC("1.00", "2", "1.0000"),
        TC("1E+0", "Inf", "1"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    private fun test1(tc: TC) {
        if (verbose)
            println(tc)
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
            assertEquals(tc.divByZero, ctx.isSet(DecException.DIVIDE_BY_ZERO),
                "DIVIDE_BY_ZERO mismatch for pow(${tc.baseStr}, ${tc.powerStr})")
            assertEquals(tc.overflow, ctx.isSet(DecException.OVERFLOW),
                "OVERFLOW mismatch for pow(${tc.baseStr}, ${tc.powerStr})")
            assertEquals(tc.underflow, ctx.isSet(DecException.UNDERFLOW),
                "UNDERFLOW mismatch for pow(${tc.baseStr}, ${tc.powerStr})")
            assertEquals(tc.invalidOperation, ctx.isSet(DecException.INVALID_OPERATION),
                "INVALID_OPERATION mismatch for pow(${tc.baseStr}, ${tc.powerStr})")
            assertEquals(tc.inexact, ctx.isSet(DecException.INEXACT),
                "INEXACT mismatch for pow(${tc.baseStr}, ${tc.powerStr})")
        }

    }

    @Test
    fun testSqrt10() {
        val sqrt10 = Decimal.TEN.sqrt()
        val tenPowHalf = Decimal.TEN.pow("0.5".toDecimal())
        if (verbose)
            println("sqrt10:$sqrt10 tenPowHalf:$tenPowHalf")
        assertTrue(sqrt10 bitwiseEQ tenPowHalf)
    }

    @Test
    fun testSqrt2x4() {
        val sqrt2x4 = Decimal.TWO.sqrt() * Decimal.FOUR
        val powTwoTwoPoint5 = Decimal.TWO.pow("2.5".toDecimal())
        if (verbose)
            println("sqrt2x4:$sqrt2x4 powTwoTwoPoint5:$powTwoTwoPoint5")
        assertTrue(sqrt2x4 bitwiseEQ powTwoTwoPoint5)
    }

}