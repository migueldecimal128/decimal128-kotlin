package com.decimal128

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger
import java.util.*

class TestMagicU32 {

    val verbose = false

    val POW10_32 = LongArray(20).apply {
        this[0] = 1
        for (k in 1..19) this[k] = this[k-1] * 10
    }

    // precompute for k=1..9
    val RECIP64 = LongArray(10) { k ->
        if (k == 0) 0L
        else java.lang.Long.divideUnsigned(-1L, POW10_32[k]) + 1L
    }

    // then for any u<2^32 and 1≤k≤9:
    fun div32_by64recip(u: Long, k: Int): Pair<Long,Long> {
        require(u in 0L..0xFFFF_FFFFL)
        if (k == 0) return u to 0L
        val d   = POW10_32[k]
        val M64 = RECIP64[k]
        val q   = Math.unsignedMultiplyHigh(u, M64)
        val r   = u - q * d
        return q to r
    }

    data class TC(val l: Long, val pow10: Int)

    val cases = arrayOf(
        TC(7179907454500972459, 1),
        TC(123, 0),
        TC(1004957002449959557, 15),
        TC(123, 1),
        TC(8833659103727869972, 4),
    )

    @Test
    fun testCases() {
        for (case in cases)
            test1(case)
    }

    val random = Random()

    @Test
    fun testRandom() {
        for (i in 0..<1000000) {
            val l = (random.nextInt() ushr random.nextInt(0, 30)).toLong() and 0xFFFFFFFFL
            val p = random.nextInt(10)
            val tc = TC(l, p)
            test1(tc)
        }
    }

    fun test1(tc: TC) {
        val l32 = tc.l
        val bitLen = 64 - java.lang.Long.numberOfLeadingZeros(l32)
        if (bitLen > 32) {
            println("more than 32 bits long:$l32 (bitLen:$bitLen)")
            return
        }
        val k = tc.pow10
        if (k !in 0..9) {
            println("pow10:$k out of range")
            return
        }
        test1(l32, tc.pow10)
    }

    fun test1(u: Long, k: Int) {
        val divisor = POW10_32[k]

        val expected = java.lang.Long.divideUnsigned(u, divisor)

        if (verbose)
            println("$u / 10**$k => $expected")

        val (q, r) = div32_by64recip(u, k)

        val observed = q
        if (observed != expected) {
            error("""
                |––– Division by 10^$k failed –––
                |  u          = $u
                |  expected   = $expected
                |  observed   = $observed
                |  
                |  POW10[k]   = ${POW10_32[k]}
                """.trimMargin())
        }
    }

}