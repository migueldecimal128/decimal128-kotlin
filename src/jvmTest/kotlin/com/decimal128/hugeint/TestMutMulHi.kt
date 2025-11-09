package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMutMulHi {

    val verbose = false

    val mhi = MutHugeInt()
    val prod1 = MutHugeInt()

    @Test
    fun testMutAddHi() {
        for (i in 0..<100000) {
            testHi()
        }
    }

    fun testHi() {
        mhi.setRandom(250)
        val bi = mhi.toBigInteger()
        assertTrue(mhi EQ bi.toHugeInt())
        val hi = HugeInt.fromRandom(250)
        if (verbose)
            println("mhi:$mhi hi:$hi")
        var prodBi = bi * BigInteger("$hi")
        prod1.set(mhi)
        prod1 *= hi
        val sumBiHi = prodBi.toHugeInt()
        if (prod1 NE sumBiHi) {
            println("fail! sum1:$prod1 sumBi:$prodBi")
            val foo = prod1 EQ sumBiHi
            println("foo:$foo")
        }
        assertEquals("$prod1", "$prodBi")
        assertTrue(prod1 EQ sumBiHi)

        for (i in 0..<25) {
            val hi2 = HugeInt.fromRandom(200)
            prod1 += hi2
            prodBi += BigInteger("$hi2")
        }

        if (prod1 NE prodBi.toHugeInt()) {
            println("fail")
        }
        assertTrue(prod1 EQ prodBi.toHugeInt())

        prod1 *= prod1

        val prodBi1 = prodBi * prodBi
        val prodBiHi1 = prodBi1.toHugeInt()
        if (prod1 NE prodBiHi1) {
            println("fail! sum1:$prod1 sumBiHi1:$prodBiHi1")
        }
        assertTrue(prod1 EQ prodBiHi1)
    }

}