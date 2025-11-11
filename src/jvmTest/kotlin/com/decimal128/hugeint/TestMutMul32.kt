package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigInteger
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMutMul32 {

    val verbose = false

    @Test
    fun testMutMul32() {
        val mhi = MutHugeInt()
        for (i in 0..<10000) {
            testUnsigned(mhi)
            testSigned(mhi)
        }
    }

    fun testUnsigned(mhi: MutHugeInt) {
        mhi.set(HugeInt.fromRandom(250))
        val bi = mhi.toBigInteger()
        val w = rng.nextUInt()
        if (verbose)
            println("mhi:$mhi w:$w")
        var prodBi = bi * BigInteger.valueOf(w.toLong())
        val prod1 = mhi.copy()
        prod1 *= HugeInt.from(w)
        val prod2 = mhi.copy()
        prod2 *= w
        assertTrue(prod1.toHugeInt() EQ prodBi.toHugeInt())
        assertTrue(prod1.toHugeInt() EQ prod2.toHugeInt())

        for (i in 0..<25) {
            val w2 = rng.nextUInt()
            prod1 *= HugeInt.from(w2)
            prod2 *= w2
            prodBi *= BigInteger.valueOf(w2.toLong())
        }

        assertTrue(prod1.toHugeInt() EQ prodBi.toHugeInt())
        assertTrue(prod1.toHugeInt() EQ prod2.toHugeInt())

        prod1 *= prod2
        prod2 *= prod2

        val prodBi1 = prodBi * prodBi
        assertTrue(prod1.toHugeInt() EQ prodBi1.toHugeInt())
        assertTrue(prod1.toHugeInt() EQ prod2.toHugeInt())

    }

    fun testSigned(mhi: MutHugeInt) {
        mhi.set(HugeInt.fromRandom(250))
        val bi = mhi.toBigInteger()
        val n = rng.nextInt()
        if (verbose)
            println("signed: mhi:$mhi n:$n")
        var prodBi = bi * BigInteger.valueOf(n.toLong())
        val prod1 = mhi.copy()
        prod1 *= HugeInt.from(n)
        val prod2 = mhi.copy()
        prod2 *= n
        assertTrue(prod1.toHugeInt() EQ prodBi.toHugeInt())
        assertTrue(prod1.toHugeInt() EQ prod2.toHugeInt())

        for (i in 0..<rng.nextInt(100)) {
            val n2 = rng.nextUInt()
            prod1 *= HugeInt.from(n2)
            prod2 *= n2
            prodBi *= BigInteger.valueOf(n2.toLong())
        }

        assertTrue(prod1.toHugeInt() EQ prodBi.toHugeInt())
        assertTrue(prod1.toHugeInt() EQ prod2.toHugeInt())
    }

    val rng = Random.Default

    fun randomHi(hiBitLen: Int): HugeInt {
        val rand = HugeInt.fromRandom(rng.nextInt(hiBitLen), rng)
        return if (rng.nextBoolean()) rand.negate() else rand
    }

}