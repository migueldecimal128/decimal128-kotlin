package com.decimal128.decimal

import org.junit.jupiter.api.Test

class TestJnaBidDpdShim {

    @Test
    fun test1() {
        val jnaBidDpdShim = JnaBidDpdShim.INSTANCE
        val values = listOf(
            "0", "-0",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "1000e-2", "100e-1", "10", "1e1",
            "123.45", "-9999999999999",
            "1E369", "9.99999999999999E-383", // d64 edges
            "1E6145", "9.999999999999999E-6143", // d128 edges
            "Infinity", "-Infinity", "NaN", "sNaN", "NaN(123)", "sNaN(456)"
        )

        for (v in values) {
            println("=== $v ===")

            // --- decimal128 ---
            val d128_dpd = ByteArray(16)
            val d128_bid = ByteArray(16)
            check(jnaBidDpdShim.d128_dpd_le_from_string(v, d128_dpd) == 0)
            check(jnaBidDpdShim.d128_bid_le_from_string(v, d128_bid) == 0)

            val sBuf = ByteArray(128)
            jnaBidDpdShim.d128_dpd_le_to_string(d128_dpd, sBuf, sBuf.size)
            val d128_dpd_rt = sBuf.cString()
            jnaBidDpdShim.d128_bid_le_to_string(d128_bid, sBuf, sBuf.size)
            val d128_bid_rt = sBuf.cString()

            println("d128 DPD hex: ${d128_dpd.hex()}  rt=\"$d128_dpd_rt\"")
            println("d128 BID hex: ${d128_bid.hex()}  rt=\"$d128_bid_rt\"")

            // --- decimal64 ---
            val d64_dpd = ByteArray(8)
            val d64_bid = ByteArray(8)
            check(jnaBidDpdShim.d64_dpd_le_from_string(v, d64_dpd) == 0)
            check(jnaBidDpdShim.d64_bid_le_from_string(v, d64_bid) == 0)

            val sBuf2 = ByteArray(128)
            jnaBidDpdShim.d64_dpd_le_to_string(d64_dpd, sBuf2, sBuf2.size)
            val d64_dpd_rt = sBuf2.cString()
            jnaBidDpdShim.d64_bid_le_to_string(d64_bid, sBuf2, sBuf2.size)
            val d64_bid_rt = sBuf2.cString()

            println(" d64 DPD hex: ${d64_dpd.hex()}  rt=\"$d64_dpd_rt\"")
            println(" d64 BID hex: ${d64_bid.hex()}  rt=\"$d64_bid_rt\"")
        }
    }

}