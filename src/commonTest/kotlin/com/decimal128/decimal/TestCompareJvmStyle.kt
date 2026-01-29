package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestCompareJvmStyle {

    @Test
    fun testZeroVsZero() {
        val nz = Decimal2.from("-0")
        val pz = Decimal2.from("+0")
        assertTrue(nz < pz)
        assertFalse(pz < nz)
        assertEquals(nz, nz)
        assertEquals(pz, pz)
    }

    @Test
    fun testFinitePositive() {
        assertTrue(Decimal2.from("1") < Decimal2.from("2"))
        assertTrue(Decimal2.from("1.23") == Decimal2.from("123e-2"))
        assertTrue(Decimal2.from("1000") > Decimal2.from("999"))
    }

    @Test
    fun testFiniteNegative() {
        assertTrue(Decimal2.from("-5") < Decimal2.from("-1"))
        assertEquals(Decimal2.from("-1.23"), Decimal2.from("-123e-2"))
    }

    @Test
    fun testMixedSigns() {
        assertTrue(Decimal2.from("-1") < Decimal2.from("0"))
        assertTrue(Decimal2.from("-1") < Decimal2.from("1"))
        assertTrue(Decimal2.from("0") < Decimal2.from("1"))
    }

    @Test
    fun testInfinity() {
        val negInf = Decimal2.NEG_INFINITY
        val posInf = Decimal2.from("+INF")
        assertTrue(negInf < posInf)
        assertTrue(negInf < Decimal2.NEG_ZEROe0)
        assertTrue(posInf > Decimal2.from(999999999))
    }

    @Test
    fun testNaNOrdering() {
        // All NaNs compare equal and greater than everything else
        assertTrue(Decimal2.from("NaN") > Decimal2.from("+Infinity"))
        assertEquals(Decimal2.from("NaN"), Decimal2.from("sNaN"))
        assertEquals(Decimal2.POS_QNAN, Decimal2.NEG_SNAN)
        assertEquals(Decimal2.from("NaN123"), Decimal2.from("sNaN(456)"))
    }

    @Test
    fun testCohortEquality() {
        assertEquals(Decimal2.POS_ZEROe0, Decimal2.NEG_ZEROe0)
        assertEquals(Decimal2.from("0e10"), Decimal2.from("0e-10"))
        assertEquals(Decimal2.from("1"), Decimal2.from("1.0"))
        assertEquals(Decimal2.from("1"), Decimal2.from("100e-2"))
        assertEquals(Decimal2.from("123e3"), Decimal2.from("123000"))
    }

    @Test
    fun testReferentialEquality() {
        assertSame(Decimal2.POS_ZEROe0, Decimal2.from("0"))
        assertSame(Decimal2.POS_ZEROe0, Decimal2.from("+0"))
        assertSame(Decimal2.POS_ZEROe0, Decimal2.from("000"))

        assertSame(Decimal2.NEG_ZEROe0, Decimal2.from("-0"))
        assertSame(Decimal2.NEG_ZEROe0, Decimal2.from("-000"))

        assertNotSame(Decimal2.POS_ZEROe0, Decimal2.from("0.0"))

        assertSame(Decimal2.POS_QNAN, Decimal2.from("NaN"))
        assertSame(Decimal2.POS_QNAN, Decimal2.from("+NaN0000"))
        assertSame(Decimal2.NEG_QNAN, Decimal2.from("-nan"))

        assertSame(Decimal2.POS_INFINITY, Decimal2.from("+inF"))
        assertSame(Decimal2.POS_INFINITY, Decimal2.from("infinity"))

        assertSame(Decimal2.NEG_INFINITY, Decimal2.from("-INF"))
    }
}
