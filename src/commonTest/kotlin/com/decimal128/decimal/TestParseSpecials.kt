package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestParseSpecials {

    val verbose = true

    @Test
    fun testParseInfinity() {
        val x = Decimal.from(1234567890)
        println("x:$x")

        val infinity = DecParsePrint.parseInfinityText("infinity")
        assertEquals(Decimal.POS_INFINITY, infinity)
        val inf = DecParsePrint.parseInfinityText("inf")
        assertEquals(Decimal.POS_INFINITY, inf)
        val plusINF = DecParsePrint.parseInfinityText("+INF")
        assertEquals(Decimal.POS_INFINITY, plusINF)
        val negInfinity = DecParsePrint.parseInfinityText("-Infinity")
        assertEquals(Decimal.NEG_INFINITY, negInfinity)
        val null1 = DecParsePrint.parseInfinityText("x")
        assertEquals(null, null1)
        val null2 = DecParsePrint.parseInfinityText("+Infin")
        assertEquals(null, null2)

    }

    @Test
    fun testParseNanText() {
        val nan = DecParsePrint.parseNanText("nan")
        assertEquals(Decimal.POS_QNAN, nan)
        val plusNAN = DecParsePrint.parseNanText("+NAN")
        assertEquals(Decimal.POS_QNAN, plusNAN)
        val negNaN = DecParsePrint.parseNanText("-NaN")
        assertEquals(Decimal.NEG_QNAN, negNaN)
        val negSnan = DecParsePrint.parseNanText("-Snan")
        assertEquals(Decimal.NEG_SNAN, negSnan)
        val null1 = DecParsePrint.parseNanText("NaN ")
        assertEquals("NaN", null1.toString())
        val plussNaN = DecParsePrint.parseNanText("+sNaN")
        assertEquals(Decimal.POS_SNAN, plussNaN)
        val NaN0 = DecParsePrint.parseNanText("NaN0")
        assertEquals(Decimal.POS_QNAN, NaN0)

        val null2 = DecParsePrint.parseNanText("NaN+1")
        assertEquals("NaN1", null2.toString())
        val null3 = DecParsePrint.parseNanText("")
        assertEquals(null, null3)

        val nan1 = DecParsePrint.parseNanText("nan1")
        assertEquals("NaN1", nan1.toString())
        val nan1234567890 = DecParsePrint.parseNanText("nan1234567890")
        assertEquals("NaN1234567890", nan1234567890.toString())
        val nan19 = DecParsePrint.parseNanText("nan1234567890123456789")
        assertEquals("NaN1234567890123456789", nan19.toString())
        val nan20 = DecParsePrint.parseNanText("nan12345678901234567890")
        assertEquals("NaN12345678901234567890", nan20.toString())
        val nan21 = DecParsePrint.parseNanText("nan123456789012345678901")
        assertEquals("NaN123456789012345678901", nan21.toString())
        val NAN33 = DecParsePrint.parseNanText("+NAN123456789012345678901234567890123")
        assertEquals("NaN123456789012345678901234567890123", NAN33.toString())
        // only accept the first 33 digits
        val NAN34 = DecParsePrint.parseNanText("+NAN1234567890123456789012345678901234")
        assertEquals("NaN123456789012345678901234567890123", NAN34.toString())
        val NAN33nines = DecParsePrint.parseNanText("+NAN999999999999999999999999999999999")
        assertEquals("NaN999999999999999999999999999999999", NAN33nines.toString())
        val NAN34nines = DecParsePrint.parseNanText("+NAN9999999999999999999999999999999999")
        assertEquals("NaN999999999999999999999999999999999", NAN34nines.toString())
        val NAN35nines = DecParsePrint.parseNanText("+NAN99999999999999999999999999999999999")
        assertEquals("NaN999999999999999999999999999999999", NAN35nines.toString())

    }

    val tcsGood = arrayOf(
        "123",
        "0", "-0", "1", "-2", "123", "-456", "1.23", "-4.56",
        "3.141592653589793238462643383279502",
        "6.02E+23",
        "9.999999999999999999999999999999999E+6144",

        "123_456_789",
        "123__456__789",
    )
    val tcsNull = arrayOf(
        "", "0_", "+_1", "_-2",
        "12345678901234567890123456789012345",
        "6_.02E+23",
        "9.999999999999999999999999999999999E+6145",
    )
    @Test
    fun testParseFiniteValueText() {
        for (tc in tcsGood) {
            if (verbose)
                println("tc:$tc")
            val v = DecParsePrint.parseFiniteValueText(tc)
            assertEquals(tc.replace("_", ""), v.toString())
        }
        for (tc in tcsNull) {
            if (verbose)
                println("tc:$tc")
            val v = DecParsePrint.parseFiniteValueText(tc)
            if (verbose)
                println (" => $v")
            assert(v == null || v is String)
        }
    }
}