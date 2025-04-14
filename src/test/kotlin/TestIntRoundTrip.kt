package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestIntRoundTrip {

    val cases = intArrayOf(0, 1, 9, 10, 99, 100, 999, 1000, 9999, 10000, 99999, 100000,
        999999, 1000000, 9999999, 10000000, 99999999, 100000000, Int.MAX_VALUE, Int.MIN_VALUE, -123456789, -1)

    @Test
    fun testInt32() {
        for (case in cases)
            test1(case)
    }

    fun test1(n: Int) {
        val d = DecNumber()
        val ctx = DecContext()
        val observed = d.fromInt32(n).toInt32(ctx)
        val expected = n
        if (expected != observed)
            println("n:$n  observed:$observed  expected:$expected")
        assertEquals(expected, observed)
    }

    @Test
    fun testUInt32() {
        for (case in cases)
            test1(case.toUInt())
    }

    fun test1(u: UInt) {
        val d = DecNumber()
        val ctx = DecContext()
        val observed = d.fromUInt32(u).toUInt32(ctx)
        val expected = u
        //if (expected != observed)
            println("u:$u  observed:$observed  expected:$expected")
        assertEquals(expected, observed)
    }

    val longCases = longArrayOf(0, 1, 9, 10, 99, 100, 999, 1000, 9999, 10000, 99999, 100000,
        999999, 1000000, 9999999, 10000000, 99999999, 100000000, Int.MAX_VALUE.toLong(),
        Int.MIN_VALUE.toLong(), -123456789, -1,
        Long.MAX_VALUE, Long.MIN_VALUE
    )

    @Test
    fun testInt64() {
        for (case in longCases)
            test1(case)
    }

    fun test1(l: Long) {
        val d = DecNumber()
        val ctx = DecContext()
        val observed = d.fromInt64(l).toInt64(ctx)
        val expected = l
        if (expected != observed)
            println("l:$l  observed:$observed  expected:$expected")
        assertEquals(expected, observed)
    }

    @Test
    fun testUInt64() {
        for (case in longCases)
            test1(case.toULong())
    }

    fun test1(ul: ULong) {
        val d = DecNumber()
        val ctx = DecContext()
        val observed = d.fromUInt64(ul).toUInt64(ctx)
        val expected = ul
        if (expected != observed)
            println("ul:$ul  observed:$observed  expected:$expected")
        assertEquals(expected, observed)
    }


}