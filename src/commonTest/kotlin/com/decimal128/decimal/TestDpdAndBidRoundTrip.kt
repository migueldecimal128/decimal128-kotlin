package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertTrue

class TestDpdAndBidRoundTrip {

    val verbose = false

    val tcs = arrayOf(
        "1",
        "0e+6144",
        "-0.E6139",
        "+101001100000101.000000E5138",
        "-8E-6083",
        "1E6111",
        "1E-6176",
        "1.0E11",
        "1E1000",
        "1.0E2",
        "0.0001",
        "0.00001",
        "0.000001",
        "10.00",
        "1.0",
        "1e-7",
        "1e1",
        "1e-1",
        "1e-6",
        "1.234567890123456789012345678901234",
        "9999_9999999999_9999999999_9999999999",
        "-9999_9999999999_9999999999_9999999999",
        "-9.876_1234567890_1234567890_1234567890",
        "1e-6176",
        "-9.876_1234567890_1234567890_1234567890e6144",
        "INF",
        "infinity",
        "-Inf",
        "NaN[123123456789012345678901234567890]",
    )

    @Test
    fun testCases() {
        for (tc in tcs) {
            testDpd(tc)
            testBid(tc)
        }
    }

    fun testDpd(tc: String) {
        if (verbose)
            println("tc:$tc")
        val x0 = tc.toDecimal()
        val pentad = Pentad()
        D128SerdeDpd.encodeDpd128(x0, pentad)
        val x1 = Decimal.fromDpd128(pentad.dw1, pentad.dw0)
        assertTrue(x0 bitwiseEQ x1)
    }

    fun testBid(tc: String) {
        if (verbose)
            println("tc:$tc")
        val x0 = tc.toDecimal()
        val pentad = Pentad()
        D128SerdeBid.encodeBid128(x0, pentad)
        val x1 = Decimal.fromBid128(pentad.dw1, pentad.dw0)
        assertTrue(x0 bitwiseEQ x1)
    }

}