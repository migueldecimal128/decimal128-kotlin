package com.decimal128.decimal

import kotlin.test.Test

class TestU64ToUtf8 {

    @Test
    fun test0() {
        for (digitCount in 1..19) {
            val utf8 = ByteArray(21) { 'x'.code.toByte() }
            val ret = Int256ParsePrint.u64ToUtf8(digitCount, 543210987654321L, utf8, 1)
            val str = String(utf8)
            val str2 = String(utf8, 1, ret-1)
            println("digitCount:$digitCount str:$str str2:$str2")
        }
    }
}