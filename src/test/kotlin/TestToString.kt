package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestToString {

    val cases = longArrayOf(0, 1, 9, 10, 99, 100, 999, 1000, 9999, 10000, 99999, 100000,
        999999, 1000000, 9999999, 10000000, 99999999, 100000000, Int.MAX_VALUE.toLong(),
        Int.MIN_VALUE.toLong(), -123456789, -1,
        Long.MAX_VALUE, Long.MIN_VALUE
    )

    @Test
    fun testInt64ToString() {
        for (case in cases)
            test1(case)
    }

    fun test1(l: Long) {
        val d = DecNumber()
        val ctx = DecContext()
        val observed = d.fromInt64(l).toString()
        val expected = l.toString()
        if (expected != observed)
            println("l:$l  observed:$observed  expected:$expected")
        assertEquals(expected, observed)
    }

}