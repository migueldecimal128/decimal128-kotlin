package com.decimal128.decimal

import kotlin.test.Test

class TestU64ToUtf8 {

    val verbose = false

    @Test
    fun test0() {
        var t = 0L
        for (digitCount in 1..19) {
            t = 10L * t + (digitCount % 10).toLong()
            val utf8 = ByteArray(21) { ascii_x }
            IntegerParsePrint.u64ToASCII(digitCount, t, utf8, 1)
            val str = utf8.decodeToString()
            val str2 = utf8.decodeToString(1, digitCount)
            if (verbose)
                println("digitCount:$digitCount str:$str str2:$str2")
        }
    }

    @Test
    fun testProblemChild() {
        val utf8 = ByteArray(21) { ascii_x }
        IntegerParsePrint.u64ToASCII(2, 45L, utf8, 1)
        val str = utf8.decodeToString()
        val str2 = utf8.decodeToString(1, 2)
        if (verbose)
            println("str:$str str2:$str2")
    }
}