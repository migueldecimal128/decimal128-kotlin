package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class TestMutDecMagFinalize {

    val verbose = false

    class TC(val bdA: BigDecimal, val decEnv: DecEnv) {
        constructor(str: String, decEnv: DecEnv) : this(BigDecimal(str), decEnv)
        constructor(str: String) : this(str, DecEnv())
        constructor(bdA: BigDecimal) : this(bdA, DecEnv())
    }

    val cases = arrayOf(
        TC("1e-6176"),
        TC("9e-6177"),
        TC("1e-6177"),
        TC("15e-6175"),
        TC("15e-6176"),
        TC("15e-6177"),
        TC("15e-6178"),
        TC("15e-6179"),
        TC("15e6145"),
        TC("1.234567890123456789012345678901234e6144"),
        TC("0.0000"),
        TC("0"),
        TC("1"),
        TC("1e6144"),
        TC("1e6145"),
        )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    @Test
    fun testRandom() {
        for (i in 0..<100) {
            val case = TC(randBd())
            test1(case)
        }

    }

    val random = Random()

    fun randBd() : BigDecimal {
        val bitLength = random.nextInt(0, 256)
        val bi = BigInteger(bitLength, random)
        val exp = random.nextInt(12400) - 6200
        val bd = BigDecimal(bi).scaleByPowerOfTen(exp)
        return bd
    }

    fun test1(case: TC) {
        val bdA = case.bdA
        val dec = MutDec()
        dec.set(bdA)
        println("bdA:$bdA => dec:$dec")
    }

}
