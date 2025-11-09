package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigInteger
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.test.Test

class TestMutMul64 {

    val verbose = false

    @Test
    fun testMutMul64() {
        val mhi = MutHugeInt()
        for (i in 0..<10000) {
            testUnsigned(mhi)
            testSigned(mhi)
        }
    }

    fun testUnsigned(mhi: MutHugeInt) {
        mhi.setRandom(5)
        val bi = mhi.toBigInteger()
        val dw = rng.nextULong()
        if (verbose)
            println("mhi:$mhi dw:$dw")
        var prodBi = bi * BigInteger("$dw")
        val prod1 = mhi.copy()
        prod1 *= HugeInt.from(dw)
        val prod2 = mhi.copy()
        prod2 *= dw
        assertTrue(prod1 EQ prodBi.toHugeInt())
        assertTrue(prod1 EQ prod2)

        for (i in 0..<10) {
            val dw2 = rng.nextULong()
            prod1 *= HugeInt.from(dw2)
            prod2 *= dw2
            prodBi *= BigInteger("$dw2")
        }

        assertTrue(prod1 EQ prodBi.toHugeInt())
        assertTrue(prod1 EQ prod2)

        prod1 *= prod2
        prod2 *= prod2

        val prodBi1 = prodBi * prodBi
        assertTrue(prod1 EQ prodBi1.toHugeInt())
        assertTrue(prod1 EQ prod2)

    }

    fun testSigned(mhi: MutHugeInt) {
        mhi.setRandom(250)
        val bi = mhi.toBigInteger()
        val l = rng.nextLong()
        if (verbose)
            println("signed: mhi:$mhi l:$l")
        var prodBi = bi * BigInteger.valueOf(l)
        val prod1 = mhi.copy()
        prod1 *= HugeInt.from(l)
        val prod2 = mhi.copy()
        prod2 *= l
        assertTrue(prod1 EQ prodBi.toHugeInt())
        assertTrue(prod1 EQ prod2)

        for (i in 0..<rng.nextInt(50)) {
            val l2 = rng.nextLong()
            prod1 *= HugeInt.from(l2)
            prod2 *= l2
            prodBi *= BigInteger("$l2")
        }

        assertTrue(prod1 EQ prodBi.toHugeInt())
        assertTrue(prod1 EQ prod2)
    }

    val rng = Random.Default

    fun randomHi(hiBitLen: Int): HugeInt {
        val rand = HugeInt.fromRandom(rng.nextInt(hiBitLen), rng)
        return if (rng.nextBoolean()) rand.negate() else rand
    }

}