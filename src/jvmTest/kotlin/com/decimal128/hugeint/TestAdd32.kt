package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import java.math.BigInteger
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAdd32 {

    val verbose = false

    @Test
    fun testAdd32() {
        for (i in 0..<1000000)
            test1()
    }

    fun test1() {
        val hi = randomHi(300)
        val bi = hi.toBigInteger()
        val w = rng.nextUInt()
        if (verbose)
            println("hi:$hi w:$w")
        val sumBi = (bi + BigInteger.valueOf(w.toLong())).toHugeInt()
        val sum1 = hi + HugeInt.from(w)
        val sum2 = hi + w
        val sum3 = w + hi
        assertEquals(sumBi, sum1)
        assertEquals(sum1, sum2)
        assertEquals(sum2, sum3)

    }

    val rng = Random.Default

    fun randomHi(hiBitLen: Int) =
        HugeInt.fromRandom(rng.nextInt(hiBitLen), rng)

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

}