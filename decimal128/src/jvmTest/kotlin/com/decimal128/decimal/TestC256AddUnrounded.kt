package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.util.*

class TestC256AddUnrounded {

    val verbose = false

    class TC(val biA:BigInteger, val biB:BigInteger) {
        val biSum = biA.add(biB)

        constructor(a:String, b:String) : this(BigInteger(a), BigInteger(b))
        init {
            check (biA.bitLength() < 128) {println("biA too big: $biA")}
            check (biB.bitLength() < 128) {println("biB too big: $biB")}
        }
    }

    val cases = arrayOf(
        TC("96391704742582514987", "0"),

        TC("9999999999999999999999999999999999", "1"),
        TC("11002044283145426452", "83"),
        TC("9898648910501246606", "8919245473293500599"),
        TC("96391704742582514987", "0"),
        TC("3000000000000000000000000000000000", "3000000000000000000000000000000000"),
        TC("7155194563848783881538838834860681", "1"),
        TC("4364312389114578406261075676734450", "6718763324937326040912731267187235"),
        TC(ONE.shiftLeft(127).subtract(ONE), ONE.shiftLeft(127).subtract(ONE)),
        TC(ONE.shiftLeft(64), ZERO),
        TC("0", "1"), TC("1", "2"),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
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
        val bitLength = random.nextInt(0, 128)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun test1(case: TC) {
        val expected = case.biSum
        if (expected.bitLength() > 256)
            return
        val coeffA = newCoeff(case.biA)
        val coeffB = newCoeff(case.biB)
        val coeffC = C256()
        c256SetAddAligned(coeffC, coeffA, coeffB, Pentad())
        if (verbose)
            println("$coeffA + $coeffB = $coeffC   expected:$expected")
        val oldDigitCount = coeffC.digitLen
        if (! coeffC.c256HasValidLengths()) {
            val digitCount = coeffC.digitLen
            println("bad digit count $coeffA + $coeffB = $coeffC was $oldDigitCount should be $digitCount")
            throw RuntimeException()
        }
        val biC = coeffC.coeffToBigInteger()
        if (! biC.equals(expected))
            println("$coeffA + $coeffB = $coeffC   expected:$expected")
        assert (biC.equals(expected))

        c256SetAddAligned(coeffA, coeffA, coeffB, Pentad())
        assert (coeffA.coeffToBigInteger().equals(expected))
    }

}