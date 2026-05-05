package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestPrintNAN {

    val verbose = false

    data class TC(
        val sign: Boolean,
        val signaling: Boolean,
        val payload: Long,           // 0 = no payload
        val nanMinusSign: Boolean,
        val nanPayload: Boolean,
        val collapseSNAN: Boolean,
        val specialsCase: SpecialsCase,
        val expected: String
    ) {
        override fun toString() =
            "sign=$sign signaling=$signaling payload=$payload " +
                    "nanMinusSign=$nanMinusSign nanPayload=$nanPayload collapseSNAN=$collapseSNAN " +
                    "case=$specialsCase → \"$expected\""
    }

    val tcs = arrayOf(
        TC(sign = true,  signaling = false, payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "-NaN"),

        // --- MIXEDCASE (GDAS canonical), no payload ---
        TC(sign = false, signaling = false, payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "NaN"),
        TC(sign = true,  signaling = false, payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "-NaN"),
        TC(sign = false, signaling = true,  payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "sNaN"),
        TC(sign = true,  signaling = true,  payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "-sNaN"),

        // --- MIXEDCASE, with payload ---
        TC(sign = false, signaling = false, payload = 123,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "NaN123"),
        TC(sign = true,  signaling = false, payload = 123,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "-NaN123"),
        TC(sign = false, signaling = true,  payload = 456,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "sNaN456"),
        TC(sign = true,  signaling = true,  payload = 456,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "-sNaN456"),

        // --- nanMinusSign = false: sign suppressed (matches Java Double / JS) ---
        TC(sign = true,  signaling = false, payload = 0,
            nanMinusSign = false, nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "NaN"),
        TC(sign = true,  signaling = true,  payload = 0,
            nanMinusSign = false, nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "sNaN"),
        TC(sign = true,  signaling = false, payload = 123,
            nanMinusSign = false, nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "NaN123"),

        // --- nanPayload = false: payload digits suppressed ---
        TC(sign = false, signaling = false, payload = 123,
            nanMinusSign = true,  nanPayload = false, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "NaN"),
        TC(sign = true,  signaling = true,  payload = 456,
            nanMinusSign = true,  nanPayload = false, collapseSNAN = false,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "-sNaN"),

        // --- collapseSNAN = true: sNaN renders as NaN ---
        TC(sign = false, signaling = true,  payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = true,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "NaN"),
        TC(sign = true,  signaling = true,  payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = true,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "-NaN"),
        TC(sign = false, signaling = true,  payload = 789,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = true,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "NaN789"),

        // --- LOWERCASE ---
        TC(sign = false, signaling = false, payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.LOWERCASE, expected = "nan"),
        TC(sign = true,  signaling = false, payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.LOWERCASE, expected = "-nan"),
        TC(sign = false, signaling = true,  payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.LOWERCASE, expected = "snan"),
        TC(sign = true,  signaling = true,  payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.LOWERCASE, expected = "-snan"),
        TC(sign = false, signaling = false, payload = 42,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.LOWERCASE, expected = "nan42"),

        // --- UPPERCASE (IBM mainframe) ---
        TC(sign = false, signaling = false, payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.UPPERCASE, expected = "NAN"),
        TC(sign = true,  signaling = false, payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.UPPERCASE, expected = "-NAN"),
        TC(sign = false, signaling = true,  payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.UPPERCASE, expected = "SNAN"),
        TC(sign = true,  signaling = true,  payload = 0,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.UPPERCASE, expected = "-SNAN"),
        TC(sign = false, signaling = false, payload = 999,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.UPPERCASE, expected = "NAN999"),
        TC(sign = true,  signaling = true,  payload = 1234,
            nanMinusSign = true,  nanPayload = true, collapseSNAN = false,
            specialsCase = SpecialsCase.UPPERCASE, expected = "-SNAN1234"),

        // --- Combinations: collapseSNAN + nanMinusSign = false + payload ---
        TC(sign = true,  signaling = true,  payload = 7,
            nanMinusSign = false, nanPayload = true, collapseSNAN = true,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "NaN7"),

        // --- All suppressed: most stripped form ---
        TC(sign = true,  signaling = true,  payload = 999,
            nanMinusSign = false, nanPayload = false, collapseSNAN = true,
            specialsCase = SpecialsCase.MIXEDCASE, expected = "NaN"),
    )
    @Test
    fun testPrintNAN() {
        for (tc in tcs) {
            test1(tc)
        }
    }

    fun test1(tc: TC) {
        if (verbose) {
            println(tc)
        }

        val x = Decimal.nan(sign = tc.sign, signaling = tc.signaling, payload = tc.payload)
        val printPrefs = PrintPrefs(
            nanMinusSign = tc.nanMinusSign,
            nanPayload = tc.nanPayload,
            collapseSNAN = tc.collapseSNAN,
            specialsCase = tc.specialsCase
        )
        val ctx = DecContext.decimal128IEEE().with(printPrefs)
        val actual = ctx.eval { x.toString() }

        assertEquals(tc.expected, actual, "$tc")
    }
}