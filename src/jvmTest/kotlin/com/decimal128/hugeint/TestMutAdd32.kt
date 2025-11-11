package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigInteger
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMutAdd32 {

    val verbose = false

    @Test
    fun testMutAdd32() {
        val mhi = HugeIntAccumulator()
        for (i in 0..<100000) {
            testUnsigned(mhi)
            testSigned(mhi)
        }
    }

    fun testUnsigned(mhi: HugeIntAccumulator) {
        mhi.set(HugeInt.fromRandom(200))
        val bi = mhi.toBigInteger()
        val w = rng.nextUInt()
        if (verbose)
            println("mhi:$mhi w:$w")
        var sumBi = bi + BigInteger.valueOf(w.toLong())
        val sum1 = mhi.copy()
        sum1 += HugeInt.from(w)
        val sum2 = mhi.copy()
        sum2 += w
        assertEquals("$sum1", "$sumBi")
        assertTrue(sum1.toHugeInt() EQ sumBi.toHugeInt())
        assertTrue(sum1.toHugeInt() EQ sum2.toHugeInt())

        for (i in 0..<25) {
            val w2 = rng.nextUInt()
            sum1 += HugeInt.from(w2)
            sum2 += w2
            sumBi += BigInteger.valueOf(w2.toLong())
        }

        assertTrue(sum1.toHugeInt() EQ sumBi.toHugeInt())
        assertEquals("$sum1", "$sum2")
        assertTrue(sum1.toHugeInt() EQ sum2.toHugeInt())

        val sum1Save = HugeIntAccumulator().set(sum1)
        sum1 += sum2
        sum2 += sum2

        val sumBi1 = sumBi + sumBi
        assertTrue(sum1.toHugeInt() EQ sumBi1.toHugeInt())
        assertTrue(sum1.toHugeInt() EQ sum2.toHugeInt())

    }

    fun testSigned(mhi: HugeIntAccumulator) {
        mhi.set(HugeInt.fromRandom(200))
        val bi = mhi.toBigInteger()
        val n = rng.nextInt()
        if (verbose)
            println("mhi:$mhi n:$n")
        var sumBi = bi + BigInteger.valueOf(n.toLong())
        val sum1 = mhi.copy()
        sum1 += HugeInt.from(n)
        val sum2 = mhi.copy()
        sum2 += n
        assertTrue(sum1.toHugeInt() EQ sumBi.toHugeInt())
        assertTrue(sum1.toHugeInt() EQ sum2.toHugeInt())

        for (i in 0..<rng.nextInt(1000)) {
            val n2 = rng.nextUInt()
            sum1 += HugeInt.from(n2)
            sum2 += n2
            sumBi += BigInteger.valueOf(n2.toLong())
        }

        assertTrue(sum1.toHugeInt() EQ sumBi.toHugeInt())
        assertTrue(sum1.toHugeInt() EQ sum2.toHugeInt())
    }

    val rng = Random.Default

    fun randomHi(hiBitLen: Int): HugeInt {
        val rand = HugeInt.fromRandom(rng.nextInt(hiBitLen), rng)
        return if (rng.nextBoolean()) rand.negate() else rand
    }

}