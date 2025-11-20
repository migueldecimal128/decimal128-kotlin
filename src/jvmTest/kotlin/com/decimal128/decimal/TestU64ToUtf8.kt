package com.decimal128.decimal

import kotlin.test.Test

class TestU64ToUtf8 {

    @Test
    fun test0() {
        var t = 0uL
        for (digitCount in 1..19) {
            t = 10uL * t + (digitCount % 10).toULong()
            val utf8 = ByteArray(21) { 'x'.code.toByte() }
            val ret = IntegerParsePrint.u64ToUtf8(digitCount, t, utf8, 1)
            val str = String(utf8)
            val str2 = String(utf8, 1, ret-1)
            println("digitCount:$digitCount str:$str str2:$str2")
        }
    }

    @Test
    fun testProblemChild() {
        val utf8 = ByteArray(21) { 'x'.code.toByte() }
        val ret = IntegerParsePrint.u64ToUtf8(2, 45uL, utf8, 1)
        val str = String(utf8)
        val str2 = String(utf8, 1, ret-1)
        println("str:$str str2:$str2")
    }
}