package com.decimal128.hugeint

import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAdd64 {

    @Test
    fun testAdd64() {
        for (i in 0..<10000)
            test1()
    }

    fun test1() {
        val hi = randomHi(5)
        val ul = rng.nextULong()
        val hiL = HugeInt.from(ul)
        println("hi:$hi ul:$hiL")
        val sum0 = hi + hiL
        val sum1 = hi + ul

        if (sum0 NE sum1)
            println("sum0:$sum0 sum1:$sum1")

        val sum2 = hi + ul
        println("sum2:$sum2")

        assertEquals(sum0, sum1)

    }

    val rng = Random.Default

    fun randomHi(hiBitLen: Int) =
        HugeInt.fromRandom(rng.nextInt(hiBitLen), rng)

    @Test
    fun testProblemChild() {
        val hi = HugeInt.from(2)
        val ul = 9280946495979987673uL
        val sum = hi + ul
        val sum2 = hi + HugeInt.from(ul)
        assertEquals(sum2, sum)
    }

}