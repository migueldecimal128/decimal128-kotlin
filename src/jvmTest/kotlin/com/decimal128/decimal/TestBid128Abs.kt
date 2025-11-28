package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class TestBid128Abs {

    val verbose = true

    @Test
    fun testBid128Abs() {
        val fileText = IntelTest::class.java.getResource("/intel/readtest.in")!!.readText()
        val allTests = IntelTest.parseAllTests(fileText)

        val absTests = allTests.filter { it.funcStr == "bid128_abs" }
        val testCount = absTests.size
        if (verbose) {
            println("testBid128Abs(): testCount:$testCount")
        }

        absTests.forEach { test ->
            val op1 = test.op1Bid128
            val expected = test.resBid128
            val status = test.status
            if (verbose) {
                println(test.testLine)
                println(" $op1 => $expected")
            }
            val observed = op1.abs()
            assertEquals(expected, observed)
            assertEquals(status, 0)
        }
    }
}