package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger.ONE

class TestCalcDigitLenFromBitLen {

    val verbose = false

    @Test
    fun testCalcDigitLenFromBitLen() {
        for (bitLen in 1..681)
            test1(bitLen)
    }

    fun calcMinDigitLenForBitLen_1(bitLen: Int): Int {
        return (((bitLen - 1) * 1233) shr 12) + 1
    }

    fun calcMinDigitLenForBitLen_2(bitLen: Int): Int {
        return ((bitLen * 1233) shr 12) + 1
    }

    fun test1(bitLen: Int) {
        val bi = ONE.shiftLeft(bitLen - 1)
        val expected = bi.toString().length
        val observed1 = calcMinDigitLenForBitLen_1(bitLen)
        val observed2 = calcMinDigitLenForBitLen_2(bitLen)
        if (verbose)
            println("bitLen:$bitLen expected:$expected observed1:$observed1 observed2:$observed2")
        assertEquals(expected, observed1)
        //assertEquals(expected, observed2)
    }
}