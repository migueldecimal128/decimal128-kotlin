package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger
import java.math.BigInteger.TEN
import java.util.*

class TestCoeffAbsDiffScaled {

    val verbose = false

    class TC(val biX: BigInteger, val scaleDelta: Int, val biY: BigInteger) {
        val biSigned = biX.multiply(TEN.pow(scaleDelta)).subtract(biY)
        val biZ = biSigned.abs()
        val expectedResidue = if (biSigned.signum() < 0) Residue.EXACT_NEGATED else Residue.EXACT

        constructor(x: String, scaleDelta: Int, y: String) : this(BigInteger(x), scaleDelta, BigInteger(y))
    }

    val cases = arrayOf(
        TC("0", 5, "1"),
        TC("218977346744994542298305086404522", 31, "0"),
        TC("1", 1, "2"),
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
        val bitLength = random.nextInt(0, 129)
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
        val coeffX = Coeff(case.biX)
        val coeffY = Coeff(case.biY)
        val coeffZ = Coeff()
        if (verbose)
            println("${case.biX} * 10**${case.scaleDelta} - ${case.biY} => expected:$expected")
        val observedResidue = coeffZ.absDiff(coeffX, case.scaleDelta, coeffY)
        val observed = coeffZ.coeffToBigInteger()
        assertEquals(expected, observed)
        assertEquals(case.expectedResidue, observedResidue)
    }

}