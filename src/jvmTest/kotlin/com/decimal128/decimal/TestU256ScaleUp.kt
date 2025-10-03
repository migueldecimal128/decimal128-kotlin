package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*

class TestU256ScaleUp {

    val verbose = false

    class TC(val biA: BigInteger, val pow10: Int) {
        val biProduct = biA.multiply(BigInteger.TEN.pow(pow10))

        constructor(a: String, b: Int) : this(BigInteger(a), b)
    }

    val cases = arrayOf(
        TC("17158008246618608233531190970817760693366", 17),
        TC(BigInteger.TEN.pow(76).subtract(BigInteger.ONE), 1),
        TC("2", 59),
        TC("2", 58),
        TC("0", 60),
        TC("1", 1),
        TC("999", 1),
        TC("999", 2),
        TC("999999999999999999", 1),
        TC("999999999999999999", 2),
        TC("999999999999999999", 3),
        TC("9999999999999999999", 1),
        TC("9999999999999999999", 2),
        TC("9999999999999999999", 3),
        TC("99999999999999999999", 1),
        TC("99999999999999999999", 2),
        TC("99999999999999999999", 3),
        TC(BigInteger.TEN.pow(76).subtract(BigInteger.ONE), 1),
    )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testHsdis() {
        val tc = TC("17158008246618608233531190970817760693366", 17)
        for (i in 0..<100000)
            test1(tc)
    }

    val deltas = arrayOf(BigInteger.ONE.negate(), BigInteger.ZERO, BigInteger.ONE)

    @Test
    fun testBoundaries() {
        for (i in 0..<77) {
            val biX = BigInteger.TEN.pow(i)
            for (pow10 in 0..<77-i) {
                for (deltaX in deltas) {
                    val biA = biX.add(deltaX)
                    val tc = TC(biA, pow10)
                    if (tc.biProduct.bitLength() <= 256)
                        test1(tc)
                }
            }
        }
    }

    val random = Random()

    @Test
    fun testRandom() {
        for (i in 0..<1000) {
            val bi = randBi()
            val pow10 = randPow(bi)
            val case = TC(bi, pow10)
            test1(case)
        }

    }

    fun randPow(bi: BigInteger) : Int {
        val biDigitCount = bi.toString().length
        val maxPow = 78 - biDigitCount
        val randPow = random.nextInt(maxPow)
        return randPow
    }

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 255)
        val bi = BigInteger(bitLength, random)
        return bi
    }




    fun test1(case: TC) {
        val expected = case.biProduct
        if (expected.bitLength() > 256) {
            println("product would overflow ... skipped")
            return
        }
        val coeffA = newCoeff(case.biA)
        val coeffObserved = U256()
        val pow10 = case.pow10
        val decEnv = DecEnv()
        if (verbose)
            println("$coeffA (${coeffA.digitLen}) * 10**$pow10 = expected:$expected")
        coeffObserved.u256SetScaleUpPow10(coeffA, pow10)
        val observed = coeffObserved.coeffToBigInteger()
        if (! observed.equals(expected))
            println("$coeffA (${coeffA.digitLen}) * 10**$pow10 = $coeffObserved (${coeffObserved.digitLen})  expected:$expected")
        assert (observed.equals(expected))
    }

}