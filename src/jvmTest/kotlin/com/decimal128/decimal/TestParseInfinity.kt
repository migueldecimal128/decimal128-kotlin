package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestParseInfinity {

    val verbose = true

    @Test
    fun testParseInfinity() {
        val x = Dec2.from(1234567890)
        println("x:$x")

        val infinity = Dec2ParsePrint.parseInfinityText("infinity")
        assertEquals(Dec2.POS_INFINITY, infinity)
        val inf = Dec2ParsePrint.parseInfinityText("inf")
        assertEquals(Dec2.POS_INFINITY, inf)
        val plusINF = Dec2ParsePrint.parseInfinityText("+INF")
        assertEquals(Dec2.POS_INFINITY, plusINF)
        val negInfinity = Dec2ParsePrint.parseInfinityText("-Infinity")
        assertEquals(Dec2.NEG_INFINITY, negInfinity)
        val null1 = Dec2ParsePrint.parseInfinityText("x")
        assertEquals(null, null1)
        val null2 = Dec2ParsePrint.parseInfinityText("+Infin")
        assertEquals(null, null2)

    }

    @Test
    fun testParseNanText() {
        val nan = Dec2ParsePrint.parseNanText("nan")
        assertEquals(Dec2.POS_QNAN, nan)
        val plusNAN = Dec2ParsePrint.parseNanText("+NAN")
        assertEquals(Dec2.POS_QNAN, plusNAN)
        val negNaN = Dec2ParsePrint.parseNanText("-NaN")
        assertEquals(Dec2.NEG_QNAN, negNaN)
        val negSnan = Dec2ParsePrint.parseNanText("-Snan")
        assertEquals(Dec2.NEG_SNAN, negSnan)
        val null1 = Dec2ParsePrint.parseNanText("NaN ")
        assertEquals("NaN", null1.toString())
        val plussNaN = Dec2ParsePrint.parseNanText("+sNaN")
        assertEquals(Dec2.POS_SNAN, plussNaN)
        val NaN0 = Dec2ParsePrint.parseNanText("NaN0")
        assertEquals(Dec2.POS_QNAN, NaN0)

        val null2 = Dec2ParsePrint.parseNanText("NaN+1")
        assertEquals("NaN1", null2.toString())
        val null3 = Dec2ParsePrint.parseNanText("")
        assertEquals(null, null3)

        val nan1 = Dec2ParsePrint.parseNanText("nan1")
        assertEquals("NaN1", nan1.toString())
        val nan1234567890 = Dec2ParsePrint.parseNanText("nan1234567890")
        assertEquals("NaN1234567890", nan1234567890.toString())
        val nan19 = Dec2ParsePrint.parseNanText("nan1234567890123456789")
        assertEquals("NaN1234567890123456789", nan19.toString())
        val nan20 = Dec2ParsePrint.parseNanText("nan12345678901234567890")
        assertEquals("NaN12345678901234567890", nan20.toString())
        val nan21 = Dec2ParsePrint.parseNanText("nan123456789012345678901")
        assertEquals("NaN123456789012345678901", nan21.toString())
        val NAN33 = Dec2ParsePrint.parseNanText("+NAN123456789012345678901234567890123")
        assertEquals("NaN123456789012345678901234567890123", NAN33.toString())
        // only accept the first 33 digits
        val NAN34 = Dec2ParsePrint.parseNanText("+NAN1234567890123456789012345678901234")
        assertEquals("NaN123456789012345678901234567890123", NAN34.toString())
        val NAN33nines = Dec2ParsePrint.parseNanText("+NAN999999999999999999999999999999999")
        assertEquals("NaN999999999999999999999999999999999", NAN33nines.toString())
        val NAN34nines = Dec2ParsePrint.parseNanText("+NAN9999999999999999999999999999999999")
        assertEquals("NaN999999999999999999999999999999999", NAN34nines.toString())
        val NAN35nines = Dec2ParsePrint.parseNanText("+NAN99999999999999999999999999999999999")
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
            val v = Dec2ParsePrint.parseFiniteValueText(tc)
            assertEquals(tc.replace("_", ""), v.toString())
        }
        for (tc in tcsNull) {
            val v = Dec2ParsePrint.parseFiniteValueText(tc)
            assertEquals(null, v)
        }
    }
}