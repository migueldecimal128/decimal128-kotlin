package com.decimal128.decimal

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestDecWithScale {

    val verbose = true

    data class TC(val str: String, val scale: Int, val expected: String)

    val tcs = arrayOf(
        TC("100", -2, "1E+2"),

        TC("0.10", 1, "0.1"),
        TC("1", 1, "1.0"),
        TC("1", 2, "1.00"),
        TC("1", 33, "1.000000000000000000000000000000000"),
        TC("0.10", 1, "0.1"),
        TC("0", 2, "0.00"),
        TC("0.10", 1, "0.1"),
        TC("0.12", 1, "0.1"),
        TC("1.20000", 1, "1.2"),
        TC("1e-2", 4, "0.0100"),

        TC("1.000000000", 0, "1"),
        TC("100", -2, "1E+2"),
        TC("1.23", 0, "1"),

        // Q_TINY boundary
        TC("1e-6176", 6176, "1E-6176"),        // exactly at Q_TINY, no change
        TC("1e-6175", 6176, "1.0E-6175"),       // scale down to Q_TINY
        TC("1e-6175", 6177, "NaN"),            // below Q_TINY

// Q_MAX boundary
        TC("1e+6111", -6111, "1E+6111"),       // exactly at Q_MAX, no change
        TC("1e+6110", -6111, "0E+6111"),       // scale up to Q_MAX
        TC("1e+6110", -6112, "NaN"),           // above Q_MAX

// Precision boundary (34 digits)
        TC("1", 33, "1.000000000000000000000000000000000"),   // exactly 34 digits
        TC("1", 34, "NaN"),                                   // would exceed 34 digits

// Rounding at scale down
        TC("1.005", 2, "1.00"),
        TC("1.115", 2, "1.12"),    // round half up
        TC("1.004", 2, "1.00"),    // round down
        TC("9.999", 2, "10.00"),   // rounding carry

// Rounding carry at precision boundary
        TC("9999999999999999999999999999999999", 0, "9999999999999999999999999999999999"),  // no change
        TC("9999999999999999999999999999999.999", 0, "10000000000000000000000000000000"),  // rounding carry would exceed 34 digits
        TC("9999999999999999999999999999999999.9", 0, "NaN"),  // 34 nines + rounding carry = 35 digits

// Negative exponent boundaries
        TC("1e+6144", 0, "NaN"),               // supernormal, can't scale down to 0
        TC("1e+6144", -6111, "1.000000000000000000000000000000000E+6144"),  // already at Q_MAX, no change needed

// Sign preservation
        TC("-1e-6176", 6176, "-1E-6176"),      // negative at Q_TINY
        TC("-0", 2, "-0.00"),                  // negative zero
    )

    @Test
    fun testCases() {
        for (tc in tcs) {
            if (verbose)
                println(tc)
            test1(tc)
        }
    }

    fun test1(tc: TC) {
        val ctx = DecContext.decimal128Kotlin()
        ctx.eval {
            val x = Decimal.from(tc.str)
            val scaled = x.withScale(tc.scale)
            assertEquals(tc.expected, scaled.toString())
            if (scaled.isFinite()) {
                assertEquals(-tc.scale, scaled.quantumInt())
                assertEquals((-tc.scale).toDecimal(), scaled.quantum())
            }
        }
    }

}
