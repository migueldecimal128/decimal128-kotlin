package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestPrintInfinity {

    val verbose = false

    data class TC(
        val sign: Boolean,
        val infinityShort: Boolean,
        val specialsCase: SpecialsCase,
        val expected: String
    ) {
        override fun toString() =
            "signBit=$sign infinityShort=$infinityShort case=$specialsCase → \"$expected\""
    }

    val tcs = arrayOf(
        TC(sign = false, infinityShort = false, specialsCase = SpecialsCase.UPPERCASE, expected = "INFINITY"),

        // --- MIXEDCASE (GDAS canonical), full word ---
        TC(sign = false, infinityShort = false, specialsCase = SpecialsCase.MIXEDCASE, expected = "Infinity"),
        TC(sign = true,  infinityShort = false, specialsCase = SpecialsCase.MIXEDCASE, expected = "-Infinity"),

        // --- MIXEDCASE, short form ---
        TC(sign = false, infinityShort = true,  specialsCase = SpecialsCase.MIXEDCASE, expected = "Inf"),
        TC(sign = true,  infinityShort = true,  specialsCase = SpecialsCase.MIXEDCASE, expected = "-Inf"),

        // --- LOWERCASE (Swift/C/Python float / JS lowercase), full word ---
        TC(sign = false, infinityShort = false, specialsCase = SpecialsCase.LOWERCASE, expected = "infinity"),
        TC(sign = true,  infinityShort = false, specialsCase = SpecialsCase.LOWERCASE, expected = "-infinity"),

        // --- LOWERCASE, short form ---
        TC(sign = false, infinityShort = true,  specialsCase = SpecialsCase.LOWERCASE, expected = "inf"),
        TC(sign = true,  infinityShort = true,  specialsCase = SpecialsCase.LOWERCASE, expected = "-inf"),

        // --- UPPERCASE (IBM mainframe / printf %F), full word ---
        TC(sign = false, infinityShort = false, specialsCase = SpecialsCase.UPPERCASE, expected = "INFINITY"),
        TC(sign = true,  infinityShort = false, specialsCase = SpecialsCase.UPPERCASE, expected = "-INFINITY"),

        // --- UPPERCASE, short form ---
        TC(sign = false, infinityShort = true,  specialsCase = SpecialsCase.UPPERCASE, expected = "INF"),
        TC(sign = true,  infinityShort = true,  specialsCase = SpecialsCase.UPPERCASE, expected = "-INF"),
    )

    @Test
    fun testPrintInfinity() {
        for (tc in tcs) {
            test1(tc)
        }
    }

    fun test1(tc: TC) {
        if (verbose) {
            println(tc)
        }

        val x = if (tc.sign) Decimal.NEG_INFINITY else Decimal.POS_INFINITY
        val printPrefs = PrintPrefs(
            infinityShort = tc.infinityShort,
            specialsCase = tc.specialsCase
        )
        val ctx = DecContext.decimal128IEEE().with(printPrefs)
        val actual = ctx.eval { x.toString() }

        assertEquals(tc.expected, actual, "$tc")
    }
}