package com.decimal128.decimal

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.TimeSource

class TestBExpMinMax {

    val verbose = false

    private fun trueBExpMin(bitLen: Int, qExp: Int): Int {
        if (bitLen == 0) return 0
        val x = (bitLen - 1).toDouble() + qExp.toDouble() * LOG2_10_DOUBLE
        return floor(x).toInt()
    }

    private fun trueBExpMax(bitLen: Int, qExp: Int): Int {
        if (bitLen == 0) return 0
        val x = bitLen.toDouble() + qExp.toDouble() * LOG2_10_DOUBLE
        return ceil(x).toInt() - 1
    }

    private val LOG2_10_DOUBLE = 3.32192809488736234787

    @Test
    fun testExhaustive() {
        var deltaZeroCount = 0
        var deltaOneCount = 0
        for (bitLen in 0..255) {
            for (qExp in -7000..7000) {
                val min1 = calcBExpMin(bitLen, qExp)
                val min2 = trueBExpMin(bitLen, qExp)
                val max1 = calcBExpMax(bitLen, qExp)
                val max2 = trueBExpMax(bitLen, qExp)

                assertEquals(min2, min1, "bExpMin mismatch at bitLen=$bitLen qExp=$qExp")
                assertEquals(max2, max1, "bExpMax mismatch at bitLen=$bitLen qExp=$qExp")

                val delta = max1 - min1
                assertTrue(delta in 0..1)
                if (delta == 0)
                    ++deltaZeroCount
                else
                    ++deltaOneCount
            }
        }
        if (verbose)
            println("deltaZeroCount:$deltaZeroCount deltaOneCount:$deltaOneCount")
    }


    @Test
    fun throughput() {
        var checksum = 0
        val mark = TimeSource.Monotonic.markNow()
        for (bitLen in 0..255) {
            for (qExp in -7000..7000) {
                checksum += calcBExpMin(bitLen, qExp)
                checksum += calcBExpMax(bitLen, qExp)
            }
        }
        val elapsed = mark.elapsedNow()
        println("elapsed: $elapsed checksum: $checksum")
        assertTrue(checksum != Int.MIN_VALUE)
    }
}