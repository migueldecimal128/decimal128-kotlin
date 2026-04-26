package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestDecimalQuantize {

    val verbose = false

    data class TC(val xStr: String, val yStr: String, val expectedStr: String)

    val tcs = arrayOf(

        TC("1.234", "0.01", "1.23"),  // truncate

        TC("9", "0.00", "9.00"),
        TC("-INF", "INF", "-Infinity"),
        TC("nan", "-snan99", "-NaN99"),
        TC("1234567890_1234567890_1234567890_1234", "0", "1234567890123456789012345678901234"),
        TC("1234567890_1234567890_1234567890_1234", "0.0", "NaN"),
        TC("1e-6176", "0.0", "0.0"), // very tiny value gets rounded to 0.0

        // Scale up: basic
        TC("1.5", "0.001", "1.500"),
        TC("0", "0.00", "0.00"),
        TC("-0", "0.00", "-0.00"),

// Scale down with rounding
        TC("1.234", "0.01", "1.23"),  // truncate
        TC("1.235", "0.01", "1.24"),  // round half up
        TC("1.999", "0.01", "2.00"),  // round carries

// Exponent boundaries
        TC("1e+6111", "1e+6111", "1E+6111"),  // max exponent
        TC("1e-6176", "1e-6176", "1E-6176"),  // min exponent (Q_TINY)
        TC("1e-6176", "1e-6175", "0E-6175"),  // subnormal scale down to zero

// Max coefficient
        TC("9999999999999999999999999999999999", "1", "9999999999999999999999999999999999"),
        TC("9999999999999999999999999999999999", "0.1", "NaN"), // would exceed precision

// Infinity cases
        TC("INF", "INF", "Infinity"),
        TC("-INF", "-INF", "-Infinity"),
        TC("INF", "1", "NaN"),
        TC("1", "INF", "NaN"),

// NaN propagation priority
        TC("snan", "1", "NaN"),       // sNaN signals
        TC("nan99", "1", "NaN99"),    // qNaN payload preserved
        TC("snan99", "nan1", "NaN99"), // sNaN takes priority over qNaN
        TC("snan1", "snan2", "NaN1"), // first sNaN takes priority

// Sign preservation
        TC("-0", "0", "-0"),
        TC("-1.5", "0.0", "-1.5"),

        // Rounding modes
        TC("1.235", "0.01", "1.24"),  // already have round half up, need round half to even
        TC("1.245", "0.01", "1.24"),  // round half to even (4 is even, rounds down)

// NaN payload preservation through quantize
        TC("nan99", "nan1", "NaN99"),  // x's payload preserved when both qNaN

// Large scale up
        TC("1", "1e-33", "1.000000000000000000000000000000000"),  // 33 decimal places, exactly 34 digits
        TC("1", "1e-34", "NaN"),  // would exceed 34 digits

// Negative values rounding
        TC("-1.235", "0.01", "-1.24"),  // rounding with negative sign
        TC("-1.999", "0.01", "-2.00"),  // rounding carry with negative sign

// Zero with various exponents
        TC("0.00", "0", "0"),
        TC("0", "0.00", "0.00"),
        TC("-0.00", "0", "-0"),
    )

    @Test
    fun testCases() {
        for (tc in tcs) {
            test(tc)
        }
    }

    fun test(tc: TC) {
        if (verbose)
            println("$tc")
        val ctx = DecContext.decimal128Kotlin().with(DecPrefs.KOTLIN_DEFAULT.copy(printExponentPlusSign = true))
        ctx.eval {
            val x = tc.xStr.toDecimal()
            val y = tc.yStr.toDecimal()

            val observed = x.quantize(y)
            assertEquals(tc.expectedStr, "$observed")
            if (y.isFinite() && observed.isFinite()) {
                assertEquals(y.quantumInt(), observed.quantumInt())
                assertEquals(y.quantum(), observed.quantum())
            }
        }
    }
}
