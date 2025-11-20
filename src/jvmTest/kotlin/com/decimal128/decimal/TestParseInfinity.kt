package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestParseInfinity {

    @Test
    fun testParseInfinity() {
        val x = Dec2.from(1234567890)
        println("x:$x")

        val infinity = Dec2.parseInfinityText("infinity")
        assertEquals(Dec2.POS_INFINITY, infinity)
        val inf = Dec2.parseInfinityText("inf")
        assertEquals(Dec2.POS_INFINITY, inf)
        val plusINF = Dec2.parseInfinityText("+INF")
        assertEquals(Dec2.POS_INFINITY, plusINF)
        val negInfinity = Dec2.parseInfinityText("-Infinity")
        assertEquals(Dec2.NEG_INFINITY, negInfinity)
        val null1 = Dec2.parseInfinityText("x")
        assertEquals(null, null1)
        val null2 = Dec2.parseInfinityText("+Infin")
        assertEquals(null, null2)

    }

    @Test
    fun testParseNanText() {
        val nan = Dec2.parseNanText("nan")
        assertEquals(Dec2.POS_QNAN, nan)
        val plusNAN = Dec2.parseNanText("+NAN")
        assertEquals(Dec2.POS_QNAN, plusNAN)
        val negNaN = Dec2.parseNanText("-NaN")
        assertEquals(Dec2.NEG_QNAN, negNaN)
        val negSnan = Dec2.parseNanText("-Snan")
        assertEquals(Dec2.NEG_SNAN, negSnan)
        val null1 = Dec2.parseNanText("NaN ")
        assertEquals(null, null1)
        val plussNaN = Dec2.parseNanText("+sNaN")
        assertEquals(Dec2.POS_SNAN, plussNaN)
        val NaN0 = Dec2.parseNanText("NaN0")
        assertEquals(Dec2.POS_QNAN, NaN0)

        val null2 = Dec2.parseNanText("NaN+1")
        assertEquals(null, null2)
        val null3 = Dec2.parseNanText("")
        assertEquals(null, null3)

        val nan1 = Dec2.parseNanText("nan1")
        assertEquals("NaN1", nan1.toString())
        val nan1234567890 = Dec2.parseNanText("nan1234567890")
        assertEquals("NaN1234567890", nan1234567890.toString())
        val nan19 = Dec2.parseNanText("nan1234567890123456789")
        assertEquals("NaN1234567890123456789", nan19.toString())
        val nan20 = Dec2.parseNanText("nan12345678901234567890")
        assertEquals("NaN12345678901234567890", nan20.toString())
        val nan21 = Dec2.parseNanText("nan123456789012345678901")
        assertEquals("NaN123456789012345678901", nan21.toString())
        val NAN34 = Dec2.parseNanText("+NAN1234567890123456789012345678901234")
        assertEquals("NaN1234567890123456789012345678901234", NAN34.toString())
        val NAN34nines = Dec2.parseNanText("+NAN9999999999999999999999999999999999")
        assertEquals("NaN9999999999999999999999999999999999", NAN34nines.toString())
        val NAN35nines = Dec2.parseNanText("+NAN99999999999999999999999999999999999")
        assertEquals(null, NAN35nines)

    }

    val tcsGood = arrayOf(
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
            val v = Dec2.parseFiniteValueText(tc)
            assertEquals(tc.replace("_", ""), v.toString())
        }
        for (tc in tcsNull) {
            val v = Dec2.parseFiniteValueText(tc)
            assertEquals(null, v)
        }
    }
}