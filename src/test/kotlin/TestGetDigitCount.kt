package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/*
fun getDigitCount(n: Int) : Int {
    val digitCount = 1 +
            ((9 - n) ushr 31) + ((99 - n) ushr 31) + ((999 - n) ushr 31) +
            ((9999 - n) ushr 31) + ((99999 - n) ushr 31) + ((999999 - n) ushr 31) +
            ((9999999 - n) ushr 31) + ((99999999 - n) ushr 31) + ((999999999 - n) ushr 31)
    return digitCount
}
*/
class TestGetDigitCount {

    val cases = intArrayOf(0, 1, 9, 10, 99, 100, 999, 1000, 9999, 10000, 99999, 100000,
        999999, 1000000, 9999999, 10000000, 99999999, 100000000, Int.MAX_VALUE)

    @Test
    fun test() {
        for (case in cases)
            test1(case)
    }

    fun test1(c: Int) {
        val expected = c.toString().length
        val observed = getDigitCount(c)
        if (expected != observed)
            println("c:$c  observed:$observed  expected:$expected")
        assertEquals(expected, observed)
    }

    @Test
    fun testArray() {
        for (len in 1 .. 3) {
            for (case in cases) {
                val uar = IntArray(len)
                uar[len - 1] = case
                test1Array(uar)
            }
        }
    }

    fun test1Array(uar: IntArray) {
        val len = uar.size
        assert(len > 0)
        val msd = uar[len - 1]
        val expected = if (msd == 0) 1 else 8 * (len - 1) + getDigitCount(msd)
        val observed = getDigits(uar, len)
        //if (expected != observed)
            println("uar:${uar.contentToString()}  observed:$observed  expected:$expected")
        assertEquals(expected, observed)
    }
}