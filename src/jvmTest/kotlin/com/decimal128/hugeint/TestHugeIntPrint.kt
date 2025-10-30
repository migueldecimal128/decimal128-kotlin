package com.decimal128.hugeint

import com.decimal128.decimal.testMultiplyX1
import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import java.math.BigInteger
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals

class TestHugeIntPrint {

    val verbose = false

    @Test
    fun testRandom() {
        for (i in 0..<1000000)
            testRandom1()
    }

    fun testRandom1() {
        val bi = randBi()
        val biStr = bi.toString()
        val hi = HugeInt.from(biStr)
        val hiStr = hi.toString()
        assertEquals(biStr, hiStr)
    }

    val random = java.util.Random()

    fun randBi() : BigInteger {
        val bitLength = random.nextInt(0, 256)
        val bi = BigInteger(bitLength, random)
        return bi
    }

}