package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertArrayEquals


class TestEncodeDecodeDpd128 {

    val verbose = false

    val tcs = arrayOf(
        "NaN1234",
        "1000000000000000000e-6175",
        "10000000000000000000e-6175",
        "100000000000000000000e-6175",
        "1000000000000000000000e-6175",
        "100000000000000000000000000000e-6175",
        "100000000000000000000000000000e-6175",
        "100000000000000000000000000000e-6175",
        "1000000000000000000000000000000e-6175",
        "10000000000000000000000000000000e-6175",
        "100000000000000000000000000000000e-6175",
        "-100000000000000000000000000000000e-6175",
        "1e-6176",
        "10e-6176",
        "1e-6175",
        "1e0",
        "0e0",
        "123",
        "0",
        "SNaN",
        "QNaN",
        "nanABC",
        "nan(123)",
        "NaN123456789",
        "-inf",
        "+NaN",
        "NaN", "+NaN", "-NaN",
        "0", "+0", "-0", "0e0", "0e+0", "0e-0", "+0e0", "+0e+0", "+0e-0", "-0e0", "-0e+0", "-0e-0",
        "1", "+1", "-1", "1e0", "1e+0", "1e-0", "+1e0", "+1e+0", "+1e-0", "-1e0", "-1e+0", "-1e-0",
        "10", "+10", "-10", "1e1", "1e+1", "1e-1", "+1e1", "+1e+1", "+1e-1", "-1e1", "-1e+1", "-1e-1",
        "NaN", "+NaN", "-NaN",
        "Inf", "infinity", "+Inf", "+INFINITY", "-INF", "-Infinity",
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
        val dpd128LE = ByteArray(16)
        require (jnaBidDpdShim.d128_dpd_le_from_string(str, dpd128LE) == 0)

        val d = Decimal(str, zeroNanPayload = false)
        val dpdLeBytes = d.encodeLittleEndianBytesDpd128()
        if (verbose) {
            println(" dpd128LE:${dpd128LE.spacedHex()}")
            println(" dpdLeBytes:${dpdLeBytes.spacedHex()}")
        }
        val d2 = Decimal.decodeLittleEndianDpd128(dpdLeBytes)
        if (verbose) {
            println("d:$d d2:$d2")
        }
        val dpdLeLongs = d.encodeLittleEndianLongsDpd128()
        val dpdBeBytes = d.encodeBigEndianBytesDpd128()
        val dpdBeLongs = d.encodeBigEndianLongsDpd128()

        //if (! (bid128LE contentEquals dEncoded)) {
        if (verbose) {
            println(" dpd128LE:${dpd128LE.spacedHex()}")
            println(" dpdLeBytes:${dpdLeBytes.spacedHex()}")
            println(" dpdLeLongs:${dpdLeLongs.spacedHex()}")
            println(" dpdBeBytes:${dpdBeBytes.spacedHex()}")
            println(" dpdBeLongs:${dpdBeLongs.spacedHex()}")
        }
        assertArrayEquals(dpd128LE, dpdLeBytes)

        val dBeBytes = Decimal.decodeBigEndianDpd128(dpdBeBytes)
        val dBeLongs = Decimal.decodeBigEndianDpd128(dpdBeLongs)
        val dLeBytes = Decimal.decodeLittleEndianDpd128(dpdLeBytes)
        val dLeLongs = Decimal.decodeLittleEndianDpd128(dpdLeLongs)

        assert(d.isNaN() && dBeBytes.isNaN() || d == dBeBytes)
        assert(d.isNaN() && dBeLongs.isNaN() || d == dBeLongs)
        assert(d.isNaN() && dLeBytes.isNaN() || d == dLeBytes)
        assert(d.isNaN() && dLeLongs.isNaN() || d == dLeLongs)
    }
}
