package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigInteger
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.random.nextULong
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMutAddHi {

    val verbose = false

    val mhi = MutHugeInt()
    val sum1 = MutHugeInt()
    val sum2 = MutHugeInt()

    @Test
    fun testMutAddHi() {
        for (i in 0..<100000) {
            testHi()
        }
    }

    fun testHi() {
        mhi.setRandom(200)
        val bi = mhi.toBigInteger()
        assertTrue(mhi EQ bi.toHugeInt())
        val hi = HugeInt.fromRandom(200)
        if (verbose)
            println("mhi:$mhi hi:$hi")
        var sumBi = bi + BigInteger("$hi")
        sum1.setZero()
        sum1 += mhi
        sum1 += HugeInt.from(hi)
        sum2.set(mhi)
        sum2 += hi
        val sumBiHi = sumBi.toHugeInt()
        if (sum1 NE sumBiHi) {
            println("fail! sum1:$sum1 sumBi:$sumBi")
            val foo = sum1 EQ sumBiHi
            println("foo:$foo")
        }
        assertEquals("$sum1", "$sumBi")
        assertTrue(sum1 EQ sumBiHi)
        assertTrue(sum1 EQ sum2)

        for (i in 0..<25) {
            val hi2 = HugeInt.fromRandom(200)
            sum1 += HugeInt.from(hi2)
            sum2 += hi2
            sumBi += BigInteger("$hi2")
        }

        if (sum1 NE sumBi.toHugeInt()) {
            println("fail")
        }
        assertTrue(sum1 EQ sumBi.toHugeInt())
        assertTrue(sum1 EQ sum2)

        val sum1Save = MutHugeInt().set(sum1)
        val sum2Save = MutHugeInt().set(sum2)
        sum1 += sum2
        sum2 += sum2

        val sumBi1 = sumBi + sumBi
        val sumBiHi1 = sumBi1.toHugeInt()
        if (sum1 NE sumBiHi1) {
            println("fail! sum1:$sum1 sumBiHi1:$sumBiHi1")
            sum1Save += sum2Save
            println("sum1Save:$sum1Save sum2Save:$sum2Save")
        }
        assertTrue(sum1 EQ sumBiHi1)
        assertTrue(sum1 EQ sum2)

    }

}