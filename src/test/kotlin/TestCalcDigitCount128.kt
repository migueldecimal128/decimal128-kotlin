package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.math.BigInteger
class TestCalcDigitCount128 {

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
        val observed = calcDigitCount128(dw1, dw0)
        assertEquals(expected, observed)
    }

}