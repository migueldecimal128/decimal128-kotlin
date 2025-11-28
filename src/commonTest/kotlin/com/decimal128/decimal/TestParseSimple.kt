package com.decimal128.decimal

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class TestParseSimple {

    val verbose = true

    @Test
    fun test() {
        // if you supply one arg remember to make it in canonical format
        test1("-0.E6139", "-0E+6111")
        test1("+101001100000101.000000E5138", "1.01001100000101000000E+5152")
        //test1("+101001100000101.000000E6138") ... invalid ... out of range
        test1("-8.E-6083", "-8E-6083")
        test1("1E+6111")
        test1("1E-6176")
        test1("10e10", "1.0E+11")
        test1("1E+1000")
        test1("10e1", "1.0E+2")
        test1(".0000000001e10", "1")
        test1("0.001e-1", "0.0001")
        test1("0.000001e1", "0.00001")
        test1("0.000001")
        test1("10.00")
        test1("1.0")
        test1("1e-7", "1E-7")
        test1("1e1", "1E+1")
        test1("1e-1", "0.1")
        test1("1e-6", "0.000001")
        test1("1")
    }

    @Test
    fun testTheUpperAtmosphere() {
        test1("1e6131", "1.00000000000000000000E+6131")
        test1("1e6132", "1.000000000000000000000E+6132")

        test1("1e6111", "1E+6111")
        test1("1e6112", "1.0E+6112")
        test1("1e6113", "1.00E+6113")
        test1("1e6114", "1.000E+6114")
        test1("1e6115", "1.0000E+6115")
        test1("1e6116", "1.00000E+6116")
        test1("1e6117", "1.000000E+6117")
        test1("1e6118", "1.0000000E+6118")
        test1("1e6119", "1.00000000E+6119")
        test1("1e6120", "1.000000000E+6120")
        test1("1e6130", "1.0000000000000000000E+6130")
        test1("1e6131", "1.00000000000000000000E+6131")
        test1("1e6132", "1.000000000000000000000E+6132")
        test1("1e6133", "1.0000000000000000000000E+6133")
        test1("1e6134", "1.00000000000000000000000E+6134")
        test1("1e6135", "1.000000000000000000000000E+6135")
        test1("1e6141", "1.000000000000000000000000000000E+6141")
        test1("1e6142", "1.0000000000000000000000000000000E+6142")
        test1("1e6143", "1.00000000000000000000000000000000E+6143")
        test1("1e6144", "1.000000000000000000000000000000000E+6144")
        test1("10e6111", "1.0E+6112")
        test1("10.0e6111", "1.00E+6112")
        test1("100.0e6111", "1.000E+6113")
        test1("100e6111", "1.00E+6113")
        test1("1000000000000e6111", "1.000000000000E+6123")
        test1("1000000000000.0e6111", "1.0000000000000E+6123")
        test1("100000000000000000000000000000000e6111", "1.00000000000000000000000000000000E+6143")
        test1("100000000000000000000000000000000.0e6111", "1.000000000000000000000000000000000E+6143")
        test1("10000000000000000000000000000000.00e6111", "1.000000000000000000000000000000000E+6142")
        test1("1000000000.0000000000000000000000e6111", "1.0000000000000000000000000000000E+6120")
        test1("1000000000000000000000000000000000e6111", "1.000000000000000000000000000000000E+6144")
        test1("10e6112", "1.00E+6113")
        test1("1e6112", "1.0E+6112")
    }

    @Test
    fun testTheSoundBarrier() {
        assertThrows<IllegalArgumentException> { test1("1e6145") }
        assertThrows<IllegalArgumentException> { test1("12345678901234567890123456789012345") }
        assertThrows<IllegalArgumentException> { test1("1.2345678901234567890123456789012345") }
        assertThrows<IllegalArgumentException> { test1("Infinity and beyond")}
        assertThrows<IllegalArgumentException> { test1("1.0000000000000000000000000000000000E+6144") }
    }

    fun test1(parse: String, expected: String = parse) {
        if (verbose)
            println("parse:$parse expected:$expected")
        val d = Decimal.from(parse)
        val render = d.toString()
        assertEquals(expected, render)
    }
}