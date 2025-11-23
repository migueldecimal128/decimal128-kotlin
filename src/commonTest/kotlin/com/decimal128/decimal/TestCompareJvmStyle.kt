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
        val nz = Dec2.from("-0")
        val pz = Dec2.from("+0")
        assertTrue(nz < pz)
        assertFalse(pz < nz)
        assertEquals(nz, nz)
        assertEquals(pz, pz)
    }

    @Test
    fun testFinitePositive() {
        assertTrue(Dec2.from("1") < Dec2.from("2"))
        assertTrue(Dec2.from("1.23") == Dec2.from("123e-2"))
        assertTrue(Dec2.from("1000") > Dec2.from("999"))
    }

    @Test
    fun testFiniteNegative() {
        assertTrue(Dec2.from("-5") < Dec2.from("-1"))
        assertEquals(Dec2.from("-1.23"), Dec2.from("-123e-2"))
    }

    @Test
    fun testMixedSigns() {
        assertTrue(Dec2.from("-1") < Dec2.from("0"))
        assertTrue(Dec2.from("-1") < Dec2.from("1"))
        assertTrue(Dec2.from("0") < Dec2.from("1"))
    }

    @Test
    fun testInfinity() {
        val negInf = Dec2.NEG_INFINITY
        val posInf = Dec2.from("+INF")
        assertTrue(negInf < posInf)
        assertTrue(negInf < Dec2.NEG_ZEROe0)
        assertTrue(posInf > Dec2.from(999999999))
    }

    @Test
    fun testNaNOrdering() {
        // All NaNs compare equal and greater than everything else
        assertTrue(Dec2.from("NaN") > Dec2.from("+Infinity"))
        assertEquals(Dec2.from("NaN"), Dec2.from("sNaN"))
        assertEquals(Dec2.POS_QNAN, Dec2.NEG_SNAN)
        assertEquals(Dec2.from("NaN123"), Dec2.from("sNaN(456)"))
    }

    @Test
    fun testCohortEquality() {
        assertEquals(Dec2.POS_ZEROe0, Dec2.NEG_ZEROe0)
        assertEquals(Dec2.from("0e10"), Dec2.from("0e-10"))
        assertEquals(Dec2.from("1"), Dec2.from("1.0"))
        assertEquals(Dec2.from("1"), Dec2.from("100e-2"))
        assertEquals(Dec2.from("123e3"), Dec2.from("123000"))
    }

    @Test
    fun testReferentialEquality() {
        assertSame(Dec2.POS_ZEROe0, Dec2.from("0"))
        assertSame(Dec2.POS_ZEROe0, Dec2.from("+0"))
        assertSame(Dec2.POS_ZEROe0, Dec2.from("000"))

        assertSame(Dec2.NEG_ZEROe0, Dec2.from("-0"))
        assertSame(Dec2.NEG_ZEROe0, Dec2.from("-000"))

        assertNotSame(Dec2.POS_ZEROe0, Dec2.from("0.0"))

        assertSame(Dec2.POS_QNAN, Dec2.from("NaN"))
        assertSame(Dec2.POS_QNAN, Dec2.from("+NaN0000"))
        assertSame(Dec2.NEG_QNAN, Dec2.from("-nan"))

        assertSame(Dec2.POS_INFINITY, Dec2.from("+inF"))
        assertSame(Dec2.POS_INFINITY, Dec2.from("infinity"))

        assertSame(Dec2.NEG_INFINITY, Dec2.from("-INF"))
    }
}
