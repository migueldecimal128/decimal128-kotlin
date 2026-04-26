package com.decimal128.decimal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigInteger
import java.util.*

class TestMagicU64 {

    val verbose = false

    data class Magic(val m: Long, val add: Boolean, val s: Int)

    /**
     * Compute magic number and shift for unsigned division by d (any 1 ≤ d < 2^64).
     * Returns:
     *   m   = low 64 bits of the “true” multiplier,
     *   add = true if the true multiplier had bit-64 set (i.e. needed 65 bits),
     *   s   = the right-shift amount beyond 64.
     */
    fun magicu64(d: Long): Magic {
        require(d != 0L) { "divisor must be nonzero" }
        val N      = 64
        val twoToN = BigInteger.ONE.shiftLeft(N)           // 2^64
        // Reconstruct the unsigned 64-bit divisor in a BigInteger:
        val high32 = d ushr 32                             // logical shift, bits 63–32
        val low32  = d and 0xFFFFFFFFL                     // bits 31–0
        val biD    = BigInteger.valueOf(high32)
            .shiftLeft(32)
            .or(BigInteger.valueOf(low32))
        // anc = 2^N - 1 - ((2^N - 1) mod d)
        val biN1   = twoToN - BigInteger.ONE
        val anc    = biN1 - biN1.mod(biD)

        val twoNm1 = BigInteger.ONE.shiftLeft(N - 1)       // 2^(N-1)
        var p       = (N - 1).toLong()
        var q1      = twoNm1.divide(anc)
        var r1      = twoNm1.remainder(anc)
        var q2 = twoNm1.divide(biD)
        var r2 = twoNm1.remainder(biD)
        //var q2      = twoNm1.add(BigInteger.ONE).divide(biD)
        //var r2      = twoNm1.add(BigInteger.ONE).remainder(biD)
        var delta: BigInteger

        do {
            p += 1
            q1 = q1.shiftLeft(1); r1 = r1.shiftLeft(1)
            if (r1 >= anc) { q1 += BigInteger.ONE; r1 -= anc }
            q2 = q2.shiftLeft(1); r2 = r2.shiftLeft(1)
            if (r2 >= biD)  { q2 += BigInteger.ONE; r2 -= biD }
            delta = biD - r2
        } while (q1 < delta || (q1 == delta && r1 == BigInteger.ZERO))

        val Mtrue   = q2 + BigInteger.ONE
        val addFlag = Mtrue.testBit(N)                    // was bit-64 set?
        val m_mod   = Mtrue.clearBit(N).toLong()           // low 64 bits as signed Long
        val s        = (p - N).toInt()

        return Magic(m_mod, addFlag, s)
    }

    val POW10 = LongArray(20).apply {
        this[0] = 1
        for (k in 1..19) this[k] = this[k-1] * 10
    }
    val MAGIC_POW10_64 = LongArray(20)
    val SHIFT_POW10_64 = IntArray(20)
    val ADD_FLAG_POW10_64 = BooleanArray(20)

    fun initialize() {
        MAGIC_POW10_64[0] = 1
        SHIFT_POW10_64[0] = 0
        ADD_FLAG_POW10_64[0] = true
        for (k in 1..19) {
            val d     = POW10[k]
            val magic = magicu64(d)
            MAGIC_POW10_64[k]   = magic.m
            SHIFT_POW10_64[k] = magic.s
            ADD_FLAG_POW10_64[k] = magic.add
        }
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
        initialize()
        for (case in cases)
            test1(case)
    }

    val random = Random()

    @Test
    fun testRandom() {
        initialize()
        for (i in 0..<1000000) {
            val l = random.nextLong() ushr random.nextInt(0, 62)
            val p = random.nextInt(20)
            val tc = TC(l, p)
            test1(tc)
        }
    }

    fun test1(tc: TC) {
        test1(tc.l, tc.pow10)
    }

    fun test1(u: Long, k: Int) {
        val divisor = POW10[k]

        val expected = java.lang.Long.divideUnsigned(u, divisor)

        if (verbose)
            println("$u / 10**$k => $expected")

        val m = MAGIC_POW10_64[k]
        val s = SHIFT_POW10_64[k]
        val addFlag = ADD_FLAG_POW10_64[k]
        val toAdd = if (addFlag) u else 0L
        val hi = Math.unsignedMultiplyHigh(u, m)
        val sumLo = hi + toAdd
        val carryAdd = if (java.lang.Long.compareUnsigned(sumLo, hi) < 0) (1L shl -s) else 0L
        val observed = carryAdd or (sumLo ushr s)

        if (observed != expected) {
            error("""
      |––– Division by 10^$k failed –––
      |  u          = $u
      |  expected   = $expected
      |  observed   = $observed
      |  
      |  m          = $m
      |  addFlag    = $addFlag
      |  shift      = $s
      |  
      |  hi         = $hi
      |  toAdd      = $toAdd
      |  sumLo      = $sumLo
      |  carryAdd   = $carryAdd
      |  
      |  POW10[k]   = ${POW10[k]}
      """.trimMargin())
        }

        assertEquals(expected, observed)
    }

}