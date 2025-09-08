package com.decimal128.hugeint

import org.junit.jupiter.api.Test

class TestHugeIntParse {
    val verbose = true

    class TC(val str: String, val isValid: Boolean = true) {
    }

    val tcs = arrayOf(
        TC("0"),
        TC("1"),
        TC("-1"),
        TC("0x0"),
        TC("0x", false),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: TC) {
        if (verbose)
            println("tc:${tc.str} isValid:${tc.isValid}")
        try {
            val hi = HugeInt.fromString(tc.str)
            require (tc.isValid) {"should have succeeded"}
        } catch (e: Exception) {
            require (! tc.isValid) {"should have failed"}
        }
    }
}
