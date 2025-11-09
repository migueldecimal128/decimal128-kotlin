package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMutSubHi {

    val verbose = false

    val mhi = MutHugeInt()
    val diff1 = MutHugeInt()
    val diff2 = MutHugeInt()

    @Test
    fun testMutSubHi() {
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
        var diffBi = bi - BigInteger("$hi")
        diff1.set(mhi)
        diff1 -= HugeInt.from(hi)
        diff2.set(mhi)
        diff2 -= hi
        val diffBiHi = diffBi.toHugeInt()
        if (diff1 NE diffBiHi) {
            println("fail! sum1:$diff1 sumBi:$diffBi")
            val foo = diff1 EQ diffBiHi
            println("foo:$foo")
        }
        assertEquals("$diff1", "$diffBi")
        assertTrue(diff1 EQ diffBiHi)
        assertTrue(diff1 EQ diff2)

        for (i in 0..<25) {
            val hi2 = HugeInt.fromRandom(200)
            diff1 -= HugeInt.from(hi2)
            diff2 -= hi2
            diffBi -= BigInteger("$hi2")
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
        val diffBiHi1 = diffBi1.toHugeInt()
        if (diff1 NE diffBiHi1) {
            println("fail! diff1:$diff1 diffBiHi1:$diffBiHi1")
            diff1Save -= diff2Save
            println("diff1Save:$diff1Save diff2Save:$diff2Save")
        }
        assertTrue(diff1 EQ diffBiHi1)
        assertTrue(diff1 EQ diff2)

    }

}
