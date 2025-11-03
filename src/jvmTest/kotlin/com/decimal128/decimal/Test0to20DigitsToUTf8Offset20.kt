package com.decimal128.decimal

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.test.assertEquals

@OptIn(ExperimentalUnsignedTypes::class)
class Test0to20DigitsToUTf8Offset20 {

    val tcs = ulongArrayOf(
        0uL,
        1234uL,
    )

    @Test
    fun testCases() {
        for (tc in tcs) {
            test1(tc)
        }
    }

    val rand = Random.Default

    @Test
    fun testRandom() {
        for (i in 0..<1000000)
            test1(rand.nextULong())
    }

    val utf8 = ByteArray(128)

    fun test1(dw: ULong) {

        val expected = dw.toString()
        val digitPrintCount = expected.length

        Int256ParsePrint.zeroTo20DigitsToUtf8Offset20(digitPrintCount, dw.toLong(), utf8)
        val observed = String(utf8, 20, digitPrintCount)
        assertEquals(expected, observed)
    }
}