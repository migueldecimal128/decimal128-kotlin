package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.*
import kotlin.math.min
import kotlin.math.max

class TestC256ScaleDownPerf {

    val verbose = false

    val testSize = 100_000
    val iterCount = 20
    val mdValues = Array<MutDec>(testSize) { MutDec() }
    val scaleValues = IntArray(testSize)

    val random = Random()

    @Test
    fun testRandom() {
        buildRandom()
        runThru() // warmup
        val startTime = System.currentTimeMillis()
        for (i in 0..<iterCount)
            runThru()
        val stopTime = System.currentTimeMillis()
        val elapsedMillis = stopTime - startTime
        if (verbose)
            println("elapsedMillis:$elapsedMillis")
    }

    fun buildRandom() {
        for (i in mdValues.indices) {
            val md = mdValues[i]
            val bi = randBi()
            md.u256Set(bi)
            val excess = max(min(md.digitLen - 34, 44), BARRETT_POW10_MAXX)
            if (verbose)
                println("excess:$excess")
            if (excess > 0)
                scaleValues[i] = excess
            else
                scaleValues[i] = 1
        }
    }


    fun runThru() {
        val mdT = MutDec()
        for (i in mdValues.indices) {
            val md = mdValues[i]
            val pow10 = scaleValues[i]
            val residue = divRangeRecipMulPow10(mdT, md, pow10)
        }
    }

    fun randBi() : BigInteger {
        while (true) {
            val bitLength = random.nextInt(66, 230)
            val bi1 = BigInteger(bitLength, random)
            val bi = bi1 or (BigInteger.ONE shl bitLength - 1)
            if (bi.toString().length < RRMP10_Q_MAXX)
                return bi
        }
    }

}