package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAdd64 {

    val verbose = false

    @Test
    fun testAdd64() {
        for (i in 0..<1000000)
            test1()
    }

    fun test1() {
        val hi = randomHi(300)
        val ul = rng.nextULong()
        val hiL = HugeInt.from(ul)
        if (verbose)
            println("hi:$hi ul:$hiL")
        val sumBi = (hi.toBigInteger() + hiL.toBigInteger()).toHugeInt()
        val sum0 = hi + hiL
        val sum1 = hi + ul

        assertEquals(sumBi, sum0)
        assertEquals(sum0, sum1)

    }

    val rng = Random.Default

    fun randomHi(hiBitLen: Int) =
        HugeInt.fromRandom(rng.nextInt(hiBitLen), rng)

    @Test
    fun testProblemChild() {
        val hi = HugeInt.from("35689796992407102546798857499")
        val ul = 13719079755528411212uL
        val sum = hi + ul
        val sum2 = hi + HugeInt.from(ul)
        assertEquals(sum2, sum)
    }

}