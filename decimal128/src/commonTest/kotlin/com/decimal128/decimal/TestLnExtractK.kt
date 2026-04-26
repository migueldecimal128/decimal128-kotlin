package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestLnExtractK {

    private val ctx38 = DecContext.decimal128Extended38()

    private fun dec(s: String) = MutDec().set(s, ctx38)
    private fun extractK(s: String, ctx: DecContext) = extractKMostSigDigitRounded(dec(s), ctx)

    // ---- single digit coefficient --------------------------------------------

    @Test fun singleDigit1() = assertEquals(1, extractK("1", ctx38))
    @Test fun singleDigit5() = assertEquals(5, extractK("5", ctx38))
    @Test fun singleDigit9() = assertEquals(9, extractK("9", ctx38))

    // ---- round down (second digit < 5) --------------------------------------

    @Test fun roundDown11()  = assertEquals(1, extractK("1.1", ctx38))
    @Test fun roundDown14()  = assertEquals(1, extractK("1.4999", ctx38))
    @Test fun roundDown34()  = assertEquals(3, extractK("3.4999", ctx38))
    @Test fun roundDown94()  = assertEquals(9, extractK("9.4999", ctx38))

    // ---- round ties away (second digit == 5) --------------------------------

    @Test fun tiesAway15()   = assertEquals(2, extractK("1.5", ctx38))
    @Test fun tiesAway25()   = assertEquals(3, extractK("2.5", ctx38))
    @Test fun tiesAway35()   = assertEquals(4, extractK("3.5", ctx38))
    @Test fun tiesAway85()   = assertEquals(9, extractK("8.5", ctx38))

    // ---- round up (second digit > 5) ----------------------------------------

    @Test fun roundUp16()    = assertEquals(2, extractK("1.6", ctx38))
    @Test fun roundUp36()    = assertEquals(4, extractK("3.6", ctx38))
    @Test fun roundUp96()    = assertEquals(10, extractK("9.6", ctx38))

    // ---- k == 10 boundary ---------------------------------------------------

    @Test fun kEquals10_95()    = assertEquals(10, extractK("9.5", ctx38))
    @Test fun kEquals10_99()    = assertEquals(10, extractK("9.9999", ctx38))
    @Test fun kEquals10_exact() = assertEquals(10, extractK("9.5000000000000000000000000000000000000", ctx38))

    // ---- various exponents (should not affect k) ----------------------------

    @Test fun largeExponent()   = assertEquals(3, extractK("3.4E+100", ctx38))
    @Test fun negExponent()     = assertEquals(7, extractK("7.2E-50", ctx38))
    @Test fun manyDigits()      = assertEquals(6, extractK("6.499999999999999999999999999999999999", ctx38))
    @Test fun manyDigitsUp()    = assertEquals(7, extractK("6.500000000000000000000000000000000001", ctx38))
}