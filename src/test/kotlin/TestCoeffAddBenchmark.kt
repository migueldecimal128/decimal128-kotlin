package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.ONE

import java.util.Random
import kotlin.math.min

class TestCoeffAddBenchmark {

    val random = Random()

    val size = 1000000
    val data = Array<Coeff>(size) {
        val bi = randBi()
        Coeff(bi)
    }

    fun randBi() :BigInteger {
        val bitLength0 = random.nextInt(0, 255)
        val bitLength1 = random.nextInt(0, 255)
        val bitLength2 = random.nextInt(0, 255)
        val bitLength3 = random.nextInt(0, 255)
        val bitLength = min(min(min(bitLength0, bitLength1), bitLength2), bitLength3)
        val bi = BigInteger(bitLength, random)
        return bi
    }

    fun benchmark():Int {
        val s = Coeff()
        for (i in 0..<size-1) {
            val x = data[i]
            val y = data[i + 1]
            CoeffAdd.coeffAddUnscaled_bit(s, x, y)
        }
        return s.bitLen
    }

    val runCount = 100
    @Test
    fun testBenchmark() {
        benchmark()
        benchmark()
        var s = 0L
        val start = System.currentTimeMillis()
        for (i in 0..<runCount)
            s+= benchmark()
        val end = System.currentTimeMillis()
        val secs = (end-start).toDouble()/1000.0
        println("s:$s after $secs")
    }

}
