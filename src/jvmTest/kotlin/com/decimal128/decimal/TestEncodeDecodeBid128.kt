package com.decimal128.decimal

import com.decimal128.decimal.DecContext.Companion.DECIMAL128
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertArrayEquals


class TestEncodeDecodeBid128 {

    val verbose = false

    val tcs = arrayOf(
        "inf",
        "-100000000000000000000000000000000e-6175",
        "1e-6176",
        "10e-6176",
        "1e-6175",
        "1e0",
        "0e0",
        "123",
        "SNaN",
        "QNaN",
        "nanABC",
        "nan(123)",
        "NaN123456789", // payloads not yet handled
        "99999999999999999999999999999999999E6111",
        "-inf",
        "+NaN",
        "NaN", "+NaN", "-NaN",
        "0", "+0", "-0", "0e0", "0e+0", "0e-0", "+0e0", "+0e+0", "+0e-0", "-0e0", "-0e+0", "-0e-0",
        "1", "+1", "-1", "1e0", "1e+0", "1e-0", "+1e0", "+1e+0", "+1e-0", "-1e0", "-1e+0", "-1e-0",
        "10", "+10", "-10", "1e1", "1e+1", "1e-1", "+1e1", "+1e+1", "+1e-1", "-1e1", "-1e+1", "-1e-1",
        "NaN", "+NaN", "-NaN",
        "Inf", "infinity", "+Inf", "+INFINITY", "-INF", "-Infinity",
        "1.234567890123456789012345678901234e6144",
        "9999999999999999999999999999999999E6111",
        "99999999999999999999999999999999999E6111",
    )

    val jnaBidDpdShim = JnaBidDpdShim.INSTANCE

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    val fptestOperandFilename = "/fptest/fptestOperandsUniq.txt"

    @Test
    fun testFptestOperands() {
        this::class.java.getResourceAsStream(fptestOperandFilename)!!
            .bufferedReader()
            .useLines { lines ->
                lines.forEach { line ->
                    test1(line)
                }
            }
    }

    fun test1(str: String) {

        val DECIMAL128_ZERO_NAN_PAYLOAD = DECIMAL128.with(DECIMAL128.decPrefs.copy(parseDiscardNanPayload = true))

        if (verbose)
            println(str)
        val d = MutDec().set(str, DECIMAL128_ZERO_NAN_PAYLOAD)
        val bidLeBytes = d.encodeLittleEndianBytesBid128()
        val bidLeLongs = d.encodeLittleEndianLongsBid128()
        val bidBeBytes = d.encodeBigEndianBytesBid128()
        val bidBeLongs = d.encodeBigEndianLongsBid128()

        val bid128LE = ByteArray(16)
        check(jnaBidDpdShim.d128_bid_le_from_string(str, bid128LE) == 0)

        //if (! (bid128LE contentEquals dEncoded)) {
        if (verbose) {
            println(" bid128LE:${bid128LE.spacedHex()}")
            println(" bidLeBytes:${bidLeBytes.spacedHex()}")
            println(" bidLeLongs:${bidLeLongs.spacedHex()}")
            println(" bidBeBytes:${bidBeBytes.spacedHex()}")
            println(" bidBeLongs:${bidBeLongs.spacedHex()}")
        }
        assertArrayEquals(bid128LE, bidLeBytes)

        val dBeBytes = MutDec.decodeBigEndianBid128(bidBeBytes)
        val dBeLongs = MutDec.decodeBigEndianBid128(bidBeLongs)
        val dLeBytes = MutDec.decodeLittleEndianBid128(bidLeBytes)
        val dLeLongs = MutDec.decodeLittleEndianBid128(bidLeLongs)

        if (verbose) {
            println("dBeBytes:$dBeBytes")
            println("dBeLongs:$dBeLongs")
            println("dLeBytes:$dLeBytes")
            println("dLeLongs:$dLeLongs")
        }
        assert(d.isNaN() && dBeBytes.isNaN() || d == dBeBytes)
//        assert(d.isNaN() && dBeLongs.isNaN() || d == dBeLongs)
//        assert(d.isNaN() && dLeBytes.isNaN() || d == dLeBytes)
//        assert(d.isNaN() && dLeLongs.isNaN() || d == dLeLongs)
    }
}