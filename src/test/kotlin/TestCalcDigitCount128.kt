package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.math.BigInteger
import java.util.*

class TestCalcDigitCount128 {

    val cases = arrayOf(
        "67427991645008806127",
        )

    @Test
    fun testCases() {
        for (case in cases) {
            val bi = BigInteger(case)
            test1(bi)
        }
    }

    @Test
    fun test() {
        test1(BigInteger.ONE.shiftLeft(64))
        for (pow in 20..37)
            test3(BigInteger.TEN.pow(pow))
        test1(BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE))
    }

    fun test3(bi: BigInteger) {
        test1(bi.subtract(BigInteger.ONE))
        test1(bi)
        test1(bi.add(BigInteger.ONE))
    }

    fun test1(bi: BigInteger) {
        val biStrLen = bi.toString().length
        val expected = biStrLen
        val dw0 = bi.toLong()
        val dw1 = bi.shiftRight(64).toLong()
        val observed = CoeffDigitCount.calcDigitCount128(dw1, dw0)
        if (! expected.equals(observed))
            println("$bi expected:$expected observed:$observed")
        assertEquals(expected, observed)
    }

    @Test
    fun testRandom() {
        for (i in 1..100000)
            test1Random()
    }

    val random = Random()

    fun test1Random() {
        var bi = BigInteger.ZERO
        do {
            val bitLength = random.nextInt(65, 129)
            bi = BigInteger(bitLength, random)
        } while (bi.bitLength() < 65)
        test1(bi)
    }



}