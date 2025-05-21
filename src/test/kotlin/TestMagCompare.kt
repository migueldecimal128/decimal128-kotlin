package com.decimal128

import com.decimal128.RoundingDirection.Companion.ROUND_TIES_TO_AWAY
import com.decimal128.RoundingDirection.Companion.ROUND_TOWARD_ZERO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.math.MathContext
import java.util.*

class TestMagCompare {

    val verbose = false

    class TC(val bdA: BigDecimal, val bdB: BigDecimal) {
        constructor(strA: String, strB: String) : this(BigDecimal(strA), BigDecimal(strB))
        val expected = bdA.compareTo(bdB)
    }

    val cases = arrayOf(
        TC("111111111122222222223333333333", "1e29"),
        TC("18201130246974906665726322771E+130", "883410224284068E+144"),
        TC("1.8201130246974906665726322771E+158", "8.83410224284068E+158"),
        TC("1.22916168990409031159019911E+4455", "7.038647239E+1529"),
        TC("1", "1"),
        TC("1", "2"),
        TC("10", "1e0"),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testRandom() {
        for (i in 0..<100000) {
            val x = randBd()
            val y = when (random.nextInt(5)) {
                0 -> x.add(BigDecimal.ONE.scaleByPowerOfTen(-x.scale()))
                1 -> x
                2-> x.multiply(BigDecimal.TWO)
                3-> BigDecimal(randBd().unscaledValue(), x.scale())
                else -> randBd()
            }
            val case = TC(x, y)
            test1(case)
        }

    }

    val random = Random()

    fun randBd() : BigDecimal {
        val bitLength = random.nextInt(0, 110)
        val bi = BigInteger(bitLength, random)
        val exp = random.nextInt(1000) - 500
        val bd = BigDecimal(bi).scaleByPowerOfTen(exp)
        return bd
    }

    fun test1(case: TC) {
        val bdA = case.bdA
        val bdB = case.bdB
        val expected = case.expected
        val magA = Mag(bdA)
        val magB = Mag(bdB)
        if (verbose)
            println("bdA:$bdA compare bdB:$bdB => $expected")
        val observed = magA.magCompareTo(magB)
        assertEquals(expected, observed)
        val observed2 = magB.magCompareTo(magA)
        assertEquals(-expected, observed2)
        val observedEQforward = magA.magEQ(magB)
        assertEquals(expected == 0, observedEQforward)
        val observedEQreverse = magB.magEQ(magA)
        assertEquals(expected == 0, observedEQreverse)
    }

}
