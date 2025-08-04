package com.decimal128

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestU256RoundTrip {

    val cases : Array<String> = arrayOf("0", "1", "9", "10", "99")

    @Test
    fun test() {
        for (case in cases)
            test1(case)
    }

    fun test1(str: String) {
        val coeff = U256()
        coeff.u256Set(str)
        val expected = str
        val observed = coeff.toString();
        if (expected != observed)
            println("expected:$expected observed:$observed ")
        assertEquals(expected, observed)
    }

    fun test1(bi: BigInteger) {
        val coeff = U256()
        coeff.u256Set(bi)
        val observed = coeff.coeffToBigInteger()
        assert (bi.equals(observed))
    }

    @Test
    fun testRandomRoundTrip() {
        val random = Random()
        for (i in 0..<10000) {
            val bitLength = random.nextInt(0, 257)
            val biRandom = BigInteger(bitLength, random)
            test1(biRandom)
        }

    }

}