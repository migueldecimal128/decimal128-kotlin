package com.decimal128.decimal.intel

import com.decimal128.decimal.Ieee754Class
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class TestBid128IeeeClass {

    val verbose = false

    /**
     * from Intel bid_functions.h
     *
     * typedef enum class_types {
     *   signalingNaN,
     *   quietNaN,
     *   negativeInfinity,
     *   negativeNormal,
     *   negativeSubnormal,
     *   negativeZero,
     *   positiveZero,
     *   positiveSubnormal,
     *   positiveNormal,
     *   positiveInfinity
     * } class_t;
     *
     * This is the same order as found in IEEE754-2019.
     * Therefore, I used the same order.
     * Therefore, our enum ordinals are the same.
     */

    @Test
    fun testBid128IeeeClass() {
        val fileText = IntelTest::class.java.getResource("/intel/readtest.in")!!.readText()
        val allTests = IntelTest.parseAllTests(fileText)

        val classTests = allTests.filter { it.funcStr == "bid128_class" }
        val testCount = classTests.size
        if (verbose) {
            println("testBid128IeeeClass(): testCount:$testCount")
        }

        classTests.forEach { test ->
            val op1 = test.op1Bid128
            val expected = test.resInt
            val status = test.status
            if (verbose) {
                println(test.testLine)
                println(" $op1 => $expected ${Ieee754Class.fromOrdinal(expected)}")
            }

            val observed = op1.ieeeClass().ordinal
            assertEquals(expected, observed)
        }
    }
}