package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestFormatStyle {

    val verbose = true

    data class TC(
        val str: String,
        val auto: String,
        val engineering: String,
        val scientific: String,
        val qExp: String)

    val tcs = arrayOf(
        TC("-0",    auto = "-0",      engineering = "-0",      scientific = "-0E+0",    qExp = "-0E+0"),

        // --- Zero (cohort matters; 0 vs 0E+0 vs 0.0) ---
        TC("0",     auto = "0",       engineering = "0",       scientific = "0E+0",     qExp = "0E+0"),
        TC("-0",    auto = "-0",      engineering = "-0",      scientific = "-0E+0",    qExp = "-0E+0"),
        TC("0.0",   auto = "0.0",     engineering = "0.0",     scientific = "0E-1",     qExp = "0E-1"),
        TC("0E+5",  auto = "0E+5",    engineering = "0.0E+6",  scientific = "0E+5",     qExp = "0E+5"),
        TC("0E-5",  auto = "0.00000", engineering = "0.00000", scientific = "0E-5",     qExp = "0E-5"),

        // --- Simple integers ---
        TC("1",    auto = "1",    engineering = "1",    scientific = "1E+0",     qExp = "1E+0"),
        TC("-1",   auto = "-1",   engineering = "-1",   scientific = "-1E+0",    qExp = "-1E+0"),
        TC("123",  auto = "123",  engineering = "123",  scientific = "1.23E+2",  qExp = "123E+0"),
        TC("-123", auto = "-123", engineering = "-123", scientific = "-1.23E+2", qExp = "-123E+0"),

        // --- Cohort: trailing zeros preserved ---
        TC("1.0",   auto = "1.0",   engineering = "1.0",   scientific = "1.0E+0",   qExp = "10E-1"),
        TC("1.00",  auto = "1.00",  engineering = "1.00",  scientific = "1.00E+0",  qExp = "100E-2"),
        TC("100",   auto = "100",   engineering = "100",   scientific = "1.00E+2",  qExp = "100E+0"),
        TC("100.0", auto = "100.0", engineering = "100.0", scientific = "1.000E+2", qExp = "1000E-1"),

        // --- Plain decimal range (qExp < 0, sciExp >= -6) ---
        TC("0.1",      auto = "0.1",      engineering = "0.1",      scientific = "1E-1",     qExp = "1E-1"),
        TC("0.01",     auto = "0.01",     engineering = "0.01",     scientific = "1E-2",     qExp = "1E-2"),
        TC("0.001",    auto = "0.001",    engineering = "0.001",    scientific = "1E-3",     qExp = "1E-3"),
        TC("0.000001", auto = "0.000001", engineering = "0.000001", scientific = "1E-6",     qExp = "1E-6"),
        TC("1.23",     auto = "1.23",     engineering = "1.23",     scientific = "1.23E+0",  qExp = "123E-2"),
        TC("12.34",    auto = "12.34",    engineering = "12.34",    scientific = "1.234E+1", qExp = "1234E-2"),

        // --- Crosses minPlainExponent boundary (default -6): plain form gives way to scientific ---
        TC("0.0000001",     auto = "1E-7",      engineering = "100E-9",    scientific = "1E-7",      qExp = "1E-7"),
        TC("0.00000012345", auto = "1.2345E-7", engineering = "123.45E-9", scientific = "1.2345E-7", qExp = "12345E-11"),

        // --- Positive exponent: AUTO and ALWAYS_SCIENTIFIC scientific; ENGINEERING uses x10^(3k) ---
        TC("1E+1",    auto = "1E+1",    engineering = "10",      scientific = "1E+1",    qExp = "1E+1"),
        TC("1E+2",    auto = "1E+2",    engineering = "100",     scientific = "1E+2",    qExp = "1E+2"),
        TC("1E+3",    auto = "1E+3",    engineering = "1E+3",    scientific = "1E+3",    qExp = "1E+3"),
        TC("1E+4",    auto = "1E+4",    engineering = "10E+3",   scientific = "1E+4",    qExp = "1E+4"),
        TC("1E+5",    auto = "1E+5",    engineering = "100E+3",  scientific = "1E+5",    qExp = "1E+5"),
        TC("1E+6",    auto = "1E+6",    engineering = "1E+6",    scientific = "1E+6",    qExp = "1E+6"),
        TC("12.3E+5", auto = "1.23E+6", engineering = "1.23E+6", scientific = "1.23E+6", qExp = "123E+4"),

        // --- Engineering tens grouping (1.23E+5 → 123E+3) ---
        TC("1.23E+4",  auto = "1.23E+4",  engineering = "12.3E+3",  scientific = "1.23E+4",  qExp = "123E+2"),
        TC("1.23E+5",  auto = "1.23E+5",  engineering = "123E+3",   scientific = "1.23E+5",  qExp = "123E+3"),
        TC("1.234E+5", auto = "1.234E+5", engineering = "123.4E+3", scientific = "1.234E+5", qExp = "1234E+2"),

        // --- Subnormal-range tiny values ---
        TC("1E-10", auto = "1E-10", engineering = "100E-12", scientific = "1E-10", qExp = "1E-10"),
        TC("1E-11", auto = "1E-11", engineering = "10E-12",  scientific = "1E-11", qExp = "1E-11"),
        TC("1E-12", auto = "1E-12", engineering = "1E-12",   scientific = "1E-12", qExp = "1E-12"),

        // --- Negative values mirror positive ---
        TC("-1.23",    auto = "-1.23",    engineering = "-1.23",   scientific = "-1.23E+0", qExp = "-123E-2"),
        TC("-1E-7",    auto = "-1E-7",    engineering = "-100E-9", scientific = "-1E-7",    qExp = "-1E-7"),
        TC("-1.23E+5", auto = "-1.23E+5", engineering = "-123E+3", scientific = "-1.23E+5", qExp = "-123E+3"),

        // --- 34-digit coefficient at various exponents ---
        TC(
            "1234567890123456789012345678901234",
            auto         = "1234567890123456789012345678901234",
            engineering  = "1234567890123456789012345678901234",
            scientific   = "1.234567890123456789012345678901234E+33",
            qExp         = "1234567890123456789012345678901234E+0"
        ),

        // --- Specials (FormatStyle has no effect on non-finites; same in all 4 columns) ---
        TC("Infinity",  auto = "Infinity",  engineering = "Infinity",  scientific = "Infinity",  qExp = "Infinity"),
        TC("-Infinity", auto = "-Infinity", engineering = "-Infinity", scientific = "-Infinity", qExp = "-Infinity"),
        TC("NaN",       auto = "NaN",       engineering = "NaN",       scientific = "NaN",       qExp = "NaN"),
        TC("-NaN",      auto = "-NaN",      engineering = "-NaN",      scientific = "-NaN",      qExp = "-NaN"),
        TC("sNaN",      auto = "sNaN",      engineering = "sNaN",      scientific = "sNaN",      qExp = "sNaN"),
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
        val ctxAlwaysScientific = ctx.with(printPrefs.copy(formatStyle = FormatStyle.ALWAYS_SCIENTIFIC))
        val ctxCoeffQExp = ctx.with(printPrefs.copy(formatStyle = FormatStyle.COEFFICIENT_QEXPONENT))

        val auto = ctxAuto.eval { x.toString() }
        val engineering = ctxEngineering.eval { x.toString() }
        val alwaysScientific = ctxAlwaysScientific.eval { x.toString() }
        val coeffQExp = ctxCoeffQExp.eval { x.toString() }

        assertEquals(tc.auto, auto, "expected auto")
        assertEquals(tc.engineering, engineering, "expected engineering")
        assertEquals(tc.scientific, alwaysScientific, "expected always scientific")
        assertEquals(tc.qExp, coeffQExp, "expected coeff exp")

    }
}