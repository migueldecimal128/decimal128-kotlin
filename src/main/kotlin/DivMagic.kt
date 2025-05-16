@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128

import com.decimal128.Residue.Companion.EXACT
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul4
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul3
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul2
import com.decimal128.CoeffRecipMulPow5.coeffRecipMul1
import java.lang.Long.compareUnsigned
import java.lang.Math.unsignedMultiplyHigh
import java.math.BigInteger
import java.math.BigInteger.ZERO
import java.math.BigInteger.ONE
import java.math.BigInteger.TWO
import java.math.BigInteger.TEN
import kotlin.math.ceil

object DivMagic {

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
        val hi32 = d ushr 32                             // logical shift, bits 63–32
        val lo32  = d and 0xFFFFFFFFL                     // bits 31–0
        val biD    = BigInteger.valueOf(hi32)
            .shiftLeft(32)
            .or(BigInteger.valueOf(lo32))
        // anc = 2^N - 1 - ((2^N - 1) mod d)
        val biN1   = twoToN - BigInteger.ONE
        val anc    = biN1 - biN1.mod(biD)

        val twoNm1 = BigInteger.ONE.shiftLeft(N - 1)       // 2^(N-1)
        var p       = (N - 1).toLong()
        var q1      = twoNm1.divide(anc)
        var r1      = twoNm1.remainder(anc)
        var q2      = twoNm1.divide(biD)
        var r2      = twoNm1.remainder(biD)
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

    val MAX_POW10_64 = 20
    val MAGIC_POW10_64 = LongArray(20)
    val FLAG_SHIFT_POW10_64 = ByteArray(20)

    fun initializeMagicPow10_64() {
        MAGIC_POW10_64[0] = 1
        FLAG_SHIFT_POW10_64[0] = Byte.MIN_VALUE
        for (k in 1..<MAX_POW10_64) {
            val d     = POW10[k]
            val magic = magicu64(d)
            MAGIC_POW10_64[k]   = magic.m
            FLAG_SHIFT_POW10_64[k] =
                (if (magic.add) 0x80 or magic.s else magic.s).toByte()
        }
    }

    fun magicDivPow10(z: Coeff, x: Coeff, pow10: Int): Residue {
        initializeMagicPow10_64()
        val m = MAGIC_POW10_64[pow10]
        val flagShift = FLAG_SHIFT_POW10_64[pow10].toInt()
        val residue = _magicDivide1x1(z, x.dw0, m, flagShift)
        return residue
    }

    private inline fun _magicDivide1x1(
        q: Coeff,
        x0: Long,
        m: Long,
        flagShift: Int,
    ): Residue {
        val s = flagShift and 0x3F
        val qLostCarry = 1L shl -s
        val addMask = (flagShift shr 31).toLong()
        val pp00Hi = unsignedMultiplyHigh(x0, m)
        val pp00Lo = x0 * m
        val p0 = pp00Lo
        val p1 = pp00Hi
        val qLo = p1 + (x0 and addMask)
        val qCarryAdd = if (compareUnsigned(qLo, p1) < 0) qLostCarry else 0L
        val q0 = qCarryAdd + (qLo ushr s)
        q.coeffSet64(q0)

        val roundBit = (p1 shr (s - 1)).toInt() and 1
        val cmpLo = compareUnsigned(p0, m)
        val hiFracMask = (1L shl (s - 1)) - 1L
        val stickyBit = if ((cmpLo >= 0) or ((p1 and hiFracMask) != 0L)) 1 else 0
        val residue = Residue.residueFrom(roundBit, stickyBit)
        return residue
    }

}
