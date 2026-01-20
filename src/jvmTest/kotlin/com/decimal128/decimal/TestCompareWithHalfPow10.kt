package com.decimal128.decimal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.util.*

class TestCompareWithHalfPow10 {

    val verbose = false

    class TC(val biA:BigInteger) {
        val bitLen = biA.bitLength()
        val digitLen = biA.toString().length
        val half = BigInteger.TEN.pow(digitLen).shiftRight(1)
        val expected = biA.compareTo(half)
        constructor(a:String) : this(BigInteger(a))
    }

    val cases = arrayOf(
        TC("1096304271182483964159462071707506698374233386357680501"),
        TC("3115923653267705855292437738999867925214881860306447889664"),
        TC("44710045652749765965"),
        TC("22447106069505776011"),
        TC("159742644080491113530329846633679635398"),
        TC("0"),
        TC("4"),
        TC("5"),
        TC("6"),
        TC("49"),
        TC("50"),
        TC("51"),
        TC("4999999999999999999"),
        TC("5000000000000000000"),
        TC("5000000000000000001"),
        TC(ONE.shiftLeft(64).subtract(ONE)),
        TC(ONE.shiftLeft(256).subtract(ONE)),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val case = TC(randBi())
            test1(case)
        }

    }

    val random = Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 256)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun test1(case: TC) {
        val biA = case.biA
        val bitLen = case.bitLen
        val digitLen = case.digitLen
        val expected = case.expected
        if (verbose)
            println("$biA ($digitLen) compareWithHalfPow10 expected:$expected")
        val observed = when {
            digitLen < MIN_POW10_DIGIT_LEN_128 ->
                compareWithHalfPow10_1(biA.toLong(), digitLen)
            digitLen < MIN_POW10_DIGIT_LEN_192 ->
                compareWithHalfPow10_2(biA.shiftRight(64).toLong(), biA.toLong(), digitLen)
            digitLen < MIN_POW10_DIGIT_LEN_256 ->
                compareWithHalfPow10_3(biA.shiftRight(128).toLong(), biA.shiftRight(64).toLong(), biA.toLong(), digitLen)
            else ->
                compareWithHalfPow10_4(biA.shiftRight(192).toLong(), biA.shiftRight(128).toLong(), biA.shiftRight(64).toLong(), biA.toLong(), digitLen)
        }
        assertEquals(expected, observed)
    }

}