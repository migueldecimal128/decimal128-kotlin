package com.decimal128.decimal

import org.junit.jupiter.api.Test
import java.lang.Long.compareUnsigned

class TestHotSpot {

    val random = java.util.Random()
    @Test
    fun testSumU64() {
        for (i in 0..<100000) {
            val r0 = random.nextLong()
            val r1 = random.nextLong()
            val r2 = random.nextLong()
            val r3 = random.nextLong()
            when (r0 and 0x03) {
                0L -> test0(r0, r1, r2, r3)
                1L -> test1(r0)
                2L -> test2(r0)
                else -> test3(r0)
            }
        }
    }

    fun test0(r: Long, s: Long, t: Long, u: Long): Long {
        var total = 0L
        for (i in 0..<10) {
            val (carry, sum) = umul128x128to128(r, s, t, u)
            total += carry + sum
        }
        return total
    }

    fun test1(r: Long): Long {
        var total = 0L
        for (i in 0..<10) {
            val (carry, sum) = sumU64(test0(r, i.toLong(), r, total), r, r, r)
            total += carry + sum
        }
        return total
    }

    fun test2(r: Long): Long {
        var total = 0L
        for (i in 0..<10) {
            val (carry, sum) = sumU64(i.toLong(), test1(r), r, r)
            total += carry + sum
        }
        return total
    }

    fun test3(r: Long): Long {
        var total = 0L
        for (i in 0..<10) {
            val (carry, sum) = sumU64(i.toLong(), test1(r), test2(r), r)
            total += carry + sum
        }
        return total
    }
}