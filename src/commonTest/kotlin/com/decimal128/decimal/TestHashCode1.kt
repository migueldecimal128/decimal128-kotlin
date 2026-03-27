package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestHashCode1 {
    val verbose = false

    //@Test
    fun testConstantHashCodes() {
        /*
        private const val HASH_CODE_SIGN_TRUE = 1251
        private const val HASH_CODE_SIGN_FALSE = 1257
        private const val HASH_CODE_POS_ZERO = 9999
        private const val HASH_CODE_NEG_ZERO = -9999
        private const val HASH_CODE_POS_INFINITY = 99999
        private const val HASH_CODE_NEG_INFINITY = -99999
        private const val HASH_CODE_NAN = 1
         */
        if (verbose) {
            println("constant hash codes")
            println("sign false:" + false.hashCode())
            println("sign true:" + true.hashCode())
            println("+0:" + calcHashCode(false, 0, 0uL, 0uL))
            println("-0:" + calcHashCode(true, 0, 0uL, 0uL))
            //println("+INF:" + calcHashCode(false, NON_FINITE_INF, 0uL, 0uL))
            //println("-INF:" + calcHashCode(true, NON_FINITE_INF, 0uL, 0uL))
            //println("NAN:" + calcHashCode(false, NON_FINITE_QNAN, 0uL, 0uL))
        }
    }

    fun calcHashCode(sign: Boolean, qExp: Int, dw1: ULong, dw0: ULong): Int {
        var hc = 0
        hc = hc * 31 + sign.hashCode()
        hc = hc * 31 + qExp.hashCode() // jvm n.hashCode() = n
        hc = hc * 31 + dw1.hashCode() // jvm l.hashCode() = (l xor (l ushr 32)).toInt()a
        hc = hc * 31 + dw0.hashCode()
        return hc
    }

    @Test
    fun confirmIntegerPrimitiveHashCodes() {
        // original jvm implementation of
        // Integer.hashCode(int n) = n
        assertEquals(1234.hashCode(), 1234)
        assertEquals(1234u.hashCode(), 1234)
        // original jvm implementation of
        // Long.hashCode(long l) = (int)(l xor (l >>> 32))
        assertEquals(
            9876543210L.hashCode(),
            (9876543210L xor (9876543210L ushr 32)).toInt()
        )
        assertEquals(
            9876543210uL.hashCode(),
            (9876543210uL xor (9876543210uL shr 32)).toInt()
        )
    }

    //@Test
    fun testHashFnzEqual() {
        testPair(true, "1", "1.0")
        testPair(true, "10", "1e1")
        testPair(true, "-0.1234", "-1234e-4")
        testPair(true, "10e-6176", "1e-6175")
        testPair(true,
            "1000000000000000000000000000000000e1000",
            "1e1033")
        testPair(true,
            "-123456789001234567890000000000",
            "-12345678900123456789e10",
        )
        testPair(true,
            "-999999999900000000000000000000",
            "-9999999999e20",
        )
        testPair(true,
            "-9999000000000000000000000000000000e6000",
            "-9999e6030",
        )
    }

    @Test
    fun testHashFnzNotEqual() {
        testPair(false, "2", "1.999999999999999999999999999999999")
        testPair(false, "10", "1.01e1")
        testPair(false, "-1", "1")
    }

    @Test
    fun testHashFnzClamping() {
        testPair(true, "100e6111", "1000e6110")
        testPair(true, "9.123456e6111", "9123456e6105")

        testPair(true, "1e6112", "1.0E+6112")

        testPair(true, "1e6130", "1.0000000000000000000E+6130")
        testPair(true, "1e6131", "1.00000000000000000000E+6131")
        testPair(true, "1e6132", "1.000000000000000000000E+6132")
        testPair(true, "1e6133", "1.0000000000000000000000E+6133")
        testPair(true, "1e6134", "1.00000000000000000000000E+6134")
        testPair(true, "1e6135", "1.000000000000000000000000E+6135")

        testPair(true, "1e6143", "1.00000000000000000000000000000000E+6143")
        testPair(true, "1e6144", "1.000000000000000000000000000000000E+6144")

    }

    @Test
    fun testHashZero() {
        testPair(true, "0", "0.0000")

        testPair(true, "0", "0e1")
        testPair(true, "0", "0.0000")
        testPair(true, "0.0", "+0.0e6111")

        testPair(true, "-0", "-0e1")
        testPair(true, "-0", "-0.0000")
        testPair(true, "-0.0", "-0.0e6111")

        testPair(false, "-0", "+0e1")
        testPair(false, "0", "-0.0000")
    }

    @Test
    fun testHashInfinity() {
        testPair(true, "Inf", "+Infinity")
        testPair(true, "-INF", "-inf")

        testPair(false, "INF", "-inf")
    }

    @Test
    fun testHashNaN() {
        testPair(true, "NaN", "-NaN")
        testPair(true, "-nan", "NAN123")
    }

    @Test
    fun testHashMix() {
        testPair(false, "NaN", "inf")
        testPair(false, "0e0", "123.456")
        testPair(false, "inf", "-NaN")
        testPair(false, "inf", "0")
    }

    fun testPair(expectEqual: Boolean, xStr: String, yStr: String) {
        val x = Decimal.from(xStr)
        val y = Decimal.from(yStr)

        testPair(expectEqual, x, y)
    }

    fun testPair(expectEqual: Boolean, x: Decimal, y: Decimal) {
        if (verbose)
            println("expectEquals:$expectEqual, x:$x, y:$y")
        val hcX = x.hashCode()
        val hcY = y.hashCode()
        if (expectEqual)
            assertEquals(hcX, hcY)
        else {
            // remember ... these are hashCodes
            // two values might hash to the same value even though
            // they are not equals()
            assertNotEquals(hcX, hcY,
                "should not be equal ... unless (very rare) hash collision")
        }
    }
}