package com.decimal128

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
        var q2      = twoNm1.add(BigInteger.ONE).divide(biD)
        var r2      = twoNm1.add(BigInteger.ONE).remainder(biD)
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
    val ADD_FLAG_AND_SHIFT_POW10_64 = ByteArray(20)

    fun initialize() {
        MAGIC_POW10_64[0] = 1
        ADD_FLAG_AND_SHIFT_POW10_64[0] = Byte.MIN_VALUE
        for (k in 1..19) {
            val d     = POW10[k]
            val magic = magicu64(d)
            MAGIC_POW10_64[k]   = magic.m
            ADD_FLAG_AND_SHIFT_POW10_64[k] =
                (if (magic.add) 0x80 or magic.s else magic.s).toByte()
        }
    }

    data class TC(val l: Long, val pow10: Int)

    val cases = arrayOf(
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
        for (i in 0..<100000000) {
            val l = (random.nextLong() and Long.MIN_VALUE) ushr random.nextInt(0, 62)
            val p = random.nextInt(20)
            val tc = TC(l, p)
            test1(tc)
        }
    }

    fun test1(tc: TC) {
        test1(tc.l, tc.pow10)
    }

    fun test1(l: Long, pow10: Int) {
        if (l < 0)
            return
        val divisor = POW10[pow10]

        val expected = java.lang.Long.divideUnsigned(l, divisor)

        if (verbose)
            println("$l / 10**$pow10 => $expected")

        val m = MAGIC_POW10_64[pow10]
        val flagAndShift = ADD_FLAG_AND_SHIFT_POW10_64[pow10]
        val s = flagAndShift.toInt() and 0x3F
        val add = (flagAndShift.toLong() shr 63) and l
        val beforeShift = Math.unsignedMultiplyHigh(l, m) + add
        val observed = beforeShift ushr s

        assertEquals(expected, observed)
    }

}