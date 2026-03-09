package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class TestIntelBidRoundTrip1 {

    val verbose = true

    val tcs = arrayOf(
        "[78000000000000000000000000000000]", // Infinity

        //"[7c003fffffffffff38c15b0affffffff]", // 34 digits ... too many
        //"[7c003fffffffffff38c15b08ffffffff]", // 34 digits ... too many
        "[0001ed09bead87c0378d8e62ffffffff]",
        "[78000000000000000000000000000000]", // Infinity
        "[7c000000000000000000000000000000]", // qNaN
        "[7e000000000000000000000000000000]", // sNaN
        "[00000000000000000000000000000000]",
        "[2ffe4d723cabcb53dd5f2ab27379cfc8]",
        "[ab5b7f8969162c5f,9951aecf3b28ba61]",
        "[fe001538549b96bc,d8bac0361145a524]",
        "[2FFC7C94BB6248E89D4E9D197FA236D6]",
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    fun test1(tc: String) {
        if (verbose)
            println("tc:$tc")
        val (isValid, dw1, dw0) = D128SerdeBid.parseIntelBidHex(tc)
        assertTrue(isValid)
        val ctx = DecContext.DECIMAL128
        ctx.decFlags.clearAll()
        val decimal = D128SerdeBid.decodeBid128(dw1, dw0, ctx)
        val roundTrip = LongArray(2)
        D128SerdeBid.encodeBid128(decimal, roundTrip, isLittleEndian = false)
        assertEquals(dw1, roundTrip[0])
        assertEquals(dw0, roundTrip[1])
        D128SerdeBid.encodeBid128(decimal, roundTrip, isLittleEndian = true)
        assertEquals(dw0, roundTrip[0])
        assertEquals(dw1, roundTrip[1])
    }
}