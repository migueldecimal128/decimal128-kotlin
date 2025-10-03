package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.util.*

class TestMutDecSqrt{

    val verbose = false

    class TC(val bd: BigDecimal) {
        constructor(str: String) : this(BigDecimal(str))
        val sqrt0 = bd.sqrt(MathContext.DECIMAL128)
        val sqrt0Squared = sqrt0.multiply(sqrt0)
        val isPerfect = sqrt0Squared.compareTo(bd) == 0
        val zeroPadding = 34 - sqrt0.precision()
        val scale = bd.scale()
        val sqrt = if (isPerfect) {
            if ((scale and 1) != 0 && (bd.unscaledValue().mod(BigInteger.TEN).signum() == 0)) {
                sqrt0.setScale((scale + 1) / 2)
            } else {
                sqrt0
            }
        } else {
            sqrt0.setScale(sqrt0.scale() + zeroPadding, RoundingMode.UNNECESSARY)
        }
    }

    val tcs = arrayOf (
        TC("625"),
        //TC("+10E-1"),
        //TC("+10E-1839"),
        TC("+0.0E4019"),
        TC("+2139362027"),
        TC("+2139362027E-4288"),
        TC("1e1"),
        TC("1"),
        TC("2"),
        TC("10"),
        TC("4"),
        TC("16"),
        TC("256"),
        TC("400"),
        TC("40000"),
        TC("4000000"),
        TC("2"),
        TC("900"),
        TC("10000"),
        TC("1234567890123456789012345678901234"),
    )

    @Test
    fun testCases() {
        for (tc in tcs)
            test1(tc)
    }

    @Test
    fun testProblemChild() {
        val tc = TC("+10E-3")
        test1(tc)
    }

    @Test
    fun testRandom() {
        for (i in 0..<10000) {
            val tc = TC(randBd())
            test1(tc)
        }

    }

    val random = Random()

    fun randBd() : BigDecimal {
        val bitLength = random.nextInt(0, 112)
        val bi = BigInteger(bitLength, random)
        val exp = random.nextInt(3*4096) - 6176
        val bd = BigDecimal(bi).scaleByPowerOfTen(exp)
        return bd
    }

    fun test1(tc: TC) {
        val dec = MutDec()
        val decSqrt = MutDec()
        dec.set(tc.bd)
        val decEnv = DecEnv()
        decSqrt.setSqrt(dec, decEnv)
        val expected = tc.sqrt
        assertEquals(expected.unscaledValue(), decSqrt.coeffToBigInteger())
        assertEquals(-expected.scale(), decSqrt.qExp)
    }

}
