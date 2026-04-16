package com.decimal128.decimal

import kotlin.random.Random
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
            testDpdLongs(tc)
            testDpdBytes(tc)

            testBid(tc)
            testBidLongs(tc)
            testBidBytes(tc)
        }
    }

    fun testDpd(tc: String) {
        if (verbose)
            println("tc:$tc")
        val x0 = tc.toDecimal()
        val pentad = Pentad()
        dpd128Encode(x0, pentad)
        val x1 = Decimal.decodeDpd128(pentad.dw1, pentad.dw0)
        assertTrue(x0 bitwiseEQ x1)
    }

    fun testDpdLongs(tc: String) {
        if (verbose)
            println("tc:$tc")
        val x0 = tc.toDecimal()
        val longs = LongArray(4)
        val offset = Random.nextInt(3)
        val isLittleEndian = Random.nextBoolean()
        x0.encodeDpd128(longs, offset, isLittleEndian)
        val t = longs[offset]
        longs[offset] = longs[offset + 1]
        longs[offset + 1] = t
        val x1 = Decimal.decodeDpd128(longs, offset, !isLittleEndian)
        assertTrue(x0 bitwiseEQ x1)
    }

    fun testDpdBytes(tc: String) {
        if (verbose)
            println("tc:$tc")
        val x0 = tc.toDecimal()
        val bytes = ByteArray(20)
        val offset = Random.nextInt(5)
        val isLittleEndian = Random.nextBoolean()
        x0.encodeDpd128(bytes, offset, isLittleEndian)
        swapBytes(bytes, offset, 16)
        val x1 = Decimal.decodeDpd128(bytes, offset, !isLittleEndian)
        assertTrue(x0 bitwiseEQ x1)
    }

    fun testBid(tc: String) {
        if (verbose)
            println("tc:$tc")
        val x0 = tc.toDecimal()
        val pentad = Pentad()
        bid128Encode(x0, pentad)
        val x1 = Decimal.decodeBid128(pentad.dw1, pentad.dw0)
        assertTrue(x0 bitwiseEQ x1)
    }

    fun testBidLongs(tc: String) {
        if (verbose)
            println("tc:$tc")
        val x0 = tc.toDecimal()
        val longs = LongArray(4)
        val offset = Random.nextInt(3)
        val isLittleEndian = Random.nextBoolean()
        x0.encodeBid128(longs, offset, isLittleEndian)
        val t = longs[offset]
        longs[offset] = longs[offset + 1]
        longs[offset + 1] = t
        val x1 = Decimal.decodeBid128(longs, offset, !isLittleEndian)
        assertTrue(x0 bitwiseEQ x1)
    }

    fun testBidBytes(tc: String) {
        if (verbose)
            println("tc:$tc")
        val x0 = tc.toDecimal()
        val bytes = ByteArray(20)
        val offset = Random.nextInt(5)
        val isLittleEndian = Random.nextBoolean()
        x0.encodeBid128(bytes, offset, isLittleEndian)
        swapBytes(bytes, offset, 16)
        val x1 = Decimal.decodeBid128(bytes, offset, !isLittleEndian)
        assertTrue(x0 bitwiseEQ x1)
    }

    fun swapBytes(bytes: ByteArray, off: Int, len: Int) {
        require(off >= 0 && off + len <= bytes.size)
        require(len >= 0)

        var lo = off
        var hi = off + len - 1
        while (lo < hi) {
            val tmp = bytes[lo]
            bytes[lo] = bytes[hi]
            bytes[hi] = tmp
            lo++
            hi--
        }
    }


}
