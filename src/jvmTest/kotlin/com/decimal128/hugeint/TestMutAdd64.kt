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

class TestMutAdd64 {

    val verbose = false

    val mhi = MutHugeInt()
    val sum1 = MutHugeInt()
    val sum2 = MutHugeInt()

    @Test
    fun testMutAdd64() {
        for (i in 0..<100000) {
            testUnsigned()
            testSigned()
        }
    }

    fun testUnsigned() {
        mhi.set(HugeInt.fromRandom(200))
        val bi = mhi.toBigInteger()
        assertTrue(mhi EQ bi.toHugeInt())
        val dw = rng.nextULong()
        if (verbose)
            println("mhi:$mhi dw:$dw")
        var sumBi = bi + BigInteger("$dw")
        sum1.setZero()
        sum1 += mhi
        sum1 += HugeInt.from(dw)
        sum2.set(mhi)
        sum2 += dw
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
            val dw2 = rng.nextULong()
            sum1 += HugeInt.from(dw2)
            sum2 += dw2
            sumBi += BigInteger("$dw2")
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

    fun testSigned() {
        mhi.set(HugeInt.fromRandom(200))
        val bi = mhi.toBigInteger()
        val l = rng.nextLong()
        if (verbose)
            println("mhi:$mhi l:$l")
        var sumBi = bi + BigInteger.valueOf(l)
        val sum1 = mhi.copy()
        sum1 += HugeInt.from(l)
        val sum2 = mhi.copy()
        sum2 += l
        assertTrue(sum1 EQ sumBi.toHugeInt())
        assertTrue(sum1 EQ sum2)

        for (i in 0..<rng.nextInt(100)) {
            val n2 = rng.nextUInt()
            sum1 += HugeInt.from(n2)
            sum2 += n2
            sumBi += BigInteger.valueOf(n2.toLong())
        }

        assertTrue(sum1 EQ sumBi.toHugeInt())
        assertTrue(sum1 EQ sum2)
    }

    val rng = Random.Default

    fun randomHi(hiBitLen: Int): HugeInt {
        val rand = HugeInt.fromRandom(rng.nextInt(hiBitLen), rng)
        return if (rng.nextBoolean()) rand.negate() else rand
    }

}