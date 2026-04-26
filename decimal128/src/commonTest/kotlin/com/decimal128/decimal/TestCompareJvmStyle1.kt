package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestCompareJvmStyle1 {

    @Test
    fun testZeroVsZero() {
        val nz = Decimal.from("-0")
        val pz = Decimal.from("+0")
        assertTrue(nz < pz)
        assertFalse(pz < nz)
        assertEquals(nz, nz)
        assertEquals(pz, pz)
    }

    @Test
    fun testFinitePositive() {
        assertTrue(Decimal.from("1") < Decimal.from("2"))
        assertTrue(Decimal.from("1.23") == Decimal.from("123e-2"))
        assertTrue(Decimal.from("1000") > Decimal.from("999"))
    }

    @Test
    fun testFiniteNegative() {
        assertTrue(Decimal.from("-5") < Decimal.from("-1"))
        assertEquals(Decimal.from("-1.23"), Decimal.from("-123e-2"))
    }

    @Test
    fun testMixedSigns() {
        assertTrue(Decimal.from("-1") < Decimal.from("0"))
        assertTrue(Decimal.from("-1") < Decimal.from("1"))
        assertTrue(Decimal.from("0") < Decimal.from("1"))
    }

    @Test
    fun testInfinity() {
        val negInf = Decimal.NEG_INFINITY
        val posInf = Decimal.from("+INF")
        assertTrue(negInf < posInf)
        assertTrue(negInf < Decimal.NEG_ZEROe0)
        assertTrue(posInf > Decimal.from(999999999))
    }

    @Test
    fun testNaNOrdering() {
        // All NaNs compare equal and greater than everything else
        assertTrue(Decimal.from("NaN") > Decimal.from("+Infinity"))
        assertEquals(Decimal.from("NaN"), Decimal.from("sNaN"))
        assertEquals(Decimal.POS_QNAN, Decimal.NEG_SNAN)
        assertEquals(Decimal.from("NaN123"), Decimal.from("sNaN(456)"))
    }

    @Test
    fun testCohortEquality() {
        assertEquals(Decimal.POS_ZEROe0, Decimal.NEG_ZEROe0)
        assertEquals(Decimal.from("0e10"), Decimal.from("0e-10"))
        assertEquals(Decimal.from("1"), Decimal.from("1.0"))
        assertEquals(Decimal.from("1"), Decimal.from("100e-2"))
        assertEquals(Decimal.from("123e3"), Decimal.from("123000"))
    }

    @Test
    fun testReferentialEquality() {
        assertSame(Decimal.POS_ZEROe0, Decimal.from("0"))
        assertSame(Decimal.POS_ZEROe0, Decimal.from("+0"))
        assertSame(Decimal.POS_ZEROe0, Decimal.from("000"))

        assertSame(Decimal.NEG_ZEROe0, Decimal.from("-0"))
        assertSame(Decimal.NEG_ZEROe0, Decimal.from("-000"))

        assertNotSame(Decimal.POS_ZEROe0, Decimal.from("0.0"))

        assertSame(Decimal.POS_QNAN, Decimal.from("NaN"))
        assertSame(Decimal.POS_QNAN, Decimal.from("+NaN0000"))
        assertSame(Decimal.NEG_QNAN, Decimal.from("-nan"))

        assertSame(Decimal.POS_INFINITY, Decimal.from("+inF"))
        assertSame(Decimal.POS_INFINITY, Decimal.from("infinity"))

        assertSame(Decimal.NEG_INFINITY, Decimal.from("-INF"))
    }
}
