package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

class TestDivModPow10 {

    fun powerOf10(n: Int): Long {
        require(n >= 0 && n <= 19)
        var result = 1L
        repeat(n) {
            result *= 10L
        }
        return result
    }

    @Test
    fun test() {
//        for (i in minDividendDigitCount..maxDividendDigitCount) {
        for (i in 1..19) {
            val dividend = powerOf10(i)
            for (divisorPow10 in minDivisorPow10..maxDivisorPow10) {
                test3(dividend, divisorPow10)
            }
        }
        for (divisorPow10 in 1..19)
            test1(-1, divisorPow10)
    }


    fun test3(l: Long, divisorPow10: Int) {
        test1(l- 1, divisorPow10)
        test1(l, divisorPow10)
        test1(l + 1, divisorPow10)
    }

    fun test1(l: Long, divisorPow10: Int) {
        val biDividend = bigIntegerFromLong(l)
        val biDivisor = BigInteger.TEN.pow(divisorPow10)
        val (biQuot, biRem) = biDividend.divideAndRemainder(biDivisor)

        val (quot, rem) = RecipMulPow10.divModPow10(calcDigitCount64(l), l, divisorPow10)

        assertEquals(biQuot, bigIntegerFromLong(quot))
        assertEquals(biRem, bigIntegerFromLong(rem))
    }

    fun bigIntegerFromLong(l: Long) : BigInteger {
        var bi = BigInteger((l and Long.MAX_VALUE).toString())
        if (l < 0)
            bi = bi or BigInteger.ONE.shiftLeft(63)
        return bi
    }

}