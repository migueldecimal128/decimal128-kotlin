package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestC256Print {

    val verbose = true

    val tcs = arrayOf(
        "12345678901234567890123456789012345678901234567890",
    )

    @Test
    fun testCases() {
        for (tc in tcs) {
            test(tc)
        }
    }

    fun test(tc: String) {
        val c = C256()
        c.c256Set(tc)
        val observed = c.toString()
        assertEquals(tc, observed)
    }
}