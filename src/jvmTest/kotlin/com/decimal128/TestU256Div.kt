package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.util.*

class TestU256Div {

    val verbose = false

    class TC(val biA: BigInteger, val biB: BigInteger) {
        val divideByZero = biB.bitLength() == 0
        val biResult = if (divideByZero) BigInteger.ZERO else biA.divide(biB)

        constructor(a: String, b:String) : this(BigInteger(a), BigInteger(b))
    }

    val cases = arrayOf(
        TC("181579471713411303459569947105762891142062069", "8285480218070722738815141008944333556901"),
        TC(ONE.shiftLeft(120), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(121), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(122), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(123), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(124), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(125), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(126), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(127), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(128), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(191), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(192), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(255), ONE.shiftLeft(32)),
        TC("99999999999", "9999999999"),
        TC(ONE.shiftLeft(33), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(62), ONE.shiftLeft(32)),
        TC(ONE.shiftLeft(127), ONE.shiftLeft(32)),
        TC("1", "0"),
        TC("7", "1"),
        TC("8", "2"),
        TC("9", "9"),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    val deltas = arrayOf(ONE.negate(), BigInteger.ZERO, ONE)

    @Test
    fun testBoundaries() {
        for (i in 0..<77) {
            val biX = BigInteger.TEN.pow(i)
            for (j in 0..<(44-i)) {
                val biY = BigInteger.TEN.pow(j)
                for (deltaX in deltas) {
                    val biA = biX.add(deltaX)
                    for (deltaY in deltas) {
                        val biB = biY.add(deltaY)
                        val tc = TC(biA, biB)
                        if (tc.biResult.bitLength() <= 256)
                            test1(tc)
                    }
                }
            }
        }
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val case = TC(randBi(), randBi())
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
        val expected = case.biResult
        if (case.divideByZero || expected.bitLength() > 256)
            return
        val coeffA = newCoeff(case.biA)
        val coeffB = newCoeff(case.biB)
        val coeffC = U256()
        if (verbose)
            println("$coeffA (${coeffA.digitLen}) / $coeffB (${coeffB.digitLen}) = expected:$expected")
        coeffC.u256SetDiv(coeffA, coeffB)
        val biC = coeffC.coeffToBigInteger()
        if (! biC.equals(expected))
            println("$coeffA (${coeffA.digitLen}) / $coeffB (${coeffB.digitLen}) = $coeffC (${coeffC.digitLen})  expected:$expected")
        assert (biC.equals(expected))
        if (random.nextBoolean()) {
            coeffA.u256SetDiv(coeffA, coeffB)
            assert(coeffA.coeffToBigInteger().equals(expected))
        } else {
            coeffB.u256SetDiv(coeffA, coeffB)
            assert(coeffB.coeffToBigInteger().equals(expected))

        }
    }

}
