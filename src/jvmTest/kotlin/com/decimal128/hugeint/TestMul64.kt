package com.decimal128.hugeint

import com.decimal128.hugeint.HugeIntExtensions.toBigInteger
import com.decimal128.hugeint.HugeIntExtensions.toHugeInt
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMul64 {

    val verbose = false

    @Test
    fun testMul64() {
        for (i in 0..<1000000)
            test1()
    }

    fun test1() {
        val hi = randomHi(300)
        val dw = rng.nextULong()
        val hiDw = HugeInt.from(dw)
        if (verbose)
            println("hi:$hi dw:$dw")
        val prodBi = (hi.toBigInteger() * hiDw.toBigInteger()).toHugeInt()
        val prod0 = hi * hiDw
        val prod1 = hi * dw
        val prod2 = dw * hi

        assertEquals(prodBi, prod0)
        assertEquals(prod0, prod1)
        assertEquals(prod0, prod2)

    }

    val rng = Random.Default

    fun randomHi(hiBitLen: Int) =
        HugeInt.fromRandom(rng.nextInt(hiBitLen), rng)

    @Test
    fun testProblemChild() {
        val hi = HugeInt.from("35689796992407102546798857499")
        val dw = 13719079755528411212uL
        val prod = hi + dw
        val prod2 = hi + HugeInt.from(dw)
        assertEquals(prod2, prod)
    }

}