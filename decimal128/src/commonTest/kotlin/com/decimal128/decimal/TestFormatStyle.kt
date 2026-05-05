package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestFormatStyle {

    val verbose = false

    data class TC(
        val str: String,
        val auto: String,
        val exponential: String,
        val engineering: String,
        val raw: String
    )

    val tcs = arrayOf(
        // --- Zero (cohort matters; 0 vs 0E+0 vs 0.0) ---
        TC("0",     auto = "0",       exponential = "0E+0",     engineering = "0",       raw = "0E+0"),
        TC("-0",    auto = "-0",      exponential = "-0E+0",    engineering = "-0",      raw = "-0E+0"),
        TC("0.0",   auto = "0.0",     exponential = "0E-1",     engineering = "0.0",     raw = "0E-1"),
        TC("0E+5",  auto = "0E+5",    exponential = "0E+5",     engineering = "0.0E+6",  raw = "0E+5"),
        TC("0E-5",  auto = "0.00000", exponential = "0E-5",     engineering = "0.00000", raw = "0E-5"),

        // --- Simple integers ---
        TC("1",     auto = "1",       exponential = "1E+0",     engineering = "1",       raw = "1E+0"),
        TC("-1",    auto = "-1",      exponential = "-1E+0",    engineering = "-1",      raw = "-1E+0"),
        TC("123",   auto = "123",     exponential = "1.23E+2",  engineering = "123",     raw = "123E+0"),
        TC("-123",  auto = "-123",    exponential = "-1.23E+2", engineering = "-123",    raw = "-123E+0"),

        // --- Cohort: trailing zeros preserved ---
        TC("1.0",   auto = "1.0",     exponential = "1.0E+0",   engineering = "1.0",     raw = "10E-1"),
        TC("1.00",  auto = "1.00",    exponential = "1.00E+0",  engineering = "1.00",    raw = "100E-2"),
        TC("100",   auto = "100",     exponential = "1.00E+2",  engineering = "100",     raw = "100E+0"),
        TC("100.0", auto = "100.0",   exponential = "1.000E+2", engineering = "100.0",   raw = "1000E-1"),

        // --- Plain decimal range (qExp < 0, sciExp >= -6) ---
        TC("0.1",       auto = "0.1",       exponential = "1E-1",     engineering = "0.1",       raw = "1E-1"),
        TC("0.01",      auto = "0.01",      exponential = "1E-2",     engineering = "0.01",      raw = "1E-2"),
        TC("0.001",     auto = "0.001",     exponential = "1E-3",     engineering = "0.001",     raw = "1E-3"),
        TC("0.000001",  auto = "0.000001",  exponential = "1E-6",     engineering = "0.000001",  raw = "1E-6"),
        TC("1.23",      auto = "1.23",      exponential = "1.23E+0",  engineering = "1.23",      raw = "123E-2"),
        TC("12.34",     auto = "12.34",     exponential = "1.234E+1", engineering = "12.34",     raw = "1234E-2"),

        // --- Crosses minPlainExponent boundary (default -6): plain form gives way to scientific ---
        TC("0.0000001",     auto = "1E-7",      exponential = "1E-7",      engineering = "100E-9",    raw = "1E-7"),
        TC("0.00000012345", auto = "1.2345E-7", exponential = "1.2345E-7", engineering = "123.45E-9", raw = "12345E-11"),

        // --- Positive exponent: AUTO and EXPONENTIAL scientific; ENGINEERING uses x10^(3k) ---
        TC("1E+1",    auto = "1E+1",    exponential = "1E+1",    engineering = "10",      raw = "1E+1"),
        TC("1E+2",    auto = "1E+2",    exponential = "1E+2",    engineering = "100",     raw = "1E+2"),
        TC("1E+3",    auto = "1E+3",    exponential = "1E+3",    engineering = "1E+3",    raw = "1E+3"),
        TC("1E+4",    auto = "1E+4",    exponential = "1E+4",    engineering = "10E+3",   raw = "1E+4"),
        TC("1E+5",    auto = "1E+5",    exponential = "1E+5",    engineering = "100E+3",  raw = "1E+5"),
        TC("1E+6",    auto = "1E+6",    exponential = "1E+6",    engineering = "1E+6",    raw = "1E+6"),
        TC("12.3E+5", auto = "1.23E+6", exponential = "1.23E+6", engineering = "1.23E+6", raw = "123E+4"),

        // --- Engineering tens grouping (1.23E+5 → 123E+3) ---
        TC("1.23E+4",  auto = "1.23E+4",  exponential = "1.23E+4",  engineering = "12.3E+3",  raw = "123E+2"),
        TC("1.23E+5",  auto = "1.23E+5",  exponential = "1.23E+5",  engineering = "123E+3",   raw = "123E+3"),
        TC("1.234E+5", auto = "1.234E+5", exponential = "1.234E+5", engineering = "123.4E+3", raw = "1234E+2"),

        // --- Subnormal-range tiny values ---
        TC("1E-10", auto = "1E-10", exponential = "1E-10", engineering = "100E-12", raw = "1E-10"),
        TC("1E-11", auto = "1E-11", exponential = "1E-11", engineering = "10E-12",  raw = "1E-11"),
        TC("1E-12", auto = "1E-12", exponential = "1E-12", engineering = "1E-12",   raw = "1E-12"),

        // --- Negative values mirror positive ---
        TC("-1.23",    auto = "-1.23",    exponential = "-1.23E+0",  engineering = "-1.23",    raw = "-123E-2"),
        TC("-1E-7",    auto = "-1E-7",    exponential = "-1E-7",     engineering = "-100E-9",  raw = "-1E-7"),
        TC("-1.23E+5", auto = "-1.23E+5", exponential = "-1.23E+5",  engineering = "-123E+3",  raw = "-123E+3"),

        // --- 34-digit coefficient at various exponents ---
        TC(
            "1234567890123456789012345678901234",
            auto        = "1234567890123456789012345678901234",
            exponential = "1.234567890123456789012345678901234E+33",
            engineering = "1234567890123456789012345678901234",
            raw         = "1234567890123456789012345678901234E+0"
        ),

        // --- Specials (FormatStyle has no effect on non-finites; same in all 4 columns) ---
        TC("Infinity",  auto = "Infinity",  exponential = "Infinity",  engineering = "Infinity",  raw = "Infinity"),
        TC("-Infinity", auto = "-Infinity", exponential = "-Infinity", engineering = "-Infinity", raw = "-Infinity"),
        TC("NaN",       auto = "NaN",       exponential = "NaN",       engineering = "NaN",       raw = "NaN"),
        TC("-NaN",      auto = "-NaN",      exponential = "-NaN",      engineering = "-NaN",      raw = "-NaN"),
        TC("sNaN",      auto = "sNaN",      exponential = "sNaN",      engineering = "sNaN",      raw = "sNaN"),
    )

    @Test
    fun testFormatStyle() {
        for (tc in tcs) {
            test1(tc)
        }
    }

    fun test1(tc: TC) {
        if (verbose) {
            println(tc)
        }

        val x = tc.str.toDecimal()
        val printPrefs = PrintPrefs.DEFAULT_IEEE
        val ctx = DecContext.decimal128IEEE()
        val ctxAuto = ctx.with(printPrefs.copy(formatStyle = FormatStyle.AUTO))
        val ctxEngineering = ctx.with(printPrefs.copy(formatStyle = FormatStyle.ENGINEERING))
        val ctxAlwaysScientific = ctx.with(printPrefs.copy(formatStyle = FormatStyle.EXPONENTIAL))
        val ctxCoeffQExp = ctx.with(printPrefs.copy(formatStyle = FormatStyle.RAW))

        val auto = ctxAuto.eval { x.toString() }
        val engineering = ctxEngineering.eval { x.toString() }
        val alwaysScientific = ctxAlwaysScientific.eval { x.toString() }
        val coeffQExp = ctxCoeffQExp.eval { x.toString() }

        assertEquals(tc.auto, auto, "expected auto")
        assertEquals(tc.engineering, engineering, "expected engineering")
        assertEquals(tc.exponential, alwaysScientific, "expected always scientific")
        assertEquals(tc.raw, coeffQExp, "expected coeff exp")

    }
}