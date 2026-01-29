package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestParseSpecials {

    val verbose = true

    @Test
    fun testParseInfinity() {
        val x = Decimal2.from(1234567890)
        println("x:$x")

        val infinity = DecParsePrint.parseInfinityText("infinity")
        assertEquals(Decimal2.POS_INFINITY, infinity)
        val inf = DecParsePrint.parseInfinityText("inf")
        assertEquals(Decimal2.POS_INFINITY, inf)
        val plusINF = DecParsePrint.parseInfinityText("+INF")
        assertEquals(Decimal2.POS_INFINITY, plusINF)
        val negInfinity = DecParsePrint.parseInfinityText("-Infinity")
        assertEquals(Decimal2.NEG_INFINITY, negInfinity)
        val null1 = DecParsePrint.parseInfinityText("x")
        assertEquals(null, null1)
        val null2 = DecParsePrint.parseInfinityText("+Infin")
        assertEquals(null, null2)

    }

    @Test
    fun testParseNanText() {
        val nan = DecParsePrint.parseNanText("nan")
        assertEquals(Decimal2.POS_QNAN, nan)
        val plusNAN = DecParsePrint.parseNanText("+NAN")
        assertEquals(Decimal2.POS_QNAN, plusNAN)
        val negNaN = DecParsePrint.parseNanText("-NaN")
        assertEquals(Decimal2.NEG_QNAN, negNaN)
        val negSnan = DecParsePrint.parseNanText("-Snan")
        assertEquals(Decimal2.NEG_SNAN, negSnan)
        val null1 = DecParsePrint.parseNanText("NaN ")
        assertEquals("NaN", null1.toString())
        val plussNaN = DecParsePrint.parseNanText("+sNaN")
        assertEquals(Decimal2.POS_SNAN, plussNaN)
        val NaN0 = DecParsePrint.parseNanText("NaN0")
        assertEquals(Decimal2.POS_QNAN, NaN0)

        val nanPlus1 = DecParsePrint.parseNanText("NaN+1")
        val nanPlus1Str = nanPlus1.toString()
        assertEquals("NaN1", nanPlus1Str)
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
        // NANs that overflow canonical 33 nines get clamped
        val NAN34 = DecParsePrint.parseNanText("+NAN1234567890123456789012345678901234")
        assertEquals("NaN999999999999999999999999999999999", NAN34.toString())
        val NAN32nines = DecParsePrint.parseNanText("+NAN99999999999999999999999999999999")
        assertEquals("NaN99999999999999999999999999999999", NAN32nines.toString())
        val NAN33nines = DecParsePrint.parseNanText("+NAN999999999999999999999999999999999")
        assertEquals("NaN999999999999999999999999999999999", NAN33nines.toString())
        val NAN34nines = DecParsePrint.parseNanText("+NAN9999999999999999999999999999999999")
        assertEquals("NaN999999999999999999999999999999999", NAN34nines.toString())
        val NAN35nines = DecParsePrint.parseNanText("+NAN99999999999999999999999999999999999")
        assertEquals("NaN999999999999999999999999999999999", NAN35nines.toString())

        // now, allow oversize payload
        val NAN34oversize = DecParsePrint.parseNanText("+NAN1234567890123456789012345678901234",
            allowOversizePayload = true)
        assertEquals("NaN1234567890123456789012345678901234", NAN34oversize.toString())

        val NAN35ninesOversize = DecParsePrint.parseNanText("+NAN99999999999999999999999999999999999",
            allowOversizePayload = true)
        assertEquals("NaN99999999999999999999999999999999999", NAN35ninesOversize.toString())

        val NAN128bitsOversize = DecParsePrint.parseNanText("+NAN340282366920938463463374607431768211455",
            allowOversizePayload = true)
        // clamp at nines38
        assertEquals("NaN99999999999999999999999999999999999999", NAN128bitsOversize.toString())



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