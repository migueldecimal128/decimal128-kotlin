package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger
import java.util.*

import com.decimal128.RecipMulPow10.divModPow10
import com.decimal128.RecipMulPow10.getMultShift

class TestRecipMulPow10 {

    val verbose = false

    class TC(val biA: BigInteger, val pow10: Int) {
        constructor(a: String, b: Int) : this(BigInteger(a), b)

        val qrPair = biA.divideAndRemainder(BigInteger.TEN.pow(pow10))
        val biQuotient = qrPair[0]
        val biRemainder = qrPair[1]
    }

    val cases = arrayOf(
        TC("999999999999999999", 1),
        TC("514113841839100794470103300866030655828223544458509797475011690", 47),
        TC("1", 0),
        TC("999999999999999999", 1),

        TC("999", 1),
        TC("999", 2),
        //TC(BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE).divide(BigInteger.TEN), 1),
        //TC("1", 1),
        TC("999999999999999999", 1),
        TC("999999999999999999", 2),
        TC("999999999999999999", 3),
        //TC(BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE).divide(BigInteger.TEN), 1),
    )

    //@Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    val deltas = arrayOf(BigInteger.ONE.negate(), BigInteger.ZERO, BigInteger.ONE)

    //@Test
    fun testBoundaries() {
        for (i in 2..77) {
            val biX = BigInteger.TEN.pow(i)
            for (pow10 in 0..44) {
                for (deltaX in deltas) {
                    val biA = biX.add(deltaX)
                    val tc = TC(biA, pow10)
                    test1(tc)
                }
            }
        }
    }

    val random = Random()

    //@Test
    fun testRandomMul() {
        for (i in 0..<10000) {
            val bi = randBi()
            val pow10 = randPow(bi)
            val case = TC(bi, pow10)
            test1(case)
        }

    }

    fun randPow(bi: BigInteger) : Int {
        //val biDigitCount = bi.toString().length
        //val maxPow = 78 - biDigitCount
        val maxPow = 45
        val randPow = random.nextInt(maxPow)
        return randPow
    }

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 256)
        val bi = BigInteger(bitLength, random)
        return bi
    }






    fun test1(case: TC) {

        val coeffA = Coeff(case.biA)
        val pow10 = case.pow10
        if (verbose)
            println("$coeffA (${coeffA.digitCount}) divMod 10**$pow10 = expected:${case.biQuotient} ${case.biRemainder}")
        val coeffQ = Coeff()
        val coeffR = Coeff()

        if (coeffA.digitCount < MIN_DIVIDEND_DIGIT_COUNT || pow10 < MIN_DIVISOR_POW10 ||
            (coeffA.digitCount + pow10) >= MAX_DIVIDEND_DIGIT_COUNT ) {
            println("out of range")
            return
        }

        val digitCount = case.biA.toString().length
        val (mult, shift) = getMultShift(digitCount, case.pow10)
        val recipQuotient = case.biA.multiply(mult).shiftRight(shift)
        if (verbose)
            println("mult:$mult shift:$shift")
        if (shift >= 0)
            assertEquals(case.biQuotient, recipQuotient)

        divModPow10(coeffQ, coeffR, coeffA, pow10)
        val biQ = coeffQ.toBigInteger()
        val biR = coeffR.toBigInteger()
        if (! (biQ.equals(case.biQuotient) && biR.equals(case.biRemainder)))
            println("observed $biQ : $biR  expected ${case.biQuotient} : ${case.biRemainder}")
        assert (biQ.equals(case.biQuotient) && biR.equals(case.biRemainder))
    }

}

