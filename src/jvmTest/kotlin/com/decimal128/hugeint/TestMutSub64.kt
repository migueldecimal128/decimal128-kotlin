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

class TestMutSub64 {

    val verbose = false

    val mhi = MutHugeInt()
    val diff1 = MutHugeInt()
    val diff2 = MutHugeInt()

    @Test
    fun testMutSub64() {
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
        var diffBi = bi - BigInteger("$dw")
        diff1.set(mhi)
        diff1 -= HugeInt.from(dw)
        diff2.set(mhi)
        diff2 -= dw
        val diffBiHi = diffBi.toHugeInt()
        if (diff1 NE diffBiHi) {
            println("fail! diff1:$diff1 diffBi:$diffBi")
            val foo = diff1 EQ diffBiHi
            println("foo:$foo")
        }
        assertEquals("$diff1", "$diffBi")
        assertTrue(diff1 EQ diffBiHi)
        assertTrue(diff1 EQ diff2)

        for (i in 0..<25) {
            val dw2 = rng.nextULong()
            diff1 -= HugeInt.from(dw2)
            diff2 -= dw2
            diffBi -= BigInteger("$dw2")
        }

        if (diff1 NE diffBi.toHugeInt()) {
            println("fail")
        }
        assertTrue(diff1 EQ diffBi.toHugeInt())
        assertTrue(diff1 EQ diff2)

        val diff1Save = MutHugeInt().set(diff1)
        val diff2Save = MutHugeInt().set(diff2)
        diff1 -= diff2
        diff2 -= diff2

        val diffBi1 = diffBi - diffBi
        val sumBiHi1 = diffBi1.toHugeInt()
        if (diff1 NE sumBiHi1) {
            println("fail! diff1:$diff1 diffBiHi1:$sumBiHi1")
            diff1Save -= diff2Save
            println("diff1Save:$diff1Save diff2Save:$diff2Save")
        }
        assertTrue(diff1 EQ sumBiHi1)
        assertTrue(diff1 EQ diff2)

    }

    fun testSigned() {
        mhi.set(HugeInt.fromRandom(200))
        val bi = mhi.toBigInteger()
        val l = rng.nextLong()
        if (verbose)
            println("mhi:$mhi l:$l")
        var diffBi = bi - BigInteger.valueOf(l)
        diff1.set(mhi)
        diff1 -= HugeInt.from(l)
        diff2.set(mhi)
        diff2 -= l
        assertTrue(diff1 EQ diffBi.toHugeInt())
        assertTrue(diff1 EQ diff2)

        for (i in 0..<rng.nextInt(100)) {
            val l2 = rng.nextLong()
            diff1 -= HugeInt.from(l2)
            diff2 -= l2
            diffBi -= BigInteger.valueOf(l2)
            val foo = true
        }

        assertTrue(diff1 EQ diffBi.toHugeInt())
        assertTrue(diff1 EQ diff2)
    }

    val rng = Random.Default

    fun randomHi(hiBitLen: Int): HugeInt {
        val rand = HugeInt.fromRandom(rng.nextInt(hiBitLen), rng)
        return if (rng.nextBoolean()) rand.negate() else rand
    }

}