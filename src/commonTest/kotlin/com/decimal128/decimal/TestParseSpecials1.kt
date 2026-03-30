package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestParseSpecials1 {

    val verbose = true

    @Test
    fun testParseInfinity() {
        val x = Decimal.from(1234567890)
        if (verbose)
            println("x:$x")

        val infinity = D128ParsePrint.parseInfinityText("infinity")
        assertEquals(Decimal.POS_INFINITY, infinity)
        val inf = D128ParsePrint.parseInfinityText("inf")
        assertEquals(Decimal.POS_INFINITY, inf)
        val plusINF = D128ParsePrint.parseInfinityText("+INF")
        assertEquals(Decimal.POS_INFINITY, plusINF)
        val negInfinity = D128ParsePrint.parseInfinityText("-Infinity")
        assertEquals(Decimal.NEG_INFINITY, negInfinity)
        val null1 = D128ParsePrint.parseInfinityText("x")
        assertEquals(null, null1)
        val null2 = D128ParsePrint.parseInfinityText("+Infin")
        assertEquals(null, null2)

    }

    @Test
    fun testParseNanText() {
        val nan = D128ParsePrint.parseNanText("nan")
        assertEquals(Decimal.POS_QNAN, nan)
        val plusNAN = D128ParsePrint.parseNanText("+NAN")
        assertEquals(Decimal.POS_QNAN, plusNAN)
        val negNaN = D128ParsePrint.parseNanText("-NaN")
        assertEquals(Decimal.NEG_QNAN, negNaN)
        val negSnan = D128ParsePrint.parseNanText("-Snan")
        assertEquals(Decimal.NEG_SNAN, negSnan)
        val plussNaN = D128ParsePrint.parseNanText("+sNaN")
        assertEquals(Decimal.POS_SNAN, plussNaN)
        val NaN0 = D128ParsePrint.parseNanText("NaN0")
        assertEquals(Decimal.POS_QNAN, NaN0)

        val null1 = D128ParsePrint.parseNanText("NaN ")
        assertEquals(InvalidOperationReason.PARSE_NON_DIGIT_AFTER_NAN.toString(), null1.toString())
        val nanPlus1 = D128ParsePrint.parseNanText("NaN+1")
        val nanPlus1Str = nanPlus1.toString()
        assertEquals(InvalidOperationReason.PARSE_NON_DIGIT_AFTER_NAN.toString(), nanPlus1Str)
        val null3 = D128ParsePrint.parseNanText("")
        assertEquals(null, null3)

        val nan1 = D128ParsePrint.parseNanText("nan1")
        assertEquals("NaN1", nan1.toString())
        val nan1234567890 = D128ParsePrint.parseNanText("nan1234567890")
        assertEquals("NaN1234567890", nan1234567890.toString())
        val nan19 = D128ParsePrint.parseNanText("nan1234567890123456789")
        assertEquals("NaN1234567890123456789", nan19.toString())
        val nan20 = D128ParsePrint.parseNanText("nan12345678901234567890")
        assertEquals("NaN12345678901234567890", nan20.toString())
        val nan21 = D128ParsePrint.parseNanText("nan123456789012345678901")
        assertEquals("NaN123456789012345678901", nan21.toString())
        val NAN33 = D128ParsePrint.parseNanText("+NAN123456789012345678901234567890123")
        assertEquals("NaN123456789012345678901234567890123", NAN33.toString())
        // NANs that overflow canonical 33 nines become zero ...
        //  ... close reading of IEEE754-2019 3.5.2
        val NAN34 = D128ParsePrint.parseNanText("+NAN1234567890123456789012345678901234")
        assertEquals("NaN", NAN34.toString())
        val NAN32nines = D128ParsePrint.parseNanText("+NAN99999999999999999999999999999999")
        assertEquals("NaN99999999999999999999999999999999", NAN32nines.toString())
        val NAN33nines = D128ParsePrint.parseNanText("+NAN999999999999999999999999999999999")
        assertEquals("NaN999999999999999999999999999999999", NAN33nines.toString())
        val NAN34nines = D128ParsePrint.parseNanText("+NAN9999999999999999999999999999999999")
        assertEquals("NaN", NAN34nines.toString())
        val NAN35nines = D128ParsePrint.parseNanText("+NAN99999999999999999999999999999999999")
        assertEquals("NaN", NAN35nines.toString())

    }

    val tcsGood = arrayOf(
        "-0",
        "123",
        "0", "-0", "1", "-2", "123", "-456", "1.23", "-4.56",
        "3.141592653589793238462643383279502",
        "6.02E+23",
        "9.999999999999999999999999999999999E+6144",

        "123_456_789",
        "123__456__789",
    )

    @Test
    fun testParseFiniteValueText_good() {
        for (tc in tcsGood) {
            if (verbose)
                println("tc:$tc")
            val v = D128ParsePrint.parseFiniteValueText(tc)
            assertEquals(tc.replace("_", ""), v.toString())
        }
    }

    val tcsNull = arrayOf(
        "+.",
        "", "0_", "+_1", "_-2",
        "6_.02E+23",
        "123 ",
        "123..45",
        "1.2.3",
        "6.023E+",
        "6.zero",
        "+", "-",
        "+."

    )

    @Test
    fun testParseFiniteValueText_bad_java() {
        for (tc in tcsNull) {
            if (verbose)
                println("tc:$tc")
            val v = D128ParsePrint.parseFiniteValueText(tc)
            if (verbose)
                println (" => $v")
            assertTrue(v == null || v is InvalidOperationReason)
        }
    }

    @Test
    fun testParseFiniteValueText_bad_ieee() {
        val decPrefs = DecPrefs().copy(parseMalformedThrowsNumberFormatException = false,
            parseThrowOnDigitOverflow = false, parseThrowOnOutOfRange = false)
        val decContext = DecContext.decimal128IEEE().with(decPrefs)
        for (tc in tcsNull) {
            if (verbose)
                println("tc:$tc")
            val v = Decimal.from(tc, decContext)
            if (verbose)
                println(" => $v")
        }
    }

    @Test
    fun testTooManyDigitsNative() {
        // confirm that too many digits leads to NumberFormatException
        assertFailsWith<NumberFormatException> { "12345678901234567890".toLong() }
    }
}