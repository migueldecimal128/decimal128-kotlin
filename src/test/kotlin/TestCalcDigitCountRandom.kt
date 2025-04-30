package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.math.BigInteger

import java.util.Random
class TestCalcDigitCountRandom {

    val cases = arrayOf(
        "296010764949953917450610088237966833637"
    )
    @Test
    fun testCases() {
        for (case in cases) {
            val bi = BigInteger(case)
            test1(bi)
        }
    }

    @Test
    fun testRandom() {
        for (i in 1..1000000)
            test1Random()
    }

    val random = Random()

    fun test1Random() {
        val bitLength = random.nextInt(1, 255)
        val bi = BigInteger(bitLength, random)
        test1(bi)
    }

    fun test1(bi: BigInteger) {
        val biStrLen = bi.toString().length
        val expected = if (bi.equals(BigInteger.ZERO)) 0 else biStrLen
        val dw0 = bi.toLong()
        val dw1 = bi.shiftRight(64).toLong()
        val dw2 = bi.shiftRight(128).toLong()
        val dw3 = bi.shiftRight(192).toLong()
        val observed = CoeffDigitLen.calcDigitLen(dw3, dw2, dw1, dw0)
        if (observed != expected)
            println("failed on $bi ($biStrLen)")
        assertEquals(expected, observed)
    }

}