package com.decimal128

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.math.BigInteger.ONE

import java.util.Random
class TestCalcDigitLenBenchmark {

    val size = 1000000
    val data64 = LongArray(size)

    val random = Random()

    fun initialize() {
        for (i in 0..<size step 2) {
            val r = random.nextLong()
            val shift = random.nextInt(64)
            val x = (r and if (shift == 0) -1 else (1L shl shift) - 1) or 1
            val bitLen = 64 - java.lang.Long.numberOfLeadingZeros(x)
            data64[i] = bitLen.toLong()
            data64[i + 1] = x
        }
    }

    fun benchmark64():Int {
        var s = 0
        for (i in 0..<size step 2) {
            val bitLen = data64[i].toInt()
            val x0 = data64[i + 1]
            s += CoeffDigitLen.calcDigitLen64map(bitLen, x0)
        }
        return s
    }

    fun benchmark192():Int {
        var s = 0
        for (i in 0..<size-2 step 2) {
            val bitLen = data64[i].toInt() + 128
            val x2 = data64[i + 1]
            val x1 = data64[i + 2]
            val x0 = data64[i + 3]
            s += CoeffDigitLen.calcDigitLen192map(bitLen, x2, x1, x0)
        }
        return s
    }

    val runCount = 10
    @Test
    fun testBenchmark64() {
        initialize()
        benchmark64()
        benchmark64()
        var s = 0L
        val start = System.currentTimeMillis()
        for (i in 0..<runCount)
            s+= benchmark64()
        val end = System.currentTimeMillis()
        val secs = (end-start).toDouble()/1000.0
        println("s:$s after $secs")
    }

    @Test
    fun testBenchmark192() {
        initialize()
        benchmark64()
        benchmark64()
        var s = 0L
        val start = System.currentTimeMillis()
        for (i in 0..<runCount)
            s+= benchmark192()
        val end = System.currentTimeMillis()
        val secs = (end-start).toDouble()/1000.0
        println("s:$s after $secs")
    }

    @Test
    fun generateExactMaps() {
        var map64 = 0L
        var map128 = 0L
        var map192 = 0L
        var map256 = 0L
        for (bitLen in 1..256) {
            val biLo = ONE.shiftLeft(bitLen - 1)
            val biHi = ONE.shiftLeft(bitLen).subtract(ONE)
            val loDigitCount = biLo.toString().length
            val hiDigitCount = biHi.toString().length
            if (loDigitCount == hiDigitCount) {
                when {
                    (bitLen <= 64) -> map64 = map64 or (1L shl (bitLen - 1))
                    (bitLen <= 128) -> map128 = map128 or (1L shl (bitLen - 1))
                    (bitLen <= 192) -> map192 = map192 or (1L shl (bitLen - 1))
                    (bitLen <= 256) -> map256 = map256 or (1L shl (bitLen - 1))
                }
            }
        }
        println(String.format("map64:0x%016X map128:0x%016X map192:0x%016X map256:0x%016X",
            map64, map128, map192, map256))

    }

}