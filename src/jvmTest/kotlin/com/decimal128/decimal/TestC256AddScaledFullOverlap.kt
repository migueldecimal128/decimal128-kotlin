package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger
import java.math.BigInteger.TEN
import java.util.*

class TestC256AddScaledFullOverlap {

    val verbose = false

    class TC(val biX: BigInteger, val scaleDelta: Int, val biY: BigInteger) {
        val biZ = biX.multiply(TEN.pow(scaleDelta)).add(biY)

        constructor(x: String, scaleDelta: Int, y: String) : this(BigInteger(x), scaleDelta, BigInteger(y))
    }

    val cases = arrayOf(
        TC("123456789012345678", 3, "0"),
        TC("1", 1, "2"),
        TC("123456789012345678", 1, "0"),
        TC("123456789012345678", 2, "0"),
        TC("123456789012345678", 3, "0"),
        TC("123456789012345678", 4, "0"),
        TC("123456789012345678", 5, "0"),
        TC("218977346744994542298305086404522", 31, "0"),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val case = TC(randBi(), randScaleDelta(), randBi())
            test1(case)
        }

    }

    val random = Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 128)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun randScaleDelta(): Int {
        val scaleDelta = random.nextInt(0, 34)
        return scaleDelta
    }

    fun test1(case: TC) {
        val expected = case.biZ
        if (expected.bitLength() > 256)
            return
        val coeffX = newCoeff(case.biX)
        val coeffY = newCoeff(case.biY)
        val coeffZ = C256()
        if (verbose)
            println("${case.biX} * 10**${case.scaleDelta} + ${case.biY} => expected:$expected")
        c256SetAdd(coeffZ, coeffX, case.scaleDelta, coeffY)
        val observed = coeffZ.coeffToBigInteger()
        assertEquals(expected, observed)
    }

}