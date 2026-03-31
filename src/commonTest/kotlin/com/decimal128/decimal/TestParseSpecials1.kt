package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestParseSpecials1 {

    val verbose = false

    @Test
    fun testParseInfinity() {
        val x = Decimal.from(1234567890)
        if (verbose)
            println("x:$x")

        val infinity = D128Parse.parseDecimal("infinity")
        assertEquals(Decimal.POS_INFINITY, infinity)
        val inf = D128Parse.parseDecimal("inf")
        assertEquals(Decimal.POS_INFINITY, inf)
        val plusINF = D128Parse.parseDecimal("+INF")
        assertEquals(Decimal.POS_INFINITY, plusINF)
        val negInfinity = D128Parse.parseDecimal("-Infinity")
        assertEquals(Decimal.NEG_INFINITY, negInfinity)
        assertFailsWith<NumberFormatException> { D128Parse.parseDecimal("x") }

        assertFailsWith<NumberFormatException> { D128Parse.parseDecimal("+Infin") }

    }

    @Test
    fun testParseNanText() {
        val nan = D128Parse.parseDecimal("nan")
        assertEquals(Decimal.POS_QNAN, nan)
        val plusNAN = D128Parse.parseDecimal("+NAN")
        assertEquals(Decimal.POS_QNAN, plusNAN)
        val negNaN = D128Parse.parseDecimal("-NaN")
        assertEquals(Decimal.NEG_QNAN, negNaN)
        val negSnan = D128Parse.parseDecimal("-Snan")
        assertEquals(Decimal.NEG_SNAN, negSnan)
        val plussNaN = D128Parse.parseDecimal("+sNaN")
        assertEquals(Decimal.POS_SNAN, plussNaN)
        val NaN0 = D128Parse.parseDecimal("NaN0")
        assertEquals(Decimal.POS_QNAN, NaN0)

        assertFailsWith<NumberFormatException> { D128Parse.parseDecimal("NaN ") }
        assertFailsWith<NumberFormatException> { D128Parse.parseDecimal("NaN+1") }
        assertFailsWith<NumberFormatException> { D128Parse.parseDecimal("") }

        val nan1 = D128Parse.parseDecimal("nan1")
        assertEquals("NaN1", nan1.toString())
        val nan1234567890 = D128Parse.parseDecimal("nan1234567890")
        assertEquals("NaN1234567890", nan1234567890.toString())
        val nan19 = D128Parse.parseDecimal("nan1234567890123456789")
        assertEquals("NaN1234567890123456789", nan19.toString())
        val nan20 = D128Parse.parseDecimal("nan12345678901234567890")
        assertEquals("NaN12345678901234567890", nan20.toString())
        val nan21 = D128Parse.parseDecimal("nan123456789012345678901")
        assertEquals("NaN123456789012345678901", nan21.toString())
        val NAN33 = D128Parse.parseDecimal("+NAN123456789012345678901234567890123")
        assertEquals("NaN123456789012345678901234567890123", NAN33.toString())
        // NANs that overflow canonical 33 nines become zero ...
        //  ... close reading of IEEE754-2019 3.5.2
        val NAN34 = D128Parse.parseDecimal("+NAN1234567890123456789012345678901234")
        assertEquals("NaN", NAN34.toString())
        val NAN32nines = D128Parse.parseDecimal("+NAN99999999999999999999999999999999")
        assertEquals("NaN99999999999999999999999999999999", NAN32nines.toString())
        val NAN33nines = D128Parse.parseDecimal("+NAN999999999999999999999999999999999")
        assertEquals("NaN999999999999999999999999999999999", NAN33nines.toString())
        val NAN34nines = D128Parse.parseDecimal("+NAN9999999999999999999999999999999999")
        assertEquals("NaN", NAN34nines.toString())
        val NAN35nines = D128Parse.parseDecimal("+NAN99999999999999999999999999999999999")
        assertEquals("NaN", NAN35nines.toString())

    }

    val tcsGood = arrayOf(
        "-0",
        "123",
        "0", "-0", "1", "-2", "123", "-456", "1.23", "-4.56",
        "3.141592653589793238462643383279502",
        "6.023e23",
        "9.999999999999999999999999999999999e6144",

        "123_456_789",
        "123__456__789",
    )

    @Test
    fun testParseFiniteValueText_good() {
        val ctxLowerCaseE = DecContext.decimal128Kotlin().with(DecPrefs.KOTLIN_DEFAULT.copy(printExponentLowercaseE = true))
        ctxLowerCaseE.eval {
            for (tc in tcsGood) {
                if (verbose)
                    println("tc:$tc")
                val v = D128Parse.parseDecimal(tc)
                assertEquals(tc.replace("_", ""), v.toString())
            }
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
            val msg = assertFailsWith<NumberFormatException> { D128Parse.parseDecimal(tc) }
            if (verbose)
                println (" => $msg")
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