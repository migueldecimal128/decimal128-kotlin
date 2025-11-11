package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigInteger
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMutSub32 {

    val verbose = false

    @Test
    fun testMutSub32() {
        val mhi = MutHugeInt()
        for (i in 0..<100000) {
            testUnsigned(mhi)
            testSigned(mhi)
        }
    }

    fun testUnsigned(mhi: MutHugeInt) {
        mhi.set(HugeInt.fromRandom(200))
        val bi = mhi.toBigInteger()
        val w = rng.nextUInt()
        if (verbose)
            println("mhi:$mhi w:$w")
        var diffBi = bi - BigInteger.valueOf(w.toLong())
        val diff1 = mhi.copy()
        diff1 -= HugeInt.from(w)
        val diff2 = mhi.copy()
        diff2 -= w
        assertTrue(diff1.toHugeInt() EQ diffBi.toHugeInt())
        assertTrue(diff1.toHugeInt() EQ diff2.toHugeInt())

        for (i in 0..<25) {
            val w2 = rng.nextUInt()
            diff1 -= HugeInt.from(w2)
            diff2 -= w2
            diffBi -= BigInteger.valueOf(w2.toLong())
        }

        assertTrue(diff1.toHugeInt() EQ diffBi.toHugeInt())
        assertTrue(diff1.toHugeInt() EQ diff2.toHugeInt())

        val sum1Save = MutHugeInt().set(diff1)
        diff1 -= diff2
        diff2 -= diff2

        val diffBi1 = diffBi - diffBi
        assertTrue(diff1.toHugeInt() EQ diffBi1.toHugeInt())
        assertTrue(diff1.toHugeInt() EQ diff2.toHugeInt())

    }

    fun testSigned(mhi: MutHugeInt) {
        mhi.set(HugeInt.fromRandom(200))
        val bi = mhi.toBigInteger()
        val n = rng.nextInt()
        if (verbose)
            println("mhi:$mhi n:$n")
        var diffBi = bi - BigInteger.valueOf(n.toLong())
        val diff1 = mhi.copy()
        diff1 -= HugeInt.from(n)
        val diff2 = mhi.copy()
        diff2 -= n
        assertTrue(diff1.toHugeInt() EQ diffBi.toHugeInt())
        assertTrue(diff1.toHugeInt() EQ diff2.toHugeInt())

        for (i in 0..<rng.nextInt(1000)) {
            val n2 = rng.nextUInt()
            diff1 -= HugeInt.from(n2)
            diff2 -= n2
            diffBi -= BigInteger.valueOf(n2.toLong())
        }

        assertTrue(diff1.toHugeInt() EQ diffBi.toHugeInt())
        assertTrue(diff1.toHugeInt() EQ diff2.toHugeInt())
    }

    val rng = Random.Default

    fun randomHi(hiBitLen: Int): HugeInt {
        val rand = HugeInt.fromRandom(rng.nextInt(hiBitLen), rng)
        return if (rng.nextBoolean()) rand.negate() else rand
    }

    @Test
    fun testProblemChild() {
        val hi = HugeInt.from("35689796992407102546798857499")
        val w = 137190797555284112
        val sum = hi + w
        val sum2 = hi + HugeInt.from(w)
        assertEquals(sum2, sum)
    }

    @Test
    fun testProblemChild2() {
        val hi = HugeInt.from("13814960379311575371116077557")
        val w = 2401666871u
        val sum0 = hi + HugeInt.from(w)
        val sum1 = hi + w
        val sum2 = w + hi
        assertEquals(sum0, sum1)
        assertEquals(sum0, sum2)
    }

    @Test
    fun testProblemChild3() {
        val hi = HugeInt.from("-1044467618609941889539867")
        val bi = hi.toBigInteger()
        val n = -818208931
        val sum0 = hi + HugeInt.from(n)
        val sum1 = hi + n
        val sum2 = n + hi
        assertEquals(sum0, sum1)
        assertEquals(sum0, sum2)
    }

    @Test
    fun testProblem3() {
        val hi = HugeInt.from("35689796992407102546798857499")
        val bi = hi.toBigInteger()
        val hi2 = bi.toHugeInt()
        assert(hi EQ hi2)
    }

}