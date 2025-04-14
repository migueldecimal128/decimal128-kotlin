package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import java.math.BigInteger
class TestCalcDigitCount192 {

    @Test
    fun test() {
        test1(BigInteger.ONE.shiftLeft(128))
        for (pow in 39..56)
            test3(BigInteger.TEN.pow(pow))
        test1(BigInteger.ONE.shiftLeft(192).subtract(BigInteger.ONE))
    }

    fun test3(bi: BigInteger) {
        test1(bi.subtract(BigInteger.ONE))
        test1(bi)
        test1(bi.add(BigInteger.ONE))
    }

    @Test
    fun test_39() {
        test1(BigInteger.TEN.pow(39).subtract(BigInteger.ONE))
    }

    fun test1(bi: BigInteger) {
        val biStrLen = bi.toString().length
        val expected = biStrLen
        val dw0 = bi.toLong()
        val dw1 = bi.shiftRight(64).toLong()
        val dw2 = bi.shiftRight(128).toLong()
        val observed = calcDigitCount192(dw2, dw1, dw0)
        if (observed != expected)
            println(bi)
        assertEquals(expected, observed)
    }

}