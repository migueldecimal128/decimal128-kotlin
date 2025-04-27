package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.math.BigInteger
import java.util.*

class TestCalcCoeffDigitCount256 {

    @Test
    fun test() {
        test1(BigInteger.ONE.shiftLeft(192))
        for (pow in 58..76)
            test3(BigInteger.TEN.pow(pow))
        test1(BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE))
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
        val dw2 = bi.shiftRight(128).toLong()
        val dw3 = bi.shiftRight(192).toLong()
        val observed = CoeffDigitCount.calcDigitCount256(dw3, dw2, dw1, dw0)
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
            val bitLength = random.nextInt(193, 257)
            bi = BigInteger(bitLength, random)
        } while (bi.bitLength() < 193)
        test1(bi)
    }


}