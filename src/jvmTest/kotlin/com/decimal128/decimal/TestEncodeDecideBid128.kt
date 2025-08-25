package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertArrayEquals


class TestEncodeDecideBid128 {

    val verbose = false

    val tcs = arrayOf(
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

    val fptestOperandFilename = "/fptestOperandsUniq.txt"

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
        if (verbose)
            println(str)
        val d = Decimal(str, zeroNanPayload = true)
        val dEncoded = d.encodeLittleEndianBytesBid128()

        val bid128LE = ByteArray(16)
        check(jnaBidDpdShim.d128_bid_le_from_string(str, bid128LE) == 0)

        //if (! (bid128LE contentEquals dEncoded)) {
        if (verbose) {
            println(" bid128LE:${bid128LE.hex()}")
            println(" dEncoded:${dEncoded.hex()}")
        }
        assertArrayEquals(bid128LE, dEncoded)
    }
}