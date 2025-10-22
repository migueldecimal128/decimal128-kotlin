package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger
import java.util.*
import kotlin.math.min
import kotlin.math.max

class TestU256ScaleDownPerf {

    val verbose = true

    val testSize = 1000000
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
        println("elapsedMillis:$elapsedMillis")
    }

    fun buildRandom() {
        for (i in mdValues.indices) {
            val md = mdValues[i]
            val bi = randBi()
            md.u256Set(bi)
            val excess = max(min(md.digitLen - 34, 44), 10)
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
//            val residue = U256ScalePow10.u256ScaleDownPow10(mdT, md, pow10)
            val residue = DivRangeRecipMulPow10.rangeDivPow10(mdT, md, pow10)
        }
    }

    fun randBi() : BigInteger {
        while (true) {
            val bitLength = random.nextInt(66, 230)
            val bi1 = BigInteger(bitLength, random)
            val bi = bi1 or (BigInteger.ONE shl bitLength - 1)
            if (bi.toString().length < Q_MAXX)
                return bi
        }
    }

}