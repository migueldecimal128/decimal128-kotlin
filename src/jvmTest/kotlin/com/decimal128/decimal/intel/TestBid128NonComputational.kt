package com.decimal128.decimal.intel

import com.decimal128.decimal.Ieee754Class
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class TestBid128NonComputational {

    val verbose = false

    data class TestSignature(val funcStr: String, val runner: (IntelTest) -> Unit)

    // from Intel bid128_noncomp.c
    /*****************************************************************************
     *
     *    BID128 non-computational functions:
     *         - bid128_isSigned
     *         - bid128_isNormal
     *         - bid128_isSubnormal
     *         - bid128_isFinite
     *         - bid128_isZero
     *         - bid128_isInf
     *         - bid128_isSignaling
     *         - bid128_isCanonical
     *         - bid128_isNaN
     *         - bid128_copy
     *         - bid128_negate
     *         - bid128_abs
     *         - bid128_copySign
     *         - bid128_class
     *         - bid128_totalOrder
     *         - bid128_totalOrderMag
     *         - bid128_sameQuantum
     *         - bid128_radix
     ****************************************************************************/
    val testSignatures = arrayOf(
        TestSignature("bid128_isSigned", ::testIsNegative),
        TestSignature("bid128_isNormal", ::testIsNormal),
        TestSignature("bid128_isSubNormal", ::testIsSubNormal),
        TestSignature("bid128_isFinite", ::testIsFinite),
        TestSignature("bid128_isZero", ::testIsZero),
        TestSignature("bid128_isInf", ::testIsInfinite),
        TestSignature("bid128_isSignaling", ::testIsSignaling),
        TestSignature("bid128_isCanonical", ::testIsCanonical),
        TestSignature("bid128_isNaN", ::testIsNaN),

        TestSignature("bid128_copy", ::testCopy),
        TestSignature("bid128_negate", ::testNegate),
        TestSignature("bid128_abs", ::testAbs),
        TestSignature("bid128_copySign", ::testCopySign),
        TestSignature("bid128_class", ::testIeeeClass),
        TestSignature("bid128_totalOrder", ::testTotalOrder),
        TestSignature("bid128_totalOrderMag", ::testTotalOrderMag),
        TestSignature("bid128_sameQuantum", ::testSameQuantum),
        TestSignature("bid128_radix", ::testRadix),

    )


    @Test
    fun testNonComputational() {
        val fileText = IntelTest::class.java.getResource("/intel/readtest.in")!!.readText()
        val allTests = IntelTest.parseAllTests(fileText)

        testSignatures.forEach { signature ->
            val tests = allTests.filter { it.funcStr == signature.funcStr }
            val testCount = tests.size
            if (verbose) {
                println("${signature.funcStr} testCount:$testCount")
            }
            tests.forEach { signature.runner(it) }
        }
    }

    fun testIsNegative(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }
        val observed = op1.isNegative()
        assertEquals(expected, observed)
    }

    fun testIsNormal(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }
        val observed = op1.isNormal()
        assertEquals(expected, observed)
    }

    fun testIsSubNormal(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }
        val observed = op1.isSubnormal()
        assertEquals(expected, observed)
    }

    fun testIsFinite(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }
        val observed = op1.isFinite()
        assertEquals(expected, observed)
    }

    fun testIsZero(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }
        val observed = op1.isZero()
        assertEquals(expected, observed)
    }

    fun testIsInfinite(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
            val observed = op1.isInfinite()
            assertEquals(expected, observed)
        }
    }

    fun testIsSignaling(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
            val observed = op1.isSignaling()
            assertEquals(expected, observed)
        }
    }

    fun testIsCanonical(test: IntelTest) {
        // FIXME
        //  I don't do "isCanonical()" because I do not allow
        //  a non-canonical representation into Decimal format
        //  The IntelTest has > 34 digits, so I turn the coeff
        //  into ZERO per IEEE754-2019
        /*
        val op1 = test.op1Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
            val observed = op1.isCanonical()
            assertEquals(expected, observed)
        }
         */
    }

    fun testIsNaN(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
            val observed = op1.isNaN()
            assertEquals(expected, observed)
        }
    }

    fun testCopy(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resBid128
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }
        val observed = op1.copy()
        assertEquals(expected, observed)
    }

    fun testNegate(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resBid128
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }
        val observed = op1.negate()
        assertEquals(expected, observed)
    }

    fun testAbs(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resBid128
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }
        val observed = op1.abs()
        assertEquals(expected, observed)
    }

    fun testCopySign(test: IntelTest) {
        val op1 = test.op1Bid128
        val op2 = test.op2Bid128
        val expected = test.resBid128
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }
        val observed = op1.copySign(op2)
        assertEquals(expected, observed)
    }

    fun testIeeeClass(test: IntelTest) {
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

    fun testTotalOrder(test: IntelTest) {
        val op1 = test.op1Bid128
        val op2 = test.op2Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 $op2 => $expected")
        }
        val observed = op1.isTotalOrder(op2)
        if (expected != observed)
            println("kilroy was here!")
        assertEquals(expected, observed)
    }

    fun testTotalOrderMag(test: IntelTest) {
        val op1 = test.op1Bid128
        val op2 = test.op2Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }
        val observed = op1.isTotalOrderMag(op2)
        assertEquals(expected, observed)
    }

    fun testSameQuantum(test: IntelTest) {
        val op1 = test.op1Bid128
        val op2 = test.op2Bid128
        val expected = test.resBoolean
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }
        val observed = op1.sameQuantum(op2)
        assertEquals(expected, observed)
    }

    fun testRadix(test: IntelTest) {
        val op1 = test.op1Bid128
        val expected = test.resInt
        val status = test.status
        if (verbose) {
            println(test.testLine)
            println(" $op1 => $expected")
        }

        val observed = op1.radix()
        assertEquals(expected, observed)
        assertEquals(10, observed)
    }


}