package com.decimal128.decimal

import com.decimal128.decimal.Residue.Companion.EXACT
import com.decimal128.decimal.Residue.Companion.GT_HALF
import com.decimal128.decimal.Residue.Companion.HALF
import com.decimal128.decimal.Residue.Companion.LT_HALF
import org.junit.jupiter.api.Test
import java.lang.Math.unsignedMultiplyHigh

class TestReciprocalDivisionMultipliers {

    private val POW10 = LongArray(20)
    init {
        POW10[0] = 1L
        for (i in 1..<POW10.size)
            POW10[i] = 10 * POW10[i - 1]
    }
    private val MAX_POW10_32 = 10
    private val RECIP64_32 = LongArray(MAX_POW10_32) { k ->
        if (k == 0) 0L
        else java.lang.Long.divideUnsigned(-1L, POW10[k]) + 1L
    }

    private /*inline*/ fun _recipPow10_32(x0_0: Long, pow10: Int): Pair<Long, Long> {
        assert((x0_0 ushr 32) == 0L)
        assert(pow10 in 0..<MAX_POW10_32)
        // when pow10 == 0 then M64 will be
        val divisor = POW10[pow10]
        val half = divisor ushr 1
        val M64 = RECIP64_32[pow10]

        val p = Math.unsignedMultiplyHigh(x0_0, M64)
        val q0 = if (pow10 == 0) x0_0 else p
        val r   = x0_0 - (q0 * divisor)
        return q0 to r
    }

    private fun _recipPow10_64(x0: Long, pow10: Int): Pair<Long, Long> {
        assert(pow10 >= 0 && pow10 < RECIP64_32.size)
        // when pow10 == 0 then M64 will be
        val x0_1 = x0 ushr 32
        val x0_0 = x0 and 0xFFFF_FFFFL
        val divisor = POW10[pow10]
        val half = divisor ushr 1
        val M64 = RECIP64_32[pow10]

        val p0_1 = Math.unsignedMultiplyHigh(x0_1, M64)
        val q0_1 = if (pow10 == 0) x0_1 else p0_1
        val r0_1 = x0_1 - (q0_1 * divisor)

        val t0_0 = (r0_1 shl 32) or x0_0
        var q0_0 = unsignedMultiplyHigh(t0_0, M64)
        val p0_0 = q0_0 * divisor
        var r0_0 = t0_0 - p0_0
        val overshoot0_0 = r0_0 shr 63 // 0 or -1
        q0_0 += overshoot0_0
        r0_0 += divisor and overshoot0_0

        val q0 = (q0_1 shl 32) or q0_0
        val r = r0_0
        return q0 to r
    }



    // this test fails
    //@Test
    fun test() {
        test1(10L, RECIP64_32[1], 1)
    }

    fun test1(d: Long, m:Long, pow10: Int) {
        val maxN = 1L shl 48
        var q = java.lang.Long.divideUnsigned(maxN, d)
        var r = -1 - (q * d)
        var n = maxN
        do {
            val (qHat, rHat) = _recipPow10_64(n, pow10)
            require(qHat == q)
            require(rHat == r)

            --r
            if (r < 0) {
                --q
                if ((q and (q - 1)) == 0L)
                    println("q:$q")
                r = d - 1
            }
            --n
        } while (n != 0L)
    }
}